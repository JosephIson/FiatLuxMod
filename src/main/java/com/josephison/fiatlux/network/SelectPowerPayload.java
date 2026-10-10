package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound: the player chose a power on the power wheel. */
public record SelectPowerPayload(ResourceLocation power) implements CustomPacketPayload {
    public static final Type<SelectPowerPayload> TYPE = new Type<>(FiatLux.id("select_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPowerPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(SelectPowerPayload::new, SelectPowerPayload::power).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
