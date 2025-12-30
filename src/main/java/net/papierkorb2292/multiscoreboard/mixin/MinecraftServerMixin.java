package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.papierkorb2292.multiscoreboard.CustomSidebarPacked;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManager;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManagerContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements ServerNbtSidebarManagerContainer  {

    @Shadow
    @Final
    private ServerScoreboard scoreboard;
    @Unique
    private ServerNbtSidebarManager multiScoreboard$nbtSidebarManager = null;

    @Override
    public ServerNbtSidebarManager multiScoreboard$getNbtSidebarManager() {
        return multiScoreboard$nbtSidebarManager;
    }

    @ModifyExpressionValue(
            method = "createLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getDataStorage()Lnet/minecraft/world/level/storage/DimensionDataStorage;"
            )
    )
    private DimensionDataStorage multiScoreboard$initNbtSideManager(DimensionDataStorage persistentStateManager) {
        //noinspection DataFlowIssue
        multiScoreboard$nbtSidebarManager = persistentStateManager.computeIfAbsent(ServerNbtSidebarManager.getPersistentStateType((MinecraftServer)(Object)this));
        return persistentStateManager;
    }

    @Inject(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"
            )
    )
    private void multiScoreboard$tickNbtSidebarManager(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if(multiScoreboard$nbtSidebarManager != null)
            multiScoreboard$nbtSidebarManager.tick();
    }

    @ModifyArg(
            method = "createLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/CommandStorage;<init>(Lnet/minecraft/world/level/storage/DimensionDataStorage;)V"
            )
    )
    private DimensionDataStorage multiScoreboard$readCustomSidebarPacked(DimensionDataStorage persistentStateManager) {
        ((CustomSidebarPacked.Container)scoreboard).multiScoreboard$setCustomSidebarPacked(
                ((CustomSidebarPacked.Container)persistentStateManager.computeIfAbsent(ScoreboardSaveData.TYPE)).multiScoreboard$getCustomSidebarPacked()
        );
        return persistentStateManager;
    }
}
