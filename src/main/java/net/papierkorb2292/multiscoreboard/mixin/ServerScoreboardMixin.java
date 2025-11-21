package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardState;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.papierkorb2292.multiscoreboard.CustomSidebarPacked;
import net.papierkorb2292.multiscoreboard.MultiScoreboardRecordRecorder;
import net.papierkorb2292.multiscoreboard.ToggleSingleScoreSidebarS2CPacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin extends ScoreboardMixin {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private Set<ScoreboardObjective> syncableObjectives;

    @Shadow public abstract void startSyncing(ScoreboardObjective objective);

    @Shadow public abstract int countDisplaySlots(ScoreboardObjective objective);

    @Shadow public abstract void stopSyncing(ScoreboardObjective objective);

    @Shadow
    protected abstract void markDirty();

    @Shadow
    public abstract void setObjectiveSlot(ScoreboardDisplaySlot slot, @Nullable ScoreboardObjective objective);

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
                    target = "Lnet/minecraft/scoreboard/ServerScoreboard;markDirty()V"
            )
    )
    private void multiScoreboard$clearClientSidebarIfNecessary(ScoreboardDisplaySlot slot, ScoreboardObjective objective, CallbackInfo ci) {
        if(slot == ScoreboardDisplaySlot.SIDEBAR && objective == null) {
            this.server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null));
        }
    }

    @ModifyReturnValue(
            method = "countDisplaySlots",
            at = @At("RETURN")
    )
    private int multiScoreboard$adjustSlotCountForSidebar(int slot, ScoreboardObjective objective) {
        if(multiScoreboard$sidebarObjectives.contains(objective)) slot++;
        if(multiScoreboard$singleScoreSidebars.containsKey(objective)) slot++;
        return slot;
    }

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(ScoreboardObjective objective) {
        super.multiScoreboard$removeObjectiveFromSidebar(objective);
        markDirty();
        this.server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective));
    }

    @Override
    public boolean multiScoreboard$toggleSingleScoreSidebar(ScoreboardObjective objective, String scoreHolder) {
        var added = super.multiScoreboard$toggleSingleScoreSidebar(objective, scoreHolder);
        if(added) {
            if(!syncableObjectives.contains(objective)) startSyncing(objective);
        } else {
            if (countDisplaySlots(objective) == 0) stopSyncing(objective);
        }
        markDirty();
        for(var player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new ToggleSingleScoreSidebarS2CPacket(objective.getName(), scoreHolder));
        }
        return added;
    }

    @Inject(
            method = "read",
            at = @At("RETURN")
    )
    private void multiScoreboard$readCustomSidebar(ScoreboardState.Packed packed, CallbackInfo ci) {
        final var customPacked = MultiScoreboardRecordRecorder.CUSTOM_SIDEBAR_KEY.getOrNull(packed);
        if(customPacked == null)
            return;

        for(final var objectiveName : customPacked.sidebarObjectives()) {
            final var objective = getNullableObjective(objectiveName);
            if (objective != null)
                setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
        }

        for(final var entry : customPacked.singleScoreSidebarObjectives().entrySet()) {
            final var objectiveName = entry.getKey();
            final var  objective = getNullableObjective(objectiveName);
            if(objective == null) continue;
            for(String name : entry.getValue())
                multiScoreboard$toggleSingleScoreSidebar(objective, name);
        }
    }

    @ModifyReturnValue(
            method = "toPacked",
            at = @At("RETURN")
    )
    private ScoreboardState.Packed multiScoreboard$packCustomSidebar(ScoreboardState.Packed original) {
        final var customPacked = new CustomSidebarPacked(
            multiScoreboard$sidebarObjectives.stream().map(ScoreboardObjective::getName).toList(),
            multiScoreboard$singleScoreSidebars.entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().getName(),
                    entry -> new ArrayList<>(entry.getValue())
            ))
        );
        return MultiScoreboardRecordRecorder.copyScoreboardStateWithCustomState(original, customPacked);
    }
}
