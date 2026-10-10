package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.cosmos.Galaxy;
import com.josephison.fiatlux.world.cosmos.Planet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Clientbound: what the sky needs to show the created universe - galaxies, orbiting planets (with
 * enough orbital data for clients to keep them moving smoothly between syncs), the Big Bang
 * timestamp and the moral climate (tints and omens).
 */
public record SyncCosmosPayload(long bigBangTime, long serverTime, double orbitDaysPerTick, List<GalaxyView> galaxies,
                                List<PlanetView> planets, float corruption, boolean rebellion, int underworldEra)
        implements CustomPacketPayload {
    public static final Type<SyncCosmosPayload> TYPE = new Type<>(FiatLux.id("sync_cosmos"));

    public record GalaxyView(float yaw, float pitch, float size, int hue, int arms, float twist) {}

    public record PlanetView(String name, int type, float orbitRadius, float periodDays, float phase, float inclination,
                             int color, int atmosphere, float size, boolean inhabited) {}

    public static SyncCosmosPayload of(CosmicExpanseSavedData c, MinecraftServer server) {
        List<GalaxyView> g = new ArrayList<>();
        for (Galaxy x : c.galaxies) g.add(new GalaxyView(x.yaw(), x.pitch(), x.size(), x.hue(), x.arms(), x.twist()));
        List<PlanetView> p = new ArrayList<>();
        for (Planet x : c.planets) {
            p.add(new PlanetView(x.name, x.type.ordinal(), (float) x.orbitRadius, (float) x.orbitPeriodDays, (float) x.phase,
                    (float) x.inclination, x.type.baseColor, x.atmosphere, (float) x.type.size, x.hasLife()));
        }
        var cfg = FiatLuxConfig.COMMON;
        double perTick = cfg.simulationEnabled.get() ? cfg.simulationSpeed.get() * cfg.orbitSpeedMultiplier.get() / 24000.0 : 0;
        return new SyncCosmosPayload(c.bigBangTime, server.overworld().getGameTime(), perTick, g, p,
                (float) c.corruption, c.rebellionActive, c.underworld.era.ordinal());
    }

    public static final StreamCodec<ByteBuf, SyncCosmosPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncCosmosPayload decode(ByteBuf b) {
            long bang = b.readLong(), now = b.readLong();
            double perTick = b.readDouble();
            int ng = ByteBufCodecs.VAR_INT.decode(b);
            List<GalaxyView> g = new ArrayList<>(ng);
            for (int i = 0; i < ng; i++) g.add(new GalaxyView(b.readFloat(), b.readFloat(), b.readFloat(), b.readInt(), b.readByte(), b.readFloat()));
            int np = ByteBufCodecs.VAR_INT.decode(b);
            List<PlanetView> p = new ArrayList<>(np);
            for (int i = 0; i < np; i++) {
                p.add(new PlanetView(ByteBufCodecs.STRING_UTF8.decode(b), b.readByte(), b.readFloat(), b.readFloat(), b.readFloat(),
                        b.readFloat(), b.readInt(), b.readInt(), b.readFloat(), b.readBoolean()));
            }
            return new SyncCosmosPayload(bang, now, perTick, g, p, b.readFloat(), b.readBoolean(), b.readByte());
        }

        @Override
        public void encode(ByteBuf b, SyncCosmosPayload s) {
            b.writeLong(s.bigBangTime);
            b.writeLong(s.serverTime);
            b.writeDouble(s.orbitDaysPerTick);
            ByteBufCodecs.VAR_INT.encode(b, s.galaxies.size());
            for (GalaxyView g : s.galaxies) {
                b.writeFloat(g.yaw()); b.writeFloat(g.pitch()); b.writeFloat(g.size()); b.writeInt(g.hue()); b.writeByte(g.arms()); b.writeFloat(g.twist());
            }
            ByteBufCodecs.VAR_INT.encode(b, s.planets.size());
            for (PlanetView p : s.planets) {
                ByteBufCodecs.STRING_UTF8.encode(b, p.name());
                b.writeByte(p.type()); b.writeFloat(p.orbitRadius()); b.writeFloat(p.periodDays()); b.writeFloat(p.phase());
                b.writeFloat(p.inclination()); b.writeInt(p.color()); b.writeInt(p.atmosphere()); b.writeFloat(p.size()); b.writeBoolean(p.inhabited());
            }
            b.writeFloat(s.corruption);
            b.writeBoolean(s.rebellion);
            b.writeByte(s.underworldEra);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
