package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SidebarSingleScoresRenderable implements SidebarRenderable {

    private static final int SCORE_GAP = 2;
    private final ScoreboardObjective objective;

    public SidebarSingleScoresRenderable(ScoreboardObjective objective) {
        this.objective = objective;
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(DrawContext context, InGameHud inGameHud) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var title = objective.getDisplayName();
        var entries = buildEntries();
        int titleWidth = textRenderer.getWidth(title);
        int maxWidth = Math.max(titleWidth, getMaxEntryWidth(entries));
        int titleLowerY = context.getScaledWindowHeight() / 2;
        int border = 3;
        int leftX = context.getScaledWindowWidth() - maxWidth - border;
        int rightX = context.getScaledWindowWidth() - border + 2;
        int titleBackgroundColor = MinecraftClient.getInstance().options.getTextBackgroundColor(0.4f);
        context.fill(leftX - 2, titleLowerY - textRenderer.fontHeight - 1, rightX, titleLowerY, titleBackgroundColor);
        context.drawText(textRenderer, title, leftX + maxWidth / 2 - titleWidth / 2, titleLowerY - textRenderer.fontHeight, Colors.WHITE, false);
        renderEntries(context, entries, maxWidth);
    }

    public void renderEntries(DrawContext context, List<Entry> entries, int maxWidth) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int upperY = MinecraftClient.getInstance().getWindow().getScaledHeight() / 2;
        int border = 3;
        int leftX = MinecraftClient.getInstance().getWindow().getScaledWidth() - maxWidth - border;
        int rightX = MinecraftClient.getInstance().getWindow().getScaledWidth() - border + 2;
        int entryBackgroundColor = MinecraftClient.getInstance().options.getTextBackgroundColor(0.3f);
        for(int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var entryUpperY = upperY + i * MinecraftClient.getInstance().textRenderer.fontHeight + (i + 1) * SCORE_GAP;
            context.fill(leftX - 2, entryUpperY - 1, rightX, entryUpperY + MinecraftClient.getInstance().textRenderer.fontHeight, entryBackgroundColor);
            context.drawText(textRenderer, entry.name, leftX, entryUpperY, Colors.WHITE, false);
            context.drawText(textRenderer, entry.value, rightX - textRenderer.getWidth(entry.value), entryUpperY, Colors.WHITE, false);
        }
    }

    public List<Entry> buildEntries() {
        var scoreNames = getSingleScores();
        if(scoreNames == null) return List.of();
        return scoreNames.stream().map(name -> {
            Team team = objective.getScoreboard().getScoreHolderTeam(name);
            Text nameText = Team.decorateName(team, Text.literal(name));
            var score = objective.getScoreboard().getScore(ScoreHolder.fromName(name), objective);
            if(score == null)
                return new Entry(nameText, MultiScoreboardClient.NO_DATA_TEXT);
            var scoreValue = score.getScore();
            return new Entry(nameText, StyledNumberFormat.RED.format(scoreValue));
        }).sorted(Comparator.comparing(entry -> entry.name.getString())).toList();
    }

    public int getMaxEntryWidth(List<Entry> entries) {
        int entryGap = MinecraftClient.getInstance().textRenderer.getWidth(": ");
        return entries.stream()
                .mapToInt(entry -> MinecraftClient.getInstance().textRenderer.getWidth(entry.name) + MinecraftClient.getInstance().textRenderer.getWidth(entry.value) + entryGap)
                .max()
                .orElse(0);
    }

    @Override
    public int calculateHeight() {
        return getEntriesHeight() + MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public int getEntriesHeight() {
        var singleScores = getSingleScores();
        return singleScores == null ? 0 : (MinecraftClient.getInstance().textRenderer.fontHeight + SCORE_GAP) * singleScores.size();
    }

    @Nullable
    private Set<String> getSingleScores() {
        return ((MultiScoreboardSidebarInterface)objective.getScoreboard()).multiScoreboard$getSingleScoreSidebars().get(objective);
    }

    public record Entry(Text name, Text value) { }
}
