package net.papierkorb2292.multiscoreboard;

import net.minecraft.scoreboard.ScoreboardObjective;

import java.util.Map;
import java.util.Set;

public interface MultiScoreboardSidebarInterface {
    void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective);
    boolean multiScoreboard$toggleSingleScoreSidebar(ScoreboardObjective objective, String scoreHolder);
    Set<ScoreboardObjective> multiScoreboard$getSidebarObjectives();
    Map<ScoreboardObjective, Set<String>> multiScoreboard$getSingleScoreSidebars();

}
