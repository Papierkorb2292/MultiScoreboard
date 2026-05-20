package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

public interface SidebarRenderable {
    String getSortName();
    void render(GuiGraphicsExtractor context, Hud hud);

    int calculateHeight();
}
