package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.gui.DrawContext;

public interface SidebarRenderable {
    String getSortName();
    void render(DrawContext context);

    int calculateHeight();
}
