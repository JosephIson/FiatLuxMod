package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.flood.BirdEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The raven (black) and the dove (white) of the ark; the dove's olive leaf shows once she has plucked it. */
public class BirdRenderer extends GeoEntityRenderer<BirdEntity> {
    public BirdRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("bird"), false) {
            @Override
            public ResourceLocation getTextureResource(BirdEntity bird) {
                return FiatLux.id(bird.kind() == BirdEntity.RAVEN ? "textures/entity/bird_raven.png" : "textures/entity/bird.png");
            }
        });
        this.shadowRadius = 0.15f;
    }

    @Override
    public void preRender(PoseStack poseStack, BirdEntity bird, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        model.getBone("olive").ifPresent(b -> b.setHidden(!bird.carriesLeaf()));
        super.preRender(poseStack, bird, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
