package com.josephison.fiatlux.client;

import com.josephison.fiatlux.network.SyncCosmosPayload;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Client copy of the created universe, with orbit extrapolation between server syncs. */
public final class ClientCosmos {
    private static SyncCosmosPayload last = new SyncCosmosPayload(-1, 0, 0, List.of(), List.of(), 0f, false, 0, List.of());
    private static long receivedAt;

    private ClientCosmos() {}

    public static void set(SyncCosmosPayload p) {
        last = p;
        var level = Minecraft.getInstance().level;
        receivedAt = level == null ? 0 : level.getGameTime();
    }

    public static SyncCosmosPayload get() {
        return last;
    }

    /** Current orbital phase (0..1) of a planet, advanced locally since the last sync. */
    public static float phase(SyncCosmosPayload.PlanetView p, float partialTick) {
        var level = Minecraft.getInstance().level;
        double elapsed = level == null ? 0 : (level.getGameTime() - receivedAt) + partialTick;
        double ph = p.phase() + elapsed * last.orbitDaysPerTick() / Math.max(0.05, p.periodDays());
        return (float) (ph - Math.floor(ph));
    }

    /** Seconds since the Big Bang as seen by this client, or -1 if it has not happened. */
    public static float secondsSinceBigBang(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (last.bigBangTime() < 0 || level == null) return -1f;
        long now = last.serverTime() + (level.getGameTime() - receivedAt);
        return Math.max(0f, (now - last.bigBangTime() + partialTick) / 20f);
    }
}
