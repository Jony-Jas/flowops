package com.flowops.execution_engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.execution_engine.grpc.PluginServiceClient;
import com.flowops.execution_engine.model.Step;
import com.flowops.common.grpc.PluginServiceGrpc;
import com.flowops.execution_engine.persistence.redis.RedisExecutionRepository;
import com.flowops.common.grpc.PluginIO;
import com.flowops.common.grpc.PluginIOType;
import com.flowops.common.grpc.PluginMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * StepExecutor that provides a Callable factory for executing plugin tasks.
 *
 * The returned Callable runs the plugin in the worker thread (no internal thread spawn).
 * Future.cancel(true) will interrupt that thread (best-effort cancellation).
 */
@Component
public class StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(StepExecutor.class);

    private final PluginServiceClient pluginClient;
    private final JarCache jarCache;
    private final RedisExecutionRepository redisRepo;
    private final ObjectMapper om = new ObjectMapper();

    public StepExecutor(PluginServiceClient pluginClient, JarCache jarCache, RedisExecutionRepository redisRepo) {
        this.pluginClient = pluginClient;
        this.jarCache = jarCache;
        this.redisRepo = redisRepo;
    }

    /**
     * Create a Callable that, when invoked, executes the step and returns outputs map.
     *
     * This Callable:
     *  - obtains plugin metadata & jar (if metadata param is null it will fetch it)
     *  - caches jar via JarCache
     *  - loads Task class (subclass of com.flowops.sdk.core.BaseTask or implementing Task)
     *  - injects inputs (resolving references from Redis context)
     *  - invokes start() on the task instance (in current thread)
     *  - on completion/interrupt collects outputs and returns them
     */
    public Callable<Map<String, Object>> createExecutionCallable(Step step, String flowId, String runId, PluginMetadata providedMetadata) {
        return () -> {
            String pluginId = step.getPluginId();
            if (pluginId == null || pluginId.isBlank()) {
                throw new IllegalArgumentException("Step missing pluginId: " + step.getStepId());
            }

            // 1) metadata (use provided or fetch)
            PluginMetadata metadata = providedMetadata;
            if (metadata == null) {
                try {
                    metadata = pluginClient.getMetadata(pluginId, null);
                } catch (Exception ex) {
                    log.warn("Failed to fetch metadata for plugin {}: {}", pluginId, ex.getMessage());
                    metadata = null;
                }
            }

            // 2) jar bytes and caching
            byte[] jarBytes = null;
            Path jarPath;
            try {
                if (metadata != null && metadata.getId() != null && !metadata.getId().isEmpty()) {
                    jarBytes = pluginClient.getJarBytes(pluginId, metadata.getId());
                    jarPath = jarCache.getOrWriteJar(pluginId, metadata.getId(), jarBytes);
                } else {
                    // fallback: try to fetch without id
                    jarBytes = pluginClient.getJarBytes(pluginId, "");
                    jarPath = jarCache.getOrWriteJar(pluginId, "latest", jarBytes);
                }
            } catch (Exception ex) {
                log.error("Failed to fetch or cache plugin jar for {}: {}", pluginId, ex.getMessage(), ex);
                throw new RuntimeException("Cannot load plugin jar: " + ex.getMessage(), ex);
            }

            // 3) load class and instantiate task
            Object taskInstance;
            Class<?> taskClass;
            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, this.getClass().getClassLoader())) {
                taskClass = findTaskClassInJar(jarPath, loader);
                if (taskClass == null) {
                    throw new IllegalStateException("No Task implementation (BaseTask) found in plugin jar: " + pluginId);
                }
                taskInstance = taskClass.getDeclaredConstructor().newInstance();

                // 4) resolve inputs (reads redis context)
                Map<String, Object> resolvedInputs = resolveInputs(step, flowId, runId, metadata);

                // 5) inject inputs into task instance
                injectInputs(taskInstance, resolvedInputs);

                // 6) invoke start() - in this thread so interrupt works via Future.cancel(true)
                try {
                    log.debug("Invoking start() on plugin task {} for step {}", taskClass.getName(), step.getStepId());
                    taskClass.getMethod("start").invoke(taskInstance);
                } catch (Exception startEx) {
                    // If underlying start threw an InvocationTargetException with cause, unwrap for clarity
                    Throwable cause = startEx.getCause() != null ? startEx.getCause() : startEx;
                    log.error("Plugin start() failed for step {}: {}", step.getStepId(), cause.getMessage(), cause);
                    // If thread was interrupted, attempt best-effort stop()
                    if (Thread.currentThread().isInterrupted()) {
                        callStopIfPresent(taskInstance);
                        throw new RuntimeException("Plugin execution interrupted", cause);
                    }
                    // propagate
                    throw new RuntimeException("Plugin start() failed: " + cause.getMessage(), cause);
                }

                // 7) after normal completion collect outputs
                Map<String, Object> outputs = collectOutputs(taskInstance, metadata);
                return outputs;

            } catch (IOException ioe) {
                throw new RuntimeException("Failed to load plugin jar: " + ioe.getMessage(), ioe);
            } finally {
                // Note: if thread was interrupted while start() running, we cannot forcibly stop it here.
                // Best-effort: if interrupted flag set after return, try calling stop() on instance (not guaranteed)
                // but since 'taskInstance' scope ends here, we attempt nothing further.
            }
        };
    }

    // -------------------- Helper methods --------------------

    private Class<?> findTaskClassInJar(Path jarPath, URLClassLoader loader) throws IOException {
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    try {
                        Class<?> c = Class.forName(className, false, loader);
                        // check BaseTask presence
                        try {
                            Class<?> baseTaskClass = Class.forName("com.flowops.sdk.core.BaseTask", false, this.getClass().getClassLoader());
                            if (baseTaskClass.isAssignableFrom(c) && !c.isInterface()) {
                                return c;
                            }
                        } catch (ClassNotFoundException ignored) {
                            // fallback: check for Task interface
                            try {
                                Class<?> taskInterface = Class.forName("com.flowops.sdk.core.Task", false, this.getClass().getClassLoader());
                                if (taskInterface.isAssignableFrom(c) && !c.isInterface()) return c;
                            } catch (ClassNotFoundException ignored2) {
                            }
                        }
                    } catch (Throwable ex) {
                        // ignore classes that cannot be loaded by plugin loader (they may need other deps)
                        log.debug("Skipping class {} due to load error: {}", className, ex.getMessage());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve inputs (literal or reference). Uses plugin metadata to coerce types.
     * References like "stepId.outputKey" or "${stepId.outputKey}" are resolved from Redis context.
     */
    private Map<String, Object> resolveInputs(Step step, String flowId, String runId, PluginMetadata metadata) throws Exception {
        Map<String, Object> resolved = new HashMap<>();
        Map<String, String> rawInputs = step.getInputs() == null ? Collections.emptyMap() : step.getInputs();

        Map<String, String> ctx = redisRepo.getContext(flowId, runId);

        for (Map.Entry<String, String> e : rawInputs.entrySet()) {
            String inputName = e.getKey();
            String rawVal = e.getValue();
            Object finalVal;

            if (rawVal == null) {
                finalVal = null;
            } else if (looksLikeReference(rawVal)) {
                String refKey = extractRefKey(rawVal);
                if (ctx.containsKey(refKey)) {
                    String json = ctx.get(refKey);
                    PluginIO io = findPluginInput(metadata, inputName);
                    if (io == null) {
                        finalVal = om.readValue(json, Object.class);
                    } else {
                        finalVal = convertJsonToType(json, io.getType());
                    }
                } else {
                    throw new IllegalStateException("Missing context for reference " + refKey + " required by input " + inputName);
                }
            } else {
                PluginIO io = findPluginInput(metadata, inputName);
                if (io == null) {
                    finalVal = rawVal;
                } else {
                    finalVal = convertStringToType(rawVal, io.getType());
                }
            }
            resolved.put(inputName, finalVal);
        }
        return resolved;
    }

    private boolean looksLikeReference(String raw) {
        return raw.contains(".") || (raw.startsWith("${") && raw.contains("."));
    }

    private String extractRefKey(String raw) {
        if (raw.startsWith("${") && raw.endsWith("}")) {
            return raw.substring(2, raw.length() - 1);
        } else {
            return raw;
        }
    }

    private PluginIO findPluginInput(PluginMetadata metadata, String inputName) {
        if (metadata == null) return null;
        for (PluginIO io : metadata.getInputsList()) {
            if (io.getName().equals(inputName)) return io;
        }
        return null;
    }

    private Object convertJsonToType(String json, PluginIOType type) throws IOException {
        if (type == null) return om.readValue(json, Object.class);
        switch (type) {
            case INTEGER: return om.readValue(json, Integer.class);
            case FLOAT: return om.readValue(json, Double.class);
            case BOOLEAN: return om.readValue(json, Boolean.class);
            case STRING:
            default: return om.readValue(json, String.class);
        }
    }

    private Object convertStringToType(String raw, PluginIOType type) {
        if (type == null) return raw;
        switch (type) {
            case INTEGER: return Integer.parseInt(raw);
            case FLOAT: return Double.parseDouble(raw);
            case BOOLEAN: return Boolean.parseBoolean(raw);
            case STRING:
            default: return raw;
        }
    }

    /**
     * Inject inputs into task instance fields. Prefer fields annotated with @Input(name="...").
     */
    private void injectInputs(Object taskInstance, Map<String, Object> resolvedInputs) {
        Class<?> clazz = taskInstance.getClass();
        for (Map.Entry<String, Object> e : resolvedInputs.entrySet()) {
            String name = e.getKey();
            Object value = e.getValue();

            Field target = findAnnotatedField(clazz, "Input", name);
            if (target == null) {
                try {
                    target = clazz.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (target != null) {
                try {
                    target.setAccessible(true);
                    Object coerced = coerceValueToFieldType(value, target.getType());
                    target.set(taskInstance, coerced);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to inject input " + name + " into " + clazz.getName(), ex);
                }
            } else {
                log.debug("No field found for input {} on {}", name, clazz.getName());
            }
        }
    }

    private Field findAnnotatedField(Class<?> clazz, String annotationSimpleName, String name) {
        for (Field f : clazz.getDeclaredFields()) {
            for (Annotation a : f.getAnnotations()) {
                if (a.annotationType().getSimpleName().equals(annotationSimpleName)) {
                    try {
                        Object n = a.annotationType().getMethod("name").invoke(a);
                        if (name.equals(String.valueOf(n))) return f;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private Object coerceValueToFieldType(Object value, Class<?> fieldType) {
        if (value == null) return null;
        if (fieldType.isAssignableFrom(value.getClass())) return value;

        if (fieldType == int.class || fieldType == Integer.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(String.valueOf(value));
        }
        if (fieldType == long.class || fieldType == Long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(String.valueOf(value));
        }
        if (fieldType == double.class || fieldType == Double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return Double.parseDouble(String.valueOf(value));
        }
        if (fieldType == float.class || fieldType == Float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            return Float.parseFloat(String.valueOf(value));
        }
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            if (value instanceof Boolean) return value;
            return Boolean.parseBoolean(String.valueOf(value));
        }
        if (fieldType == String.class) {
            return String.valueOf(value);
        }

        // fallback: use ObjectMapper to convert
        return om.convertValue(value, fieldType);
    }

    /**
     * Collect outputs after task completes. Use plugin metadata outputs if available or @Output annotation.
     */
    private Map<String, Object> collectOutputs(Object taskInstance, PluginMetadata metadata) {
        Map<String, Object> outputs = new HashMap<>();
        Class<?> clazz = taskInstance.getClass();

        Map<String, PluginIOType> outputsDef = new LinkedHashMap<>();
        if (metadata != null) {
            for (PluginIO io : metadata.getOutputsList()) outputsDef.put(io.getName(), io.getType());
        }

        // prefer metadata-defined outputs
        for (Map.Entry<String, PluginIOType> entry : outputsDef.entrySet()) {
            String outName = entry.getKey();
            Field outField = findAnnotatedField(clazz, "Output", outName);
            if (outField == null) {
                try {
                    outField = clazz.getDeclaredField(outName);
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (outField != null) {
                try {
                    outField.setAccessible(true);
                    outputs.put(outName, outField.get(taskInstance));
                } catch (IllegalAccessException ex) {
                    log.warn("Failed to read output field {}: {}", outName, ex.getMessage());
                }
            } else {
                log.debug("No output field found for {} on {}", outName, clazz.getName());
            }
        }

        // fallback: collect annotated outputs if none found
        if (outputs.isEmpty()) {
            for (Field f : clazz.getDeclaredFields()) {
                if (hasAnnotationSimpleName(f, "Output")) {
                    try {
                        f.setAccessible(true);
                        outputs.put(f.getName(), f.get(taskInstance));
                    } catch (IllegalAccessException ignored) { }
                }
            }
        }
        return outputs;
    }

    private boolean hasAnnotationSimpleName(Field f, String ann) {
        for (Annotation a : f.getAnnotations()) {
            if (a.annotationType().getSimpleName().equals(ann)) return true;
        }
        return false;
    }

    /**
     * Best-effort: call stop() on the task instance if present.
     */
    private void callStopIfPresent(Object taskInstance) {
        try {
            var m = taskInstance.getClass().getMethod("stop");
            if (m != null) {
                try {
                    m.invoke(taskInstance);
                } catch (Throwable t) {
                    log.warn("Invoking stop() on plugin instance failed: {}", t.getMessage());
                }
            }
        } catch (NoSuchMethodException ignored) { }
    }
}
