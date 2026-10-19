package com.syaru.mesubnetbus.core;

import java.util.*;

/** Identity-based graph of networks, rebuilt only when topology or configuration changes. */
public final class ExposureGraph<N> {
    public enum Result { ACCEPTED, SAME_NETWORK, CYCLE, DUPLICATE_PATH }
    private final Map<N, Set<N>> edges = new IdentityHashMap<>();

    public Result add(N source, N target) {
        if (source == target) return Result.SAME_NETWORK;
        if (reachable(target, source)) return Result.CYCLE;
        Set<N> targets = edges.computeIfAbsent(source, ignored -> identitySet());
        if (!targets.add(target)) return Result.DUPLICATE_PATH;
        // A diamond exposes a physical inventory twice to the same terminal.
        // Reject the added edge rather than inventing or duplicating stock.
        for (N root : edges.keySet()) {
            Set<N> visited = identitySet();
            if (duplicate(root, visited)) {
                targets.remove(target);
                return Result.DUPLICATE_PATH;
            }
        }
        return Result.ACCEPTED;
    }

    private boolean duplicate(N current, Set<N> visited) {
        if (!visited.add(current)) return true;
        for (N next : edges.getOrDefault(current, Set.of())) if (duplicate(next, visited)) return true;
        return false;
    }

    private boolean reachable(N start, N goal) {
        Set<N> visited = identitySet();
        ArrayDeque<N> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            N node = queue.remove();
            if (node == goal) return true;
            if (visited.add(node)) queue.addAll(edges.getOrDefault(node, Set.of()));
        }
        return false;
    }
    private static <T> Set<T> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<>()); }
}
