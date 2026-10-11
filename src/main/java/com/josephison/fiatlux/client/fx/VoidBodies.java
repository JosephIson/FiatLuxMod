package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.client.ClientCosmos;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.VoidSpace;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The bodies of the Void, drawn in the world so they can be approached: the face of the deep and
 * the Spirit moving upon it before the Big Bang (Genesis 1:2), Heaven's Gate of pearl, the greater
 * light (Genesis 1:16) and every created world on its orbit - a lit globe with its own surface,
 * atmosphere, night-side city lights and (for gas giants) rings. Shares its geometry with the
 * server through {@link VoidSpace}, so flying into a world's body is what carries you down onto it.
 *
 * Bodies further than the far plane are drawn as scaled impostors at a fixed distance, so they keep
 * their true apparent size at any render distance.
 */
public final class VoidBodies {
    private static final double IMPOSTOR = 300.0;

    private VoidBodies() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != FiatLuxDimensions.VOID) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        float bang = ClientCosmos.secondsSinceBigBang(partial);
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();

        // solid bodies
        VertexConsumer solid = buffers.getBuffer(RenderType.debugQuads());
        float deep = bang < 0 ? 1f : Mth.clamp(1f - bang / 20f, 0f, 1f);
        if (deep > 0) drawDeep(solid, m, cam, time, deep);
        drawGate(solid, m, time);
        float sunGrowth = bang < 0 ? 0f : Mth.clamp((bang - 4f) / 6f, 0f, 1f);
        if (sunGrowth > 0) sphere(solid, m, cam, VoidSpace.SUN, VoidSpace.SUN_RADIUS * sunGrowth, -1, 0, time, 0);
        var planets = ClientCosmos.get().planets();
        Vec3[] pos = new Vec3[planets.size()];
        for (int i = 0; i < planets.size(); i++) {
            SyncCosmosPayload.PlanetView p = planets.get(i);
            pos[i] = VoidSpace.planetPos(p.orbitRadius(), ClientCosmos.phase(p, partial), p.inclination());
            sphere(solid, m, cam, pos[i], VoidSpace.planetRadius(p.size()), p.type(), p.name().hashCode(), time, p.inhabited() ? 1 : 0);
            if (p.type() == PlanetType.GAS_GIANT.ordinal()) rings(solid, m, cam, pos[i], VoidSpace.planetRadius(p.size()), p.color());
        }
        var holes = ClientCosmos.get().blackHoles();
        for (var h : holes) horizon(solid, m, cam, new Vec3(h.x(), h.y(), h.z()), h.radius());
        buffers.endBatch(RenderType.debugQuads());

        // light: the gate's glory, the sun's corona, atmospheres and orbit paths
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        gateGlory(glow, m, left, up, time);
        if (sunGrowth > 0) {
            Vec3 s = impostor(cam, VoidSpace.SUN, VoidSpace.SUN_RADIUS);
            double k = scale(cam, VoidSpace.SUN);
            halo(glow, m, s, left, up, (float) (VoidSpace.SUN_RADIUS * k * sunGrowth), 0.98f, 3.2f, 0xFFF0B0, 0.85f);
            halo(glow, m, s, left, up, (float) (VoidSpace.SUN_RADIUS * k * sunGrowth), 1.0f, 6.5f, 0xFFB050, 0.35f);
        }
        for (int i = 0; i < planets.size(); i++) {
            SyncCosmosPayload.PlanetView p = planets.get(i);
            float r = (float) VoidSpace.planetRadius(p.size());
            orbit(glow, m, p.orbitRadius(), p.inclination(), p.color());
            Vec3 c = impostor(cam, pos[i], r);
            float rr = (float) (r * scale(cam, pos[i]));
            halo(glow, m, c, left, up, rr, 0.97f, 1.22f, p.atmosphere() & 0xFFFFFF, 0.7f);
        }
        for (var h : holes) blackHoleLight(glow, m, cam, left, up, new Vec3(h.x(), h.y(), h.z()), h.radius(), h.name().hashCode(), time);
        buffers.endBatch(RenderType.lightning());

        // names of the worlds, so the Creator can find them
        Font font = mc.font;
        for (int i = 0; i < planets.size(); i++) {
            SyncCosmosPayload.PlanetView p = planets.get(i);
            double d = cam.distanceTo(pos[i]);
            if (d > 420) continue;
            float r = (float) VoidSpace.planetRadius(p.size());
            Vec3 at = impostor(cam, pos[i].add(0, r * 1.35, 0), r);
            ps.pushPose();
            ps.translate(at.x, at.y, at.z);
            ps.mulPose(camera.rotation());
            float s = (float) (0.025 * Math.max(1.0, Math.min(d, IMPOSTOR) / 6.0));
            ps.scale(s, -s, s);
            Component name = Component.literal(p.name());
            font.drawInBatch(name, -font.width(name) / 2f, 0, 0xFFF5E6B8, false, ps.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
            ps.popPose();
        }
        for (var h : holes) {
            Vec3 hc = new Vec3(h.x(), h.y(), h.z());
            double d = cam.distanceTo(hc);
            if (d > 420) continue;
            Vec3 at = impostor(cam, hc.add(0, h.radius() * 4.2, 0), h.radius());
            ps.pushPose();
            ps.translate(at.x, at.y, at.z);
            ps.mulPose(camera.rotation());
            float s = (float) (0.025 * Math.max(1.0, Math.min(d, IMPOSTOR) / 6.0));
            ps.scale(s, -s, s);
            Component name = Component.literal(h.name());
            font.drawInBatch(name, -font.width(name) / 2f, 0, 0xFFC8A0FF, false, ps.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
            ps.popPose();
        }
        buffers.endBatch();
        ps.popPose();
    }

    // ------------------------------------------------------------------ black holes

    /** The event horizon: a sphere of perfect black, from which no light returns. */
    private static void horizon(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 center, double radius) {
        Vec3 c = impostor(cam, center, radius);
        double r = radius * scale(cam, center);
        int lat = 14, lon = 24;
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                Vec3[] v = new Vec3[4];
                for (int k = 0; k < 4; k++) {
                    double th = Math.PI * (i + (k >= 2 ? 1 : 0)) / lat - Math.PI / 2;
                    double ph = Math.PI * 2 * (j + (k == 1 || k == 2 ? 1 : 0)) / lon;
                    v[k] = c.add(Math.cos(th) * Math.cos(ph) * r, Math.sin(th) * r, Math.cos(th) * Math.sin(ph) * r);
                }
                quad(vc, m, v[0], v[1], v[2], v[3], 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000);
            }
        }
    }

    /** Disk spin axis of a black hole, varied by its name so no two lie alike. */
    private static Vec3 axis(int seed) {
        double a = (seed & 1023) / 1023.0 * Math.PI * 2;
        return new Vec3(Math.cos(a) * 0.6, 1.0, Math.sin(a) * 0.6).normalize();
    }

    /**
     * The light around a black hole: the accretion disk (white-hot within, cooling to red without,
     * turning faster toward the centre, its approaching side brighter), the far side of the disk bent
     * over and under the shadow, the photon ring hugging the shadow's edge, twin jets along the spin
     * axis, and a faint violet halo.
     */
    private static void blackHoleLight(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f left, Vector3f up, Vec3 center, double radius,
                                       int seed, float time) {
        Vec3 c = impostor(cam, center, radius);
        double k = scale(cam, center);
        double R = radius * k;
        Vec3 n = axis(seed);
        Vec3 u = n.cross(new Vec3(0, 0, 1)).normalize(), w = n.cross(u).normalize();
        Vec3 toCam = cam.subtract(c).normalize();
        // the accretion disk
        int rings = 9, seg = 96;
        double r0 = R * 1.8, r1 = R * 7.0;
        for (int i = 0; i < rings; i++) {
            double ra = r0 + (r1 - r0) * i / rings, rb = r0 + (r1 - r0) * (i + 1) / rings;
            for (int j = 0; j < seg; j++) {
                double[] ang = {Math.PI * 2 * j / seg, Math.PI * 2 * (j + 1) / seg};
                Vec3[] v = new Vec3[4];
                int[] col = new int[4];
                for (int q = 0; q < 4; q++) {
                    double rr = q < 2 ? ra : rb;
                    double a = ang[q == 0 || q == 3 ? 0 : 1];
                    Vec3 radial = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
                    v[q] = c.add(radial.scale(rr));
                    double t = (rr - r0) / (r1 - r0);
                    double spin = time * 0.09 * Math.pow(R / rr, 1.5) * 6;
                    double swirl = 0.68 + 0.32 * Math.sin(a * 7 - spin + rr * 0.9 / Math.max(0.01, k));
                    Vec3 vel = n.cross(radial);                                   // orbital direction
                    double beam = Mth.clamp(1.0 + 0.7 * vel.dot(toCam), 0.35, 1.7);   // Doppler beaming
                    float rC = (float) Mth.lerp(t, 255, 200), gC = (float) Mth.lerp(t, 245, 60), bC = (float) Mth.lerp(t, 220, 25);
                    float bright = (float) (swirl * beam * (1.0 - t * 0.55));
                    int alpha = (int) (255 * Mth.clamp(bright, 0, 1) * Math.min(1.0, (1 - t) * 4) * Math.min(1.0, t * 6 + 0.4));
                    col[q] = argb(alpha, rC * bright, gC * bright, bC * bright);
                }
                quad(vc, m, v[0], v[1], v[2], v[3], col[0], col[1], col[2], col[3]);
                quad(vc, m, v[3], v[2], v[1], v[0], col[3], col[2], col[1], col[0]);   // seen from either face
            }
        }
        // apparent size of the shadow from here
        double d = Math.max(R * 1.05, cam.distanceTo(c));
        double sil = R * d / Math.sqrt(d * d - R * R);
        // projected disk plane on screen, for the lensed image
        double nx = n.x * left.x + n.y * left.y + n.z * left.z, ny = n.x * up.x + n.y * up.y + n.z * up.z;
        double nl = Math.max(1e-4, Math.sqrt(nx * nx + ny * ny));
        nx /= nl; ny /= nl;
        int lseg = 72;
        for (int j = 0; j < lseg; j++) {
            float a0 = Mth.TWO_PI * j / lseg, a1 = Mth.TWO_PI * (j + 1) / lseg;
            Vec3 d0 = screen(left, up, a0), d1 = screen(left, up, a1);
            // the far side of the disk, bent around the shadow: strongest above and below the disk plane
            double i0 = Math.pow(Math.abs(Math.cos(a0) * nx + Math.sin(a0) * ny), 2.2), i1 = Math.pow(Math.abs(Math.cos(a1) * nx + Math.sin(a1) * ny), 2.2);
            int l0 = argb((int) (230 * i0), 255, 200, 120), l1 = argb((int) (230 * i1), 255, 200, 120);
            int o0 = argb(0, 255, 140, 60), o1 = argb(0, 255, 140, 60);
            quad(vc, m, c.add(d0.scale(sil * 1.18)), c.add(d1.scale(sil * 1.18)), c.add(d1.scale(sil * 2.2)), c.add(d0.scale(sil * 2.2)), l0, l1, o1, o0);
            // the photon ring
            int p = argb(255, 255, 244, 220), p0 = argb(0, 255, 230, 190);
            quad(vc, m, c.add(d0.scale(sil * 1.01)), c.add(d1.scale(sil * 1.01)), c.add(d1.scale(sil * 1.13)), c.add(d0.scale(sil * 1.13)), p, p, p0, p0);
            // a faint violet halo
            int h = argb(40, 150, 110, 255), h0 = argb(0, 120, 80, 255);
            quad(vc, m, c.add(d0.scale(sil * 1.13)), c.add(d1.scale(sil * 1.13)), c.add(d1.scale(sil * 9)), c.add(d0.scale(sil * 9)), h, h, h0, h0);
        }
        // twin jets along the spin axis
        for (int dir = -1; dir <= 1; dir += 2) {
            Vec3 ax = n.scale(dir);
            Vec3 side = ax.cross(toCam);
            side = side.lengthSqr() < 1e-6 ? u : side.normalize();
            int steps = 12;
            for (int i = 0; i < steps; i++) {
                double t0 = i / (double) steps, t1 = (i + 1) / (double) steps;
                double len = R * 30;
                Vec3 a = c.add(ax.scale(R * 1.2 + len * t0)), b = c.add(ax.scale(R * 1.2 + len * t1));
                double w0 = R * (0.25 + 1.3 * t0), w1 = R * (0.25 + 1.3 * t1);
                float flick = 0.8f + 0.2f * Mth.sin(time * 0.3f + i + dir);
                int c0 = argb((int) (200 * (1 - t0) * flick), 150, 180, 255), c1 = argb((int) (200 * (1 - t1) * flick), 150, 180, 255);
                quad(vc, m, a.add(side.scale(w0)), b.add(side.scale(w1)), b.subtract(side.scale(w1)), a.subtract(side.scale(w0)), c0, c1, c1, c0);
                int k0 = argb((int) (255 * (1 - t0)), 235, 240, 255), k1 = argb((int) (255 * (1 - t1)), 235, 240, 255);
                quad(vc, m, a.add(side.scale(w0 * 0.2)), b.add(side.scale(w1 * 0.2)), b.subtract(side.scale(w1 * 0.2)), a.subtract(side.scale(w0 * 0.2)), k0, k1, k1, k0);
            }
        }
    }

    private static Vec3 screen(Vector3f left, Vector3f up, float a) {
        return new Vec3(left.x * Mth.cos(a) + up.x * Mth.sin(a), left.y * Mth.cos(a) + up.y * Mth.sin(a), left.z * Mth.cos(a) + up.z * Mth.sin(a));
    }

    // ------------------------------------------------------------------ helpers

    /** Distance-preserving impostor: bodies beyond IMPOSTOR are drawn at IMPOSTOR along their true direction. */
    private static Vec3 impostor(Vec3 cam, Vec3 pos, double radius) {
        Vec3 d = pos.subtract(cam);
        double len = d.length();
        if (len <= IMPOSTOR) return pos;
        return cam.add(d.scale(IMPOSTOR / len));
    }

    private static double scale(Vec3 cam, Vec3 pos) {
        double len = pos.distanceTo(cam);
        return len <= IMPOSTOR ? 1.0 : IMPOSTOR / len;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb0, int argb1, int argb2, int argb3) {
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(argb0);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(argb1);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(argb2);
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(argb3);
    }

    private static int argb(int a, float r, float g, float b) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp((int) r, 0, 255) << 16) | (Mth.clamp((int) g, 0, 255) << 8) | Mth.clamp((int) b, 0, 255);
    }

    // ------------------------------------------------------------------ the face of the deep

    /** Dark waters far below, rippling, with the Spirit of God moving upon them as a drifting light. */
    private static void drawDeep(VertexConsumer vc, Matrix4f m, Vec3 cam, float time, float alpha) {
        double y = 40.0;
        int n = 24;
        double size = 360.0;
        double cx = Math.floor(cam.x / 15.0) * 15.0, cz = Math.floor(cam.z / 15.0) * 15.0;
        double sx = VoidSpace.GATE.x + Math.sin(time * 0.0035) * 90, sz = VoidSpace.GATE.z + 40 + Math.cos(time * 0.0027) * 90;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Vec3[] v = new Vec3[4];
                int[] col = new int[4];
                for (int k = 0; k < 4; k++) {
                    double x = cx - size + (i + (k == 1 || k == 2 ? 1 : 0)) * size * 2 / n;
                    double z = cz - size + (j + (k >= 2 ? 1 : 0)) * size * 2 / n;
                    double ripple = Math.sin(x * 0.07 + time * 0.03) * Math.cos(z * 0.06 - time * 0.025);
                    v[k] = new Vec3(x, y + ripple * 0.6, z);
                    double edge = Math.max(Math.abs(x - cx), Math.abs(z - cz)) / size;
                    double spirit = Math.exp(-((x - sx) * (x - sx) + (z - sz) * (z - sz)) / 1800.0);
                    float lum = (float) (8 + ripple * 5 + spirit * 70);
                    int a = (int) (255 * alpha * Mth.clamp(1.0 - edge * edge, 0, 1));
                    col[k] = argb(a, lum * 0.55f, lum * 0.7f, lum * 1.35f + 10);
                }
                quad(vc, m, v[0], v[1], v[2], v[3], col[0], col[1], col[2], col[3]);
            }
        }
    }

    // ------------------------------------------------------------------ Heaven's Gate

    /** A gate of one pearl: two pillars, a crowning arch, and a shimmering veil of Heaven's light. */
    private static void drawGate(VertexConsumer vc, Matrix4f m, float time) {
        Vec3 g = VoidSpace.GATE;
        float hw = (float) VoidSpace.GATE_HALF_WIDTH, hh = (float) VoidSpace.GATE_HALF_HEIGHT;
        int pearl = 0xFFECE6F4, pearlShade = 0xFFC8C0D8;
        for (int side = -1; side <= 1; side += 2) {
            box(vc, m, g.add(side * (hw + 0.9), -0.5, 0), 0.9, hh + 1.6, 1.4, pearl, pearlShade);
        }
        for (int k = 0; k < 14; k++) {   // the arch
            double a0 = Math.PI * k / 14, a1 = Math.PI * (k + 1) / 14;
            double am = (a0 + a1) / 2;
            Vec3 c = g.add(Math.cos(am) * (hw + 0.9), hh - 0.5 + Math.sin(am) * (hw * 0.7), 0);
            box(vc, m, c, 0.95, 0.95, 1.5, pearl, pearlShade);
        }
        // the veil: Heaven's light seen through the gate, shimmering
        int rows = 8;
        for (int i = 0; i < rows; i++) {
            float y0 = -hh + (2 * hh) * i / rows, y1 = -hh + (2 * hh) * (i + 1) / rows;
            float s0 = 0.5f + 0.5f * Mth.sin(time * 0.08f + i * 0.9f), s1 = 0.5f + 0.5f * Mth.sin(time * 0.08f + (i + 1) * 0.9f);
            int c0 = argb(170, 255, 236 + 19 * s0, 190 + 50 * s0), c1 = argb(170, 255, 236 + 19 * s1, 190 + 50 * s1);
            quad(vc, m, g.add(-hw, y0, 0), g.add(hw, y0, 0), g.add(hw, y1, 0), g.add(-hw, y1, 0), c0, c0, c1, c1);
        }
    }

    private static void box(VertexConsumer vc, Matrix4f m, Vec3 c, double hx, double hy, double hz, int light, int shade) {
        Vec3[] p = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            p[i] = c.add((i & 1) == 0 ? -hx : hx, (i & 2) == 0 ? -hy : hy, (i & 4) == 0 ? -hz : hz);
        }
        quad(vc, m, p[2], p[3], p[7], p[6], light, light, light, light);       // top
        quad(vc, m, p[0], p[1], p[5], p[4], shade, shade, shade, shade);       // bottom
        quad(vc, m, p[0], p[1], p[3], p[2], light, light, light, light);       // -z
        quad(vc, m, p[4], p[5], p[7], p[6], light, light, light, light);       // +z
        quad(vc, m, p[0], p[4], p[6], p[2], shade, shade, shade, shade);       // -x
        quad(vc, m, p[1], p[5], p[7], p[3], shade, shade, shade, shade);       // +x
    }

    /** Rays of glory streaming from the gate - the only light in the formless dark. */
    private static void gateGlory(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, float time) {
        Vec3 g = VoidSpace.GATE;
        halo(vc, m, g, left, up, 3.0f, 0.0f, 4.2f, 0xFFE6A8, 0.55f);
        for (int i = 0; i < 16; i++) {
            float a = Mth.TWO_PI * i / 16 + time * 0.002f;
            float len = 12f + 8f * Mth.sin(i * 2.7f + time * 0.03f);
            float w = 0.35f;
            Vec3 d = new Vec3(Mth.cos(a), Mth.sin(a), 0);
            Vec3 n = new Vec3(-Mth.sin(a), Mth.cos(a), 0).scale(w);
            Vec3 a0 = g.add(d.scale(5.5)), a1 = g.add(d.scale(5.5 + len));
            int c0 = argb(110, 255, 230, 170), c1 = argb(0, 255, 210, 140);
            quad(vc, m, a0.add(n), a1.add(n.scale(2.5)), a1.subtract(n.scale(2.5)), a0.subtract(n), c0, c1, c1, c0);
        }
    }

    // ------------------------------------------------------------------ globes

    /**
     * A lit globe. type -1 is the greater light (self-luminous); otherwise the surface follows the
     * world type, the night side is dark (with city lights if inhabited) and the surface turns.
     */
    private static void sphere(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 center, double radius, int type, int seed, float time, int lights) {
        if (radius <= 0.01) return;
        Vec3 c = impostor(cam, center, radius);
        double r = radius * scale(cam, center);
        int lat = 16, lon = 28;
        PlanetType pt = type >= 0 && type < PlanetType.values().length ? PlanetType.values()[type] : null;
        Vec3 toSun = VoidSpace.SUN.subtract(center);
        toSun = toSun.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : toSun.normalize();
        float spin = time * 0.0015f + (seed & 255) * 0.1f;
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                Vec3[] v = new Vec3[4];
                int[] col = new int[4];
                for (int k = 0; k < 4; k++) {
                    double th = Math.PI * (i + (k >= 2 ? 1 : 0)) / lat - Math.PI / 2;
                    double ph = Math.PI * 2 * (j + (k == 1 || k == 2 ? 1 : 0)) / lon;
                    Vec3 n = new Vec3(Math.cos(th) * Math.cos(ph), Math.sin(th), Math.cos(th) * Math.sin(ph));
                    v[k] = c.add(n.scale(r));
                    col[k] = surface(pt, n, seed, spin, toSun, time, lights);
                }
                quad(vc, m, v[0], v[1], v[2], v[3], col[0], col[1], col[2], col[3]);
            }
        }
    }

    private static int surface(PlanetType type, Vec3 n, int seed, float spin, Vec3 toSun, float time, int lights) {
        if (type == null) {   // the sun: a white-gold furnace with drifting granulation
            float gran = (float) (0.5 + 0.5 * Math.sin(n.x * 9 + time * 0.02) * Math.sin(n.y * 11 - time * 0.017) * Math.sin(n.z * 8 + time * 0.013));
            return argb(255, 255, 222 + 30 * gran, 140 + 90 * gran);
        }
        // turn the surface: rotate the lookup normal about the axis
        double cs = Math.cos(spin), sn = Math.sin(spin);
        double nx = n.x * cs - n.z * sn, nz = n.x * sn + n.z * cs, ny = n.y;
        double s = (seed % 97) * 0.13;
        double field = Math.sin(nx * 4.1 + s) * Math.sin(ny * 3.7 + s * 1.3) * Math.sin(nz * 4.3 + s * 0.7)
                + 0.5 * Math.sin(nx * 9.3 - s) * Math.sin(ny * 8.1 + s) * Math.sin(nz * 7.7 + s * 2);
        int base = type.baseColor, accent = type.accentColor;
        float br = (base >> 16) & 255, bg = (base >> 8) & 255, bb = base & 255;
        float ar = (accent >> 16) & 255, ag = (accent >> 8) & 255, ab = accent & 255;
        float r, g, b;
        boolean glowing = false;
        switch (type) {
            case EARTHLIKE -> {
                boolean land = field > 0.12;
                r = land ? ar : br; g = land ? ag : bg; b = land ? ab : bb;
                if (Math.abs(ny) > 0.86) { r = g = b = 245; }                          // polar ice
                if (field > 0.55 && Math.abs(ny) < 0.6) { r = 200; g = 180; b = 120; }  // deserts and mountains
            }
            case VOLCANIC -> {
                boolean lava = Math.abs(field) < 0.08;
                r = lava ? ar : br * (0.7f + (float) field * 0.3f); g = lava ? ag : bg * 0.8f; b = lava ? ab : bb * 0.8f;
                glowing = lava;
            }
            case BARREN_MOON -> {
                float crater = (float) Math.max(0, field) * 0.35f;
                r = br * (1 - crater); g = bg * (1 - crater); b = bb * (1 - crater);
            }
            case GAS_GIANT -> {
                double band = Math.sin(ny * 14 + field * 1.6) * 0.5 + 0.5;
                r = Mth.lerp((float) band, br, ar); g = Mth.lerp((float) band, bg, ag); b = Mth.lerp((float) band, bb, ab);
                if (Math.abs(ny + 0.25) < 0.08 && Math.abs(nx - 0.5) < 0.25) { r = 200; g = 110; b = 70; }  // a great storm
            }
            default -> {   // crystalline: pale facets catching the light
                double facet = Math.floor((field + 1) * 4) / 4;
                r = Mth.lerp((float) facet * 0.5f, br, ar); g = Mth.lerp((float) facet * 0.5f, bg, ag); b = Mth.lerp((float) facet * 0.5f, bb, ab);
            }
        }
        double lit = Math.max(0, n.x * toSun.x + n.y * toSun.y + n.z * toSun.z);
        float light = (float) (0.07 + 0.93 * lit);
        if (glowing) light = Math.max(light, 0.9f);
        r *= light; g *= light; b *= light;
        if (lights > 0 && lit < 0.08 && field > 0.2 && (Math.sin(nx * 40 + seed) * Math.sin(nz * 37) > 0.55)) {   // cities in the night
            r = 255; g = 214; b = 130;
        }
        return argb(255, r, g, b);
    }

    /** The rings of a gas giant: banded, tilted with its orbit's plane. */
    private static void rings(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 center, double radius, int color) {
        Vec3 c = impostor(cam, center, radius);
        double k = scale(cam, center);
        double r0 = radius * 1.45 * k, r1 = radius * 2.3 * k;
        double tilt = 0.32;
        int bands = 5;
        for (int b = 0; b < bands; b++) {
            double ra = r0 + (r1 - r0) * b / bands, rb = r0 + (r1 - r0) * (b + 1) / bands - 0.15 * k;
            int a = 150 + (b % 2) * 60;
            int col = argb(a, ((color >> 16) & 255) * 0.9f, ((color >> 8) & 255) * 0.85f, (color & 255) * 0.75f);
            for (int i = 0; i < 48; i++) {
                double a0 = Math.PI * 2 * i / 48, a1 = Math.PI * 2 * (i + 1) / 48;
                Vec3 p0 = c.add(Math.cos(a0) * ra, Math.sin(a0) * ra * tilt, Math.sin(a0) * ra);
                Vec3 p1 = c.add(Math.cos(a1) * ra, Math.sin(a1) * ra * tilt, Math.sin(a1) * ra);
                Vec3 p2 = c.add(Math.cos(a1) * rb, Math.sin(a1) * rb * tilt, Math.sin(a1) * rb);
                Vec3 p3 = c.add(Math.cos(a0) * rb, Math.sin(a0) * rb * tilt, Math.sin(a0) * rb);
                quad(vc, m, p0, p1, p2, p3, col, col, col, col);
            }
        }
    }

    /** A camera-facing glowing annulus from inner to outer radius (multiples of r). */
    private static void halo(VertexConsumer vc, Matrix4f m, Vec3 c, Vector3f left, Vector3f up, float r, float inner, float outer, int rgb, float alpha) {
        int red = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int a0 = (int) (255 * alpha);
        for (int i = 0; i < 40; i++) {
            float t0 = Mth.TWO_PI * i / 40, t1 = Mth.TWO_PI * (i + 1) / 40;
            Vec3 d0 = new Vec3(left.x * Mth.cos(t0) + up.x * Mth.sin(t0), left.y * Mth.cos(t0) + up.y * Mth.sin(t0), left.z * Mth.cos(t0) + up.z * Mth.sin(t0));
            Vec3 d1 = new Vec3(left.x * Mth.cos(t1) + up.x * Mth.sin(t1), left.y * Mth.cos(t1) + up.y * Mth.sin(t1), left.z * Mth.cos(t1) + up.z * Mth.sin(t1));
            quad(vc, m, c.add(d0.scale(r * inner)), c.add(d1.scale(r * inner)), c.add(d1.scale(r * outer)), c.add(d0.scale(r * outer)),
                    argb(a0, red, g, b), argb(a0, red, g, b), argb(0, red, g, b), argb(0, red, g, b));
        }
    }

    /** A faint path along a world's orbit, so its course through the Expanse can be followed. */
    private static void orbit(VertexConsumer vc, Matrix4f m, float orbitRadius, float inclination, int color) {
        int seg = 160;
        double w = 0.35;
        int col = argb(60, ((color >> 16) & 255) * 0.6f + 100, ((color >> 8) & 255) * 0.6f + 90, (color & 255) * 0.6f + 60);
        for (int i = 0; i < seg; i++) {
            Vec3 a = VoidSpace.planetPos(orbitRadius, i / (double) seg, inclination), b = VoidSpace.planetPos(orbitRadius, (i + 1) / (double) seg, inclination);
            Vec3 out = a.subtract(VoidSpace.SUN).normalize().scale(w);
            quad(vc, m, a.subtract(out), b.subtract(out), b.add(out), a.add(out), col, col, col, col);
        }
    }
}
