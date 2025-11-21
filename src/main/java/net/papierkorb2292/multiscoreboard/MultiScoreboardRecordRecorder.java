package net.papierkorb2292.multiscoreboard;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.scoreboard.ScoreboardState;
import recordrecoder.api.record.ComponentKeyRegistry;
import recordrecoder.api.record.RecordComponentKey;

/**
 * Adds custom sidebar data to {@link ScoreboardState.Packed}
 */
public class MultiScoreboardRecordRecorder implements Runnable {

    public static final RecordComponentKey<CustomSidebarPacked> CUSTOM_SIDEBAR_KEY;

    @Override
    public void run() {
        ComponentKeyRegistry.INSTANCE.register(CUSTOM_SIDEBAR_KEY);
    }

    public static ScoreboardState.Packed copyScoreboardStateWithCustomState(ScoreboardState.Packed vanillaState, CustomSidebarPacked customState) {
        for(var key : ComponentKeyRegistry.INSTANCE.getForClass(ScoreboardState.Packed.class)) {
            copyKey(key, vanillaState);
        }
        CUSTOM_SIDEBAR_KEY.queueNext(customState);
        return new ScoreboardState.Packed(vanillaState.objectives(), vanillaState.scores(), vanillaState.displaySlots(), vanillaState.teams());
    }

    private static <T> void copyKey(RecordComponentKey<T> key, Record record) {
        key.queueNext(key.getOrNull(record));
    }

    static {
        var remapper = FabricLoader.getInstance().getMappingResolver();
        var mappedName = remapper.mapClassName("intermediary", "net.minecraft.class_273$class_10745").replace(".", "/");
        CUSTOM_SIDEBAR_KEY = RecordComponentKey.create(
                "custom_sidebar",
                mappedName,
                CustomSidebarPacked.class
        );
    }
}
