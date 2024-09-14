package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.List;

public record SetNbtSidebarS2CPacket(String nbtSidebarName, List<NbtElement> nbt) implements FabricPacket {
    public static PacketType<SetNbtSidebarS2CPacket> PACKET_TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "set_nbt_sidebar"), SetNbtSidebarS2CPacket::read);

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(nbtSidebarName);
        buf.writeCollection(nbt, PacketByteBuf::writeNbt);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }

    public static SetNbtSidebarS2CPacket read(PacketByteBuf buf) {
        return new SetNbtSidebarS2CPacket(buf.readString(), buf.readList(nbtBuf -> nbtBuf.readNbt(NbtSizeTracker.of(2097152L))));
    }
}
