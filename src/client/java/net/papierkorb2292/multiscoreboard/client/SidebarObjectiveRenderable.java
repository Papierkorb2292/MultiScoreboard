package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import net.papierkorb2292.multiscoreboard.mixin.client.HudAccessor;

public class SidebarObjectiveRenderable implements SidebarRenderable {

    public static final ThreadLocal<Integer> CURRENT_MAX_WIDTH = new ThreadLocal<>();

    private final Objective objective;
    private final SidebarSingleScoresRenderable singleScoresRenderable;
    private final boolean showSingleScores;

    public SidebarObjectiveRenderable(Objective objective, boolean showSingleScores) {
        this.objective = objective;
        this.singleScoresRenderable = new SidebarSingleScoresRenderable(objective);
        this.showSingleScores = showSingleScores;
    }

    public SidebarObjectiveRenderable(Objective objective) {
        this(objective, true);
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(GuiGraphicsExtractor context, Hud hud) {
        var singleScoresEntries = singleScoresRenderable.buildEntries();
        CURRENT_MAX_WIDTH.set(singleScoresRenderable.getMaxEntryWidth(singleScoresEntries));
        ((HudAccessor) hud).callDisplayScoreboardSidebar(context, objective);
        if(showSingleScores) {
            context.pose().pushMatrix();
            context.pose().translate(0, calculateVanillaEntriesHeight());
            singleScoresRenderable.renderEntries(context, singleScoresEntries, CURRENT_MAX_WIDTH.get());
            context.pose().popMatrix();
        }
        CURRENT_MAX_WIDTH.remove();
    }

    private int calculateVanillaEntriesHeight() {
        return Math.min(15, objective.getScoreboard().listPlayerScores(objective).size()) * Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public int calculateHeight() {
        final var vanillaHeight = Minecraft.getInstance().font.lineHeight + calculateVanillaEntriesHeight();
        if(!showSingleScores)
            return vanillaHeight;
        return vanillaHeight + singleScoresRenderable.getEntriesHeight();
    }
}
