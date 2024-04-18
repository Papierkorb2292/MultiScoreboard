package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import net.papierkorb2292.multiscoreboard.ToggleSingleScoreSidebarS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(
            method = "sendScoreboard",
            at = @At("HEAD")
    )
    private void multiScoreboard$resetClientSidebarObjectives(ServerScoreboard scoreboard, ServerPlayerEntity player, CallbackInfo ci) {
        player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null));
    }

    @Inject(
            method = "sendScoreboard",
            at = @At("TAIL")
    )
    private void multiScoreboard$sendSidebarObjectives(ServerScoreboard scoreboard, ServerPlayerEntity player, CallbackInfo ci, @Local Set<ScoreboardObjective> sentObjectives) {
        var sidebarObjectives = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives();
        for(var objective : sidebarObjectives) {
            if(!sentObjectives.contains(objective)) {
                for(var packet : scoreboard.createChangePackets(objective)) {
                    player.networkHandler.sendPacket(packet);
                }
                sentObjectives.add(objective);
            }
        }

        var singleScoreSidebars = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSingleScoreSidebars();
        for(var entry : singleScoreSidebars.entrySet()) {
            var objective = entry.getKey();
            if(!sentObjectives.contains(objective)) {
                for(var packet : scoreboard.createChangePackets(objective)) {
                    player.networkHandler.sendPacket(packet);
                }
                sentObjectives.add(objective);
            }
            for(var scoreHolder : entry.getValue()) {
                ServerPlayNetworking.send(player, new ToggleSingleScoreSidebarS2CPacket(objective.getName(), scoreHolder));
            }
        }
    }
}
