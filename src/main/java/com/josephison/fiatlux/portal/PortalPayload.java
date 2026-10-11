package com.josephison.fiatlux.portal;

import com.josephison.fiatlux.FiatLux;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Clientbound: what is on the far side of a portal carried by an entity (a rift, a TARDIS door).
 *
 * @param entity   the entity whose opening it is
 * @param shape    the opening's shape ("rift" or "door")
 * @param ax       the point of the snapshot (in world coordinates of the far side) that the base of the opening looks out from ...
 * @param destYaw  ... and the way the far side faces there (degrees)
 * @param fog      the colour of the far side's air and haze (RGB)
 * @param snapshot the far side
 */
public record PortalPayload(int entity, String shape, double ax, double ay, double az, float destYaw, int fog, RegionSnapshot snapshot)
        implements CustomPacketPayload {
    public static final Type<PortalPayload> TYPE = new Type<>(FiatLux.id("portal"));
    public static final StreamCodec<ByteBuf, PortalPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PortalPayload decode(ByteBuf buf) {
            return new PortalPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readFloat(), buf.readInt(), RegionSnapshot.read(buf));
        }

        @Override
        public void encode(ByteBuf buf, PortalPayload p) {
            ByteBufCodecs.VAR_INT.encode(buf, p.entity());
            ByteBufCodecs.STRING_UTF8.encode(buf, p.shape());
            buf.writeDouble(p.ax());
            buf.writeDouble(p.ay());
            buf.writeDouble(p.az());
            buf.writeFloat(p.destYaw());
            buf.writeInt(p.fog());
            p.snapshot().write(buf);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
