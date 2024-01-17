package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective);

    @Unique
    private int multiScoreboard$getObjectiveContentRenderHeight(Scoreboard scoreboard, ScoreboardObjective objective) {
        return scoreboard.getScoreboardEntries(objective).size() * 9;
    }

    @Unique
    private int multiScoreboard$calculateSidebarHeights(Scoreboard scoreboard, LocalRef<Map<ScoreboardObjective, Integer>> scoreboardHeightsRef) {
        var scoreboardHeights = new HashMap<ScoreboardObjective, Integer>();

        var sidebarObjectives = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives();
        var totalHeight = 0;
        for(var objective : sidebarObjectives) {
            var height = multiScoreboard$getObjectiveContentRenderHeight(scoreboard, objective) + 20;
            scoreboardHeights.put(objective, height);
            totalHeight += height;
        }

        scoreboardHeightsRef.set(scoreboardHeights);
        return totalHeight;
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            )
    )
    private void multiScoreboard$translateSidebarTeamObjective(InGameHud inGameHud, DrawContext context, ScoreboardObjective teamObjective, Operation<Void> op, @Share("scoreboardHeights") LocalRef<Map<ScoreboardObjective, Integer>> scoreboardHeightsRef) {
        var scoreboard = Objects.requireNonNull(client.world).getScoreboard();

        var teamScoreboardHeight = multiScoreboard$getObjectiveContentRenderHeight(scoreboard, teamObjective);
        var totalRestHeight = multiScoreboard$calculateSidebarHeights(scoreboard, scoreboardHeightsRef);

        context.getMatrices().push();
        context.getMatrices().translate(0, -totalRestHeight / 2f, 0);
        op.call(inGameHud, context, teamObjective);
        context.getMatrices().translate(0, teamScoreboardHeight + 20, 0);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
                    )
            )
    )
    private void multiScoreboard$renderSidebarObjectives(DrawContext context, float tickDelta, CallbackInfo ci, @Local(ordinal = 1) ScoreboardObjective teamObjective, @Share("scoreboardHeights") LocalRef<Map<ScoreboardObjective, Integer>> scoreboardHeightsRef) {
        if(scoreboardHeightsRef.get() == null) {
            var scoreboard = Objects.requireNonNull(client.world).getScoreboard();
            var totalHeight = multiScoreboard$calculateSidebarHeights(scoreboard, scoreboardHeightsRef);
            if(scoreboardHeightsRef.get().isEmpty()) return;

            var firstHeight = scoreboardHeightsRef.get().values().iterator().next();

            context.getMatrices().push();
            context.getMatrices().translate(0, (firstHeight-totalHeight) / 2f, 0);
        }

        for(var objective : scoreboardHeightsRef.get().entrySet()) {
            renderScoreboardSidebar(context, objective.getKey());
            context.getMatrices().translate(0, objective.getValue(), 0);
        }

        context.getMatrices().pop();
    }
}
