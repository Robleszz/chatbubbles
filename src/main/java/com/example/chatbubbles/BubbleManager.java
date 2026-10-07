package com.example.chatbubbles;

import net.minecraft.client.MinecraftClient;

import java.util.*;

public final class BubbleManager {
    private static final Map<UUID, ArrayDeque<Bubble>> BUBBLES = new HashMap<>();
    private static final int HARD_CAP = 10;

    private BubbleManager() {}

    public static void add(UUID uuid, String name, String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (message == null || message.isBlank()) return;
        ArrayDeque<Bubble> q = BUBBLES.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        q.addLast(new Bubble(mc.textRenderer, uuid, name, message.strip()));
        while (q.size() > HARD_CAP) q.removeFirst();
    }

    /** Orden: el más antiguo primero, el más nuevo al final. */
    public static ArrayDeque<Bubble> get(UUID uuid) {
        return BUBBLES.get(uuid);
    }

    public static void prune(long nowMs, long lifetimeMs) {
        Iterator<Map.Entry<UUID, ArrayDeque<Bubble>>> it = BUBBLES.entrySet().iterator();
        while (it.hasNext()) {
            ArrayDeque<Bubble> q = it.next().getValue();
            while (!q.isEmpty() && nowMs - q.peekFirst().createdMs > lifetimeMs) q.removeFirst();
            if (q.isEmpty()) it.remove();
        }
    }

    public static void clear() { BUBBLES.clear(); }
}
