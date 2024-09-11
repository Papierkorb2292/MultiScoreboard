package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.mixin.client.InGameHudAccessor;

public class SidebarObjectiveRenderable implements SidebarRenderable {

    public static final ThreadLocal<Integer> CURRENT_MAX_WIDTH = new ThreadLocal<>();

    private final ScoreboardObjective objective;
    private final SidebarSingleScoresRenderable singleScoresRenderable;
    private final boolean showSingleScores;

    public SidebarObjectiveRenderable(ScoreboardObjective objective, boolean showSingleScores) {
        this.objective = objective;
        this.singleScoresRenderable = new SidebarSingleScoresRenderable(objective);
        this.showSingleScores = showSingleScores;
    }

    public SidebarObjectiveRenderable(ScoreboardObjective objective) {
        this(objective, true);
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(DrawContext context, InGameHud inGameHud) {
        var singleScoresEntries = singleScoresRenderable.buildEntries();
        CURRENT_MAX_WIDTH.set(singleScoresRenderable.getMaxEntryWidth(singleScoresEntries));
        ((InGameHudAccessor)inGameHud).callRenderScoreboardSidebar(context, objective);
        if(showSingleScores) {
            context.getMatrices().push();
            context.getMatrices().translate(0, calculateVanillaEntriesHeight(), 0);
            singleScoresRenderable.renderEntries(context, singleScoresEntries, CURRENT_MAX_WIDTH.get());
            context.getMatrices().pop();
        }
        CURRENT_MAX_WIDTH.remove();
    }

    private int calculateVanillaEntriesHeight() {
        return Math.min(15, objective.getScoreboard().getScoreboardEntries(objective).size()) * MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    @Override
    public int calculateHeight() {
        final var vanillaHeight = MinecraftClient.getInstance().textRenderer.fontHeight + calculateVanillaEntriesHeight();
        if(!showSingleScores)
            return vanillaHeight;
        return vanillaHeight + singleScoresRenderable.getEntriesHeight();
    }
}
