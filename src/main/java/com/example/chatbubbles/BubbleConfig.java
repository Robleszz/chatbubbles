package com.example.chatbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/** Se guarda en config/chatbubbles.json (Cloth AutoConfig). */
@Config(name = "chatbubbles")
public class BubbleConfig implements ConfigData {
    public boolean enabled = true;
    public boolean showOwnBubbles = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int durationSeconds = 5;

    @ConfigEntry.BoundedDiscrete(min = 50, max = 300)
    public int bubbleScalePercent = 100;

    @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
    public int fontScalePercent = 100;

    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int backgroundOpacityPercent = 70;

    @ConfigEntry.BoundedDiscrete(min = 4, max = 128)
    public int maxDistanceBlocks = 32;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int maxStackedBubbles = 4;

    public static BubbleConfig get() {
        return AutoConfig.getConfigHolder(BubbleConfig.class).getConfig();
    }
}
