package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.papierkorb2292.multiscoreboard.CustomSidebarPacked;
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
public abstract class ServerScoreboardMixin extends ScoreboardMixin implements CustomSidebarPacked.Container {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private Set<Objective> trackedObjectives;

    @Shadow public abstract void startTrackingObjective(Objective objective);

    @Shadow public abstract int getObjectiveDisplaySlotCount(Objective objective);

    @Shadow public abstract void stopTrackingObjective(Objective objective);

    @Shadow
    protected abstract void setDirty();

    @Shadow
    public abstract void setDisplayObjective(DisplaySlot slot, @Nullable Objective objective);

    @ModifyReturnValue(
            method = "getStartTrackingPackets",
            at = @At("RETURN")
    )
    private List<Packet<?>> multiScoreboard$createChangePackets(List<Packet<?>> packets, Objective objective) {
        if(multiScoreboard$sidebarObjectives.contains(objective)) {
            packets.add(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
        }
        return packets;
    }

    @Inject(
            method = "setDisplayObjective",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/ServerScoreboard;setDirty()V"
            )
    )
    private void multiScoreboard$clearClientSidebarIfNecessary(DisplaySlot slot, Objective objective, CallbackInfo ci) {
        if(slot == DisplaySlot.SIDEBAR && objective == null) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
        }
    }

    @ModifyReturnValue(
            method = "getObjectiveDisplaySlotCount",
            at = @At("RETURN")
    )
    private int multiScoreboard$adjustSlotCountForSidebar(int slot, Objective objective) {
        if(multiScoreboard$sidebarObjectives.contains(objective)) slot++;
        if(multiScoreboard$singleScoreSidebars.containsKey(objective)) slot++;
        return slot;
    }

    @Override
    public void multiScoreboard$removeObjectiveFromSidebar(Objective objective) {
        super.multiScoreboard$removeObjectiveFromSidebar(objective);
        setDirty();
        this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
    }

    @Override
    public boolean multiScoreboard$toggleSingleScoreSidebar(Objective objective, String scoreHolder) {
        var added = super.multiScoreboard$toggleSingleScoreSidebar(objective, scoreHolder);
        setDirty();
        if(added) {
            if(!trackedObjectives.contains(objective)) startTrackingObjective(objective);
        } else {
            if (getObjectiveDisplaySlotCount(objective) == 0) stopTrackingObjective(objective);
        }
        setDirty();
        for(var player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, new ToggleSingleScoreSidebarS2CPacket(objective.getName(), scoreHolder));
        }
        return added;
    }

    @Inject(
            method = "storeToSaveDataIfDirty",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/ScoreboardSaveData;setData(Lnet/minecraft/world/scores/ScoreboardSaveData$Packed;)V"
            )
    )
    private void multiScoreboard$writeCustomSidebarPacked(ScoreboardSaveData state, CallbackInfo ci) {
        ((CustomSidebarPacked.Container)state).multiScoreboard$setCustomSidebarPacked(multiScoreboard$getCustomSidebarPacked());
    }

    @Override
    public void multiScoreboard$setCustomSidebarPacked(CustomSidebarPacked packed) {
        if(packed == null)
            return;

        for(final var objectiveName : packed.sidebarObjectives()) {
            final var objective = getObjective(objectiveName);
            if (objective != null)
                setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        }

        for(final var entry : packed.singleScoreSidebarObjectives().entrySet()) {
            final var objectiveName = entry.getKey();
            final var  objective = getObjective(objectiveName);
            if(objective == null) continue;
            for(String name : entry.getValue())
                multiScoreboard$toggleSingleScoreSidebar(objective, name);
        }
    }

    @Override
    public CustomSidebarPacked multiScoreboard$getCustomSidebarPacked() {
        return new CustomSidebarPacked(
            multiScoreboard$sidebarObjectives.stream().map(Objective::getName).toList(),
            multiScoreboard$singleScoreSidebars.entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().getName(),
                    entry -> new ArrayList<>(entry.getValue())
            ))
        );
    }
}
