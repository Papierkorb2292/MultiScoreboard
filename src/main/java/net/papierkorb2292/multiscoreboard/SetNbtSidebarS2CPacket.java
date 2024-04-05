package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SetNbtSidebarS2CPacket implements FabricPacket {
    public static final PacketType<SetNbtSidebarS2CPacket> TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "set_nbt_sidebar"), packet -> {
        var name = packet.readString();
        var nbtSize = packet.readVarInt();
        var nbtList = new ArrayList<NbtElement>(nbtSize);
        for (int i = 0; i < nbtSize; i++) {
            nbtList.add(packet.readNbt(NbtTagSizeTracker.of(0x200000L)));
        }
        return new SetNbtSidebarS2CPacket(name, nbtList);
    });

    public String nbtSidebarName;
    public List<NbtElement> nbt;

    public SetNbtSidebarS2CPacket(String nbtSidebarName, List<NbtElement> nbt) {
        this.nbtSidebarName = nbtSidebarName;
        this.nbt = nbt;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(nbtSidebarName);
        buf.writeVarInt(nbt.size());
        for (NbtElement nbtElement : nbt) {
            buf.writeNbt(nbtElement);
        }
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
