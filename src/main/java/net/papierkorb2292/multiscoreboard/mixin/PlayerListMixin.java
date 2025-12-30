package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import net.papierkorb2292.multiscoreboard.ToggleSingleScoreSidebarS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(
            method = "updateEntireScoreboard",
            at = @At("HEAD")
    )
    private void multiScoreboard$resetClientSidebarObjectives(ServerScoreboard scoreboard, ServerPlayer player, CallbackInfo ci) {
        player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
    }

    @Inject(
            method = "updateEntireScoreboard",
            at = @At("TAIL")
    )
    private void multiScoreboard$sendSidebarObjectives(ServerScoreboard scoreboard, ServerPlayer player, CallbackInfo ci, @Local Set<Objective> sentObjectives) {
        var sidebarObjectives = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives();
        for(var objective : sidebarObjectives) {
            if(!sentObjectives.contains(objective)) {
                for(var packet : scoreboard.getStartTrackingPackets(objective)) {
                    player.connection.send(packet);
                }
                sentObjectives.add(objective);
            }
        }

        var singleScoreSidebars = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSingleScoreSidebars();
        for(var entry : singleScoreSidebars.entrySet()) {
            var objective = entry.getKey();
            if(!sentObjectives.contains(objective)) {
                for(var packet : scoreboard.getStartTrackingPackets(objective)) {
                    player.connection.send(packet);
                }
                sentObjectives.add(objective);
            }
            for(var scoreHolder : entry.getValue()) {
                ServerPlayNetworking.send(player, new ToggleSingleScoreSidebarS2CPacket(objective.getName(), scoreHolder));
            }
        }
    }
}
