package com.flowops.execution_engine.engine;

import java.util.*;

/**
 * DAG validator utilities.
 * Uses Kahn's algorithm to detect cycles; additionally verifies indegree/adjaency sizes.
 */
public class DAGValidator {

    /**
     * Validates the DAG has no cycles. Throws IllegalStateException when cycles present.
     */
    public void validateNoCycles(DAGBuilder.DAG dag) {
        Map<String, Integer> indegree = new LinkedHashMap<>(dag.getIndegree());
        Deque<String> q = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) q.add(e.getKey());
        }

        int visited = 0;
        while (!q.isEmpty()) {
            String cur = q.removeFirst();
            visited++;
            for (String dep : dag.getAdjacency().getOrDefault(cur, Collections.emptySet())) {
                indegree.put(dep, indegree.get(dep) - 1);
                if (indegree.get(dep) == 0) q.addLast(dep);
            }
        }

        if (visited != dag.getAdjacency().size()) {
            throw new IllegalStateException("Cycle detected in DAG");
        }
    }

    /**
     * Validates basic structural invariants: adjacency keys match indegree keys.
     * Throws IllegalArgumentException if mismatch detected.
     */
    public void validateStructure(DAGBuilder.DAG dag) {
        Set<String> nodesFromAdj = dag.getAdjacency().keySet();
        Set<String> nodesFromIndeg = dag.getIndegree().keySet();
        if (!nodesFromAdj.equals(nodesFromIndeg)) {
            throw new IllegalArgumentException("DAG structure mismatch: adjacency nodes and indegree nodes differ");
        }
    }
}
