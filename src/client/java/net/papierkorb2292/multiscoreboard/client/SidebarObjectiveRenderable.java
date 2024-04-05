package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.mixin.client.InGameHudAccessor;

public class SidebarObjectiveRenderable implements SidebarRenderable {
    private final ScoreboardObjective objective;

    public SidebarObjectiveRenderable(ScoreboardObjective objective) {
        this.objective = objective;
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(DrawContext context, InGameHud inGameHud) {
        ((InGameHudAccessor)inGameHud).callRenderScoreboardSidebar(context, objective);
    }

    @Override
    public int calculateHeight() {
        return (1 + objective.getScoreboard().getScoreboardEntries(objective).size()) * MinecraftClient.getInstance().textRenderer.fontHeight;
    }
}
