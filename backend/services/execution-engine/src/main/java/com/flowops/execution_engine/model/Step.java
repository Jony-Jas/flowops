package com.flowops.execution_engine.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * Minimal Step DTO used by the DAG builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true) // enables fluent setters
public class Step {
    private String stepId;
    private String pluginId;
    private Map<String, String> inputs;
    private Map<String, String> outputs;
    private String condition;
    private List<Step> branches;
    private List<Step> children;
    private Map<String, Object> config;

    public Step(String stepId) {
        this.stepId = stepId;
    }

    // --- Builders for convenience in tests/usage ---
    public static Step of(String stepId) {
        return new Step(stepId);
    }

    // Keep custom fluent helpers
    public Step withPluginId(String pluginId) { this.pluginId = pluginId; return this; }
    public Step withInputs(Map<String,String> inputs) { this.inputs = inputs; return this; }
    public Step withOutputs(Map<String,String> outputs) { this.outputs = outputs; return this; }
    public Step withCondition(String condition) { this.condition = condition; return this; }
    public Step withBranches(List<Step> branches) { this.branches = branches; return this; }
    public Step withChildren(List<Step> children) { this.children = children; return this; }
    public Step withConfig(Map<String,Object> config) { this.config = config; return this; }

    public Step addChild(Step s) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(s);
        return this;
    }

    public Step addBranch(Step s) {
        if (this.branches == null) this.branches = new ArrayList<>();
        this.branches.add(s);
        return this;
    }

    @Override
    public String toString() {
        return "Step{" + "stepId='" + stepId + '\'' + '}';
    }
}
