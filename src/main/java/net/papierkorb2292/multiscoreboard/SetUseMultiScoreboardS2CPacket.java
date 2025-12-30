package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetUseMultiScoreboardS2CPacket(boolean useMultiScoreboard) implements CustomPacketPayload {
    public static final Type<SetUseMultiScoreboardS2CPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiScoreboard.MOD_ID, "use_multi_scoreboard"));
    public static final TypeAndCodec<? super RegistryFriendlyByteBuf, SetUseMultiScoreboardS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, ByteBufCodecs.BOOL.map(SetUseMultiScoreboardS2CPacket::new, SetUseMultiScoreboardS2CPacket::useMultiScoreboard));

    @Override
    public Type<SetUseMultiScoreboardS2CPacket> type() {
        return ID;
    }
}
