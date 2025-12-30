package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveNbtSidebarS2CPacket(String nbtSidebarName) implements CustomPacketPayload {
    public static final Type<RemoveNbtSidebarS2CPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiScoreboard.MOD_ID, "remove_nbt_sidebar"));
    public static final TypeAndCodec<? super RegistryFriendlyByteBuf, RemoveNbtSidebarS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, ByteBufCodecs.STRING_UTF8.map(RemoveNbtSidebarS2CPacket::new, RemoveNbtSidebarS2CPacket::nbtSidebarName));

    @Override
    public Type<RemoveNbtSidebarS2CPacket> type() {
        return ID;
    }
}