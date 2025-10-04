package com.flowops.execution_engine.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class JarCache {
    private static final Logger log = LoggerFactory.getLogger(JarCache.class);
    private final Path cacheDir;
    private final ConcurrentMap<String, Path> cacheIndex = new ConcurrentHashMap<>();

    public JarCache() {
        this.cacheDir = Paths.get(System.getProperty("user.home"), ".flowops", "plugin-cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create plugin cache dir", e);
        }
    }

    /**
     * Returns file path of cached jar for pluginId+id if exists; otherwise writes bytes and returns path.
     */
    public Path getOrWriteJar(String pluginId, String id, byte[] jarBytes) throws IOException {
        String key = pluginId + ":" + (id == null ? "latest" : id);
        Path existing = cacheIndex.get(key);
        if (existing != null && Files.exists(existing)) return existing;

        // file name safe
        String fileName = pluginId.replaceAll("[^a-zA-Z0-9\\-_.]", "_") + "-" + (id == null ? "latest" : id) + ".jar";
        Path target = cacheDir.resolve(fileName);
        // write atomically
        Path tmp = Files.createTempFile(cacheDir, "plugin-", ".jar.tmp");
        Files.write(tmp, jarBytes, StandardOpenOption.WRITE);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        cacheIndex.put(key, target);
        log.info("Cached plugin jar {} -> {}", key, target);
        return target;
    }

    /**
     * Optional: remove cached jar (e.g., on version update)
     */
    public void remove(String pluginId, String id) {
        String key = pluginId + ":" + (id == null ? "latest" : id);
        Path p = cacheIndex.remove(key);
        if (p != null) {
            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
        }
    }
}
