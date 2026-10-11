package com.josephison.fiatlux.world;

import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The geometry of the Void - "the earth was without form, and void; and darkness was upon the face
 * of the deep" (Genesis 1:2) - the Cosmic Expanse that lies beyond Heaven, where the universe
 * begins. Shared by the server (gates, entering worlds) and the client (drawing the bodies), so both
 * agree on where everything is.
 *
 * Heaven's Gate hangs at the origin. Ahead of it, after the Big Bang, burns the greater light (Gen
 * 1:16), and every created world circles it on its orbit, a real body you can fly to and enter.
 */
public final class VoidSpace {
    /** Centre of the veil of Heaven's Gate in the Void. */
    public static final Vec3 GATE = new Vec3(0.5, 160.0, 0.5);
    public static final double GATE_HALF_WIDTH = 3.5;
    public static final double GATE_HALF_HEIGHT = 4.5;
    /** The greater light, centre of the created worlds' orbits. */
    public static final Vec3 SUN = new Vec3(0.5, 160.0, 300.5);
    public static final double SUN_RADIUS = 20.0;
    /** Blocks per astronomical unit of orbit. */
    public static final double AU = 110.0;

    private VoidSpace() {}

    /** Where a soul arrives in the Void: before the gate, facing out toward the greater light. */
    public static Vec3 arrival() {
        return new Vec3(GATE.x, GATE.y - 1.5, GATE.z + 9.0);
    }

    /** Centre of a world's body for its orbit (radius in AU, phase 0..1, inclination in degrees). */
    public static Vec3 planetPos(double orbitRadius, double phase, double inclinationDeg) {
        double a = phase * Math.PI * 2;
        double r = orbitRadius * AU;
        double tilt = Math.sin(Math.toRadians(inclinationDeg));
        return SUN.add(Math.cos(a) * r, Math.sin(a) * r * tilt, Math.sin(a) * r);
    }

    public static Vec3 planetPos(Planet p) {
        return planetPos(p.orbitRadius, p.phase, p.inclination);
    }

    /** Radius in blocks of a world's body, from its type's relative size. */
    public static double planetRadius(double size) {
        return 5.0 + size * 9.0;
    }

    /** Index of the created world whose body contains pos (within margin), or -1. */
    public static int planetAt(List<Planet> planets, Vec3 pos, double margin) {
        for (int i = 0; i < planets.size(); i++) {
            Planet p = planets.get(i);
            double r = planetRadius(p.type.size) + margin;
            if (planetPos(p).distanceToSqr(pos) <= r * r) return i;
        }
        return -1;
    }

    /** Is pos within the veil of Heaven's Gate? */
    public static boolean inGate(Vec3 pos) {
        return Math.abs(pos.x - GATE.x) < GATE_HALF_WIDTH && Math.abs(pos.y - GATE.y) < GATE_HALF_HEIGHT && Math.abs(pos.z - GATE.z) < 0.9;
    }

    /**
     * Where one rising out of a world's atmosphere appears in the Void: above its sunlit face, with
     * the greater light behind, so the world is seen in full day.
     */
    public static Vec3 leavingWorld(Planet p) {
        Vec3 c = planetPos(p);
        Vec3 toSun = SUN.subtract(c);
        toSun = toSun.lengthSqr() < 1e-6 ? new Vec3(0, 0, -1) : toSun.normalize();
        Vec3 out = toSun.scale(0.8).add(0, 0.6, 0).normalize();
        return c.add(out.scale(planetRadius(p.type.size) + 12.0));
    }

    /** Yaw (degrees, Minecraft convention) that faces from `from` toward `to`. */
    public static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }
}
