package com.example.chatbubbles;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captura el chat SIN mixins, solo con eventos de Fabric API:
 *  - CHAT: mensajes firmados de jugador (vanilla / servidores con firma) -> traen al remitente.
 *  - GAME: mensajes de sistema (plugins tipo EssentialsChat, Paper, etc.) -> se deduce el
 *          remitente buscando el nombre de un jugador cercano al inicio del texto.
 */
public final class ChatCapture {
    // Tras el nombre: ">" o "]" opcional, y luego un separador: ">", ":", "»", "›"
    private static final Pattern AFTER_NAME = Pattern.compile("^[>\\]]?\\s*[:»›>]\\s*(.+)$", Pattern.DOTALL);

    private ChatCapture() {}

    public static void init() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            if (sender == null) return;
            String text = signed != null ? signed.getSignedContent() : message.getString();
            // En 1.21.9+ GameProfile es un record: id() / name(). (Antes: getId()/getName())
            BubbleManager.add(sender.id(), sender.name(), text);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) tryParseSystemChat(message.getString());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BubbleManager.clear());
    }

    private static void tryParseSystemChat(String raw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        List<PlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
        players.sort(Comparator.comparingInt((PlayerEntity p) -> p.getName().getString().length()).reversed());

        for (PlayerEntity p : players) {
            String name = p.getName().getString();
            int idx = raw.indexOf(name);
            if (idx < 0 || idx > 30) continue; // el nombre debe estar al principio ([Rango] Nombre: ...)
            Matcher m = AFTER_NAME.matcher(raw.substring(idx + name.length()));
            if (m.matches()) {
                BubbleManager.add(p.getUuid(), name, m.group(1));
                return;
            }
        }
    }
}
