package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class RemoveNbtSidebarS2CPacket implements FabricPacket {
    public static final PacketType<RemoveNbtSidebarS2CPacket> TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "remove_nbt_sidebar"), packet -> {
        return new RemoveNbtSidebarS2CPacket(packet.readString());
    });

    public String nbtSidebarName;

    public RemoveNbtSidebarS2CPacket(String nbtSidebarName) {
        this.nbtSidebarName = nbtSidebarName;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(nbtSidebarName);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}