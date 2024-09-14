package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardState;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardState.class)
public class ScoreboardStateMixin {

    @Shadow @Final private Scoreboard scoreboard;

    @Inject(
            method = "readNbt",
            at = @At("TAIL")
    )
    private void multiScoreboard$readSidebarObjectivesNbt(NbtCompound nbt, CallbackInfoReturnable<ScoreboardState> cir) {
        var sidebarSlotObjectives = nbt.getList("SidebarSlotObjectives", NbtElement.STRING_TYPE);
        if(sidebarSlotObjectives != null) {
            for (NbtElement element : sidebarSlotObjectives) {
                var objective = scoreboard.getNullableObjective(element.asString());
                if (objective != null) {
                    scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
                }
            }
        }

        var singleScoreSidebarsNbt = nbt.getCompound("SingleScoreSidebars");
        if(singleScoreSidebarsNbt != null) {
            for(var entry : singleScoreSidebarsNbt.getKeys()) {
                var objective = scoreboard.getNullableObjective(entry);
                if(objective == null) continue;
                var scoreHolders = singleScoreSidebarsNbt.getList(entry, NbtElement.STRING_TYPE);
                for(NbtElement element : scoreHolders) {
                    ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$toggleSingleScoreSidebar(objective, element.asString());
                }
            }
        }
    }

    @Inject(
            method = "writeDisplaySlotsNbt",
            at = @At("HEAD")
    )
    private void multiScoreboard$writeSidebarObjectivesNbt(NbtCompound nbt, CallbackInfo ci) {
        var sidebarSlotObjectives = new NbtList();
        for(var objective : ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives()) {
            sidebarSlotObjectives.add(NbtString.of(objective.getName()));
        }
        nbt.put("SidebarSlotObjectives", sidebarSlotObjectives);

        var singleScoreSidebars = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSingleScoreSidebars();
        var singleScoreSidebarsNbt = new NbtCompound();
        for(var entry : singleScoreSidebars.entrySet()) {
            var scoreHolders = new NbtList();
            for(var scoreHolder : entry.getValue()) {
                scoreHolders.add(NbtString.of(scoreHolder));
            }
            singleScoreSidebarsNbt.put(entry.getKey().getName(), scoreHolders);
        }
        nbt.put("SingleScoreSidebars", singleScoreSidebarsNbt);
    }
}
