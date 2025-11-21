package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin implements MultiScoreboardSidebarInterface {

    @Shadow
    public abstract @Nullable ScoreboardObjective getNullableObjective(@Nullable String name);

    @Unique
    protected final Set<ScoreboardObjective> multiScoreboard$sidebarObjectives = new HashSet<>();
    @Unique
    protected final Map<ScoreboardObjective, Set<String>> multiScoreboard$singleScoreSidebars = new HashMap<>();

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective) {
        multiScoreboard$sidebarObjectives.remove(objective);
    }

    @Override
    public boolean multiScoreboard$toggleSingleScoreSidebar(ScoreboardObjective objective, String scoreHolder) {
        var singleScoreSidebars = multiScoreboard$singleScoreSidebars.computeIfAbsent(objective, k -> new HashSet<>());
        if(singleScoreSidebars.contains(scoreHolder)) {
            singleScoreSidebars.remove(scoreHolder);
            if(singleScoreSidebars.isEmpty())
                multiScoreboard$singleScoreSidebars.remove(objective);
            return false;
        }
        singleScoreSidebars.add(scoreHolder);
        return true;
    }

    @Override
    public Set<ScoreboardObjective> multiScoreboard$getSidebarObjectives() {
        return multiScoreboard$sidebarObjectives;
    }

    @Override
    public Map<ScoreboardObjective, Set<String>> multiScoreboard$getSingleScoreSidebars() {
        return multiScoreboard$singleScoreSidebars;
    }

    @Inject(
            method = "setObjectiveSlot",
            at = @At("HEAD"),
            cancellable = true
    )
    private void multiScoreboard$updateSidebarObjectives(ScoreboardDisplaySlot slot, ScoreboardObjective objective, CallbackInfo ci) {
        if(slot == ScoreboardDisplaySlot.SIDEBAR) {
            if(objective == null) {
                multiScoreboard$sidebarObjectives.clear();
                return;
            }
            multiScoreboard$sidebarObjectives.add(objective);
            ci.cancel();
        }
    }

    @Inject(
            method = "removeObjective",
            at = @At("HEAD")
    )
    private void multiScoreboard$removeSidebarObjective(ScoreboardObjective objective, CallbackInfo ci) {
        multiScoreboard$sidebarObjectives.remove(objective);
        multiScoreboard$singleScoreSidebars.remove(objective);
    }
}
