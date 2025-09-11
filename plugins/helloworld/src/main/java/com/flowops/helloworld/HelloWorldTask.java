package com.flowops.helloworld;

import com.flowops.sdk.core.*;
import com.flowops.sdk.annotations.*;
import com.flowops.sdk.utils.*;
import java.util.Map;

public class HelloWorldTask extends BaseTask {
    @Input(name="repeatCount")
    public int repeatCount;

    @Input(name="delay")
    public int delayMillis;

    @Output(name="message")
    private String message = "";

    @Override
    public void start() {
        for (int i = 0; i < repeatCount; i++) {
            message = "Hello World " + (i + 1);
            logger.info(message);
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                logger.error("Error occurred while sleeping", e);
            }
        }
    }
}

// Example of how to run the task standalone
// public class Main {
//     public static void main(String[] args) {
//         HellWorldTask task = new HellWorldTask();
//         AnnotationInjector.injectInputs(task, Map.of("repeatCount", 5, "delay", 1000));
//         task.start();
//         System.out.println("Final message: " + AnnotationInjector.getOutputs(task).get("message"));
//     }    
// }
