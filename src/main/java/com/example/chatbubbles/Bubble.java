package com.example.chatbubbles;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;
import java.util.UUID;

/** Un mensaje ya "maquetado" (texto envuelto y medido una sola vez). */
public final class Bubble {
    public static final int MAX_WRAP_WIDTH = 110; // en unidades de fuente
    public static final int MAX_CHARS = 160;

    public final UUID uuid;
    public final String name;
    public final long createdMs = Util.getMeasuringTimeMs();
    public final List<OrderedText> lines;
    public final float nameWidth;
    public final float textWidth;

    public Bubble(TextRenderer tr, UUID uuid, String name, String message) {
        this.uuid = uuid;
        this.name = name;
        if (message.length() > MAX_CHARS) message = message.substring(0, MAX_CHARS - 1) + "…";
        this.lines = tr.wrapLines(Text.literal(message), MAX_WRAP_WIDTH);
        this.nameWidth = tr.getWidth(name);
        float w = 0;
        for (OrderedText l : lines) w = Math.max(w, tr.getWidth(l));
        this.textWidth = w;
    }
}
