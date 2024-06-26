package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ScoreboardCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

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

    @ModifyExpressionValue(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/command/CommandManager;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "stringValue=setdisplay"
                    )
            )
    )
    private static LiteralArgumentBuilder<ServerCommandSource> multiScoreboard$addSetDisplaySingleScoreCommand(LiteralArgumentBuilder<ServerCommandSource> builder) {
        return builder.then(CommandManager.literal("indivSidebar")
                .executes(context -> {
                    var scoreboard = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard());
                    var singleScoreSidebars = scoreboard.multiScoreboard$getSingleScoreSidebars();
                    if(singleScoreSidebars.isEmpty()) {
                        context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed.none"), false);
                        return 0;
                    }
                    var count = 0;
                    for(var singleScoreSidebar : new ArrayList<>(singleScoreSidebars.entrySet())) {
                        for(var scoreHolder : singleScoreSidebar.getValue()) {
                            scoreboard.multiScoreboard$toggleSingleScoreSidebar(singleScoreSidebar.getKey(), scoreHolder);
                            count++;
                        }
                    }
                    var finalCount = count;
                    context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removedAll", finalCount), false);
                    return count;
                })
                .then(CommandManager.argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective())
                        .executes(context -> {
                            var objective = ScoreboardObjectiveArgumentType.getObjective(context, "objective");
                            var scoreboard = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard());
                            var scoreHolders = scoreboard.multiScoreboard$getSingleScoreSidebars().get(objective);
                            if(scoreHolders == null) {
                                context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed.none", objective.getDisplayName()), false);
                                return 0;
                            }
                            var count = scoreHolders.size();
                            for(var scoreHolder : new ArrayList<>(scoreHolders)) {
                                scoreboard.multiScoreboard$toggleSingleScoreSidebar(objective, scoreHolder);
                            }
                            context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed.count", count, objective.getDisplayName()), false);
                            return count;
                        })
                        .then(CommandManager.argument("target", ScoreHolderArgumentType.scoreHolder())
                                .executes(context -> {
                                    var objective = ScoreboardObjectiveArgumentType.getObjective(context, "objective");
                                    var target = ScoreHolderArgumentType.getScoreHolder(context, "target").getNameForScoreboard();
                                    var added = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard()).multiScoreboard$toggleSingleScoreSidebar(objective, target);
                                    if (added) {
                                        context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.added", target, objective.getDisplayName()), false);
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(() -> Text.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed", target, objective.getDisplayName()), false);
                                    return -1;
                                }))));
    }
}
