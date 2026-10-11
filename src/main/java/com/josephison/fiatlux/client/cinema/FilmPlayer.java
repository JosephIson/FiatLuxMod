package com.josephison.fiatlux.client.cinema;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.cinema.CinemaPayloads;
import com.josephison.fiatlux.cinema.Film;
import com.josephison.fiatlux.client.ClientCosmos;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import com.josephison.fiatlux.world.VoidSpace;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plays a film on this client: the camera leaves the player for an unseen eye that follows the film's
 * shots, the screen shows its cues (see {@link FilmOverlay}), the HUD, hand and the player's own body
 * are hidden and the player is still. Holding Jump skips it.
 */
public final class FilmPlayer {
    /** A value that eases from where it is to a new target over some ticks (the bars, the fades, the tint). */
    static final class Tween {
        float from, to;
        int fromColor, toColor;
        float start, length;

        float value(float t) {
            return Mth.lerp(progress(t), from, to);
        }

        int color(float t) {
            return lerpColor(fromColor, toColor, progress(t));
        }

        private float progress(float t) {
            if (length <= 0) return 1;
            float f = Mth.clamp((t - start) / length, 0, 1);
            return f * f * (3 - 2 * f);
        }

        void go(float now, float target, int color, int over) {
            from = value(now);
            fromColor = color(now);
            to = target;
            toColor = color;
            start = now;
            length = over;
        }

        void reset() {
            from = to = 0;
            fromColor = toColor = 0;
            length = 0;
        }
    }

    /** Words or a card on the screen: shown from `start` for the cue's length. */
    record Shown(Film.Cue cue, float start, float[] end) {
        float until() {
            return end[0];
        }
    }

    static final Tween LETTERBOX = new Tween(), FADE = new Tween(), GRADE = new Tween();
    static final List<Shown> SHOWN = new ArrayList<>();
    private static final List<float[]> SHAKES = new ArrayList<>();   // start, strength, length
    static float[] flash;   // start, strength, length, color

    @Nullable static Film film;
    static int tick;
    static int skipHeld;
    /** After a film ends in darkness, the world fades back in over these ticks. */
    static int afterglow;
    static int afterglowColor;
    private static List<Film.Cue> pending = new ArrayList<>();
    private static List<Film.Shot> shots = new ArrayList<>();
    @Nullable private static Marker eye;
    @Nullable private static CameraType cameraType;
    @Nullable private static Music music;
    private static final Map<Integer, Vec3> LAST_SEEN = new HashMap<>();
    private static float yaw, pitch, roll, fov;

