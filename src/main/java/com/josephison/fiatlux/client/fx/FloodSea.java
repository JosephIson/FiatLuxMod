package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.network.FloodPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * "And the waters prevailed exceedingly upon the earth; and all the high hills, that were under the
 * whole heaven, were covered" (Genesis 7:19). Around the towns and the ark the waters of the Flood are
 * real; beyond them the client lays the same surface out to the horizon, so the whole world is sea.
 */
public final class FloodSea {
    private static final int NEAR = 24, CELL = 8;
    private static String dim = "";
    private static boolean sea;
    private static int waterY;
    private static long bow, bowEnd, bowStart;
    private static float shownY = Float.NaN;
    private static long[] centres = new long[0];
    private static int[] radii = new int[0];

    private FloodSea() {}

    public static void update(FloodPayload p) {
        dim = p.dim();
        sea = p.sea();
        waterY = p.waterY();
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        if (p.bowTicks() > 0 && (bowEnd <= now || bow != p.bow())) bowStart = now;
        bow = p.bow();
        bowEnd = p.bowTicks() > 0 ? now + p.bowTicks() : 0;
        centres = p.centres();
        radii = p.radii();
        if (!sea) shownY = Float.NaN;
    }

    private static boolean real(double x, double z) {
        for (int i = 0; i < centres.length; i++) {
            double dx = x - BlockPos.getX(centres[i]), dz = z - BlockPos.getZ(centres[i]);
            if (dx * dx + dz * dz <= (double) radii[i] * radii[i]) return true;
        }
        return false;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || dim.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().toString().equals(dim)) return;
        if (bowEnd > mc.level.getGameTime()) rainbow(event, mc);
        if (!sea) return;
        // the surface rises and falls smoothly to where the waters stand
        float target = waterY + 0.86f;
        shownY = Float.isNaN(shownY) ? target : shownY + (target - shownY) * 0.05f;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        TextureAtlasSprite water = mc.getModelManager().getBlockModelShaper().getParticleIcon(Blocks.WATER.defaultBlockState());
        int colour = BiomeColors.getAverageWaterColor(mc.level, BlockPos.containing(cam));
        int r = colour >> 16 & 255, g = colour >> 8 & 255, b = colour & 255;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        int light = 15 << 20;   // full sky light, no block light: the lightmap darkens it by night
        int cx = (int) Math.floor(cam.x), cz = (int) Math.floor(cam.z);
        float y = shownY;
        // near the eye, one quad per block, so the water's texture keeps its size
        for (int x = cx - NEAR; x < cx + NEAR; x++) {
            for (int z = cz - NEAR; z < cz + NEAR; z++) {
                if (real(x + 0.5, z + 0.5)) continue;
                quad(vc, m, x, z, 1, y, water, r, g, b, light);
            }
        }
        // out to the horizon, in larger cells
        int far = mc.options.getEffectiveRenderDistance() * 16 + 32;
        int x0 = Math.floorDiv(cx - far, CELL) * CELL, z0 = Math.floorDiv(cz - far, CELL) * CELL;
        for (int x = x0; x < cx + far; x += CELL) {
            for (int z = z0; z < cz + far; z += CELL) {
                boolean nearCell = x + CELL > cx - NEAR && x < cx + NEAR && z + CELL > cz - NEAR && z < cz + NEAR;
                if (nearCell || real(x + CELL / 2.0, z + CELL / 2.0)) continue;
                quad(vc, m, x, z, CELL, y, water, r, g, b, light);
            }
        }
        buffers.endBatch(RenderType.translucent());
        ps.popPose();
    }

    /** "I do set my bow in the cloud" (Genesis 9:13): seven bands arching over the land beyond the ark, fading in and out. */
    private static void rainbow(RenderLevelStageEvent event, Minecraft mc) {
        float t = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float fade = Math.min(1f, Math.min((t - bowStart) / 200f, (bowEnd - t) / 300f));
        if (fade <= 0) return;
        int[] bands = {0xE8302A, 0xF08A24, 0xF4D838, 0x46C24A, 0x3A78E8, 0x4A3AB8, 0x8E48C8};
        Vec3 cam = event.getCamera().getPosition();
        double cx = BlockPos.getX(bow) + 0.5, cy = BlockPos.getY(bow) - 12, cz = BlockPos.getZ(bow) + 90;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(cx - cam.x, cy - cam.y, cz - cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        int segments = 96;
        for (int k = 0; k < bands.length; k++) {
            float outer = 100 - k * 2.6f, inner = outer - 2.6f;
            int r = bands[k] >> 16 & 255, g = bands[k] >> 8 & 255, b = bands[k] & 255;
            int a = (int) (110 * fade);
            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * i / segments, a1 = Math.PI * (i + 1) / segments;
                float x0 = (float) Math.cos(a0), y0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), y1 = (float) Math.sin(a1);
                // the ends of the bow fade into the land
                int aa = (int) (a * Math.min(1, Math.min(y0, y1) * 6 + 0.15));
                vc.addVertex(m, x0 * inner, y0 * inner, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x0 * outer, y0 * outer, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x1 * outer, y1 * outer, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x1 * inner, y1 * inner, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x1 * inner, y1 * inner, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x1 * outer, y1 * outer, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x0 * outer, y0 * outer, 0).setColor(r, g, b, aa);
                vc.addVertex(m, x0 * inner, y0 * inner, 0).setColor(r, g, b, aa);
            }
        }
        buffers.endBatch(RenderType.lightning());
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x, float z, float size, float y, TextureAtlasSprite s,
                             int r, int g, int b, int light) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        int a = 235;
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u0, v0).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, x, y, z + size).setColor(r, g, b, a).setUv(u0, v1).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, x + size, y, z + size).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, x + size, y, z).setColor(r, g, b, a).setUv(u1, v0).setLight(light).setNormal(0, 1, 0);
        // and from beneath, for those under the waters
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u0, v0).setLight(light).setNormal(0, -1, 0);
        vc.addVertex(m, x + size, y, z).setColor(r, g, b, a).setUv(u1, v0).setLight(light).setNormal(0, -1, 0);
        vc.addVertex(m, x + size, y, z + size).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setNormal(0, -1, 0);
        vc.addVertex(m, x, y, z + size).setColor(r, g, b, a).setUv(u0, v1).setLight(light).setNormal(0, -1, 0);
    }
}
