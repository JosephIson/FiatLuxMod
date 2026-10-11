package com.josephison.fiatlux.cinema;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A film: shots for the camera and cues for the screen (letterbox, fades, title cards, narration,
 * shaking, sounds and music), authored on the server and played by each watcher's client. Every time
 * is in ticks from the film's first frame.
 *
 * @param id     what it is called (the client answers with it when it starts and when it ends)
 * @param length how long it runs
 * @param stage  where the watchers are held while it runs (so the place is loaded around them), or
 *               null to leave them where they stand
 * @param dim    the dimension of the stage
 */
public record Film(String id, int length, List<Shot> shots, List<Cue> cues, String dim, Vec3 stage) {

    /** Something the camera is placed relative to, or looks at: a point, a creature, or a world in the Void. */
    public record Mark(int kind, int entity, String planet, Vec3 at) {
        public static final int POINT = 0, ENTITY = 1, PLANET = 2;

        public static Mark point(Vec3 at) {
            return new Mark(POINT, -1, "", at);
        }

        /** `at` is an offset from the creature's feet; it is followed wherever it goes. */
        public static Mark entity(Entity e, Vec3 offset) {
            return new Mark(ENTITY, e.getId(), "", offset);
        }

        /** `at` is an offset from the centre of the named world as it moves along its orbit. */
        public static Mark planet(String name, Vec3 offset) {
            return new Mark(PLANET, -1, name, offset);
        }

        static final StreamCodec<ByteBuf, Mark> CODEC = StreamCodec.of((b, m) -> {
            b.writeByte(m.kind);
            ByteBufCodecs.VAR_INT.encode(b, m.entity + 1);
            ByteBufCodecs.STRING_UTF8.encode(b, m.planet);
            vec(b, m.at);
        }, b -> new Mark(b.readByte(), ByteBufCodecs.VAR_INT.decode(b) - 1, ByteBufCodecs.STRING_UTF8.decode(b), vec(b)));
    }

