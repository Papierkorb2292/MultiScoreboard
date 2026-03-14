package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
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
    @Shadow
    @Final
    private SavedDataStorage savedDataStorage;
    @Unique
    private ServerNbtSidebarManager multiScoreboard$nbtSidebarManager = null;

    @Override
    public ServerNbtSidebarManager multiScoreboard$getNbtSidebarManager() {
        return multiScoreboard$nbtSidebarManager;
    }

    @Inject(
            method = "createLevels",
            at = @At("HEAD")
    )
    private SavedDataStorage multiScoreboard$initNbtSideManager(CallbackInfo ci) {
        //noinspection DataFlowIssue
        multiScoreboard$nbtSidebarManager = savedDataStorage.computeIfAbsent(ServerNbtSidebarManager.getPersistentStateType((MinecraftServer)(Object)this));
        return savedDataStorage;
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
                    target = "Lnet/minecraft/world/level/storage/CommandStorage;<init>(Lnet/minecraft/world/level/storage/SavedDataStorage;)V"
            )
    )
    private SavedDataStorage multiScoreboard$readCustomSidebarPacked(SavedDataStorage persistentStateManager) {
        ((CustomSidebarPacked.Container)scoreboard).multiScoreboard$setCustomSidebarPacked(
                ((CustomSidebarPacked.Container)persistentStateManager.computeIfAbsent(ScoreboardSaveData.TYPE)).multiScoreboard$getCustomSidebarPacked()
        );
        return persistentStateManager;
    }
}
