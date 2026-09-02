package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

import java.util.Collections;
import java.util.Set;

public interface SidebarRenderable {
    String getSortName();
    void render(GuiGraphicsExtractor context, Hud hud, DeltaTracker deltaTracker);
    default Set<String> getActiveEntryIds() {
        return Collections.emptySet();
    }

    int calculateHeight();
}
