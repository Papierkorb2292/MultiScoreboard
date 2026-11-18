package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import net.papierkorb2292.multiscoreboard.MultiScoreboard;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
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

@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow
    public abstract Font getFont();

    @WrapOperation(
            method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V"
            )
    )
    private void multiScoreboard$translateSidebarTeamObjective(Hud inGameHud, GuiGraphicsExtractor context, Objective teamObjective, Operation<Void> op, @Share("sidebarHeights") LocalRef<Map<SidebarRenderable, Integer>> scoreboardHeightsRef) {
        if(!MultiScoreboardClient.useMultiScoreboard()) {
            op.call(inGameHud, context, teamObjective);
            return;
        }
        var scoreboard = Objects.requireNonNull(minecraft.level).getScoreboard();

        var teamScoreboardHeight = new SidebarObjectiveRenderable(
                teamObjective,
                false
        ).calculateHeight() + MultiScoreboardClient.sidebarGap;
        var calculatedHeights = MultiScoreboardClient.calculateSidebarHeights(scoreboard, null);
        var totalRestHeight = calculatedHeights.getFirst();
        scoreboardHeightsRef.set(calculatedHeights.getSecond());

        context.pose().pushMatrix();
        //noinspection IntegerDivisionInFloatingPointContext
        context.pose().translate(0, Minecraft.getInstance().font.lineHeight -(teamScoreboardHeight + totalRestHeight)/2 + MultiScoreboardClient.getSidebarScrollTranslation() + MultiScoreboardClient.getScoreboardPositionYOffset(teamScoreboardHeight + totalRestHeight));
        op.call(inGameHud, context, teamObjective);
        context.pose().translate(0, teamScoreboardHeight);
    }

    @Inject(
            method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL")
    )
    private void multiScoreboard$renderSidebarObjectives(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci, @Local(ordinal = 1) Objective teamObjective, @Share("sidebarHeights") LocalRef<Map<SidebarRenderable, Integer>> sidebarHeightsRef) {
        if(!MultiScoreboardClient.useMultiScoreboard()) return;
        var noTeamScoreboard = sidebarHeightsRef.get() == null;
        var totalHeight = 0;
        if(noTeamScoreboard) {
            var scoreboard = Objects.requireNonNull(minecraft.level).getScoreboard();
            var calculatedHeights = MultiScoreboardClient.calculateSidebarHeights(scoreboard, null);
            totalHeight = calculatedHeights.getFirst();
            sidebarHeightsRef.set(calculatedHeights.getSecond());
        }
        if(sidebarHeightsRef.get().isEmpty()) {
            if(!noTeamScoreboard)
                context.pose().popMatrix();
            return;
        }

        var sorted = sidebarHeightsRef.get().entrySet().stream()
                .sorted(Comparator.comparing(renderable -> renderable.getKey().getSortName()))
                .iterator();

        if(noTeamScoreboard) {
            context.pose().pushMatrix();
            //noinspection IntegerDivisionInFloatingPointContext
            context.pose().translate(0, Minecraft.getInstance().font.lineHeight -totalHeight/2);
            context.pose().translate(0, MultiScoreboardClient.getSidebarScrollTranslation() + MultiScoreboardClient.getScoreboardPositionYOffset(totalHeight));
        }
        while(sorted.hasNext()) {
            var renderable = sorted.next();
            renderable.getKey().render(context, (Hud)(Object)this, tickCounter);
            context.pose().translate(0, renderable.getValue() + MultiScoreboardClient.sidebarGap);
        }

        context.pose().popMatrix();
    }

    @ModifyExpressionValue(
            method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/Scoreboard;getDisplayObjective(Lnet/minecraft/world/scores/DisplaySlot;)Lnet/minecraft/world/scores/Objective;",
                    ordinal = 1
            )
    )
    private Objective multiScoreboard$referGetSidebarObjectiveToMultiScoreboard(Objective original) {
        return original != null || MultiScoreboardClient.useMultiScoreboard()
                ? original
                : ((MultiScoreboardSidebarInterface)Objects.requireNonNull(minecraft.level).getScoreboard())
                    .multiScoreboard$getSidebarObjectives().stream().findAny().orElse(null);
    }

    @ModifyExpressionValue(
            method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(
                    value = "CONSTANT",
                    args = "intValue=9",
                    ordinal = 0
            )
    )
    private int multiScoreboard$removeSidebarHeightOffset(int fontHeight) {
        return MultiScoreboardClient.useMultiScoreboard() ? fontHeight * 3 : fontHeight;
    }

    @ModifyVariable(
            method = "displayScoreboardSidebar",
            at = @At("STORE:FIRST"),
            name = "biggestWidth"
    )
    private int multiScoreboard$applyAdditionalScoreboardMaxWidth(int maxWidth) {
        if(SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.get() != null) {
            maxWidth = Math.max(SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.get(), maxWidth);
            SidebarObjectiveRenderable.CURRENT_MAX_WIDTH.set(maxWidth);
        }
        return maxWidth;
    }

    @ModifyVariable(
            method = "displayScoreboardSidebar",
            at = @At("STORE:FIRST"),
            name = "left"
    )
    private int multiScoreboard$applyScoreboardPositionLeftX(int original, @Local(name = "biggestWidth") int maxWidth) {
        if(!MultiScoreboardClient.useMultiScoreboard())
            return original;
        return MultiScoreboardClient.getScoreboardXPosition(maxWidth, 1, 2);
    }

    @ModifyVariable(
            method = "displayScoreboardSidebar",
            at = @At("STORE:FIRST"),
            name = "right"
    )
    private int multiScoreboard$applyScoreboardPositionRightX(int original, @Local(name = "biggestWidth") int maxWidth) {
        if(!MultiScoreboardClient.useMultiScoreboard())
            return original;
        return MultiScoreboardClient.getScoreboardXPosition(maxWidth, 1, 2) + maxWidth + 2;
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE:LAST",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
            )
    )
    private void multiScoreboard$applyBackgroundColorAnimation(GuiGraphicsExtractor extractor, int x0, int y0, int x1, int y1, int col, Operation<Void> op) {
        op.call(extractor, x0, y0, x1, y1, col);
        final var animationContext = SidebarObjectiveRenderable.OBJECTIVE_ADD_ANIMATION_CONTEXT.get();
        if(animationContext != null) {
            animationContext.getFirst().outlineAddedSidebar(extractor, x0, y0 - getFont().lineHeight, x1, y1, null, (Hud)(Object)this, animationContext.getSecond());
        }
    }
}
