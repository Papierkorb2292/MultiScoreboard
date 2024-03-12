package net.papierkorb2292.multiscoreboard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.papierkorb2292.multiscoreboard.SetUseMultiScoreboardS2CPacket;

public class MultiScoreboardClient implements ClientModInitializer {
    private static boolean useMultiScoreboard = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetUseMultiScoreboardS2CPacket.TYPE, (packet, player, responseSender) -> {
            useMultiScoreboard = packet.useMultiScoreboard;
        });
        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            useMultiScoreboard = false;
        });
    }

    public static boolean useMultiScoreboard() {
        return useMultiScoreboard;
    }
}
