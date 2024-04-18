package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleSingleScoreSidebarS2CPacket(String objective, String score) implements CustomPayload {
    public static final Id<ToggleSingleScoreSidebarS2CPacket> ID = new Id<>(new Identifier(MultiScoreboard.MOD_ID, "toggle_single_score_sidebar"));
    public static final Type<? super RegistryByteBuf, ToggleSingleScoreSidebarS2CPacket> TYPE = PayloadTypeRegistry.playS2C().register(ID, PacketCodec.tuple(
            PacketCodecs.STRING,
            ToggleSingleScoreSidebarS2CPacket::objective,
            PacketCodecs.STRING,
            ToggleSingleScoreSidebarS2CPacket::score,
            ToggleSingleScoreSidebarS2CPacket::new
    ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
