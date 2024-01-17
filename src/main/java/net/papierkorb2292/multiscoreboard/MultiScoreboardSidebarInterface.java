package net.papierkorb2292.multiscoreboard;

import net.minecraft.scoreboard.ScoreboardObjective;

import java.util.Set;

public interface MultiScoreboardSidebarInterface {
    void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective);
    Set<ScoreboardObjective> multiScoreboard$getSidebarObjectives();
}
