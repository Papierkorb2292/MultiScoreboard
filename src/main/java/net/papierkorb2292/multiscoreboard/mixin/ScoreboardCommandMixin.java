package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.Commands;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import net.papierkorb2292.multiscoreboard.MultiScoreboard;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ScoreboardCommand.class)
public class ScoreboardCommandMixin {

    @Inject(
            method = "setDisplaySlot",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void multiscoreboard$hideSidebarScoreboard(CommandSourceStack source, DisplaySlot slot, Objective objective, CallbackInfoReturnable<Integer> cir) {
        var scoreboard = ((MultiScoreboardSidebarInterface)source.getServer().getScoreboard());
        if(slot == DisplaySlot.SIDEBAR && scoreboard.multiScoreboard$getSidebarObjectives().contains(objective)) {
            scoreboard.multiScoreboard$removeObjectiveFromSidebar(objective);
            source.sendSuccess(() -> Component.translatable("multiscoreboard.commands.scoreboard.objectives.display.removed", objective.getDisplayName(), slot.getSerializedName()), true);
            cir.setReturnValue(0);
        }
    }

    @WrapOperation(
            method = "clearDisplaySlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/Scoreboard;getDisplayObjective(Lnet/minecraft/world/scores/DisplaySlot;)Lnet/minecraft/world/scores/Objective;"
            )
    )
    private static Objective multiScoreboard$letClearingCheckSidebarList(Scoreboard instance, DisplaySlot slot, Operation<Objective> op) {
        if(slot != DisplaySlot.SIDEBAR)
            return op.call(instance, slot);
        return ((MultiScoreboardSidebarInterface)instance).multiScoreboard$getSidebarObjectives().stream().findFirst().orElse(null);
    }

    @ModifyExpressionValue(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "stringValue=setdisplay"
                    )
            )
    )
    private static LiteralArgumentBuilder<CommandSourceStack> multiScoreboard$addSetDisplaySingleScoreCommand(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.then(Commands.literal("indivSidebar")
                .executes(context -> {
                    var scoreboard = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard());
                    var singleScoreSidebars = scoreboard.multiScoreboard$getSingleScoreSidebars();
                    if(singleScoreSidebars.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removedAll.none"), false);
                        return 0;
                    }
                    var count = 0;
                    for(var singleScoreSidebar : new ArrayList<>(singleScoreSidebars.entrySet())) {
                        for(var scoreHolder : new ArrayList<>(singleScoreSidebar.getValue())) {
                            scoreboard.multiScoreboard$toggleSingleScoreSidebar(singleScoreSidebar.getKey(), scoreHolder);
                            count++;
                        }
                    }
                    var finalCount = count;
                    context.getSource().sendSuccess(() -> MultiScoreboard.getTranslatableTextWithCount("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removedAll", finalCount), false);
                    return count;
                })
                .then(Commands.argument("objective", ObjectiveArgument.objective())
                        .executes(context -> {
                            var objective = ObjectiveArgument.getObjective(context, "objective");
                            var scoreboard = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard());
                            var scoreHolders = scoreboard.multiScoreboard$getSingleScoreSidebars().get(objective);
                            if(scoreHolders == null) {
                                context.getSource().sendSuccess(() -> Component.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed.none", objective.getDisplayName()), false);
                                return 0;
                            }
                            var count = scoreHolders.size();
                            for(var scoreHolder : new ArrayList<>(scoreHolders)) {
                                scoreboard.multiScoreboard$toggleSingleScoreSidebar(objective, scoreHolder);
                            }
                            context.getSource().sendSuccess(() -> MultiScoreboard.getTranslatableTextWithCount("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed", count, objective.getDisplayName()), false);
                            return count;
                        })
                        .then(Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                .suggests((context, suggestionsBuilder) -> {
                                    var objective = ObjectiveArgument.getObjective(context, "objective");
                                    var entries = objective.getScoreboard().listPlayerScores(objective);

                                    var selectorFuture = ScoreHolderArgument.SUGGEST_SCORE_HOLDERS.getSuggestions(context, suggestionsBuilder);
                                    return SharedSuggestionProvider.suggest(entries, suggestionsBuilder, PlayerScoreEntry::owner, entry -> entry::owner)
                                            .thenCombine(selectorFuture, (Suggestions nameSuggestions, Suggestions selectorSuggestions) ->
                                                    Suggestions.merge(suggestionsBuilder.getInput(), List.of(nameSuggestions, selectorSuggestions)));
                                })
                                .executes(context -> {
                                    var objective = ObjectiveArgument.getObjective(context, "objective");
                                    var target = ScoreHolderArgument.getName(context, "target").getScoreboardName();
                                    var added = ((MultiScoreboardSidebarInterface)context.getSource().getServer().getScoreboard()).multiScoreboard$toggleSingleScoreSidebar(objective, target);
                                    if (added) {
                                        context.getSource().sendSuccess(() -> Component.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.added", target, objective.getDisplayName()), false);
                                        return 1;
                                    }
                                    context.getSource().sendSuccess(() -> Component.translatable("multiscoreboard.commands.scoreboard.objectives.display.indivSidebar.removed", target, objective.getDisplayName()), false);
                                    return -1;
                                }))));
    }
}
