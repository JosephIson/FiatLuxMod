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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The sky of the Void. Before the Big Bang it is utter darkness: "darkness was upon the face of the
 * deep" (Genesis 1:2), with nothing in it but Heaven's Gate behind you and the waters below. When
 * the Creator speaks, the light bursts out ahead, and the expanse fills over the following seconds
 * with stars (a dense band of them across the sky), coloured nebulae and turning galaxies.
 */
public class VoidSkyEffects extends DimensionSpecialEffects {
    private static final int STARS = 2600;
    private static final float[] DIR = new float[STARS * 3];
    private static final float[] SIZE = new float[STARS];
    private static final int[] COLOR = new int[STARS];
    private static final float[][] NEBULAE = {
            // yaw, pitch, radius, r, g, b
            {20, 18, 34, 150, 90, 230}, {140, -8, 40, 70, 140, 255}, {-70, 30, 28, 255, 110, 170},
            {-150, 4, 36, 80, 210, 200}, {75, -26, 30, 220, 150, 90}, {-15, -35, 26, 120, 100, 255},
    };

    static {
        RandomSource r = RandomSource.create(0x5EED_F1A7L);
        // the band of the galaxy: a tilted great circle most stars cluster near
        Vector3f bandNormal = new Vector3f(0.3f, 0.85f, 0.42f).normalize();
        for (int i = 0; i < STARS; i++) {
            Vector3f d = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian()).normalize();
            if (i % 5 < 3) {   // three in five fall near the band
                float off = d.dot(bandNormal);
                d.sub(new Vector3f(bandNormal).mul(off * 0.88f)).normalize();
            }
            DIR[i * 3] = d.x;
            DIR[i * 3 + 1] = d.y;
            DIR[i * 3 + 2] = d.z;
            float big = r.nextFloat();
            SIZE[i] = 0.07f + big * big * big * big * 0.3f;
            float temp = r.nextFloat();   // cool red .. white .. hot blue
            COLOR[i] = temp < 0.12f ? 0xFFD2A8 : temp < 0.75f ? 0xFFF8F0 : temp < 0.92f ? 0xE6EEFF : 0xC4D6FF;
        }
    }

    public VoidSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
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
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        float bang = ClientCosmos.secondsSinceBigBang(partialTick);
        if (bang < 0) return true;   // without form, and void: nothing but darkness
        float time = ticks + partialTick;
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);   // light on darkness

        float stars = Mth.clamp((bang - 1.5f) / 12f, 0f, 1f);
        drawNebulae(ps.last().pose(), Mth.clamp((bang - 3f) / 15f, 0f, 1f), time);
        drawStars(ps.last().pose(), stars, time);
        HeavenSkyEffects.drawGalaxies(ps, bang, time);
        if (bang < 14f) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180f));   // the light bursts out ahead of Heaven's Gate, toward the greater light
            HeavenSkyEffects.drawBigBang(ps.last().pose(), bang);
            ps.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    /** Stars appear one after another as the universe forms; each twinkles on its own rhythm. */
    private static void drawStars(Matrix4f m, float formation, float time) {
        if (formation <= 0) return;
        int shown = (int) (STARS * formation);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f up = new Vector3f(), right = new Vector3f();
        for (int i = 0; i < shown; i++) {
            float x = DIR[i * 3] * 100, y = DIR[i * 3 + 1] * 100, z = DIR[i * 3 + 2] * 100;
            Vector3f d = new Vector3f(DIR[i * 3], DIR[i * 3 + 1], DIR[i * 3 + 2]);
            right.set(Math.abs(d.y) > 0.9f ? 1 : 0, Math.abs(d.y) > 0.9f ? 0 : 1, 0).cross(d).normalize();
            up.set(d).cross(right).normalize();
            float s = SIZE[i];
            float tw = 0.65f + 0.35f * Mth.sin(time * (0.04f + (i % 7) * 0.013f) + i * 1.7f);
            int c = COLOR[i];
            int a = (int) (255 * tw * Math.min(1f, formation * 3f - i / (float) STARS * 2f + 0.3f));
            if (a <= 0) continue;
            a = Math.min(255, a);
            int r = (c >> 16) & 255, g = (c >> 8) & 255, bl = c & 255;
            b.addVertex(m, x - right.x * s - up.x * s, y - right.y * s - up.y * s, z - right.z * s - up.z * s).setColor(r, g, bl, a);
            b.addVertex(m, x + right.x * s - up.x * s, y + right.y * s - up.y * s, z + right.z * s - up.z * s).setColor(r, g, bl, a);
            b.addVertex(m, x + right.x * s + up.x * s, y + right.y * s + up.y * s, z + right.z * s + up.z * s).setColor(r, g, bl, a);
            b.addVertex(m, x - right.x * s + up.x * s, y - right.y * s + up.y * s, z - right.z * s + up.z * s).setColor(r, g, bl, a);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** Great soft clouds of coloured gas, each with brighter knots, drifting very slowly. */
    private static void drawNebulae(Matrix4f base, float formation, float time) {
        if (formation <= 0) return;
        PoseStack ps = new PoseStack();
        ps.mulPose(base);
        for (int k = 0; k < NEBULAE.length; k++) {
            float[] n = NEBULAE[k];
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(n[0] + time * 0.0008f));
            ps.mulPose(Axis.XP.rotationDegrees(n[1]));
            ps.mulPose(Axis.ZP.rotationDegrees(k * 47f));
            Matrix4f m = ps.last().pose();
            int r = (int) n[3], g = (int) n[4], bl = (int) n[5];
            for (int layer = 0; layer < 3; layer++) {
                float rad = n[2] * formation * (1f - layer * 0.28f);
                float ox = Mth.sin(k * 3.1f + layer * 2f) * n[2] * 0.25f, oy = Mth.cos(k * 1.7f + layer * 1.3f) * n[2] * 0.18f;
                BufferBuilder f = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                f.addVertex(m, ox, oy, -96f + layer * 0.1f).setColor(r, g, bl, (int) (46 * formation));
                for (int i = 0; i <= 36; i++) {
                    float a = Mth.TWO_PI * i / 36;
                    float wob = 1f + 0.3f * Mth.sin(a * 3 + k + layer) + 0.15f * Mth.sin(a * 7 + k * 2);
                    f.addVertex(m, ox + Mth.cos(a) * rad * wob, oy + Mth.sin(a) * rad * 0.62f * wob, -96f + layer * 0.1f).setColor(r, g, bl, 0);
                }
                BufferUploader.drawWithShader(f.buildOrThrow());
            }
            ps.popPose();
        }
    }
}
