package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.ScoreboardCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardCommand.class)
public class ScoreboardCommandMixin {

    @Inject(
            method = "executeSetDisplay",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void multiscoreboard$hideSidebarScoreboard(ServerCommandSource source, ScoreboardDisplaySlot slot, ScoreboardObjective objective, CallbackInfoReturnable<Integer> cir) {
        var scoreboard = ((MultiScoreboardSidebarInterface)source.getServer().getScoreboard());
        if(slot == ScoreboardDisplaySlot.SIDEBAR && scoreboard.multiScoreboard$getSidebarObjectives().contains(objective)) {
            scoreboard.multiScoreboard$removeObjectiveFromSidebar(objective);
            source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.removed", objective.getDisplayName(), slot.asString()), true);
            cir.setReturnValue(0);
        }
    }

    @WrapOperation(
            method = "executeClearDisplay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(Lnet/minecraft/scoreboard/ScoreboardDisplaySlot;)Lnet/minecraft/scoreboard/ScoreboardObjective;"
            )
    )
    private static ScoreboardObjective multiScoreboard$letClearingCheckSidebarList(Scoreboard instance, ScoreboardDisplaySlot slot, Operation<ScoreboardObjective> op) {
        if(slot != ScoreboardDisplaySlot.SIDEBAR)
            return op.call(instance, slot);
        return ((MultiScoreboardSidebarInterface)instance).multiScoreboard$getSidebarObjectives().stream().findFirst().orElse(null);
    }
}
