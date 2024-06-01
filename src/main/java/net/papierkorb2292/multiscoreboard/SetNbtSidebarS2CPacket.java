package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SetNbtSidebarS2CPacket(String nbtSidebarName, List<NbtElement> nbt) implements CustomPayload {
    public static final Id<SetNbtSidebarS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(MultiScoreboard.MOD_ID, "set_nbt_sidebar"));
    public static final Type<? super RegistryByteBuf, SetNbtSidebarS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, PacketCodec.tuple(
            PacketCodecs.STRING,
            SetNbtSidebarS2CPacket::nbtSidebarName,
            PacketCodecs.collection(ArrayList::new, PacketCodecs.NBT_ELEMENT),
            SetNbtSidebarS2CPacket::nbt,
            SetNbtSidebarS2CPacket::new
    ));

    @Override
    public Id<SetNbtSidebarS2CPacket> getId() {
        return ID;
    }
}
