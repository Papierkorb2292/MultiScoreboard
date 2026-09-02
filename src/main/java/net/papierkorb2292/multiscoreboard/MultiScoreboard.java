package net.papierkorb2292.multiscoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public static Component getTranslatableTextWithCount(String baseKey, int count, Object... args) {
        if(count == 0)
            return Component.translatable(baseKey + ".none", args);
        if(count == 1)
            return Component.translatable(baseKey + ".single", args);
        return Component.translatable(baseKey + ".count", ArrayUtils.addFirst(args, count));
    }
}
