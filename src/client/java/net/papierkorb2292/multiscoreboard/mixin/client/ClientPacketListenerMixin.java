package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import net.papierkorb2292.multiscoreboard.client.MultiScoreboardClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @WrapOperation(
            method = "handleSetDisplayObjective",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/Scoreboard;setDisplayObjective(Lnet/minecraft/world/scores/DisplaySlot;Lnet/minecraft/world/scores/Objective;)V"
            )
    )
    private void multiScoreboard$toggleObjectiveInSidebar(Scoreboard scoreboard, DisplaySlot displaySlot, Objective objective, Operation<Void> op) {
        if(displaySlot != DisplaySlot.SIDEBAR) {
            op.call(scoreboard, displaySlot, objective);
            return;
        }
        var multiScoreboard = ((MultiScoreboardSidebarInterface)scoreboard);
        if(!MultiScoreboardClient.useMultiScoreboard()) {
            multiScoreboard.multiScoreboard$getSidebarObjectives().clear();
            op.call(scoreboard, displaySlot, objective);
            return;
        }
        if(multiScoreboard.multiScoreboard$getSidebarObjectives().contains(objective)) {
            multiScoreboard.multiScoreboard$removeObjectiveFromSidebar(objective);
            MultiScoreboardClient.clampScrollTranslation();
        } else {
            op.call(scoreboard, displaySlot, objective);
        }
    }
}
