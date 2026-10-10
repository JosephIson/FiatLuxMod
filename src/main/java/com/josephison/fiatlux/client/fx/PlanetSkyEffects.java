package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.client.ClientCosmos;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import com.josephison.fiatlux.realm.RealmPool;
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
 * The sky of a created world: an atmosphere tinted by the planet's own air, a sun crossing it, stars at
 * night, and the other worlds of the cosmos hanging in the heavens. Gas giants have thick amber skies;
 * barren moons have a thin black sky even by day.
 */
public class PlanetSkyEffects extends DimensionSpecialEffects {
    public PlanetSkyEffects() {
        super(192f, true, SkyType.NONE, false, false);
    }

    private static SyncCosmosPayload.PlanetView here(ClientLevel level) {
        int slot = RealmPool.slotOf(level.dimension());
        var planets = ClientCosmos.get().planets();
        return slot >= 0 && slot < planets.size() ? planets.get(slot) : null;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        var p = level == null ? null : here(level);
        if (p == null) return fogColor.multiply(brightness * 0.94 + 0.06, brightness * 0.94 + 0.06, brightness * 0.91 + 0.09);
        int a = p.atmosphere();
        float k = brightness * 0.9f + 0.1f;
        return new Vec3(((a >> 16) & 255) / 255.0 * k, ((a >> 8) & 255) / 255.0 * k, (a & 255) / 255.0 * k);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        var here = here(level);
        if (here == null) return false;
        float sunAngle = level.getSunAngle(partialTick);
        float day = Mth.clamp(Mth.cos(sunAngle) * 2f + 0.5f, 0f, 1f);
        int type = here.type();                         // PlanetType ordinal
        float thin = type == 2 ? 0.15f : 1f;            // barren moons barely have air
        int a = here.atmosphere();
        float ar = ((a >> 16) & 255) / 255f, ag = ((a >> 8) & 255) / 255f, ab = (a & 255) / 255f;

        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = ps.last().pose();

        // atmosphere dome: horizon takes the air's colour, the zenith deepens; night is near-black
        BufferBuilder dome = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 12; i++) {
            float p0 = -90f + 180f * i / 12, p1 = -90f + 180f * (i + 1) / 12;
            int c0 = sky(p0, ar, ag, ab, day * thin), c1 = sky(p1, ar, ag, ab, day * thin);
            for (int j = 0; j < 32; j++) {
                float y0 = Mth.TWO_PI * j / 32, y1 = Mth.TWO_PI * (j + 1) / 32;
                v(dome, m, p0, y0, c0); v(dome, m, p0, y1, c0); v(dome, m, p1, y1, c1); v(dome, m, p1, y0, c1);
            }
        }
        BufferUploader.drawWithShader(dome.buildOrThrow());

        // stars: visible at night, and always on airless worlds
        float starAlpha = Mth.clamp(1f - day * thin * 1.4f, 0f, 1f);
        if (starAlpha > 0.02f) {
            BufferBuilder stars = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            ps.pushPose();
            ps.mulPose(Axis.XP.rotation(sunAngle));
            Matrix4f sm = ps.last().pose();
            java.util.Random r = new java.util.Random(here.name().hashCode());
            for (int i = 0; i < 900; i++) {
                float yaw = r.nextFloat() * Mth.TWO_PI, pitch = (float) Math.asin(r.nextFloat() * 2 - 1);
                float s = 0.15f + r.nextFloat() * 0.25f;
                float x = Mth.cos(pitch) * Mth.cos(yaw) * 95, y = Mth.sin(pitch) * 95, z = Mth.cos(pitch) * Mth.sin(yaw) * 95;
                int al = (int) (starAlpha * (150 + r.nextInt(105)));
                billboard(stars, sm, x, y, z, s, 255, 250, 235, al);
            }
            BufferUploader.drawWithShader(stars.buildOrThrow());
            ps.popPose();
        }

