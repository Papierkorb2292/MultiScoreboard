package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.papierkorb2292.multiscoreboard.MultiScoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Unique
    private static final ThreadLocal<Boolean> multiScoreboard$useThreadedCommandForValidateMessageCallback = new ThreadLocal<>();

    @WrapOperation(
            method = {
                    "handleChatCommand",
                    "handleSignedChatCommand"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryHandleChat(Ljava/lang/String;ZLjava/lang/Runnable;)V"
            )
    )
    private void multiScoreboard$findThreadedCommands(ServerGamePacketListenerImpl instance, String command, boolean bl, Runnable callback, Operation<Void> original) {
        try {
            for(var threadedCommand : MultiScoreboard.THREADED_COMMANDS) {
                if (command.startsWith(threadedCommand)) {
                    multiScoreboard$useThreadedCommandForValidateMessageCallback.set(true);
                    break;
                }
            }
            original.call(instance, command, bl, callback);
        } finally {
            multiScoreboard$useThreadedCommandForValidateMessageCallback.remove();
        }
    }

    @WrapOperation(
            method = "tryHandleChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V"
            )
    )
    private void multiScoreboard$runThreadedCommand(MinecraftServer instance, Runnable runnable, Operation<Void> original) {
        if(Boolean.TRUE.equals(multiScoreboard$useThreadedCommandForValidateMessageCallback.get())) {
            MultiScoreboard.THREADED_COMMAND_EXECUTOR.execute(runnable);
            return;
        }
        original.call(instance, runnable);
    }
}