    private FilmPlayer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FilmPlayer::onTick);
        NeoForge.EVENT_BUS.addListener(FilmPlayer::onFrame);
        NeoForge.EVENT_BUS.addListener(FilmPlayer::onAngles);
        NeoForge.EVENT_BUS.addListener(FilmPlayer::onFov);
        NeoForge.EVENT_BUS.addListener(FilmPlayer::onLayer);
        NeoForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (film != null) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block e) -> {
            if (film != null) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((RenderPlayerEvent.Pre e) -> {
            if (film != null && e.getEntity() == Minecraft.getInstance().player) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((MovementInputUpdateEvent e) -> {
            if (film == null) return;
            var in = e.getInput();
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            if (film == null) return;
            e.setSwingHand(false);
            e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((SelectMusicEvent e) -> {
            if (film != null || music != null && !music.isStopped()) e.overrideMusic(null);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> stop(false));
    }

    public static boolean playing() {
        return film != null;
    }

    // ------------------------------------------------------------------ the server's word

    public static void play(Film f) {
        Minecraft mc = Minecraft.getInstance();
        if (!FiatLuxConfig.CLIENT.cinematics.get() || mc.player == null) {
            PacketDistributor.sendToServer(new CinemaPayloads.Watch(f.id(), false));
            return;
        }
        if (film != null) stop(false);   // the server has already moved them on to this one
        film = f;
        tick = 0;
        skipHeld = 0;
        afterglow = 0;
        flash = null;
        SHOWN.clear();
        SHAKES.clear();
        LAST_SEEN.clear();
        LETTERBOX.reset();
        FADE.reset();
        GRADE.reset();
        pending = new ArrayList<>(f.cues());
        pending.sort(Comparator.comparingInt(Film.Cue::at));
        shots = new ArrayList<>(f.shots());
        shots.sort(Comparator.comparingInt(Film.Shot::start));
        cameraType = mc.options.getCameraType();
        mc.getMusicManager().stopPlaying();
        cue(0);
        PacketDistributor.sendToServer(new CinemaPayloads.Watch(f.id(), true));
    }

    public static void live(Film.Cue c) {
        if (film != null) apply(c.at(tick), tick);
    }

    // ------------------------------------------------------------------ time

    private static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (music != null && music.isStopped()) music = null;
        if (film == null) {
            if (afterglow > 0) afterglow--;
            return;
        }
        if (mc.level == null || mc.player == null) {
            stop(false);
            return;
        }
        if (!mc.isPaused()) tick++;
        cue(tick);
        SHOWN.removeIf(s -> s.until() < tick - 1);
        SHAKES.removeIf(s -> tick > s[0] + s[2]);
        boolean held = mc.screen == null && mc.options.keyJump.isDown();
        skipHeld = held ? skipHeld + 1 : Math.max(0, skipHeld - 2);
        if (skipHeld >= 24 || tick >= film.length()) stop(true);
    }

    private static void cue(int now) {
        while (!pending.isEmpty() && pending.getFirst().at() <= now) apply(pending.removeFirst(), now);
    }

    private static void apply(Film.Cue c, float now) {
        Minecraft mc = Minecraft.getInstance();
        switch (c.kind()) {
            case Film.Cue.LETTERBOX -> LETTERBOX.go(now, c.a(), 0, c.length());
            case Film.Cue.FADE -> FADE.go(now, c.a(), c.color(), c.length());
            case Film.Cue.GRADE -> GRADE.go(now, c.a(), c.color(), c.length());
            case Film.Cue.TITLE, Film.Cue.CAPTION -> SHOWN.add(new Shown(c, now, new float[]{now + c.length()}));
            case Film.Cue.NARRATE -> {
                // one voice at a time: what was being said gives way
                for (Shown s : SHOWN) if (s.cue().kind() == Film.Cue.NARRATE && s.until() > now + 8) s.end()[0] = now + 8;
                SHOWN.add(new Shown(c, now, new float[]{now + c.length()}));
            }
            case Film.Cue.SHAKE -> SHAKES.add(new float[]{now, c.a(), Math.max(1, c.length())});
            case Film.Cue.FLASH -> flash = new float[]{now, c.a(), Math.max(1, c.length()), c.color()};
            case Film.Cue.SOUND -> {
                ResourceLocation id = ResourceLocation.tryParse(c.text());
                if (id != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id),
                        c.color() / 100f, c.a()));
            }
            case Film.Cue.MUSIC -> {
                if (music != null) music.fadeOut(Math.max(1, c.length()));
                ResourceLocation id = c.text().isEmpty() ? null : ResourceLocation.tryParse(c.text());
                if (id != null) {
                    mc.getMusicManager().stopPlaying();
                    music = new Music(SoundEvent.createVariableRangeEvent(id), c.a(), Math.max(1, c.length()));
                    mc.getSoundManager().play(music);
                }
            }
            default -> {
            }
        }
    }

    /** The film is over, ended or skipped: the camera returns to the player. */
    static void stop(boolean tellServer) {
        Film f = film;
        if (f == null) return;
        Minecraft mc = Minecraft.getInstance();
        float now = tick;
        if (FADE.value(now) > 0.4f) {
            afterglow = 16;
            afterglowColor = FADE.color(now);
        }
        film = null;
        if (music != null) music.fadeOut(50);
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (cameraType != null) mc.options.setCameraType(cameraType);
        cameraType = null;
        eye = null;
        SHOWN.clear();
        SHAKES.clear();
        flash = null;
        if (tellServer && mc.getConnection() != null) PacketDistributor.sendToServer(new CinemaPayloads.Watch(f.id(), false));
    }

    // ------------------------------------------------------------------ the camera

    private static void onFrame(RenderFrameEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (film == null || mc.level == null || mc.player == null) return;
        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        float t = tick + (mc.isPaused() ? 0 : partial);
        Film.Shot s = shotAt(t);
        if (s == null) return;
        if (eye == null || eye.level() != mc.level) eye = new Marker(EntityType.MARKER, mc.level);
        float f = s.length() <= 0 ? 1 : Mth.clamp((t - s.start()) / s.length(), 0, 1);
        float k = ease(f, s.ease());
        Vec3 pos = base(s.eye(), partial).add(s.eye().at().lerp(s.eyeTo(), k));
        Vec3 aim = base(s.aim(), partial).add(s.aim().at().lerp(s.aimTo(), k));
        if (s.orbit() != 0) {
            double a = Math.toRadians(s.orbit() * k), cos = Math.cos(a), sin = Math.sin(a);
            Vec3 r = pos.subtract(aim);
            pos = aim.add(r.x * cos - r.z * sin, r.y, r.x * sin + r.z * cos);
        }
        pos = unstick(mc.level, pos, aim);
        float shake = 0;
        for (float[] sh : SHAKES) {
            float left = 1 - (t - sh[0]) / sh[2];
            if (left > 0) shake += sh[1] * left * left;
        }
        if (shake > 0) {
            pos = pos.add(Math.sin(t * 2.3) * shake * 0.06, Math.sin(t * 3.1 + 1) * shake * 0.05, Math.cos(t * 2.7) * shake * 0.06);
        }
        Vec3 d = aim.subtract(pos);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.max(1e-4, flat)));
        roll = s.roll();
        if (shake > 0) {
            yaw += (float) (Math.sin(t * 1.9) * shake * 1.2);
            pitch += (float) (Math.cos(t * 2.4 + 2) * shake * 0.9);
            roll += (float) (Math.sin(t * 1.3 + 4) * shake * 1.1);
        }
        fov = Mth.lerp(k, s.fovFrom(), s.fovTo());
        eye.setPos(pos.x, pos.y, pos.z);
        eye.xo = eye.xOld = pos.x;
        eye.yo = eye.yOld = pos.y;
        eye.zo = eye.zOld = pos.z;
        eye.setYRot(yaw);
        eye.yRotO = yaw;
        eye.setXRot(Mth.clamp(pitch, -90, 90));
        eye.xRotO = eye.getXRot();
        if (mc.getCameraEntity() != eye) mc.setCameraEntity(eye);
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (film == null || Minecraft.getInstance().getCameraEntity() != eye) return;
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setRoll(roll);
    }

    private static void onFov(ViewportEvent.ComputeFov e) {
        if (film != null && fov > 1 && Minecraft.getInstance().getCameraEntity() == eye) e.setFOV(fov);
    }

    private static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (film == null) return;
        ResourceLocation n = e.getName();
        if (!n.getNamespace().equals(FiatLux.MODID) || !(n.getPath().equals("cinema") || n.getPath().equals("fx_overlay"))) e.setCanceled(true);
    }

    /**
     * A camera that would stand inside the ground, a tree or a wall is drawn out along its line of sight
     * until it stands in the open (a shot written for one place may meet a hill in another).
     */
    private static Vec3 unstick(net.minecraft.world.level.Level level, Vec3 pos, Vec3 aim) {
        if (!solid(level, pos)) return pos;
        Vec3 d = aim.subtract(pos);
        double len = d.length();
        if (len < 0.8) return pos;
        Vec3 step = d.scale(0.25 / len);
        Vec3 p = pos;
        for (double t = 0.25; t < len - 0.6; t += 0.25) {
            p = p.add(step);
            if (!solid(level, p)) return p.add(step.scale(1.5));
        }
        return pos;
    }

    private static boolean solid(net.minecraft.world.level.Level level, Vec3 p) {
        net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(p);
        return !level.getBlockState(b).getCollisionShape(level, b).isEmpty();
    }

    @Nullable
    private static Film.Shot shotAt(float t) {
        Film.Shot best = shots.isEmpty() ? null : shots.getFirst();
        for (Film.Shot s : shots) if (s.start() <= t) best = s;
        return best;
    }

    /** Where a mark is now (the origin for a point; a creature's feet; a world's centre). */
    private static Vec3 base(Film.Mark m, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 fallback = mc.player != null ? mc.player.position() : Vec3.ZERO;
        switch (m.kind()) {
            case Film.Mark.ENTITY -> {
                Entity e = mc.level == null ? null : mc.level.getEntity(m.entity());
                if (e != null && !e.isRemoved()) {
                    Vec3 at = e.getPosition(partial);
                    LAST_SEEN.put(m.entity(), at);
                    return at;
                }
                return LAST_SEEN.getOrDefault(m.entity(), fallback);
            }
            case Film.Mark.PLANET -> {
                for (SyncCosmosPayload.PlanetView p : ClientCosmos.get().planets()) {
                    if (p.name().equals(m.planet())) return VoidSpace.planetPos(p.orbitRadius(), ClientCosmos.phase(p, partial), p.inclination());
                }
                return fallback;
            }
            default -> {
                return Vec3.ZERO;
            }
        }
    }

    private static float ease(float f, int how) {
        return switch (how) {
            case Film.Shot.SMOOTH -> f < 0.5f ? 4 * f * f * f : 1 - (float) Math.pow(-2 * f + 2, 3) / 2;
            case Film.Shot.OUT -> 1 - (1 - f) * (1 - f) * (1 - f);
            case Film.Shot.IN -> f * f * f;
            default -> f;
        };
    }

    static int lerpColor(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) Mth.lerp(t, ar, br) << 16 | (int) Mth.lerp(t, ag, bg) << 8 | (int) Mth.lerp(t, ab, bb);
    }

    // ------------------------------------------------------------------ music

    /** A film's music: it rises in, and dies away when the film moves on or ends. */
    private static final class Music extends AbstractTickableSoundInstance {
        private final float target;
        private final int rise;
        private int fall;
        private boolean falling;

        Music(SoundEvent event, float target, int rise) {
            super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.target = Mth.clamp(target, 0.01f, 1f);
            this.rise = rise;
            this.volume = 0.01f;
            this.looping = false;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        void fadeOut(int over) {
            falling = true;
            fall = Math.max(1, over);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (falling) {
                volume -= target / fall;
                if (volume <= 0.002f) stop();
            } else {
                volume = Math.min(target, volume + target / rise);
            }
        }
    }
}
