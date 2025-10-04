package com.flowops.execution_engine.engine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.flowops.execution_engine.model.Step;

/**
 * Builds a DAG (adjacency + indegree) from a list of steps.
 *
 * Rules:
 *  - Flatten children/branches recursively (duplicate stepIds are rejected)
 *  - Parse input references of the form "stepId.outputKey" or "${stepId.outputKey}"
 *    and treat them as dependencies: currentStep depends on stepId
 *
 * The adjacency map is dependency -> set(dependents).
 * The indegree map is dependent -> number of unresolved dependencies.
 */
public class DAGBuilder {

    // regex captures stepId.outputKey optionally wrapped in ${...}
    // We'll look for tokens like: step1.out1 or ${step1.out1}
    private static final Pattern REF_PATTERN = Pattern.compile("(?:\\$\\{)?([A-Za-z0-9_\\-]+)\\.([A-Za-z0-9_\\-]+)(?:})?");

    /**
     * Flatten nested steps (children + branches) to a map stepId -> Step.
     * Maintains insertion order of traversal.
     *
     * Throws IllegalArgumentException if duplicate stepId or missing stepId encountered.
     */
    public Map<String, Step> flattenSteps(List<Step> steps) {
        if (steps == null) return Collections.emptyMap();
        Map<String, Step> out = new LinkedHashMap<>();
        Deque<Step> stack = new ArrayDeque<>(steps);

        while (!stack.isEmpty()) {
            Step s = stack.pop();
            if (s.getStepId() == null || s.getStepId().trim().isEmpty()) {
                throw new IllegalArgumentException("Step found without stepId");
            }
            String id = s.getStepId();
            if (out.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate stepId found: " + id);
            }
            out.put(id, s);
            // push children and branches for traversal
            if (s.getChildren() != null) {
                // push in reverse to keep declaration order (optional)
                List<Step> children = new ArrayList<>(s.getChildren());
                Collections.reverse(children);
                for (Step c : children) stack.push(c);
            }
            if (s.getBranches() != null) {
                List<Step> branches = new ArrayList<>(s.getBranches());
                Collections.reverse(branches);
                for (Step b : branches) stack.push(b);
            }
        }
        return out;
    }

    /**
     * Build DAG adjacency and indegree maps from flattened steps.
     *
     * @param stepsMap flattened map stepId -> Step
     * @return DAG object containing adjacency and indegree
     *
     * Throws IllegalArgumentException if a referenced stepId is missing (undefined dependency).
     */
    public DAG buildDAG(Map<String, Step> stepsMap) {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        Map<String, Integer> indegree = new LinkedHashMap<>();

        // initialize
        for (String id : stepsMap.keySet()) {
            adjacency.put(id, new LinkedHashSet<>());
            indegree.put(id, 0);
        }

        for (Map.Entry<String, Step> entry : stepsMap.entrySet()) {
            String currentStepId = entry.getKey();
            Step s = entry.getValue();

            Map<String, String> inputs = s.getInputs();
            if (inputs == null) continue;

            for (String raw : inputs.values()) {
                if (raw == null) continue;
                // find all references inside the raw input string
                Matcher m = REF_PATTERN.matcher(raw);
                while (m.find()) {
                    String depStepId = m.group(1);
                    // validate existence
                    if (!stepsMap.containsKey(depStepId)) {
                        throw new IllegalArgumentException(String.format("Step '%s' references unknown dependency '%s' in input '%s'",
                                currentStepId, depStepId, raw));
                    }
                    // add edge depStepId -> currentStepId
                    Set<String> dependents = adjacency.get(depStepId);
                    // guard (should exist)
                    if (dependents == null) {
                        dependents = new LinkedHashSet<>();
                        adjacency.put(depStepId, dependents);
                    }
                    // avoid double counting duplicate references from the same dependent
                    if (!dependents.contains(currentStepId)) {
                        dependents.add(currentStepId);
                        indegree.put(currentStepId, indegree.getOrDefault(currentStepId, 0) + 1);
                    }
                }
            }
        }
        return new DAG(adjacency, indegree);
    }

    /**
     * Kahn's algorithm: produce a topological ordering of the DAG.
     * If a cycle exists, throws IllegalStateException.
     *
     * The returned list contains all stepIds in topological order.
     */
    public List<String> topologicalSort(DAG dag) {
        // copy indegree
        Map<String, Integer> indeg = new LinkedHashMap<>(dag.getIndegree());
        Deque<String> q = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indeg.entrySet()) {
            if (e.getValue() == 0) q.add(e.getKey());
        }

        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            String cur = q.removeFirst();
            order.add(cur);
            Set<String> deps = dag.getAdjacency().getOrDefault(cur, Collections.emptySet());
            for (String d : deps) {
                indeg.put(d, indeg.get(d) - 1);
                if (indeg.get(d) == 0) q.addLast(d);
            }
        }
        if (order.size() != dag.getAdjacency().size()) {
            throw new IllegalStateException("Cycle detected in DAG during topological sort");
        }
        return order;
    }

    // Simple container for adjacency + indegree
    public static class DAG {
        private final Map<String, Set<String>> adjacency;
        private final Map<String, Integer> indegree;

        public DAG(Map<String, Set<String>> adjacency, Map<String, Integer> indegree) {
            this.adjacency = Collections.unmodifiableMap(adjacency);
            this.indegree = Collections.unmodifiableMap(indegree);
        }

        public Map<String, Set<String>> getAdjacency() { return adjacency; }
        public Map<String, Integer> getIndegree() { return indegree; }
    }
}

