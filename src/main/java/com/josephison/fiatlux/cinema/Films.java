package com.josephison.fiatlux.cinema;

import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.VoidSpace;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The films of the making of all things: the beginning (darkness on the face of the deep, the word,
 * and the light bursting out into stars, nebulae, galaxies and the greater light) and the forming of
 * each world in the Void. The Son's films are in son.SonFilms.
 */
public final class Films {
    /** Ticks into the beginning at which the light is spoken; the Big Bang itself waits for this moment. */
    public static final int LET_THERE_BE_LIGHT = 140;

    private Films() {}

    /** "In the beginning": played to everyone in the Void. Returns whether anyone is watching it. */
    public static boolean beginning(ServerLevel voidLevel, String creator) {
        if (!Cinema.enabled() || voidLevel.players().isEmpty()) return false;
        int bang = LET_THERE_BE_LIGHT;
        Vec3 sun = VoidSpace.SUN;
        Film film = Film.make("beginning", FiatLuxDimensions.VOID)
                // without form, and void: skimming the face of the deep in the dark
                .move(0, bang, new Vec3(0.5, 52, 30), new Vec3(0.5, 47, 72), new Vec3(0.5, 44, 200), new Vec3(0.5, 50, 260), Film.Shot.LINEAR)
                .fade(0, 0x000000, 1, 0)
                .letterbox(0, 1, 0)
                .title(12, 86, "In the Beginning", "Genesis 1:1", 0xFFF5E6B8, 3f)
                .narrate(24, 108, "In the beginning God created the heaven and the earth. And the earth was without form, and void; "
                        + "and darkness was upon the face of the deep.", "Genesis 1:1-2")
                .fade(70, 0x000000, 0, 50)
                .sound(30, "minecraft:ambient.cave", 0.6f, 0.5f)
                .narrate(98, 30, "And the Spirit of God moved upon the face of the waters.", "Genesis 1:2")
                .narrate(126, 22, "And God said,", creator)
                // let there be light
                .title(bang, 92, "Let There Be Light", "", 0xFFFFFFFF, 3.6f)
                .flash(bang, 0xFFFFFF, 1f, 70)
                .shake(bang, 1.5f, 80)
                .sound(bang, "minecraft:entity.generic.explode", 1f, 0.5f)
                .sound(bang, "minecraft:item.trident.thunder", 1f, 0.6f)
                .sound(bang + 2, "minecraft:block.beacon.activate", 1f, 0.6f)
                .music(bang, "minecraft:music.credits", 0.9f, 30)
                // rising out of the deep as the light spreads, toward the greater light being formed
                .shot(new Film.Shot(bang, 190, Film.Mark.point(new Vec3(0.5, 47, 72)), new Vec3(0.5, 140, 40), Film.Mark.point(new Vec3(0.5, 50, 260)),
                        sun, 0, 70, 88, 0, Film.Shot.SMOOTH))
                .narrate(bang + 30, 120, "And there was light. And God saw the light, that it was good: and God divided the light from the darkness.",
                        "Genesis 1:3-4")
                // across the heavens, as the stars and the galaxies come out
                .shot(new Film.Shot(bang + 190, 140, Film.Mark.point(new Vec3(0.5, 140, 40)), new Vec3(0.5, 150, 30), Film.Mark.point(sun),
                        new Vec3(-300, 250, 120), 0, 88, 92, -4, Film.Shot.SMOOTH))
                .narrate(bang + 170, 110, "And God called the light Day, and the darkness he called Night.", "Genesis 1:5")
                // and back to the greater light
                .shot(new Film.Shot(bang + 330, 140, Film.Mark.point(new Vec3(0.5, 150, 30)), new Vec3(0.5, 166, 130), Film.Mark.point(new Vec3(-300, 250, 120)),
                        sun, 0, 92, 80, 0, Film.Shot.SMOOTH))
                .title(bang + 350, 100, "The First Day", "And the evening and the morning were the first day.", 0xFFF5D77A, 2.6f)
                .fade(bang + 420, 0x000000, 1, 40)
                .music(bang + 420, "", 0, 70)
                .lasts(bang + 470)
                .build();
        Cinema.play(film, voidLevel.players());
        return true;
    }

