package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record ToggleSingleScoreSidebarS2CPacket(String objective, String score) implements FabricPacket {
    public static final PacketType<ToggleSingleScoreSidebarS2CPacket> PACKET_TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "toggle_single_score_sidebar"), ToggleSingleScoreSidebarS2CPacket::read);

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(objective);
        buf.writeString(score);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }

    public static ToggleSingleScoreSidebarS2CPacket read(PacketByteBuf buf) {
        return new ToggleSingleScoreSidebarS2CPacket(buf.readString(), buf.readString());
    }
}
