package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(Scoreboard.class)
public class ScoreboardMixin implements MultiScoreboardSidebarInterface {

    @Unique
    protected final Set<ScoreboardObjective> multiScoreboard$sidebarObjectives = new HashSet<>();

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective) {
        multiScoreboard$sidebarObjectives.remove(objective);
    }

    @Override
    public Set<ScoreboardObjective> multiScoreboard$getSidebarObjectives() {
        return multiScoreboard$sidebarObjectives;
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
    }
}
