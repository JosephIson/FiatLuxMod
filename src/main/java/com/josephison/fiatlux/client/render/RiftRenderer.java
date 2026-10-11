package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.client.portal.PortalViews;
import com.josephison.fiatlux.infernal.RiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * A rift from the Underworld: a jagged tear in the air standing upright on the ground, fixed in the
 * world like a door with a front and a back. Its edges burn crimson and flicker; through its opening
 * the far side is seen (drawn by {@link PortalViews}), or, until that view has come, only darkness.
 * It tears open and closes again.
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {
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
        ps.mulPose(Axis.YP.rotationDegrees(-rift.getYRot()));   // fixed in the world, across its facing
        Matrix4f m = ps.last().pose();
        float[][] o = RiftEntity.outline(rift.getId(), open);
        // the burning lips of the tear, around the opening
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        float flicker = 0.75f + 0.25f * (float) Math.sin(t * 0.7f);
        lips(glow, m, o, 0.8f, 1.22f, 0xE8, 0x30, 0x18, (int) (120 * flicker));
        lips(glow, m, o, 0.8f, 0.95f, 0xFF, 0xA0, 0x40, (int) (210 * flicker));
        if (!PortalViews.showing(rift)) {   // until the far side is seen: darkness
            VertexConsumer dark = buffers.getBuffer(RenderType.debugQuads());
            band(dark, m, o, -0.8f, 0.8f, 0x08, 0x02, 0x04, 255);
        }
        ps.popPose();
        super.render(rift, yaw, partialTick, ps, buffers, light);
    }

    /** Both lips: from `inner` to `outer` times the half-width, on each side of the spine. */
    private static void lips(VertexConsumer vc, Matrix4f m, float[][] o, float inner, float outer, int r, int g, int b, int a) {
        band(vc, m, o, inner, outer, r, g, b, a);
        band(vc, m, o, -outer, -inner, r, g, b, a);
    }

    private static void band(VertexConsumer vc, Matrix4f m, float[][] o, float from, float to, int r, int g, int b, int a) {
        float[] x = o[0], w = o[1];
        int n = x.length - 1;
        for (int i = 0; i < n; i++) {
            float y0 = RiftEntity.HEIGHT * i / n, y1 = RiftEntity.HEIGHT * (i + 1) / n;
            float l0 = x[i] + w[i] * from, r0 = x[i] + w[i] * to, l1 = x[i + 1] + w[i + 1] * from, r1 = x[i + 1] + w[i + 1] * to;
            quad(vc, m, l0, r0, l1, r1, y0, y1, r, g, b, a);
            quad(vc, m, r0, l0, r1, l1, y0, y1, r, g, b, a);   // and seen from behind
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float l0, float r0, float l1, float r1, float y0, float y1, int r, int g, int b, int a) {
        vc.addVertex(m, l0, y0, 0).setColor(r, g, b, a);
        vc.addVertex(m, r0, y0, 0).setColor(r, g, b, a);
        vc.addVertex(m, r1, y1, 0).setColor(r, g, b, a);
        vc.addVertex(m, l1, y1, 0).setColor(r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity rift) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
