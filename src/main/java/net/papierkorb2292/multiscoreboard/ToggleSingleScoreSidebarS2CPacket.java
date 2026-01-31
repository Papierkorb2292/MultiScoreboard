package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleSingleScoreSidebarS2CPacket(String objective, String score) implements CustomPacketPayload {
    public static final Type<ToggleSingleScoreSidebarS2CPacket> ID = new Type<>(Identifier.fromNamespaceAndPath(MultiScoreboard.MOD_ID, "toggle_single_score_sidebar"));
    public static final TypeAndCodec<? super RegistryFriendlyByteBuf, ToggleSingleScoreSidebarS2CPacket> TYPE = PayloadTypeRegistry.clientboundPlay().register(ID, StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ToggleSingleScoreSidebarS2CPacket::objective,
            ByteBufCodecs.STRING_UTF8,
            ToggleSingleScoreSidebarS2CPacket::score,
            ToggleSingleScoreSidebarS2CPacket::new
    ));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
