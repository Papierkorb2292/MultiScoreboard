package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
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
    private void multiScoreboard$readSidebarObjectivesNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfoReturnable<ScoreboardState> cir) {
        var list = nbt.getList("SidebarSlotObjectives", NbtElement.STRING_TYPE);
        if(list == null) return;

        for(NbtElement element : list) {
            var objective = scoreboard.getNullableObjective(element.asString());
            if(objective != null) {
                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
            }
        }
    }

    @Inject(
            method = "writeDisplaySlotsNbt",
            at = @At("HEAD")
    )
    private void multiScoreboard$writeSidebarObjectivesNbt(NbtCompound nbt, CallbackInfo ci) {
        var list = new NbtList();

        for(var objective : ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives()) {
            list.add(NbtString.of(objective.getName()));
        }

        nbt.put("SidebarSlotObjectives", list);
    }
}
