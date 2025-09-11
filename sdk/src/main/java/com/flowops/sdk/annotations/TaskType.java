package com.flowops.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a task type with metadata.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskType {
    /**
     * The name of the task type.
     * @return name of the task type
     */
    String name();

    /**
     * A description of the task type.
     * @return description of the task type
     */
    String description() default "";
}