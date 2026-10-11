package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.infernal.RiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * A rift from the Underworld: a jagged tear in the air, standing upright on the ground and always
 * turned to the one who looks at it. Its heart is a darkness that lets no light through; its edges
 * burn crimson and flicker, and it tears open and closes again.
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {
    private static final int SEGMENTS = 14;

    public RiftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(RiftEntity rift, float yaw, float partialTick, PoseStack ps, MultiBufferSource buffers, int light) {
        float open = rift.openness(partialTick);
        if (open <= 0.01f) return;
        float t = rift.age() + partialTick;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-entityRenderDispatcher.camera.getYRot()));   // faces the onlooker
        Matrix4f m = ps.last().pose();
        // the jagged line of the tear: a zigzag spine, fixed for each rift
        RandomSource r = RandomSource.create(rift.getId() * 31L);
        float[] x = new float[SEGMENTS + 1], w = new float[SEGMENTS + 1];
        float height = 4.6f;
        for (int i = 0; i <= SEGMENTS; i++) {
            float k = i / (float) SEGMENTS;
            x[i] = (r.nextFloat() - 0.5f) * 0.7f;
            float bulge = (float) Math.sin(Math.PI * k);   // widest in the middle, closed at the ends
            w[i] = (0.12f + 0.55f * bulge * (0.6f + r.nextFloat() * 0.4f)) * open;
        }
        // the burning edge (glows, additive) and the darkness at its heart
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        float flicker = 0.75f + 0.25f * (float) Math.sin(t * 0.7f);
        strip(glow, m, x, w, height, 1.9f, 0xE8, 0x30, 0x18, (int) (150 * flicker));
        strip(glow, m, x, w, height, 1.35f, 0xFF, 0x80, 0x30, (int) (190 * flicker));
        VertexConsumer dark = buffers.getBuffer(RenderType.debugQuads());
        strip(dark, m, x, w, height, 0.85f, 0x08, 0x02, 0x04, 255);
        ps.popPose();
        super.render(rift, yaw, partialTick, ps, buffers, light);
    }

    private static void strip(VertexConsumer vc, Matrix4f m, float[] x, float[] w, float height, float scale, int r, int g, int b, int a) {
        for (int i = 0; i < SEGMENTS; i++) {
            float y0 = height * i / SEGMENTS, y1 = height * (i + 1) / SEGMENTS;
            float l0 = x[i] - w[i] * scale, r0 = x[i] + w[i] * scale, l1 = x[i + 1] - w[i + 1] * scale, r1 = x[i + 1] + w[i + 1] * scale;
            vc.addVertex(m, l0, y0, 0).setColor(r, g, b, a);
            vc.addVertex(m, r0, y0, 0).setColor(r, g, b, a);
            vc.addVertex(m, r1, y1, 0).setColor(r, g, b, a);
            vc.addVertex(m, l1, y1, 0).setColor(r, g, b, a);
            vc.addVertex(m, l1, y1, 0).setColor(r, g, b, a);
            vc.addVertex(m, r1, y1, 0).setColor(r, g, b, a);
            vc.addVertex(m, r0, y0, 0).setColor(r, g, b, a);
            vc.addVertex(m, l0, y0, 0).setColor(r, g, b, a);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity rift) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
