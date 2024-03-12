package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SetUseMultiScoreboardS2CPacket implements FabricPacket {
    public static final PacketType<SetUseMultiScoreboardS2CPacket> TYPE = PacketType.create(new Identifier(MultiScoreboard.MOD_ID, "use_multi_scoreboard"), packet -> {
        return new SetUseMultiScoreboardS2CPacket(packet.readBoolean());
    });

    public boolean useMultiScoreboard;

    public SetUseMultiScoreboardS2CPacket(boolean useMultiScoreboard) {
        this.useMultiScoreboard = useMultiScoreboard;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(useMultiScoreboard);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
