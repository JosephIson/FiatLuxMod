package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Clientbound: a server-authored visual event. The client composes it from particles, beams,
 * shockwave rings and screen flashes (see client.fx.FxManager).
 *
 * @param kind     which effect (FxKind ordinal)
 * @param x,y,z    origin
 * @param radius   effect radius / beam height
 * @param color    ARGB tint
 * @param duration ticks
 */
public record FxPayload(int kind, double x, double y, double z, float radius, int color, int duration)
        implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(FiatLux.id("fx"));
    public static final StreamCodec<ByteBuf, FxPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FxPayload decode(ByteBuf buf) {
            return new FxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readInt(), ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, FxPayload p) {
            ByteBufCodecs.VAR_INT.encode(buf, p.kind());
            buf.writeDouble(p.x());
            buf.writeDouble(p.y());
            buf.writeDouble(p.z());
            buf.writeFloat(p.radius());
            buf.writeInt(p.color());
            ByteBufCodecs.VAR_INT.encode(buf, p.duration());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
