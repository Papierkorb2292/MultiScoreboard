package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.server.MinecraftServer;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManagerContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "net.papierkorb2292.command_crafter.editor.debugger.server.PauseContext", remap = false)
public class PauseContextMixin {

    @Shadow
    public MinecraftServer server;

    @Inject(
            method = "suspend",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/papierkorb2292/command_crafter/mixin/MinecraftServerAccessor;setTickStartTimeNanos(J)V"
            )
    )
    private void tickNbtSidebarManager(Supplier<Throwable> executionPausedThrowable, CallbackInfo ci) {
        final var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)server).multiScoreboard$getNbtSidebarManager();
        if(nbtSidebarManager != null)
            nbtSidebarManager.tick();
    }
}