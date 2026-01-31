package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SetNbtSidebarS2CPacket(String nbtSidebarName, List<Tag> nbt) implements CustomPacketPayload {
    public static final Type<SetNbtSidebarS2CPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiScoreboard.MOD_ID, "set_nbt_sidebar"));
    public static final TypeAndCodec<? super RegistryFriendlyByteBuf, SetNbtSidebarS2CPacket> TYPE = PayloadTypeRegistry.clientboundPlay().register(ID, StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SetNbtSidebarS2CPacket::nbtSidebarName,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.TAG),
            SetNbtSidebarS2CPacket::nbt,
            SetNbtSidebarS2CPacket::new
    ));

    @Override
    public Type<SetNbtSidebarS2CPacket> type() {
        return ID;
    }
}
