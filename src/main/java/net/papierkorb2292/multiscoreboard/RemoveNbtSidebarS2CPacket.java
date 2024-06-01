package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RemoveNbtSidebarS2CPacket(String nbtSidebarName) implements CustomPayload {
    public static final Id<RemoveNbtSidebarS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(MultiScoreboard.MOD_ID, "remove_nbt_sidebar"));
    public static final Type<? super RegistryByteBuf, RemoveNbtSidebarS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, PacketCodecs.STRING.xmap(RemoveNbtSidebarS2CPacket::new, RemoveNbtSidebarS2CPacket::nbtSidebarName));

    @Override
    public Id<RemoveNbtSidebarS2CPacket> getId() {
        return ID;
    }
}