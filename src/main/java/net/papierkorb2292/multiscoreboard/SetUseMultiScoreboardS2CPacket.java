package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetUseMultiScoreboardS2CPacket(boolean useMultiScoreboard) implements CustomPayload {
    public static final Id<SetUseMultiScoreboardS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(MultiScoreboard.MOD_ID, "use_multi_scoreboard"));
    public static final Type<? super RegistryByteBuf, SetUseMultiScoreboardS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, PacketCodecs.BOOLEAN.xmap(SetUseMultiScoreboardS2CPacket::new, SetUseMultiScoreboardS2CPacket::useMultiScoreboard));

    @Override
    public Id<SetUseMultiScoreboardS2CPacket> getId() {
        return ID;
    }
}
