package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.CommonColors;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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
    public void render(GuiGraphicsExtractor context, Hud hud, DeltaTracker deltaTracker) {
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
        outlineAddedSidebar(context, leftX - 2, titleLowerY - textRenderer.lineHeight - 1, rightX, titleLowerY, null, hud, deltaTracker);
        context.text(textRenderer, title, leftX + maxWidth / 2 - titleWidth / 2, titleLowerY - textRenderer.lineHeight, CommonColors.WHITE, false);
        renderEntries(context, entries, maxWidth, hud, deltaTracker);
    }

    public void renderEntries(GuiGraphicsExtractor context, List<Entry> entries, int maxWidth, Hud hud, DeltaTracker deltaTracker) {
        var textRenderer = Minecraft.getInstance().font;
        int upperY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        int borderEdge = 1;
        int borderCenter = 2;
        int leftX = MultiScoreboardClient.getScoreboardXPosition(maxWidth, borderEdge, borderCenter);
        int rightX = leftX + maxWidth + borderCenter;
        for(int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            int entryBackgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.3f);
            var entryUpperY = upperY + i * Minecraft.getInstance().font.lineHeight + (i + 1) * SCORE_GAP;
            context.fill(leftX - 2, entryUpperY - 1, rightX, entryUpperY + Minecraft.getInstance().font.lineHeight, entryBackgroundColor);
            outlineAddedSidebar(context, leftX - 2, entryUpperY - 1, rightX, entryUpperY + Minecraft.getInstance().font.lineHeight, entry.nameString, hud, deltaTracker);
            context.text(textRenderer, entry.name, leftX, entryUpperY, CommonColors.WHITE, false);
            context.text(textRenderer, entry.value, rightX - textRenderer.width(entry.value), entryUpperY, CommonColors.WHITE, false);
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
                return new Entry(nameText, name, MultiScoreboardClient.NO_DATA_TEXT);
            var scoreValue = score.value();
            return new Entry(nameText, name, StyledFormat.SIDEBAR_DEFAULT.format(scoreValue));
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

    @Override
    public Set<String> getActiveEntryIds() {
        final var entries = getSingleScores();
        return entries != null ? entries : Collections.emptySet();
    }

    public record Entry(Component name, String nameString, Component value) { }
}
