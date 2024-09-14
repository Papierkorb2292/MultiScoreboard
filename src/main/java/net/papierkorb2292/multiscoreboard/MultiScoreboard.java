package net.papierkorb2292.multiscoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiScoreboard implements ModInitializer {
    public static final String MOD_ID = "multiscoreboard";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(new SetUseMultiScoreboardS2CPacket(true));
            ((ServerNbtSidebarManagerContainer)server).multiScoreboard$getNbtSidebarManager().onPlayerJoin(sender);
        });
    }
}
