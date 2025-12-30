package net.papierkorb2292.multiscoreboard;

import net.minecraft.world.scores.Objective;

import java.util.Map;
import java.util.Set;

public interface MultiScoreboardSidebarInterface {
    void multiScoreboard$removeObjectiveFromSidebar(Objective objective);
    boolean multiScoreboard$toggleSingleScoreSidebar(Objective objective, String scoreHolder);
    Set<Objective> multiScoreboard$getSidebarObjectives();
    Map<Objective, Set<String>> multiScoreboard$getSingleScoreSidebars();

}
