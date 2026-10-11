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
        SpeechBubble.render(serpent, serpent.speech(), "The Serpent", 1.7, 28, SpeechBubble.SERPENT,
                poseStack, buffers, entityRenderDispatcher, getFont(), partialTick);
    }
}