        // the sun (its light warmed by thick air) and the sister worlds of the cosmos
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-90f));
        ps.mulPose(Axis.XP.rotation(sunAngle));
        Matrix4f sun = ps.last().pose();
        BufferBuilder sb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        sb.addVertex(sun, 0, 100, 0).setColor(255, 252, 230, 255);
        for (int i = 0; i <= 32; i++) {
            float an = Mth.TWO_PI * i / 32;
            sb.addVertex(sun, Mth.cos(an) * 12, 100, Mth.sin(an) * 12).setColor(255, 220, 150, 0);
        }
        BufferUploader.drawWithShader(sb.buildOrThrow());
        ps.popPose();

        RenderSystem.defaultBlendFunc();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotation(sunAngle * 0.5f + 1f));
        Matrix4f pm = ps.last().pose();
        var all = ClientCosmos.get().planets();
        for (int i = 0; i < all.size(); i++) {
            var p = all.get(i);
            if (p == here) continue;
            float yaw = i * 2.4f, pitch = 0.25f + (i % 4) * 0.22f;
            float size = 2.2f + p.size() * 3.2f;
            float x = Mth.cos(pitch) * Mth.cos(yaw) * 90, y = Mth.sin(pitch) * 90, z = Mth.cos(pitch) * Mth.sin(yaw) * 90;
            int c = p.color();
            BufferBuilder disc = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            billboard(disc, pm, x, y, z, size, (c >> 16) & 255, (c >> 8) & 255, c & 255, (int) (200 * Mth.clamp(1.1f - day * thin * 0.6f, 0.3f, 1f)));
            BufferUploader.drawWithShader(disc.buildOrThrow());
        }
        ps.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    private static int sky(float pitchDeg, float ar, float ag, float ab, float day) {
        float t = (pitchDeg + 90f) / 180f;
        float up = Mth.clamp((t - 0.5f) * 2f, 0f, 1f);
        float r = ar * (1 - up * 0.45f) * day, g = ag * (1 - up * 0.35f) * day, b = (ab * (1 - up * 0.1f) + up * 0.15f) * day;
        r += 0.01f; g += 0.01f; b += 0.03f;
        return 0xFF000000 | (int) (Mth.clamp(r, 0, 1) * 255) << 16 | (int) (Mth.clamp(g, 0, 1) * 255) << 8 | (int) (Mth.clamp(b, 0, 1) * 255);
    }

    private static void v(BufferBuilder b, Matrix4f m, float pitchDeg, float yaw, int argb) {
        float p = pitchDeg * Mth.DEG_TO_RAD;
        b.addVertex(m, Mth.cos(p) * Mth.cos(yaw) * 100, Mth.sin(p) * 100, Mth.cos(p) * Mth.sin(yaw) * 100).setColor(argb);
    }

    /** A camera-independent square facing the origin at (x, y, z). */
    private static void billboard(BufferBuilder b, Matrix4f m, float x, float y, float z, float s, int r, int g, int bl, int a) {
        Vec3 n = new Vec3(x, y, z).normalize();
        Vec3 u = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 t1 = n.cross(u).normalize().scale(s), t2 = n.cross(t1).normalize().scale(s);
        b.addVertex(m, (float) (x - t1.x - t2.x), (float) (y - t1.y - t2.y), (float) (z - t1.z - t2.z)).setColor(r, g, bl, a);
        b.addVertex(m, (float) (x + t1.x - t2.x), (float) (y + t1.y - t2.y), (float) (z + t1.z - t2.z)).setColor(r, g, bl, a);
        b.addVertex(m, (float) (x + t1.x + t2.x), (float) (y + t1.y + t2.y), (float) (z + t1.z + t2.z)).setColor(r, g, bl, a);
        b.addVertex(m, (float) (x - t1.x + t2.x), (float) (y - t1.y + t2.y), (float) (z - t1.z + t2.z)).setColor(r, g, bl, a);
    }
}
