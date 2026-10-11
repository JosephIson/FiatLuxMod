package com.josephison.fiatlux.son;

import com.josephison.fiatlux.cinema.Cinema;
import com.josephison.fiatlux.cinema.Film;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.MortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The films of the Son: the cross (shot by shot through the seven words, the darkness, the death, the
 * veil and the centurion), the empty tomb at dawn, and the ascension. Their shots are timed to the
 * story's own pace, and what is said in the world becomes their subtitles as it is said.
 */
final class SonFilms {
    private SonFilms() {}

    /** Ticks a story moment `seconds` after the start of its part falls at (scaled by the story's pace). */
    private static int s(double seconds) {
        return (int) TheSon.secs(seconds);
    }

    private static boolean wanted() {
        return Cinema.enabled() && !TheSon.forcing;
    }

    /** Golgotha. Starts as he is lifted up; the words fall at 6 + 11i story-seconds. */
    static void calvary(ServerLevel level, SonData d, MortalEntity son, BlockPos at) {
        if (!wanted()) return;
        Vec3 c = Vec3.atBottomCenterOf(at);
        Vec3 top = c.add(0, 2.4, 0);
        int[] w = new int[7];
        for (int i = 0; i < 7; i++) w[i] = s(6 + i * 11);
        int end = w[6];
        Film.Builder f = Film.make("son:calvary", level.dimension()).stage(c.add(0, 26, 0));
        // he faces north (-z): the camera stands before him there
        f.move(0, w[0], c.add(-16, 9, -20), c.add(-9, 5, -13), top, top, Film.Shot.SMOOTH)
                .shot(w[0], w[1] - w[0], Film.Mark.entity(son, new Vec3(0.2, 0.6, -3.4)), new Vec3(0.1, 0.9, -2.6), Film.Mark.entity(son, new Vec3(0, 1.6, 0)),
                        new Vec3(0, 1.6, 0), 0, 0, Film.Shot.SMOOTH)
                .orbit(w[1], w[2] - w[1], c.add(0, 2, 0), 10, 2, 200, 50, Film.Shot.LINEAR)
                .shot(w[2], w[3] - w[2], Film.Mark.point(c.add(3.5, 0.4, -7.5)), c.add(2.8, 0.6, -6.5), Film.Mark.entity(son, new Vec3(0, 1.7, 0)),
                        new Vec3(0, 1.7, 0), 0, 0, Film.Shot.SMOOTH)
                .move(w[3], w[4] - w[3], c.add(6, 22, -28), c.add(8, 26, -36), top, top, Film.Shot.LINEAR)
                .shot(w[4], w[5] - w[4], Film.Mark.entity(son, new Vec3(2.6, 1.6, -0.8)), new Vec3(2.2, 1.65, -0.6), Film.Mark.entity(son, new Vec3(0, 1.6, 0)),
                        new Vec3(0, 1.6, 0), 0, 0, Film.Shot.LINEAR)
                .shot(w[5], w[6] - w[5], Film.Mark.entity(son, new Vec3(0, 1.7, -7)), new Vec3(0, 1.7, -2.4), Film.Mark.entity(son, new Vec3(0, 1.65, 0)),
                        new Vec3(0, 1.65, 0), 0, 0, Film.Shot.SMOOTH)
                // "Father, into thy hands": he gives up the ghost, and the earth quakes
                .shot(end, 50, Film.Mark.entity(son, new Vec3(0, 1.6, -3)), new Vec3(0, 1.8, -3.4), Film.Mark.entity(son, new Vec3(0, 1.6, 0)),
                        new Vec3(0, 1.6, 0), 0, 0, Film.Shot.LINEAR)
                .shot(end + 50, 70, Film.Mark.entity(son, new Vec3(0, 2, -5)), new Vec3(0, 12, -14), Film.Mark.entity(son, new Vec3(0, 1.5, 0)),
                        new Vec3(0, 1.5, 0), 0, 0, Film.Shot.SMOOTH);
        Settlement cap = Passion.capital(level, d);
        int after = end + 120;
        if (cap != null && cap.shrine != null) {
            // the veil of the temple, rent in twain
            Vec3 shrine = Vec3.atBottomCenterOf(cap.shrine);
            f.move(after, 70, shrine.add(7, 4, 7), shrine.add(5, 3, 5), shrine.add(0, 1.5, 0), shrine.add(0, 1.5, 0), Film.Shot.SMOOTH);
            after += 70;
        }
        // the centurion, and those who stood afar off
        f.move(after, 120, c.add(-6, 1.8, -10), c.add(-7, 2.2, -12), top, top, Film.Shot.LINEAR);
        f.fade(0, 0x000000, 1, 0)
                .fade(0, 0x000000, 0, 40)
                .letterbox(0, 1, 30)
                .caption(20, Math.max(60, Math.min(w[0] - 20, 140)), "Golgotha, the place of a skull")
                .music(10, "minecraft:music.overworld.deep_dark", 0.8f, 80)
                // the sixth hour: darkness over all the land
                .grade(w[1], 0x0A0612, 0.45f, 80)
                .sound(w[1], "minecraft:entity.lightning_bolt.thunder", 0.5f, 0.7f)
                .grade(w[3], 0x06030A, 0.6f, 40)
                .sound(w[3] + 10, "minecraft:entity.lightning_bolt.thunder", 0.7f, 0.5f)
                .shake(end + 4, 1.6f, 90)
                .flash(end + 4, 0x301010, 0.45f, 30)
                .sound(end + 4, "minecraft:entity.generic.explode", 1f, 0.4f)
                .fade(after + 50, 0x000000, 1, 40)
                .title(after + 80, 90, "It Is Finished", "John 19:30", 0xFFE8DCC8, 3f)
                .music(after + 90, "", 0, 80)
                .lasts(after + 180);
        Cinema.play(f.build(), Cinema.audience(level, c, d.creator));
    }

