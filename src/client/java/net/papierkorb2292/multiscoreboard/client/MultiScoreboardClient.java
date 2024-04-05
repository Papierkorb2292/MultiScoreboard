package net.papierkorb2292.multiscoreboard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtElement;
import net.papierkorb2292.multiscoreboard.RemoveNbtSidebarS2CPacket;
import net.papierkorb2292.multiscoreboard.SetNbtSidebarS2CPacket;
import net.papierkorb2292.multiscoreboard.SetUseMultiScoreboardS2CPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiScoreboardClient implements ClientModInitializer {
    private static boolean useMultiScoreboard = false;
    private static Map<String, List<NbtElement>> nbtSidebars = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetUseMultiScoreboardS2CPacket.TYPE, (packet, player, responseSender) -> {
            useMultiScoreboard = packet.useMultiScoreboard;
        });
        ClientPlayNetworking.registerGlobalReceiver(SetNbtSidebarS2CPacket.TYPE, (packet, player, responseSender) -> {
            nbtSidebars.put(packet.nbtSidebarName, packet.nbt);
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveNbtSidebarS2CPacket.TYPE, (packet, player, responseSender) -> {
            nbtSidebars.remove(packet.nbtSidebarName);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            useMultiScoreboard = false;
            nbtSidebars.clear();
        });
    }

    public static boolean useMultiScoreboard() {
        return useMultiScoreboard;
    }

    public static Map<String, List<NbtElement>> getNbtSidebars() {
        return nbtSidebars;
    }
}
