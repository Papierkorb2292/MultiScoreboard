package net.papierkorb2292.multiscoreboard;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class SyncFinishedS2CPacket implements CustomPacketPayload {
    public static final SyncFinishedS2CPacket INSTANCE = new SyncFinishedS2CPacket();
    public static final CustomPacketPayload.Type<SyncFinishedS2CPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MultiScoreboard.MOD_ID, "sync_finished"));
    public static final CustomPacketPayload.TypeAndCodec<? super RegistryFriendlyByteBuf, SyncFinishedS2CPacket> TYPE = PayloadTypeRegistry.clientboundPlay().register(ID, StreamCodec.unit(INSTANCE));

    @Override
    public CustomPacketPayload.Type<SyncFinishedS2CPacket> type() {
        return ID;
    }
}
