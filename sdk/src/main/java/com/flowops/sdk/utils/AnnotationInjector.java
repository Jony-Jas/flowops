package com.flowops.sdk.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.flowops.sdk.annotations.Input;
import com.flowops.sdk.annotations.Output;

/**
 * Utility class to inject inputs into an instance using annotations.
 */
public class AnnotationInjector {

    /**
     * Default constructor.
     */
    public AnnotationInjector() {
    }

    /**
     * Injects input values into the fields of the given instance that are annotated with @Input.
     * @param instance The object instance to inject inputs into.
     * @param inputs A map of input names to their corresponding values.
     */
    public static void injectInputs(Object instance, Map<String, Object> inputs) {
        for(Field field: instance.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(Input.class)) {
                Input inputAnnotation = field.getAnnotation(Input.class);
                String inputName = inputAnnotation.name();
                if(inputs.containsKey(inputName)) {
                    field.setAccessible(true);
                    try {
                        field.set(instance, inputs.get(inputName));
                    } catch (IllegalAccessException e) {
                        System.out.println("Failed to inject input: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Retrieves output values from the fields of the given instance that are annotated with @Output.
     * @param instance The object instance to retrieve outputs from.
     * @return A map of output names to their corresponding values.
     */
    public static Map<String, Object> getOutputs(Object instance) {
        Map<String, Object> outputs = new HashMap<>();
        
        for(Field field: instance.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(Output.class)) {
                Output outputAnnotation = field.getAnnotation(Output.class);
                String outputName = outputAnnotation.name();
                field.setAccessible(true);
                try {
                    Object value = field.get(instance);
                    outputs.put(outputName, value);
                } catch (IllegalAccessException e) {
                    System.out.println("Failed to retrieve output: " + e.getMessage());
                }
            }
        }
        return outputs;
    }
}
