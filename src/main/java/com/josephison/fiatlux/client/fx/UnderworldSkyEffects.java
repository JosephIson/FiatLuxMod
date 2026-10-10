package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.client.ClientCosmos;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Underworld sky: a near-black dome over a smouldering crimson horizon whose glow grows with
 * the infernal cities' technological era, split by the sky-fracture - the burning rent through which
 * the rebels were cast down (Revelation 12:9, Luke 10:18).
 */
public class UnderworldSkyEffects extends DimensionSpecialEffects {
    public UnderworldSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.17, 0.05, 0.04);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        float time = ticks + partialTick;
        float era = ClientCosmos.get().underworldEra();   // 0..3
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = ps.last().pose();
        BufferBuilder dome = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int bands = 12, slices = 32;
        for (int i = 0; i < bands; i++) {
            float p0 = -90f + 180f * i / bands, p1 = -90f + 180f * (i + 1) / bands;
            int c0 = color(p0, era), c1 = color(p1, era);
            for (int j = 0; j < slices; j++) {
                float y0 = Mth.TWO_PI * j / slices, y1 = Mth.TWO_PI * (j + 1) / slices;
                v(dome, m, p0, y0, c0);
                v(dome, m, p0, y1, c0);
                v(dome, m, p1, y1, c1);
                v(dome, m, p1, y0, c1);
            }
        }
        BufferUploader.drawWithShader(dome.buildOrThrow());

        // the sky-fracture: additive, pulsing, a jagged rent across the zenith
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(70f));
        ps.mulPose(Axis.ZP.rotationDegrees(20f));
        Matrix4f fm = ps.last().pose();
        float pulse = 0.7f + 0.3f * Mth.sin(time * 0.05f);
        drawFracture(fm, 3.2f, 70, 255, 120, 40, pulse);     // crimson-orange halo
        drawFracture(fm, 1.1f, 200, 255, 220, 150, pulse);   // white-gold core
        ps.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    private static int color(float pitchDeg, float era) {
        float t = (pitchDeg + 90f) / 180f;
        // the horizon smoulders brighter as the cities industrialise
        float glow = 0.5f + era * 0.2f;
        float hr = 0.32f * glow + 0.08f, hg = 0.06f * glow + 0.02f, hb = 0.04f;
        float r, g, b;
        if (t < 0.5f) {
            float k = t / 0.5f;
            r = Mth.lerp(k, 0.06f, hr); g = Mth.lerp(k, 0.02f, hg); b = Mth.lerp(k, 0.02f, hb);
        } else {
            float k = (float) Math.pow((t - 0.5f) / 0.5f, 0.6);
            r = Mth.lerp(k, hr, 0.03f); g = Mth.lerp(k, hg, 0.015f); b = Mth.lerp(k, hb, 0.03f);
        }
        return 0xFF000000 | (int) (Mth.clamp(r, 0, 1) * 255) << 16 | (int) (Mth.clamp(g, 0, 1) * 255) << 8 | (int) (Mth.clamp(b, 0, 1) * 255);
    }

    private static void v(BufferBuilder b, Matrix4f m, float pitchDeg, float yaw, int argb) {
        float p = pitchDeg * Mth.DEG_TO_RAD;
        b.addVertex(m, Mth.cos(p) * Mth.cos(yaw) * 100, Mth.sin(p) * 100, Mth.cos(p) * Mth.sin(yaw) * 100).setColor(argb);
    }

    private static void drawFracture(Matrix4f m, float width, int alpha, int r, int g, int b, float pulse) {
        BufferBuilder q = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float prevX = -95, prevY = 0;
        int segs = 26;
        for (int i = 1; i <= segs; i++) {
            float x = -95 + 190f * i / segs;
            float y = (float) (Math.sin(i * 12.9898) * 43758.5453 % 1.0) * 14f;   // deterministic jag
            float taper = Mth.sin(Mth.PI * i / segs);
            float w = width * (0.4f + taper);
            int a = (int) (alpha * pulse * (0.3f + 0.7f * taper));
            q.addVertex(m, prevX, prevY - w, -90).setColor(r, g, b, a);
            q.addVertex(m, x, y - w, -90).setColor(r, g, b, a);
            q.addVertex(m, x, y + w, -90).setColor(r, g, b, a);
            q.addVertex(m, prevX, prevY + w, -90).setColor(r, g, b, a);
            prevX = x;
            prevY = y;
        }
        BufferUploader.drawWithShader(q.buildOrThrow());
    }
}
