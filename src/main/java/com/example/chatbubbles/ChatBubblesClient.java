package com.example.chatbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

public class ChatBubblesClient implements ClientModInitializer {
    public static final String MOD_ID = "chatbubbles";

    @Override
    public void onInitializeClient() {
        AutoConfig.register(BubbleConfig.class, GsonConfigSerializer::new);
        ChatCapture.init();
        BubbleRenderer.init();
    }
}
