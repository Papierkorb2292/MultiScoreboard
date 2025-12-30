package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;

public interface SidebarRenderable {
    String getSortName();
    void render(GuiGraphics context, Gui inGameHud);

    int calculateHeight();
}
