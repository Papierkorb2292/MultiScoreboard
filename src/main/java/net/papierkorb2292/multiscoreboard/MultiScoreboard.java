package net.papierkorb2292.multiscoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiScoreboard implements ModInitializer {
    public static final String MOD_ID = "multiscoreboard";
    public static final Logger LOGGER = LogManager.getLogger();

    // Multiscoreboard commands are executed on a separate thread, so they keep working when the CommandCrafter debugger suspends the server
    public static final String[] THREADED_COMMANDS = new String[] {
            "scoreboard objectives setdisplay sidebar",
            "scoreboard objectives setdisplay indivSidebar",
            "data multiscoreboard"
    };
    public static final ExecutorService THREADED_COMMAND_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(new SetUseMultiScoreboardS2CPacket(true));
            ((ServerNbtSidebarManagerContainer)server).multiScoreboard$getNbtSidebarManager().onPlayerJoin(sender);
        });
    }
}
