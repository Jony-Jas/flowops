package com.flowops.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as an output parameter for a task or component.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Output {
    /**
     * The name of the output parameter.
     * @return name of the output parameter
     */
    String name();

    /**
     * A brief description of the output parameter.
     * @return description of the output parameter
     */
    String description() default "";
}