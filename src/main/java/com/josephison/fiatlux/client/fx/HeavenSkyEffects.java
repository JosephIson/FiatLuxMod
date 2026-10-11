package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.josephison.fiatlux.client.ClientCosmos;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Heaven's sky: a radiant gradient dome over a golden abyss, the light of the throne high in the
 * east, "a rainbow round about the throne, in sight like unto an emerald" (Revelation 4:3), and
 * slowly turning rays of glory. Rendered procedurally - no textures.
 */
public class HeavenSkyEffects extends DimensionSpecialEffects {
    private static final float THRONE_PITCH = 38f;   // elevation of the throne-light
    private static final float THRONE_YAW = -35f;

    public HeavenSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(1.0, 0.95, 0.84);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true; // no clouds above Heaven - the islands are the clouds
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        if (!FiatLuxConfig.CLIENT.heavenSkybox.get()) return false;
        float time = ticks + partialTick;
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        drawDome(ps.last().pose());
        float bang = ClientCosmos.secondsSinceBigBang(partialTick);
        if (bang >= 0) GalaxyRenderer.draw(ps, bang, time, false);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(THRONE_YAW));
        ps.mulPose(Axis.XP.rotationDegrees(THRONE_PITCH));   // +x rotation lifts the -z axis above the horizon
        // Heaven's sky is already bright, so additive light would vanish into it: these layers use normal
        // alpha blending with saturated colours to stand out against the azure and cream.
        drawRays(ps, time);
        drawRainbow(ps.last().pose(), time);
        drawThroneGlory(ps.last().pose(), time);
        // the orrery of creation: every created world circles the throne-light
        drawPlanets(ps.last().pose(), partialTick, time);
        if (bang >= 0 && bang < 14f) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            drawBigBang(ps.last().pose(), bang);
        }
        ps.popPose();

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    /** Created worlds orbiting the throne-light, with orbit paths, lit discs, atmospheres and city lights. */
    private static void drawPlanets(Matrix4f m, float partialTick, float time) {
        var planets = ClientCosmos.get().planets();
        if (planets.isEmpty()) return;
        for (SyncCosmosPayload.PlanetView p : planets) {
            float R = Math.min(78f, 20f + p.orbitRadius() * 8f);
            float tilt = Mth.cos(p.inclination() * Mth.DEG_TO_RAD * 2.2f) * 0.55f + 0.2f;
            // orbit path
            BufferBuilder path = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 96; i++) {
                float a0 = Mth.TWO_PI * i / 96, a1 = Mth.TWO_PI * (i + 1) / 96;
                float w = 0.12f;
                path.addVertex(m, Mth.cos(a0) * (R - w), Mth.sin(a0) * (R - w) * tilt, -87.5f).setColor(150, 110, 40, 70);
                path.addVertex(m, Mth.cos(a1) * (R - w), Mth.sin(a1) * (R - w) * tilt, -87.5f).setColor(150, 110, 40, 70);
                path.addVertex(m, Mth.cos(a1) * (R + w), Mth.sin(a1) * (R + w) * tilt, -87.5f).setColor(150, 110, 40, 70);
                path.addVertex(m, Mth.cos(a0) * (R + w), Mth.sin(a0) * (R + w) * tilt, -87.5f).setColor(150, 110, 40, 70);
            }
            BufferUploader.drawWithShader(path.buildOrThrow());
            float ang = ClientCosmos.phase(p, partialTick) * Mth.TWO_PI;
            float cx = Mth.cos(ang) * R, cy = Mth.sin(ang) * R * tilt;
            float size = 1.7f + p.size() * 2.2f;
            // atmosphere halo
            int at = p.atmosphere();
            BufferBuilder halo = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            halo.addVertex(m, cx, cy, -87.2f).setColor((at >> 16) & 255, (at >> 8) & 255, at & 255, 120);
            for (int i = 0; i <= 32; i++) {
                float a = Mth.TWO_PI * i / 32;
                halo.addVertex(m, cx + Mth.cos(a) * size * 1.6f, cy + Mth.sin(a) * size * 1.6f, -87.2f).setColor((at >> 16) & 255, (at >> 8) & 255, at & 255, 0);
            }
            BufferUploader.drawWithShader(halo.buildOrThrow());
            // the world itself: lit toward the throne-light at the centre
            int c = p.color();
            int r = (c >> 16) & 255, g = (c >> 8) & 255, bl = c & 255;
            float lx = -Mth.cos(ang) * 0.35f * size, ly = -Mth.sin(ang) * 0.35f * size * tilt;
            BufferBuilder disc = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            disc.addVertex(m, cx + lx, cy + ly, -87f).setColor(Math.min(255, r + 70), Math.min(255, g + 70), Math.min(255, bl + 70), 255);
            for (int i = 0; i <= 32; i++) {
                float a = Mth.TWO_PI * i / 32;
                disc.addVertex(m, cx + Mth.cos(a) * size, cy + Mth.sin(a) * size, -87f).setColor(r / 2, g / 2, bl / 2, 255);
            }
            BufferUploader.drawWithShader(disc.buildOrThrow());
            if (p.inhabited()) {
                // lights of civilisation on the night side
                BufferBuilder lights = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i < 6; i++) {
                    float a = ang + 2.4f + i * 0.45f;
                    float rr = size * (0.35f + 0.1f * (i % 3));
                    float x = cx + Mth.cos(a) * rr, y = cy + Mth.sin(a) * rr;
                    float s = 0.12f;
                    int al = (int) (180 + 60 * Mth.sin(time * 0.2f + i));
                    lights.addVertex(m, x - s, y - s, -86.9f).setColor(255, 220, 140, al);
                    lights.addVertex(m, x + s, y - s, -86.9f).setColor(255, 220, 140, al);
                    lights.addVertex(m, x + s, y + s, -86.9f).setColor(255, 220, 140, al);
                    lights.addVertex(m, x - s, y + s, -86.9f).setColor(255, 220, 140, al);
                }
                BufferUploader.drawWithShader(lights.buildOrThrow());
            }
        }
    }

    /** The first seconds after the Big Bang: an expanding shell of many-coloured light and radiating beams. */
    static void drawBigBang(Matrix4f m, float t) {
        float fade = t < 1f ? t : Mth.clamp(1f - (t - 4f) / 10f, 0f, 1f);
        float radius = 4f + t * 14f;
        int[][] shells = {{255, 255, 255}, {255, 210, 120}, {180, 120, 255}, {110, 200, 255}};
        for (int k = 0; k < shells.length; k++) {
            float r0 = Math.max(0, radius - 6f - k * 5f), r1 = radius - k * 5f;
            if (r1 <= 0) continue;
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 64; i++) {
                float a0 = Mth.TWO_PI * i / 64, a1 = Mth.TWO_PI * (i + 1) / 64;
                int a = (int) (200 * fade / (k + 1));
                b.addVertex(m, Mth.cos(a0) * r0, Mth.sin(a0) * r0, -85f).setColor(shells[k][0], shells[k][1], shells[k][2], 0);
                b.addVertex(m, Mth.cos(a1) * r0, Mth.sin(a1) * r0, -85f).setColor(shells[k][0], shells[k][1], shells[k][2], 0);
                b.addVertex(m, Mth.cos(a1) * r1, Mth.sin(a1) * r1, -85f).setColor(shells[k][0], shells[k][1], shells[k][2], a);
                b.addVertex(m, Mth.cos(a0) * r1, Mth.sin(a0) * r1, -85f).setColor(shells[k][0], shells[k][1], shells[k][2], a);
            }
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
        BufferBuilder beams = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 40; i++) {
            float a = Mth.TWO_PI * i / 40 + t * 0.05f;
            float len = radius * (1.4f + 0.6f * Mth.sin(i * 7.1f));
            beams.addVertex(m, 0, 0, -84.5f).setColor(255, 255, 255, (int) (230 * fade));
            beams.addVertex(m, Mth.cos(a - 0.02f) * len, Mth.sin(a - 0.02f) * len, -84.5f).setColor(255, 240, 200, 0);
            beams.addVertex(m, Mth.cos(a + 0.02f) * len, Mth.sin(a + 0.02f) * len, -84.5f).setColor(255, 240, 200, 0);
        }
        BufferUploader.drawWithShader(beams.buildOrThrow());
    }

    /** Latitude bands from the golden abyss below to the pale zenith. */
    private static void drawDome(Matrix4f m) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int bands = 16, slices = 32;
        float radius = 100f;
        for (int i = 0; i < bands; i++) {
            float p0 = -90f + 180f * i / bands, p1 = -90f + 180f * (i + 1) / bands;
            int c0 = domeColor(p0), c1 = domeColor(p1);
            for (int j = 0; j < slices; j++) {
                float y0 = Mth.TWO_PI * j / slices, y1 = Mth.TWO_PI * (j + 1) / slices;
                vertex(b, m, radius, p0, y0, c0);
                vertex(b, m, radius, p0, y1, c0);
                vertex(b, m, radius, p1, y1, c1);
                vertex(b, m, radius, p1, y0, c1);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static int domeColor(float pitchDeg) {
        // nadir: deep gold light rising from below; horizon: warm cream; zenith: luminous pale sky
        float t = (pitchDeg + 90f) / 180f;
        float[] nadir = {0.98f, 0.78f, 0.42f}, horizon = {1.0f, 0.95f, 0.82f}, zenith = {0.47f, 0.66f, 0.98f};
        float[] a, c;
        float k;
        if (t < 0.5f) { a = nadir; c = horizon; k = t / 0.5f; } else { a = horizon; c = zenith; k = (t - 0.5f) / 0.5f; }
        int r = (int) (Mth.lerp(k, a[0], c[0]) * 255), g = (int) (Mth.lerp(k, a[1], c[1]) * 255), bl = (int) (Mth.lerp(k, a[2], c[2]) * 255);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float radius, float pitchDeg, float yaw, int argb) {
        float p = pitchDeg * Mth.DEG_TO_RAD;
        float x = Mth.cos(p) * Mth.cos(yaw) * radius, y = Mth.sin(p) * radius, z = Mth.cos(p) * Mth.sin(yaw) * radius;
        b.addVertex(m, x, y, z).setColor(argb);
    }

    /** Long tapering rays of glory fanning from the throne-light across the sky, turning slowly. */
    private static void drawRays(PoseStack ps, float time) {
        int count = FiatLuxConfig.CLIENT.godRayCount.get();
        if (count <= 0) return;
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(time * 0.02f));
        Matrix4f m = ps.last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float dist = 90f;
        for (int i = 0; i < count; i++) {
            float a = Mth.TWO_PI * i / count + Mth.sin(i * 12.9898f) * 0.08f;
            float len = 60f + 50f * (0.5f + 0.5f * Mth.sin(i * 78.233f));
            float half = 0.035f + 0.02f * Mth.sin(time * 0.01f + i);
            float pulse = 0.55f + 0.45f * Mth.sin(time * 0.03f + i * 1.7f);
            int alpha = (int) (95 * pulse);
            b.addVertex(m, 0, 0, -dist).setColor(255, 226, 140, alpha);
            b.addVertex(m, Mth.cos(a - half) * len, Mth.sin(a - half) * len, -dist).setColor(255, 214, 120, 0);
            b.addVertex(m, Mth.cos(a + half) * len, Mth.sin(a + half) * len, -dist).setColor(255, 214, 120, 0);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        ps.popPose();
    }

    /** The emerald rainbow round about the throne (Revelation 4:3), with a faint prismatic fringe. */
    private static void drawRainbow(Matrix4f m, float time) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float dist = 89f;
        float[][] bands = {
                {16.0f, 17.6f, 40, 200, 110},    // emerald heart
                {17.6f, 18.6f, 30, 170, 160},
                {15.0f, 16.0f, 150, 220, 90},
                {18.6f, 19.4f, 70, 120, 230},
        };
        int seg = 96;
        float shimmer = 0.85f + 0.15f * Mth.sin(time * 0.05f);
        for (float[] band : bands) {
            int r = (int) band[2], g = (int) band[3], bl = (int) band[4];
            for (int i = 0; i < seg; i++) {
                float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
                int alpha = (int) (175 * shimmer);
                b.addVertex(m, Mth.cos(a0) * band[0], Mth.sin(a0) * band[0], -dist).setColor(r, g, bl, alpha / 3);
                b.addVertex(m, Mth.cos(a1) * band[0], Mth.sin(a1) * band[0], -dist).setColor(r, g, bl, alpha / 3);
                b.addVertex(m, Mth.cos(a1) * band[1], Mth.sin(a1) * band[1], -dist).setColor(r, g, bl, alpha);
                b.addVertex(m, Mth.cos(a0) * band[1], Mth.sin(a0) * band[1], -dist).setColor(r, g, bl, alpha);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** The blazing light of the throne: a white core in layered soft halos. */
    private static void drawThroneGlory(Matrix4f m, float time) {
        // {radius, centre alpha, edge r, g, b}: golden corona first, white-hot core last
        float[][] layers = {{30f, 110, 255, 200, 90}, {16f, 170, 255, 214, 120}, {9f, 230, 255, 236, 180}, {4.5f, 255, 255, 255, 250}};
        for (float[] layer : layers) {
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            float r = layer[0] * (1f + 0.04f * Mth.sin(time * 0.07f + layer[0]));
            b.addVertex(m, 0, 0, -88f).setColor(255, 255, 248, (int) layer[1]);
            for (int i = 0; i <= 48; i++) {
                float a = Mth.TWO_PI * i / 48;
                b.addVertex(m, Mth.cos(a) * r, Mth.sin(a) * r, -88f).setColor((int) layer[2], (int) layer[3], (int) layer[4], 0);
            }
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
    }
}