    /** The forming of a world, for those in the Void: the camera flies out to it as it swells out of nothing. */
    public static void genesis(ServerLevel voidLevel, @Nullable ServerPlayer caster, Planet p) {
        if (!Cinema.enabled() || voidLevel.players().isEmpty()) return;
        Vec3 at = VoidSpace.planetPos(p);
        double r = VoidSpace.planetRadius(p.type.size);
        Vec3 sunward = VoidSpace.SUN.subtract(at).normalize();
        Vec3 side = sunward.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 far = sunward.scale(r * 9).add(side.scale(r * 3)).add(up.scale(r * 2));
        Vec3 near = sunward.scale(r * 4).add(side.scale(r * 1.5)).add(up.scale(r * 0.8));
        Vec3 from = caster != null ? caster.getEyePosition() : VoidSpace.arrival().add(0, 1.6, 0);
        Vec3 toward = from.add(at.subtract(from).scale(0.15));
        Film.Mark world = Film.Mark.planet(p.name, Vec3.ZERO);
        String kind = p.type.getSerializedName().replace('_', ' ');
        String[] verse = verse(p.type);
        Film film = Film.make("genesis", FiatLuxDimensions.VOID)
                .shot(0, 70, Film.Mark.point(from), toward, world, Vec3.ZERO, 0, 70, Film.Shot.IN)
                .shot(new Film.Shot(70, 120, Film.Mark.planet(p.name, far), near, world, Vec3.ZERO, 0, 70, 70, 0, Film.Shot.SMOOTH))
                .shot(new Film.Shot(190, 150, Film.Mark.planet(p.name, near), near, world, Vec3.ZERO, 75, 70, 66, 0, Film.Shot.SMOOTH))
                .letterbox(0, 1, 25)
                .music(0, "minecraft:music.game", 0.65f, 30)
                .sound(8, "minecraft:block.respawn_anchor.charge", 0.9f, 0.6f)
                .sound(40, "minecraft:block.amethyst_block.resonate", 1f, 0.5f)
                .narrate(20, 130, verse[0], verse[1])
                .title(110, 100, p.name, (startsWithVowel(kind) ? "an " : "a ") + kind + " world", 0xFF000000 | p.type.accentColor, 3f)
                .narrate(205, 90, "And God saw that it was good.", "Genesis 1:10")
                .fade(300, 0x000000, 1, 30)
                .music(300, "", 0, 40)
                .lasts(340)
                .build();
        Cinema.play(film, voidLevel.players());
    }

    private static boolean startsWithVowel(String s) {
        return !s.isEmpty() && "aeiou".indexOf(Character.toLowerCase(s.charAt(0))) >= 0;
    }

    private static String[] verse(PlanetType type) {
        return switch (type) {
            case EARTHLIKE -> new String[]{"And God said, Let the waters under the heaven be gathered together unto one place, and let the dry land appear: "
                    + "and it was so. And God called the dry land Earth; and the gathering together of the waters called he Seas.", "Genesis 1:9-10"};
            case VOLCANIC -> new String[]{"He looketh on the earth, and it trembleth: he toucheth the hills, and they smoke.", "Psalm 104:32"};
            case BARREN_MOON -> new String[]{"He appointed the moon for seasons: the sun knoweth his going down.", "Psalm 104:19"};
            case GAS_GIANT -> new String[]{"Canst thou bind the sweet influences of Pleiades, or loose the bands of Orion?", "Job 38:31"};
            case CRYSTALLINE -> new String[]{"And the foundations of the wall of the city were garnished with all manner of precious stones.", "Revelation 21:19"};
        };
    }

    /** A short film anywhere, to see the toolkit at work: it circles whoever asks for it. */
    public static void test(ServerPlayer p) {
        Vec3 c = p.position().add(0, 1.2, 0);
        Film film = Film.make("test", p.level().dimension())
                .orbit(0, 200, c, 7, 2.5, p.getYRot(), 120, Film.Shot.SMOOTH)
                .shot(new Film.Shot(200, 120, Film.Mark.point(c.add(0, 2, 0)), c.add(0, 18, 0), Film.Mark.point(c), c, 90, 0, 0, 0, Film.Shot.SMOOTH))
                .fade(0, 0x000000, 1, 0)
                .fade(0, 0x000000, 0, 40)
                .letterbox(0, 1, 30)
                .caption(20, 120, "Somewhere in the cosmos, " + p.getGameProfile().getName())
                .title(50, 100, "Fiat Lux", "a film", 0xFFF5D77A, 3f)
                .narrate(150, 90, "And God saw every thing that he had made, and, behold, it was very good.", "Genesis 1:31")
                .shake(200, 0.8f, 40)
                .flash(200, 0xFFFFFF, 0.6f, 30)
                .sound(200, "minecraft:entity.generic.explode", 0.7f, 0.6f)
                .music(10, "minecraft:music.game", 0.6f, 40)
                .grade(220, 0x101830, 0.35f, 60)
                .fade(290, 0x000000, 1, 30)
                .music(290, "", 0, 30)
                .lasts(330)
                .build();
        Cinema.play(film, java.util.List.of(p));
    }
}