    /**
     * One camera move. The eye travels from `eye.at` to `eyeTo` (both relative to the eye's mark, or
     * absolute for a point) while it looks from `aim.at` to `aimTo`; `orbit` then swings the eye that
     * many degrees around what it looks at over the shot. `fov` 0 keeps the player's own.
     */
    public record Shot(int start, int length, Mark eye, Vec3 eyeTo, Mark aim, Vec3 aimTo, float orbit, float fovFrom, float fovTo,
                       float roll, int ease) {
        public static final int LINEAR = 0, SMOOTH = 1, OUT = 2, IN = 3;

        static final StreamCodec<ByteBuf, Shot> CODEC = StreamCodec.of((b, s) -> {
            ByteBufCodecs.VAR_INT.encode(b, s.start);
            ByteBufCodecs.VAR_INT.encode(b, s.length);
            Mark.CODEC.encode(b, s.eye);
            vec(b, s.eyeTo);
            Mark.CODEC.encode(b, s.aim);
            vec(b, s.aimTo);
            b.writeFloat(s.orbit);
            b.writeFloat(s.fovFrom);
            b.writeFloat(s.fovTo);
            b.writeFloat(s.roll);
            b.writeByte(s.ease);
        }, b -> new Shot(ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), Mark.CODEC.decode(b), vec(b), Mark.CODEC.decode(b), vec(b),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readByte()));
    }

    /**
     * Something that happens on the screen at a moment. What `text`, `sub`, `color`, `a` and `length`
     * mean depends on the kind (see the constants).
     */
    public record Cue(int at, int kind, String text, String sub, int color, float a, int length) {
        /** Black bars close to `a` (0..1 of their full height) over `length` ticks. */
        public static final int LETTERBOX = 0;
        /** The screen fades to `color` at alpha `a` over `length` ticks (over everything but words). */
        public static final int FADE = 1;
        /** A tint over the world (`color` at alpha `a`), under the bars and the words. */
        public static final int GRADE = 2;
        /** A title card: `text` large, `sub` beneath, shown for `length` ticks at scale `a`. */
        public static final int TITLE = 3;
        /** A caption in the corner (a place, an hour). */
        public static final int CAPTION = 4;
        /** Narration or speech along the bottom: `text`, with `sub` its speaker or reference. */
        public static final int NARRATE = 5;
        /** The camera shakes with strength `a`, dying away over `length` ticks. */
        public static final int SHAKE = 6;
        /** A sound `text` at volume `a` and pitch `color`/100. */
        public static final int SOUND = 7;
        /** Music `text` begins (fading in over `length` ticks at volume `a`); empty text fades it out. */
        public static final int MUSIC = 8;
        /** A flash of `color` at strength `a`, fading over `length` ticks. */
        public static final int FLASH = 9;

        static final StreamCodec<ByteBuf, Cue> CODEC = StreamCodec.of((b, c) -> {
            ByteBufCodecs.VAR_INT.encode(b, c.at);
            b.writeByte(c.kind);
            ByteBufCodecs.STRING_UTF8.encode(b, c.text);
            ByteBufCodecs.STRING_UTF8.encode(b, c.sub);
            b.writeInt(c.color);
            b.writeFloat(c.a);
            ByteBufCodecs.VAR_INT.encode(b, c.length);
        }, b -> new Cue(ByteBufCodecs.VAR_INT.decode(b), b.readByte(), ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b),
                b.readInt(), b.readFloat(), ByteBufCodecs.VAR_INT.decode(b)));

        /** The same cue, moved to another moment. */
        public Cue at(int when) {
            return new Cue(when, kind, text, sub, color, a, length);
        }
    }

    public static final StreamCodec<ByteBuf, Film> CODEC = StreamCodec.of((b, f) -> {
        ByteBufCodecs.STRING_UTF8.encode(b, f.id);
        ByteBufCodecs.VAR_INT.encode(b, f.length);
        ByteBufCodecs.VAR_INT.encode(b, f.shots.size());
        for (Shot s : f.shots) Shot.CODEC.encode(b, s);
        ByteBufCodecs.VAR_INT.encode(b, f.cues.size());
        for (Cue c : f.cues) Cue.CODEC.encode(b, c);
        ByteBufCodecs.STRING_UTF8.encode(b, f.dim);
        b.writeBoolean(f.stage != null);
        if (f.stage != null) vec(b, f.stage);
    }, b -> {
        String id = ByteBufCodecs.STRING_UTF8.decode(b);
        int length = ByteBufCodecs.VAR_INT.decode(b);
        List<Shot> shots = new ArrayList<>();
        for (int i = ByteBufCodecs.VAR_INT.decode(b); i > 0; i--) shots.add(Shot.CODEC.decode(b));
        List<Cue> cues = new ArrayList<>();
        for (int i = ByteBufCodecs.VAR_INT.decode(b); i > 0; i--) cues.add(Cue.CODEC.decode(b));
        String dim = ByteBufCodecs.STRING_UTF8.decode(b);
        Vec3 stage = b.readBoolean() ? vec(b) : null;
        return new Film(id, length, shots, cues, dim, stage);
    });

    private static void vec(ByteBuf b, Vec3 v) {
        b.writeDouble(v.x);
        b.writeDouble(v.y);
        b.writeDouble(v.z);
    }

    private static Vec3 vec(ByteBuf b) {
        return new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
    }

    public static Builder make(String id, ResourceKey<Level> dim) {
        return new Builder(id, dim);
    }

    /** Writes a film shot by shot: a cursor (`now`) moves on as shots are added, and cues fall at or after it. */
    public static final class Builder {
        private final String id;
        private final ResourceKey<Level> dim;
        private final List<Shot> shots = new ArrayList<>();
        private final List<Cue> cues = new ArrayList<>();
        private Vec3 stage;
        private int length;

        Builder(String id, ResourceKey<Level> dim) {
            this.id = id;
            this.dim = dim;
        }

        /** Hold the watchers here while it runs (near enough that the place and its people are loaded around them). */
        public Builder stage(Vec3 at) {
            stage = at;
            return this;
        }

        public Builder shot(int start, int length, Mark eye, Vec3 eyeTo, Mark aim, Vec3 aimTo, float orbit, float fov, int ease) {
            return shot(new Shot(start, length, eye, eyeTo, aim, aimTo, orbit, fov, fov, 0, ease));
        }

        public Builder shot(Shot s) {
            shots.add(s);
            this.length = Math.max(this.length, s.start() + s.length());
            return this;
        }

        /** A camera move between two points, looking from one point to another. */
        public Builder move(int start, int length, Vec3 from, Vec3 to, Vec3 lookFrom, Vec3 lookTo, int ease) {
            return shot(start, length, Mark.point(from), to, Mark.point(lookFrom), lookTo, 0, 0, ease);
        }

        /** A camera circling `around` at `radius`, `height` above it, from `fromDeg` through `degrees`. */
        public Builder orbit(int start, int length, Vec3 around, double radius, double height, float fromDeg, float degrees, int ease) {
            double a = Math.toRadians(fromDeg);
            Vec3 eye = around.add(-Math.sin(a) * radius, height, Math.cos(a) * radius);
            return shot(start, length, Mark.point(eye), eye, Mark.point(around), around, degrees, 0, ease);
        }

        public Builder cue(Cue c) {
            cues.add(c);
            length = Math.max(length, c.at() + Math.max(0, c.length()));
            return this;
        }

        public Builder letterbox(int at, float to, int over) {
            return cue(new Cue(at, Cue.LETTERBOX, "", "", 0, to, over));
        }

        public Builder fade(int at, int color, float to, int over) {
            return cue(new Cue(at, Cue.FADE, "", "", color, to, over));
        }

        public Builder grade(int at, int color, float to, int over) {
            return cue(new Cue(at, Cue.GRADE, "", "", color, to, over));
        }

        public Builder title(int at, int length, String text, String sub, int color, float scale) {
            return cue(new Cue(at, Cue.TITLE, text, sub, color, scale, length));
        }

        public Builder caption(int at, int length, String text) {
            return cue(new Cue(at, Cue.CAPTION, text, "", 0xFFF5D77A, 1, length));
        }

        public Builder narrate(int at, int length, String text, String sub) {
            return cue(new Cue(at, Cue.NARRATE, text, sub, 0xFFF5E6B8, 1, length));
        }

        public Builder shake(int at, float strength, int over) {
            return cue(new Cue(at, Cue.SHAKE, "", "", 0, strength, over));
        }

        public Builder sound(int at, String sound, float volume, float pitch) {
            return cue(new Cue(at, Cue.SOUND, sound, "", Math.round(pitch * 100), volume, 0));
        }

        public Builder music(int at, String music, float volume, int fade) {
            return cue(new Cue(at, Cue.MUSIC, music, "", 0, volume, fade));
        }

        public Builder flash(int at, int color, float strength, int over) {
            return cue(new Cue(at, Cue.FLASH, "", "", color, strength, over));
        }

        /** Let it run on to at least `length` ticks. */
        public Builder lasts(int length) {
            this.length = Math.max(this.length, length);
            return this;
        }

        public int length() {
            return length;
        }

        public Film build() {
            return new Film(id, length, List.copyOf(shots), List.copyOf(cues), dim.location().toString(), stage);
        }
    }
}
