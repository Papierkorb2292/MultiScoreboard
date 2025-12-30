package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
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
    public abstract @Nullable Objective getObjective(@Nullable String name);

    @Unique
    protected final Set<Objective> multiScoreboard$sidebarObjectives = new HashSet<>();
    @Unique
    protected final Map<Objective, Set<String>> multiScoreboard$singleScoreSidebars = new HashMap<>();

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(Objective objective) {
        multiScoreboard$sidebarObjectives.remove(objective);
    }

    @Override
    public boolean multiScoreboard$toggleSingleScoreSidebar(Objective objective, String scoreHolder) {
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
    public Set<Objective> multiScoreboard$getSidebarObjectives() {
        return multiScoreboard$sidebarObjectives;
    }

    @Override
    public Map<Objective, Set<String>> multiScoreboard$getSingleScoreSidebars() {
        return multiScoreboard$singleScoreSidebars;
    }

    @Inject(
            method = "setDisplayObjective",
            at = @At("HEAD"),
            cancellable = true
    )
    private void multiScoreboard$updateSidebarObjectives(DisplaySlot slot, Objective objective, CallbackInfo ci) {
        if(slot == DisplaySlot.SIDEBAR) {
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
    private void multiScoreboard$removeSidebarObjective(Objective objective, CallbackInfo ci) {
        multiScoreboard$sidebarObjectives.remove(objective);
        multiScoreboard$singleScoreSidebars.remove(objective);
    }
}
