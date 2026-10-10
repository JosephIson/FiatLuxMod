package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound intent: "cast this power where I am looking". The server ray-traces and validates. */
public record CastPowerPayload(ResourceLocation power) implements CustomPacketPayload {
    public static final Type<CastPowerPayload> TYPE = new Type<>(FiatLux.id("cast_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastPowerPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(CastPowerPayload::new, CastPowerPayload::power).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
