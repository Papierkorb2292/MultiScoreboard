package net.papierkorb2292.multiscoreboard.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import net.papierkorb2292.multiscoreboard.mixin.client.HudAccessor;

public class SidebarObjectiveRenderable implements SidebarRenderable {

    public static final ThreadLocal<Integer> CURRENT_MAX_WIDTH = new ThreadLocal<>();
    public static final ThreadLocal<Pair<SidebarRenderable, DeltaTracker>> OBJECTIVE_ADD_ANIMATION_CONTEXT = new ThreadLocal<>();

    public final SidebarSingleScoresRenderable singleScoresRenderable;
    private final Objective objective;
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
    public void render(GuiGraphicsExtractor context, Hud hud, DeltaTracker deltaTracker) {
        var singleScoresEntries = singleScoresRenderable.buildEntries();
        CURRENT_MAX_WIDTH.set(singleScoresRenderable.getMaxEntryWidth(singleScoresEntries));
        OBJECTIVE_ADD_ANIMATION_CONTEXT.set(Pair.of(this, deltaTracker));
        ((HudAccessor) hud).callDisplayScoreboardSidebar(context, objective);
        if(showSingleScores) {
            context.pose().pushMatrix();
            context.pose().translate(0, calculateVanillaEntriesHeight());
            singleScoresRenderable.renderEntries(context, singleScoresEntries, CURRENT_MAX_WIDTH.get(), hud, deltaTracker);
            context.pose().popMatrix();
        }
        CURRENT_MAX_WIDTH.remove();
        OBJECTIVE_ADD_ANIMATION_CONTEXT.remove();
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
