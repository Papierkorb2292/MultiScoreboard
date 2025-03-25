package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManager;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManagerContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements ServerNbtSidebarManagerContainer  {

    @Unique
    private ServerNbtSidebarManager multiScoreboard$nbtSidebarManager = null;

    @Override
    public ServerNbtSidebarManager multiScoreboard$getNbtSidebarManager() {
        return multiScoreboard$nbtSidebarManager;
    }

    @ModifyArg(
            method = "createWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;initScoreboard(Lnet/minecraft/world/PersistentStateManager;)V"
            )
    )
    private PersistentStateManager multiScoreboard$initNbtSideManager(PersistentStateManager persistentStateManager) {
        //noinspection DataFlowIssue
        multiScoreboard$nbtSidebarManager = persistentStateManager.getOrCreate(ServerNbtSidebarManager.getPersistentStateType((MinecraftServer)(Object)this));
        return persistentStateManager;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V"
            )
    )
    private void multiScoreboard$tickNbtSidebarManager(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if(multiScoreboard$nbtSidebarManager != null)
            multiScoreboard$nbtSidebarManager.tick();
    }
}
