package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface SidebarRenderable {
    String getSortName();
    void render(GuiGraphicsExtractor context, Gui inGameHud);

    int calculateHeight();
}
