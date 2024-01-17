package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(ServerScoreboard.class)
public class ServerScoreboardMixin extends ScoreboardMixin {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private Set<ScoreboardObjective> objectives;

    @ModifyReturnValue(
            method = "createChangePackets",
            at = @At("RETURN")
    )
    private List<Packet<?>> multiScoreboard$createChangePackets(List<Packet<?>> packets, ScoreboardObjective objective) {
        if(multiScoreboard$sidebarObjectives.contains(objective)) {
            packets.add(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective));
        }
        return packets;
    }

    @Inject(
            method = "setObjectiveSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/ServerScoreboard;runUpdateListeners()V"
            )
    )
    private void multiScoreboard$clearClientSidebarIfNecessary(ScoreboardDisplaySlot slot, ScoreboardObjective objective, CallbackInfo ci) {
        if(slot == ScoreboardDisplaySlot.SIDEBAR && objective == null) {
            this.server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null));
        }
    }

    @ModifyReturnValue(
            method = "getSlot",
            at = @At("RETURN")
    )
    private int multiScoreboard$adjustSlotCountForSidebar(int slot, ScoreboardObjective objective) {
        return multiScoreboard$sidebarObjectives.contains(objective) ? slot + 1 : slot;
    }

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective) {
        super.multiScoreboard$removeObjectiveFromSidebar(objective);
        this.server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective));
    }
}
