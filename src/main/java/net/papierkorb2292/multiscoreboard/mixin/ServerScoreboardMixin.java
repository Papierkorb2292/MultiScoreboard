package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardState;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.world.PersistentState;
import net.papierkorb2292.multiscoreboard.MultiScoreboardSidebarInterface;
import net.papierkorb2292.multiscoreboard.ToggleSingleScoreSidebarS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin extends ScoreboardMixin {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private Set<ScoreboardObjective> syncableObjectives;

    @Shadow protected abstract void runUpdateListeners();

    @Shadow public abstract void startSyncing(ScoreboardObjective objective);

    @Shadow public abstract int countDisplaySlots(ScoreboardObjective objective);

    @Shadow public abstract void stopSyncing(ScoreboardObjective objective);

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
        runUpdateListeners();
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
        runUpdateListeners();
        for(var player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new ToggleSingleScoreSidebarS2CPacket(objective.getName(), scoreHolder));
        }
        return added;
    }

    @ModifyReturnValue(
            method = "method_67325",
            at = @At("RETURN")
    )
    private static Codec<ScoreboardState> multiScoreboard$addSidebarsToCodec(Codec<ScoreboardState> original, PersistentState.Context context) {
        var scoreboard = context.getWorldOrThrow().getScoreboard();
        Codec<ScoreboardObjective> objectiveNameCodec = Codec.STRING.xmap(
                scoreboard::getNullableObjective,
                ScoreboardObjective::getName
        );
        Codec<Unit> objectiveSidebarCodec = objectiveNameCodec.listOf().xmap(
                objectives -> {
                    for (var objective : objectives)
                        if (objective != null)
                            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
                    return Unit.INSTANCE;
                },
                unit -> new ArrayList<>(((MultiScoreboardSidebarInterface) scoreboard).multiScoreboard$getSidebarObjectives())
        );
        Codec<Unit> singleScoreSidebarCodec = Codec.unboundedMap(
                objectiveNameCodec,
                Codec.STRING.listOf().<Set<String>>xmap(
                        HashSet::new,
                        ArrayList::new
                )
        ).xmap(map -> {
            for(var entry : map.entrySet()) {
                var objective = entry.getKey();
                if(objective == null) continue;
                for(String name : entry.getValue())
                    ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$toggleSingleScoreSidebar(objective, name);
            }
            return Unit.INSTANCE;
        }, unit -> ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSingleScoreSidebars());
        Codec<Unit> customDataCodec = RecordCodecBuilder.create(instance -> instance.group(
                objectiveSidebarCodec.fieldOf("SidebarSlotObjectives").forGetter(state -> Unit.INSTANCE),
                singleScoreSidebarCodec.fieldOf("SingleScoreSidebars").forGetter(state -> Unit.INSTANCE)
        ).apply(instance, (_void1, _void2) -> Unit.INSTANCE));
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<ScoreboardState, T>> decode(DynamicOps<T> ops, T input) {
                return original.decode(ops, input).flatMap(result ->
                        customDataCodec.decode(ops, input).map(_void -> result));
            }

            @Override
            public <T> DataResult<T> encode(ScoreboardState input, DynamicOps<T> ops, T prefix) {
                return original.encode(input, ops, prefix).flatMap(result ->
                        customDataCodec.encode(Unit.INSTANCE, ops, result));
            }
        };
    }
}
