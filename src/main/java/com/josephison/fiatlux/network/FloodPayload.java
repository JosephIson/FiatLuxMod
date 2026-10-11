package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Clientbound: where the waters of the Flood stand, so the client can carry their surface out to the
 *
 * horizon beyond the stretch where the waters are real blocks; and where the bow is set in the cloud.
 *
 * @param dim     the dimension the waters (or the bow) are upon ("" for none)
 * @param sea     whether the waters stand there now
 * @param waterY  the highest layer of water
 * @param centres the circles of real water (BlockPos longs) ...
 * @param radii   ... and their radii
 * @param bow     where the rainbow stands (BlockPos long) ...
 * @param bowTicks ... and for how many more ticks (0 for none)
 */
public record FloodPayload(String dim, boolean sea, int waterY, long[] centres, int[] radii, long bow, int bowTicks) implements CustomPacketPayload {
    public static final Type<FloodPayload> TYPE = new Type<>(FiatLux.id("flood"));
    public static final FloodPayload NONE = new FloodPayload("", false, 0, new long[0], new int[0], 0L, 0);
    public static final StreamCodec<ByteBuf, FloodPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FloodPayload decode(ByteBuf buf) {
            String dim = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean sea = buf.readBoolean();
            int y = ByteBufCodecs.VAR_INT.decode(buf);
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            long[] c = new long[n];
            int[] r = new int[n];
            for (int i = 0; i < n; i++) {
                c[i] = buf.readLong();
                r[i] = ByteBufCodecs.VAR_INT.decode(buf);
            }
            return new FloodPayload(dim, sea, y, c, r, buf.readLong(), ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, FloodPayload p) {
            ByteBufCodecs.STRING_UTF8.encode(buf, p.dim());
            buf.writeBoolean(p.sea());
            ByteBufCodecs.VAR_INT.encode(buf, p.waterY());
            ByteBufCodecs.VAR_INT.encode(buf, p.centres().length);
            for (int i = 0; i < p.centres().length; i++) {
                buf.writeLong(p.centres()[i]);
                ByteBufCodecs.VAR_INT.encode(buf, p.radii()[i]);
            }
            buf.writeLong(p.bow());
            ByteBufCodecs.VAR_INT.encode(buf, p.bowTicks());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
