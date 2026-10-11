package com.josephison.fiatlux.cinema;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Plays films to players. A watcher's client starts the film and says so; the server then holds them
 * on the film's stage (near enough that the place and its people are loaded about them), unseen,
 * unhurt and hovering, and returns them where they stood when the film ends, is skipped, or runs too
 * long. Where to return them is kept with the player, so a watcher who leaves mid-film comes back to
 * it on their next login.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Cinema {
    private static final String RETURN = "fiatlux_cinema_return";
    private static final double NEAR = 48;

    private record Watching(Film film, long sent, boolean held) {}

    private record Later(long due, Runnable run) {}

    private static final Map<UUID, Watching> WATCHING = new HashMap<>();
    private static final List<Later> LATER = new ArrayList<>();
    private static long clock;

    private Cinema() {}

    public static boolean enabled() {
        return FiatLuxConfig.COMMON.cinematics.get();
    }

    /** Those who will see a film of something at `at`: the players near it, and `also` wherever they are. */
    public static List<ServerPlayer> audience(ServerLevel level, Vec3 at, @Nullable UUID also) {
        double r = FiatLuxConfig.COMMON.cinematicRange.get();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.level() == level && p.position().distanceToSqr(at) < r * r || p.getUUID().equals(also)) out.add(p);
        }
        return out;
    }

    /**
     * Plays the film to each of them. A film already playing gives way to it (where they stood before the
     * first is still where they return to when the second ends).
     */
    public static void play(Film film, Collection<ServerPlayer> to) {
        if (!enabled()) return;
        FiatLux.LOGGER.debug("Film {} ({} ticks) for {} watcher(s)", film.id(), film.length(), to.size());
        for (ServerPlayer p : to) {
            WATCHING.put(p.getUUID(), new Watching(film, clock, false));
            PacketDistributor.sendToPlayer(p, new CinemaPayloads.Play(film));
        }
    }

    public static boolean watching(ServerPlayer p) {
        return WATCHING.containsKey(p.getUUID());
    }

    /** Is anyone watching a film whose id starts with `prefix`? */
    public static boolean showing(String prefix) {
        return WATCHING.values().stream().anyMatch(w -> w.film.id().startsWith(prefix));
    }

    /** A cue for everyone watching a film that matches, shown at once. */
    public static void live(MinecraftServer server, Film.Cue cue, Predicate<Film> which) {
        for (var e : WATCHING.entrySet()) {
            if (!which.test(e.getValue().film)) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) PacketDistributor.sendToPlayer(p, new CinemaPayloads.Live(cue));
        }
    }

    /** Words spoken in the world (narration, or a voice) become the film's subtitles for those watching one there. */
    public static void narrate(ServerLevel level, String text, String sub, int color) {
        if (WATCHING.isEmpty()) return;
        String dim = level.dimension().location().toString();
        live(level.getServer(), new Film.Cue(0, Film.Cue.NARRATE, text, sub, color, 1, Math.max(80, 50 + text.length() * 2)), f -> f.dim().equals(dim));
    }

    /** Runs `run` on the server thread `ticks` from now (a film's moment that must wait for its lead-in). */
    public static void later(int ticks, Runnable run) {
        LATER.add(new Later(clock + Math.max(1, ticks), run));
    }

    // ------------------------------------------------------------------ the watcher's word

    public static void onWatch(ServerPlayer p, CinemaPayloads.Watch w) {
        Watching now = WATCHING.get(p.getUUID());
        if (now == null || !now.film.id().equals(w.film())) return;
        if (w.started()) {
            if (now.held) return;
            hold(p, now.film);
            WATCHING.put(p.getUUID(), new Watching(now.film, now.sent, true));
        } else {
            WATCHING.remove(p.getUUID());
            release(p);
        }
    }

    private static void hold(ServerPlayer p, Film film) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(RETURN)) {
            CompoundTag r = new CompoundTag();
            r.putString("dim", p.level().dimension().location().toString());
            r.putDouble("x", p.getX());
            r.putDouble("y", p.getY());
            r.putDouble("z", p.getZ());
            r.putFloat("yaw", p.getYRot());
            r.putFloat("pitch", p.getXRot());
            r.putBoolean("mayfly", p.getAbilities().mayfly);
            r.putBoolean("flying", p.getAbilities().flying);
            r.putBoolean("invulnerable", p.isInvulnerable());
            data.put(RETURN, r);
        }
        if (film.stage() != null) {
            ServerLevel to = level(p.server, film.dim());
            if (to != null && (p.level() != to || p.position().distanceTo(film.stage()) > NEAR)) {
                p.teleportTo(to, film.stage().x, film.stage().y, film.stage().z, p.getYRot(), p.getXRot());
            }
        }
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.setInvulnerable(true);
        p.setInvisible(true);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void release(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(RETURN)) return;
        CompoundTag r = data.getCompound(RETURN);
        data.remove(RETURN);
        ServerLevel back = level(p.server, r.getString("dim"));
        Vec3 at = new Vec3(r.getDouble("x"), r.getDouble("y"), r.getDouble("z"));
        if (back != null && (p.level() != back || p.position().distanceToSqr(at) > 0.25)) {
            p.teleportTo(back, at.x, at.y, at.z, r.getFloat("yaw"), r.getFloat("pitch"));
        }
        p.getAbilities().mayfly = r.getBoolean("mayfly") || p.isCreative() || p.isSpectator();
        p.getAbilities().flying = r.getBoolean("flying") && p.getAbilities().mayfly;
        p.onUpdateAbilities();
        p.setInvulnerable(r.getBoolean("invulnerable"));
        p.setInvisible(p.hasEffect(MobEffects.INVISIBILITY));
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
    }

    @Nullable
    private static ServerLevel level(MinecraftServer server, String dim) {
        ResourceLocation id = ResourceLocation.tryParse(dim);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        clock++;
        if (!LATER.isEmpty()) {
            List<Later> due = LATER.stream().filter(l -> l.due <= clock).toList();
            LATER.removeAll(due);
            for (Later l : due) {
                try {
                    l.run.run();
                } catch (RuntimeException e) {
                    FiatLux.LOGGER.error("A film's moment failed", e);
                }
            }
        }
        if (clock % 20 == 0 && !WATCHING.isEmpty()) {
            // a watcher whose client never says the film is over is returned once it has surely ended
            for (var it = WATCHING.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (clock - e.getValue().sent < e.getValue().film.length() + 200) continue;
                it.remove();
                ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null) release(p);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !WATCHING.containsKey(p.getUUID())) release(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WATCHING.remove(event.getEntity().getUUID());   // where they stood is kept with them, for their return
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        WATCHING.clear();
        LATER.clear();
    }
}
