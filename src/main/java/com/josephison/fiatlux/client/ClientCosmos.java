package com.josephison.fiatlux.client;

import com.josephison.fiatlux.network.SyncCosmosPayload;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Client copy of the created universe, with orbit extrapolation between server syncs. */
public final class ClientCosmos {
    private static SyncCosmosPayload last = new SyncCosmosPayload(-1, 0, 0, List.of(), List.of(), 0f, false, 0, List.of());
    private static long receivedAt;
    private static boolean synced;
    /** When each world first appeared while this client watched (worlds there already at login have none). */
    private static final java.util.Map<String, Long> BORN = new java.util.HashMap<>();

    private ClientCosmos() {}

    public static void set(SyncCosmosPayload p) {
        var level = Minecraft.getInstance().level;
        if (synced && level != null) {
            java.util.Set<String> had = new java.util.HashSet<>();
            for (SyncCosmosPayload.PlanetView v : last.planets()) had.add(v.name());
            for (SyncCosmosPayload.PlanetView v : p.planets()) if (!had.contains(v.name())) BORN.put(v.name(), level.getGameTime());
        }
        synced = true;
        last = p;
        receivedAt = level == null ? 0 : level.getGameTime();
    }

    public static void reset() {
        synced = false;
        BORN.clear();
    }

    /** How fully formed a world is (0..1): a world that has just been made swells out of nothing over four seconds. */
    public static float growth(String name, float partialTick) {
        Long born = BORN.get(name);
        var level = Minecraft.getInstance().level;
        if (born == null || level == null) return 1f;
        float f = (level.getGameTime() - born + partialTick) / 80f;
        if (f >= 1f) {
            BORN.remove(name);
            return 1f;
        }
        f = Math.max(0f, f);
        // ease out with a little overshoot, as if it settles into its size
        float c = 1.4f;
        return 1 + (c + 1) * (float) Math.pow(f - 1, 3) + c * (float) Math.pow(f - 1, 2);
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
