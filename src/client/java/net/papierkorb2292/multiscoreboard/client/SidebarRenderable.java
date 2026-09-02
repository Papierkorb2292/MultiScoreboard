package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.Set;

import static net.papierkorb2292.multiscoreboard.client.MultiScoreboardClient.getSidebarAddedAnimationProgress;

public interface SidebarRenderable {
    String getSortName();
    void render(GuiGraphicsExtractor context, Hud hud, DeltaTracker deltaTracker);
    default Set<String> getActiveEntryIds() {
        return Collections.emptySet();
    }

    int calculateHeight();

    default void outlineAddedSidebar(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, String entryId, Hud hud, DeltaTracker deltaTracker) {
        final var color = MultiScoreboardClient.fadeOutSidebarAddedAnimatedBackground(this, entryId, hud, deltaTracker, 0.3f);
        final var width = Mth.ceil(3 * (1-getSidebarAddedAnimationProgress(this, entryId, hud, deltaTracker)));
        context.fill(x0, y0, x1, y0 + width, color);
        context.fill(x0, y1 - width, x1, y1, color);
        context.fill(x0, y0 + width, x0 + width, y1, color);
        context.fill(x1 - width, y0 + width, x1, y1, color);
    }
}
