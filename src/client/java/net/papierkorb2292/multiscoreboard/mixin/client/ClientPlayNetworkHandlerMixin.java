package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @WrapOperation(
            method = "onScoreboardDisplay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/Scoreboard;setObjectiveSlot(Lnet/minecraft/scoreboard/ScoreboardDisplaySlot;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            )
    )
    private void multiScoreboard$toggleObjectiveInSidebar(Scoreboard scoreboard, ScoreboardDisplaySlot displaySlot, ScoreboardObjective objective, Operation<Void> op) {
        if(displaySlot != ScoreboardDisplaySlot.SIDEBAR) {
            op.call(scoreboard, displaySlot, objective);
            return;
        }
        var multiScoreboard = ((MultiScoreboardSidebarInterface)scoreboard);
        if(multiScoreboard.multiScoreboard$getSidebarObjectives().contains(objective)) {
            multiScoreboard.multiScoreboard$removeObjectiveFromSidebar(objective);
        } else {
            op.call(scoreboard, displaySlot, objective);
        }
    }
}