    /** Ticks from the empty tomb film's start to the stone being rolled away (the film begins before it, at dawn). */
    static final int TOMB_LEAD = 120;

    /** The third day, very early in the morning. Starts TOMB_LEAD ticks before the stone is rolled away. */
    static void risen(ServerLevel level, SonData d, MortalEntity son, BlockPos tomb) {
        if (!wanted()) return;
        long day = level.getDayTime() / 24000;
        level.setDayTime(day * 24000 + 23300);   // the dawn
        Vec3 t = Vec3.atBottomCenterOf(tomb);
        Vec3 door = t.add(4, 0, 0);   // the great stone, to the east
        int r = TOMB_LEAD;
        Film film = Film.make("son:risen", level.dimension()).stage(t.add(8, 20, 0))
                .move(0, 110, t.add(18, 4, 9), t.add(13, 2.5, 5), door.add(0, 0.5, 0), door.add(0, 0.5, 0), Film.Shot.SMOOTH)
                .move(110, r - 110 + 60, door.add(5, 0.6, 2), door.add(4, 0.8, 1.5), door.add(0, 0.5, 0), door.add(0, 0.5, 0), Film.Shot.LINEAR)
                .shot(r + 60, 130, Film.Mark.entity(son, new Vec3(4, 1.5, 1.5)), new Vec3(2.6, 1.6, 0.8), Film.Mark.entity(son, new Vec3(0, 1.6, 0)),
                        new Vec3(0, 1.6, 0), 0, 0, Film.Shot.SMOOTH)
                .move(r + 190, 140, t.add(12, 4, 7), t.add(24, 13, 15), t.add(5, 0.5, 0), t.add(5, 0.5, 0), Film.Shot.SMOOTH)
                .fade(0, 0x000000, 1, 0)
                .fade(0, 0x000000, 0, 50)
                .letterbox(0, 1, 0)
                .caption(15, 90, "The third day, very early in the morning")
                .sound(20, "minecraft:ambient.cave", 0.5f, 0.6f)
                .shake(85, 0.5f, 40)
                // the angel descends, and rolls back the stone
                .flash(r, 0xFFF8E0, 1f, 50)
                .shake(r, 1.2f, 50)
                .sound(r, "minecraft:block.beacon.power_select", 1f, 0.6f)
                .music(r, "minecraft:music.overworld.cherry_grove", 0.85f, 40)
                .title(r + 70, 110, "He Is Risen", "Matthew 28:6", 0xFFFFF4D6, 3.2f)
                .fade(r + 280, 0x000000, 1, 40)
                .music(r + 290, "", 0, 60)
                .lasts(r + 330)
                .build();
        Cinema.play(film, Cinema.audience(level, t, d.creator));
    }

    /** Taken up: starts as he begins to rise from the hill. */
    static void ascension(ServerLevel level, SonData d, MortalEntity son) {
        if (!wanted()) return;
        Vec3 hill = son.position();
        Film film = Film.make("son:ascension", level.dimension()).stage(hill.add(10, 30, 10))
                .shot(0, 90, Film.Mark.entity(son, new Vec3(8, 2.2, 8)), new Vec3(6, 2, 6), Film.Mark.entity(son, new Vec3(0, 1.5, 0)), new Vec3(0, 1.5, 0),
                        0, 0, Film.Shot.SMOOTH)
                // from among the disciples, looking up after him
                .shot(90, 130, Film.Mark.point(hill.add(3, 0.6, 3)), hill.add(3, 0.6, 3), Film.Mark.entity(son, new Vec3(0, 1.2, 0)), new Vec3(0, 1.2, 0),
                        0, 0, Film.Shot.LINEAR)
                // a cloud received him out of their sight
                .shot(220, 100, Film.Mark.entity(son, new Vec3(0, 3, 9)), new Vec3(0, -2, 13), Film.Mark.entity(son, new Vec3(0, 1, 0)), new Vec3(0, 1, 0),
                        0, 0, Film.Shot.SMOOTH)
                // why stand ye gazing up into heaven?
                .move(320, 160, hill.add(6, 0.8, 6), hill.add(5, 1.2, 4.5), hill.add(0, 4, 0), hill.add(0, 7, 0), Film.Shot.LINEAR)
                .letterbox(0, 1, 30)
                .music(0, "minecraft:music.overworld.meadow", 0.8f, 60)
                .fade(250, 0xFFF8E8, 1, 50)
                .title(300, 100, "He Was Taken Up", "Acts 1:9", 0xFF8A6A2A, 3f)
                .fade(390, 0xFFF8E8, 0, 40)
                .fade(445, 0x000000, 1, 30)
                .music(440, "", 0, 60)
                .lasts(480)
                .build();
        Cinema.play(film, Cinema.audience(level, hill, d.creator));
    }

    /** Lead-in, in story seconds, for the empty tomb film at the present pace. */
    static double tombLeadSeconds() {
        return TOMB_LEAD / 20.0 * Math.max(0.05, FiatLuxConfig.COMMON.sonPace.get());
    }
}
