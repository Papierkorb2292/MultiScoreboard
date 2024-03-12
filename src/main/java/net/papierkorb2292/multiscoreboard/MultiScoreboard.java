package net.papierkorb2292.multiscoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class MultiScoreboard implements ModInitializer {
    public static final String MOD_ID = "multiscoreboard";

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(new SetUseMultiScoreboardS2CPacket(true));
        });
    }
}
