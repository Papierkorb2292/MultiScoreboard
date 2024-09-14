package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record RemoveNbtSidebarS2CPacket(String nbtSidebarName) implements FabricPacket {
    public static PacketType<RemoveNbtSidebarS2CPacket> PACKET_TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "remove_nbt_sidebar"), RemoveNbtSidebarS2CPacket::read);

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(nbtSidebarName);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }

    public static RemoveNbtSidebarS2CPacket read(PacketByteBuf buf) {
        return new RemoveNbtSidebarS2CPacket(buf.readString());
    }
}