package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record SetUseMultiScoreboardS2CPacket(boolean useMultiScoreboard) implements FabricPacket {
    public static final PacketType<SetUseMultiScoreboardS2CPacket> PACKET_TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "use_multi_scoreboard"), SetUseMultiScoreboardS2CPacket::read);

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(useMultiScoreboard);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }

    public static SetUseMultiScoreboardS2CPacket read(PacketByteBuf buf) {
        return new SetUseMultiScoreboardS2CPacket(buf.readBoolean());
    }
}
