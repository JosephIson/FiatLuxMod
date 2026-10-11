package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.client.ClientCosmos;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * Galaxies on the celestial sphere, as they unfurl after the Big Bang. A spiral galaxy has a faint
 * outer halo, a luminous disk, a golden bulge around a white core with its central black hole, and
 * arms of individual stars that shade from old gold near the core to young blue at the rim, studded
 * with pink star-forming knots and edged by dark dust lanes. Elliptical galaxies are smooth swarms of
 * old golden stars. Everything turns slowly.
 *
 * In the dark Void the light is additive and dust lanes are drawn over it; over Heaven's bright sky
 * the same shapes are drawn with ordinary alpha in pastel tones so they stay visible.
 */
public final class GalaxyRenderer {
    private static final float DIST = -86f;

    private GalaxyRenderer() {}

    public static void draw(PoseStack ps, float secondsSinceBang, float time, boolean darkSky) {
        float formation = Mth.clamp((secondsSinceBang - 2f) / 20f, 0f, 1f);
        if (formation <= 0) return;
        var galaxies = ClientCosmos.get().galaxies();
        for (int gi = 0; gi < galaxies.size(); gi++) {
            SyncCosmosPayload.GalaxyView g = galaxies.get(gi);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(g.yaw()));
            ps.mulPose(Axis.XP.rotationDegrees(g.pitch()));   // +x lifts -z above the horizon
            ps.mulPose(Axis.ZP.rotationDegrees(g.yaw() * 1.7f));
            // the disk is seen at an angle: squash one axis by the galaxy's own inclination
            float incline = 0.35f + 0.6f * Math.abs(Mth.sin(g.yaw() * 0.37f + g.pitch()));
            ps.scale(1f, incline, 1f);
            ps.mulPose(Axis.ZP.rotationDegrees(time * 0.012f * (gi % 2 == 0 ? 1 : -1)));   // slow rotation
            Matrix4f m = ps.last().pose();
            RandomSource rand = RandomSource.create((long) (g.yaw() * 1000) ^ ((long) g.hue() << 8));
            int hr = (g.hue() >> 16) & 255, hg = (g.hue() >> 8) & 255, hb = g.hue() & 255;
            if (!darkSky) {   // pastel over Heaven's light
                hr = hr * 6 / 10 + 102; hg = hg * 6 / 10 + 98; hb = hb * 6 / 10 + 94;
            }
            float size = g.size() * formation;
            if (g.arms() == 0) {
                elliptical(m, rand, size, formation, darkSky);
            } else {
                spiral(m, rand, g, size, formation, hr, hg, hb, darkSky);
            }
            ps.popPose();
        }
    }

    private static void fan(Matrix4f m, float cx, float cy, float r, float z, int red, int green, int blue, int centreAlpha, float wobble, float seed) {
        if (r <= 0.01f || centreAlpha <= 0) return;
        BufferBuilder f = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        f.addVertex(m, cx, cy, z).setColor(red, green, blue, Math.min(255, centreAlpha));
        for (int i = 0; i <= 36; i++) {
            float a = Mth.TWO_PI * i / 36;
            float w = 1f + wobble * Mth.sin(a * 3 + seed) + wobble * 0.5f * Mth.sin(a * 5 + seed * 2);
            f.addVertex(m, cx + Mth.cos(a) * r * w, cy + Mth.sin(a) * r * w, z).setColor(red, green, blue, 0);
        }
        draw(f);
    }

    private static void dot(BufferBuilder b, Matrix4f m, float x, float y, float z, float s, int r, int g, int bl, int a) {
        // a diamond reads rounder than a square at star sizes
        b.addVertex(m, x, y - s, z).setColor(r, g, bl, a);
        b.addVertex(m, x + s, y, z).setColor(r, g, bl, a);
        b.addVertex(m, x, y + s, z).setColor(r, g, bl, a);
        b.addVertex(m, x - s, y, z).setColor(r, g, bl, a);
    }

    private static void dots(Matrix4f m, java.util.List<float[]> list) {
        if (list.isEmpty()) return;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] q : list) dot(b, m, q[0], q[1], q[2], q[3], (int) q[4], (int) q[5], (int) q[6], (int) q[7]);
        draw(b);
    }

    private static void spiral(Matrix4f m, RandomSource rand, SyncCosmosPayload.GalaxyView g, float size, float formation,
                               int hr, int hg, int hb, boolean darkSky) {
        int k = darkSky ? 1 : 2;   // brighter alphas over a bright sky
        // outer halo and luminous disk
        fan(m, 0, 0, size * 1.7f, DIST - 0.3f, hr, hg, hb, (int) (26 * formation * k), 0.08f, g.yaw());
        fan(m, 0, 0, size * 1.05f, DIST - 0.2f, (hr + 255) / 2, (hg + 235) / 2, (hb + 200) / 2, (int) (60 * formation * k), 0.12f, g.pitch());
        // spiral arms: soft arm glow, then individual stars
        int arms = g.arms();
        int steps = 150;
        // the shared tesselator holds one buffer at a time: gather each layer, then draw them in turn
        java.util.List<float[]> glow = new java.util.ArrayList<>(), stars = new java.util.ArrayList<>(), knots = new java.util.ArrayList<>();
        float[][] dust = new float[arms * steps][];
        int dn = 0;
        for (int arm = 0; arm < arms; arm++) {
            for (int i = 4; i < steps; i++) {
                float t = i / (float) steps;
                float ang = t * g.twist() * Mth.TWO_PI + arm * Mth.TWO_PI / arms;
                float rad = (0.12f + t * 0.88f) * size;
                float x = Mth.cos(ang) * rad, y = Mth.sin(ang) * rad;
                // colour: old gold near the core, young blue-white toward the rim, tinted by the galaxy
                float cr = Mth.lerp(t, 255, 175), cg = Mth.lerp(t, 226, 205), cb = Mth.lerp(t, 160, 255);
                cr = cr * 0.7f + hr * 0.3f; cg = cg * 0.7f + hg * 0.3f; cb = cb * 0.7f + hb * 0.3f;
                float gw = size * 0.09f * (1.2f - t * 0.5f);
                glow.add(new float[]{x, y, DIST - 0.1f, gw, (int) cr, (int) cg, (int) cb, (int) (34 * (1f - t * 0.6f) * formation * k)});
                for (int s = 0; s < 3; s++) {
                    float jx = (float) rand.nextGaussian() * size * 0.035f, jy = (float) rand.nextGaussian() * size * 0.035f;
                    float ss = (0.05f + rand.nextFloat() * 0.09f) * Math.max(1f, size / 7f);
                    stars.add(new float[]{x + jx, y + jy, DIST, ss, Math.min(255, (int) cr + 30), Math.min(255, (int) cg + 30), Math.min(255, (int) cb + 30),
                            (int) (230 * formation * (0.6f + 0.4f * rand.nextFloat()))});
                }
                if (i % 13 == 7) {   // star-forming knots, glowing pink with hydrogen
                    knots.add(new float[]{x, y, DIST + 0.05f, size * 0.03f, 255, 120, 175, (int) (210 * formation)});
                    knots.add(new float[]{x, y, DIST + 0.04f, size * 0.07f, 255, 110, 170, (int) (60 * formation * k)});
                }
                // the dust lane runs along the inner (trailing) edge of each arm
                float da = ang - 0.16f;
                dust[dn++] = new float[]{Mth.cos(da) * rad * 0.97f, Mth.sin(da) * rad * 0.97f, gw * 0.55f, t};
            }
        }
        dots(m, glow);
        dots(m, stars);
        dots(m, knots);
        // the bulge: golden layers around a white-hot core
        fan(m, 0, 0, size * 0.34f, DIST + 0.1f, 255, 214, 150, (int) (130 * formation), 0.05f, 1f);
        fan(m, 0, 0, size * 0.17f, DIST + 0.11f, 255, 236, 200, (int) (190 * formation), 0.03f, 2f);
        fan(m, 0, 0, size * 0.07f, DIST + 0.12f, 255, 255, 245, (int) (255 * formation), 0f, 3f);
        if (darkSky) {
            // dust lanes and the central black hole darken what is behind them: drawn with ordinary alpha
            RenderSystem.defaultBlendFunc();
            BufferBuilder d = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < dn; i++) {
                float[] q = dust[i];
                if (q[3] < 0.08f || q[3] > 0.85f) continue;
                dot(d, m, q[0], q[1], DIST + 0.13f, q[2], 22, 12, 8, (int) (110 * formation * (1f - q[3])));
            }
            float bh = size * 0.022f;
            dot(d, m, 0, 0, DIST + 0.15f, bh, 0, 0, 0, (int) (255 * formation));
            draw(d);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            // the photon ring around the central black hole
            BufferBuilder ring = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 24; i++) {
                float a0 = Mth.TWO_PI * i / 24, a1 = Mth.TWO_PI * (i + 1) / 24;
                float r0 = bh * 1.05f, r1 = bh * 1.6f;
                ring.addVertex(m, Mth.cos(a0) * r0, Mth.sin(a0) * r0, DIST + 0.16f).setColor(255, 240, 210, (int) (230 * formation));
                ring.addVertex(m, Mth.cos(a1) * r0, Mth.sin(a1) * r0, DIST + 0.16f).setColor(255, 240, 210, (int) (230 * formation));
                ring.addVertex(m, Mth.cos(a1) * r1, Mth.sin(a1) * r1, DIST + 0.16f).setColor(255, 200, 140, 0);
                ring.addVertex(m, Mth.cos(a0) * r1, Mth.sin(a0) * r1, DIST + 0.16f).setColor(255, 200, 140, 0);
            }
            draw(ring);
        }
    }

    private static void elliptical(Matrix4f m, RandomSource rand, float size, float formation, boolean darkSky) {
        int k = darkSky ? 1 : 2;
        fan(m, 0, 0, size * 1.1f, DIST - 0.2f, 255, 214, 170, (int) (50 * formation * k), 0.04f, 0f);
        fan(m, 0, 0, size * 0.55f, DIST - 0.1f, 255, 228, 190, (int) (110 * formation * k), 0.03f, 1f);
        fan(m, 0, 0, size * 0.2f, DIST, 255, 245, 225, (int) (220 * formation), 0f, 2f);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 260; i++) {
            float r = (float) Math.abs(rand.nextGaussian()) * size * 0.45f;
            float a = rand.nextFloat() * Mth.TWO_PI;
            float s = (0.04f + rand.nextFloat() * 0.07f) * Math.max(1f, size / 7f);
            dot(b, m, Mth.cos(a) * r, Mth.sin(a) * r * 0.8f, DIST + 0.05f, s, 255, 226, 185, (int) (200 * formation * (1f - Math.min(1f, r / size))));
        }
        draw(b);
    }

    /** Draws a buffer if anything was added to it (a buffer can be empty, e.g. at the first frame of formation). */
    private static void draw(BufferBuilder b) {
        com.mojang.blaze3d.vertex.MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
