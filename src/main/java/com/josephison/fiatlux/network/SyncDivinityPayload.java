package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.divinity.DivineState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Clientbound: the player's divine state plus world mode/glory, for the HUD and power wheel. */
public record SyncDivinityPayload(DivineState state, PlayMode mode, long glory, int tier, float maxEssence)
        implements CustomPacketPayload {
    public static final Type<SyncDivinityPayload> TYPE = new Type<>(FiatLux.id("sync_divinity"));
    private static final StreamCodec<ByteBuf, PlayMode> MODE_CODEC =
            ByteBufCodecs.idMapper(i -> PlayMode.values()[i], PlayMode::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDivinityPayload> STREAM_CODEC = StreamCodec.composite(
            DivineState.STREAM_CODEC, SyncDivinityPayload::state,
            MODE_CODEC.cast(), SyncDivinityPayload::mode,
            ByteBufCodecs.VAR_LONG.cast(), SyncDivinityPayload::glory,
            ByteBufCodecs.VAR_INT.cast(), SyncDivinityPayload::tier,
            ByteBufCodecs.FLOAT.cast(), SyncDivinityPayload::maxEssence,
            SyncDivinityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
