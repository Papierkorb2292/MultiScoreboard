package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.papierkorb2292.multiscoreboard.client.MultiScoreboardClient;
import net.papierkorb2292.multiscoreboard.client.SidebarObjectiveRenderable;
import net.papierkorb2292.multiscoreboard.client.SidebarRenderable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @WrapOperation(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            )
    )
    private void multiScoreboard$translateSidebarTeamObjective(InGameHud inGameHud, DrawContext context, ScoreboardObjective teamObjective, Operation<Void> op, @Share("sidebarHeights") LocalRef<Map<SidebarRenderable, Integer>> scoreboardHeightsRef) {
        if(!MultiScoreboardClient.useMultiScoreboard()) {
            op.call(inGameHud, context, teamObjective);
            return;
        }
        var scoreboard = Objects.requireNonNull(client.world).getScoreboard();

        var teamScoreboardHeight = new SidebarObjectiveRenderable(teamObjective).calculateHeight();
        var calculatedHeights = MultiScoreboardClient.calculateSidebarHeights(scoreboard, null);
        var totalRestHeight = calculatedHeights.getFirst();
        scoreboardHeightsRef.set(calculatedHeights.getSecond());

        context.getMatrices().push();
        context.getMatrices().translate(0, -totalRestHeight / 2f, 0);
        op.call(inGameHud, context, teamObjective);
        context.getMatrices().translate(0, teamScoreboardHeight, 0);
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("TAIL")
    )
    private void multiScoreboard$renderSidebarObjectives(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 1) ScoreboardObjective teamObjective, @Share("sidebarHeights") LocalRef<Map<SidebarRenderable, Integer>> sidebarHeightsRef) {
        if(!MultiScoreboardClient.useMultiScoreboard()) return;
        var noTeamScoreboard = sidebarHeightsRef.get() == null;
        var totalHeight = 0;
        if(noTeamScoreboard) {
            var scoreboard = Objects.requireNonNull(client.world).getScoreboard();
            var calculatedHeights = MultiScoreboardClient.calculateSidebarHeights(scoreboard, null);
            totalHeight = calculatedHeights.getFirst();
            sidebarHeightsRef.set(calculatedHeights.getSecond());
            context.getMatrices().push();
        }
        if(sidebarHeightsRef.get().isEmpty()) return;

        var sorted = sidebarHeightsRef.get().entrySet().stream()
                .sorted(Comparator.comparing(renderable -> renderable.getKey().getSortName()))
                .iterator();

        if(noTeamScoreboard) {
            //noinspection IntegerDivisionInFloatingPointContext
            context.getMatrices().translate(0, MinecraftClient.getInstance().textRenderer.fontHeight-totalHeight/2, 0);
        }
        context.getMatrices().translate(0, MultiScoreboardClient.getSidebarScrollTranslation(), 0);
        while(sorted.hasNext()) {
            var renderable = sorted.next();
            renderable.getKey().render(context, (InGameHud)(Object)this);
            context.getMatrices().translate(0, renderable.getValue() + MultiScoreboardClient.sidebarGap, 0);
        };

        context.getMatrices().pop();
    }

    @ModifyExpressionValue(
            method = "method_55440",
            at = @At(
                    value = "CONSTANT",
                    args = "intValue=9",
                    ordinal = 0
            )
    )
    private int command_crafter$removeSidebarHeightOffset(int fontHeight) {
        return MultiScoreboardClient.useMultiScoreboard() ? fontHeight * 3 : fontHeight;
    }

    @ModifyVariable(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("LOAD:LAST"),
            ordinal = 1
    )
    private int command_crafter$applyAdditionalScoreboardMaxWidth(int maxWidth) {
        if(SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.get() != null) {
            maxWidth = Math.max(SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.get(), maxWidth);
            SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.set(maxWidth);
        }
        return maxWidth;
    }
}
