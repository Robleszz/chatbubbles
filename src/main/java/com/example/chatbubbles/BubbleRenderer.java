package com.example.chatbubbles;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.UUID;

/**
 * Dibuja las burbujas como billboards en el mundo.
 * Capas (de atrás a delante, separadas en Z para evitar z-fighting):
 *   borde -> fondo -> cabeza / texto.
 * Usa capas con test de profundidad: la burbuja se oculta tras bloques (como un cartel real).
 *
 * TODO versión: este es el ÚNICO archivo que toca la API de render, que cambia mucho entre versiones.
 */
public final class BubbleRenderer {
    private static final float BASE_SCALE = 0.025f;   // igual que los nametags
    private static final float Z_STEP = 0.5f;         // +Z = hacia la cámara
    private static final float GAP = 3f;
    private static final int LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    private BubbleRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(BubbleRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BubbleConfig cfg = BubbleConfig.get();
        if (!cfg.enabled || mc.world == null || mc.player == null) return;

        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        long now = Util.getMeasuringTimeMs();
        long lifetime = cfg.durationSeconds * 1000L;
        double maxSq = (double) cfg.maxDistanceBlocks * cfg.maxDistanceBlocks;
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();

        BubbleManager.prune(now, lifetime);

        for (PlayerEntity p : mc.world.getPlayers()) {
            var queue = BubbleManager.get(p.getUuid());
            if (queue == null || queue.isEmpty()) continue;

            boolean self = p == mc.player;
            if (self && (!cfg.showOwnBubbles || firstPerson)) continue;
            if (!self && p.isInvisible()) continue;

            Vec3d pos = p.getLerpedPos(tickDelta).add(0, p.getHeight() + 0.85 - (p.isSneaking() ? 0.25 : 0), 0);
            if (pos.squaredDistanceTo(cam) > maxSq) continue;

            drawStack(mc, cfg, matrices, consumers, camera, pos.subtract(cam), queue.descendingIterator(), now, lifetime, p.getUuid());
        }
    }

    private static void drawStack(MinecraftClient mc, BubbleConfig cfg, MatrixStack matrices,
                                  VertexConsumerProvider consumers, Camera camera, Vec3d rel,
                                  Iterator<Bubble> newestFirst, long now, long lifetime, UUID uuid) {
        float f = cfg.fontScalePercent / 100f;
        float s = BASE_SCALE * cfg.bubbleScalePercent / 100f;
        float yOff = 0;
        int shown = 0;

        while (newestFirst.hasNext() && shown++ < cfg.maxStackedBubbles) {
            Bubble b = newestFirst.next();
            long age = now - b.createdMs;

            float appear = clamp01(age / 250f);                       // 0..1 en 250 ms
            float fadeOut = clamp01((lifetime - age) / 300f);         // último 0.3 s
            float alpha = Math.min(appear, fadeOut);
            float pop = easeOutBack(appear);

            // Medidas (unidades de fuente * f)
            float pad = 4 * f, head = 9 * f, line = 9 * f;
            float w = Math.max(head + 3 * f + b.nameWidth * f, b.textWidth * f) + pad * 2;
            float h = pad * 2 + head + 2 * f + b.lines.size() * line;

            if (alpha > 0.02f) {
                matrices.push();
                matrices.translate(rel.x, rel.y, rel.z);
                matrices.multiply(camera.getRotation());
                matrices.scale(-s, -s, s);
                matrices.translate(0, -yOff, 0);          // apilar hacia arriba
                matrices.scale(pop, pop, pop);            // pop-in desde la base de la burbuja

                drawBubble(mc, cfg, matrices.peek().getPositionMatrix(), consumers, b, w, h, f, pad, head, line, alpha, uuid);
                matrices.pop();
            }
            // Las burbujas viejas "suben" suavemente mientras la nueva aparece
            yOff += (h + GAP) * easeOutBack(appear) * (0.5f + 0.5f * fadeOut);
        }
    }

    private static void drawBubble(MinecraftClient mc, BubbleConfig cfg, Matrix4f m, VertexConsumerProvider consumers,
                                   Bubble b, float w, float h, float f, float pad, float head, float line,
                                   float alpha, UUID uuid) {
        float left = -w / 2, top = -h;

        // Borde + fondo
        int bgA = (int) (cfg.backgroundOpacityPercent / 100f * 255f * alpha);
        int bdA = (int) (230 * alpha);
        VertexConsumer bg = consumers.getBuffer(RenderLayers.textBackground());
        quad(bg, m, left - 1, top - 1, -left + 1, 1, 0f, (bdA << 24) | 0x6C7BFF);
        quad(bg, m, left, top, -left, 0, Z_STEP, (bgA << 24) | 0x101018);

        // Cabeza (cara + capa de sombrero)
        Identifier skin = skinOf(mc, uuid);
        if (skin != null) {
            VertexConsumer vc = consumers.getBuffer(RenderLayers.text(skin));
            int white = ((int) (255 * alpha) << 24) | 0xFFFFFF;
            float hx = left + pad, hy = top + pad;
            texQuad(vc, m, hx, hy, hx + head, hy + head, 2 * Z_STEP, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f, white);
            texQuad(vc, m, hx - f * 0.5f, hy - f * 0.5f, hx + head + f * 0.5f, hy + head + f * 0.5f, 2.5f * Z_STEP,
                    40 / 64f, 8 / 64f, 48 / 64f, 16 / 64f, white);
        }

        TextRenderer tr = mc.textRenderer;
        int a = Math.max(4, (int) (255 * alpha)) << 24;

        // Nombre
        Matrix4f mn = new Matrix4f(m).translate(left + pad + head + 3 * f, top + pad + (head - 8 * f) / 2f, 3 * Z_STEP).scale(f, f, 1f);
        tr.draw(b.name, 0, 0, a | 0xFFD866, false, mn, consumers, TextRenderer.TextLayerType.NORMAL, 0, LIGHT);

        // Mensaje
        float ty = top + pad + head + 2 * f;
        for (OrderedText l : b.lines) {
            Matrix4f ml = new Matrix4f(m).translate(left + pad, ty, 3 * Z_STEP).scale(f, f, 1f);
            tr.draw(l, 0, 0, a | 0xFFFFFF, false, ml, consumers, TextRenderer.TextLayerType.NORMAL, 0, LIGHT);
            ty += line;
        }
    }

    // ---- helpers ----------------------------------------------------------

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, int argb) {
        vc.vertex(m, x1, y1, z).color(argb).light(LIGHT);
        vc.vertex(m, x1, y2, z).color(argb).light(LIGHT);
        vc.vertex(m, x2, y2, z).color(argb).light(LIGHT);
        vc.vertex(m, x2, y1, z).color(argb).light(LIGHT);
    }

    private static void texQuad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z,
                                float u1, float v1, float u2, float v2, int argb) {
        vc.vertex(m, x1, y1, z).color(argb).texture(u1, v1).light(LIGHT);
        vc.vertex(m, x1, y2, z).color(argb).texture(u1, v2).light(LIGHT);
        vc.vertex(m, x2, y2, z).color(argb).texture(u2, v2).light(LIGHT);
        vc.vertex(m, x2, y1, z).color(argb).texture(u2, v1).light(LIGHT);
    }

    /** TODO versión: en 1.21.9+ SkinTextures usa body().texturePath(); en <=1.21.8 era texture(). */
    private static Identifier skinOf(MinecraftClient mc, UUID uuid) {
        if (mc.getNetworkHandler() == null) return null;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(uuid);
        return e == null ? null : e.getSkinTextures().body().texturePath();
    }

    private static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float x = t - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }
}
