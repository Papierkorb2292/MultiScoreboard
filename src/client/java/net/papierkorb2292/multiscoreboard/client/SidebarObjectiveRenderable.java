package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.ScoreboardObjective;

import java.util.function.BiConsumer;

public class SidebarObjectiveRenderable implements SidebarRenderable{
    private final ScoreboardObjective objective;

    private final BiConsumer<DrawContext, ScoreboardObjective> objectiveRenderer;
    public SidebarObjectiveRenderable(ScoreboardObjective objective, BiConsumer<DrawContext, ScoreboardObjective> objectiveRenderer) {
        this.objective = objective;
        this.objectiveRenderer = objectiveRenderer;
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(DrawContext context) {
        objectiveRenderer.accept(context, objective);
    }

    @Override
    public int calculateHeight() {
        return (1 + objective.getScoreboard().getScoreboardEntries(objective).size()) * MinecraftClient.getInstance().textRenderer.fontHeight;
    }
}
