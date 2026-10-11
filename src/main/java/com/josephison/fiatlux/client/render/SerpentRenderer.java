package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.eden.SerpentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The serpent of Eden: on four legs until the curse, then upon his belly (Gen 3:14); his words float above him. */
public class SerpentRenderer extends GeoEntityRenderer<SerpentEntity> {
    private static final String[] LEGS = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};

    public SerpentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("serpent"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(1.15f);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void preRender(PoseStack poseStack, SerpentEntity serpent, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        for (String leg : LEGS) model.getBone(leg).ifPresent(b -> b.setHidden(serpent.cursed()));
        super.preRender(poseStack, serpent, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void render(SerpentEntity serpent, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(serpent, yaw, partialTick, poseStack, buffers, light);
        String words = serpent.speech();
        if (words.isEmpty() || entityRenderDispatcher.distanceToSqr(serpent) > 28 * 28) return;
        net.minecraft.client.gui.Font font = getFont();
        var lines = font.split(net.minecraft.network.chat.Component.literal(words), 170);
        poseStack.pushPose();
        poseStack.translate(0, 1.6 + lines.size() * 0.22, 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.028f, -0.028f, 0.028f);
        org.joml.Matrix4f m = poseStack.last().pose();
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            font.drawInBatch(line, -font.width(line) / 2f, i * 10, 0xFFD6F0B0, false, m, buffers,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0xD0142818, 0xF000F0);
        }
        poseStack.popPose();
    }
}
