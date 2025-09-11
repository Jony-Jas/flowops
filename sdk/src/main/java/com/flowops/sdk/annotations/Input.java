package com.flowops.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark a field as an input parameter for a task or component.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Input {
    /**
     * The name of the input parameter.
     * @return name of the input parameter
     */
    String name();

    /**
     * A brief description of the input parameter.
     * @return description of the input parameter
     */
    String description() default "";
}
