package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.CommonColors;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SidebarSingleScoresRenderable implements SidebarRenderable {

    private static final int SCORE_GAP = 2;
    private final Objective objective;

    public SidebarSingleScoresRenderable(Objective objective) {
        this.objective = objective;
    }

    @Override
    public String getSortName() {
        return objective.getName();
    }

    @Override
    public void render(GuiGraphics context, Gui inGameHud) {
        var textRenderer = Minecraft.getInstance().font;
        var title = objective.getDisplayName();
        var entries = buildEntries();
        int titleWidth = textRenderer.width(title);
        int maxWidth = Math.max(titleWidth, getMaxEntryWidth(entries));
        int titleLowerY = context.guiHeight() / 2;
        int border = 3;
        int leftX = context.guiWidth() - maxWidth - border;
        int rightX = context.guiWidth() - border + 2;
        int titleBackgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.4f);
        context.fill(leftX - 2, titleLowerY - textRenderer.lineHeight - 1, rightX, titleLowerY, titleBackgroundColor);
        context.drawString(textRenderer, title, leftX + maxWidth / 2 - titleWidth / 2, titleLowerY - textRenderer.lineHeight, CommonColors.WHITE, false);
        renderEntries(context, entries, maxWidth);
    }

    public void renderEntries(GuiGraphics context, List<Entry> entries, int maxWidth) {
        var textRenderer = Minecraft.getInstance().font;
        int upperY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        int border = 3;
        int leftX = Minecraft.getInstance().getWindow().getGuiScaledWidth() - maxWidth - border;
        int rightX = Minecraft.getInstance().getWindow().getGuiScaledWidth() - border + 2;
        int entryBackgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.3f);
        for(int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var entryUpperY = upperY + i * Minecraft.getInstance().font.lineHeight + (i + 1) * SCORE_GAP;
            context.fill(leftX - 2, entryUpperY - 1, rightX, entryUpperY + Minecraft.getInstance().font.lineHeight, entryBackgroundColor);
            context.drawString(textRenderer, entry.name, leftX, entryUpperY, CommonColors.WHITE, false);
            context.drawString(textRenderer, entry.value, rightX - textRenderer.width(entry.value), entryUpperY, CommonColors.WHITE, false);
        }
    }

    public List<Entry> buildEntries() {
        var scoreNames = getSingleScores();
        if(scoreNames == null) return List.of();
        return scoreNames.stream().map(name -> {
            PlayerTeam team = objective.getScoreboard().getPlayersTeam(name);
            Component nameText = PlayerTeam.formatNameForTeam(team, Component.literal(name));
            var score = objective.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(name), objective);
            if(score == null)
                return new Entry(nameText, MultiScoreboardClient.NO_DATA_TEXT);
            var scoreValue = score.value();
            return new Entry(nameText, StyledFormat.SIDEBAR_DEFAULT.format(scoreValue));
        }).sorted(Comparator.comparing(entry -> entry.name.getString())).toList();
    }

    public int getMaxEntryWidth(List<Entry> entries) {
        int entryGap = Minecraft.getInstance().font.width(": ");
        return entries.stream()
                .mapToInt(entry -> Minecraft.getInstance().font.width(entry.name) + Minecraft.getInstance().font.width(entry.value) + entryGap)
                .max()
                .orElse(0);
    }

    @Override
    public int calculateHeight() {
        return getEntriesHeight() + Minecraft.getInstance().font.lineHeight;
    }

    public int getEntriesHeight() {
        var singleScores = getSingleScores();
        return singleScores == null ? 0 : (Minecraft.getInstance().font.lineHeight + SCORE_GAP) * singleScores.size();
    }

    @Nullable
    private Set<String> getSingleScores() {
        return ((MultiScoreboardSidebarInterface)objective.getScoreboard()).multiScoreboard$getSingleScoreSidebars().get(objective);
    }

    public record Entry(Component name, Component value) { }
}
