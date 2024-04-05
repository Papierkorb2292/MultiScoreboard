package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

public interface SidebarRenderable {
    String getSortName();
    void render(DrawContext context, InGameHud inGameHud);

    int calculateHeight();
}
