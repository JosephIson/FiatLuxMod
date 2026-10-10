package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.StationedSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The guardian sword "which turned every way" (Gen 3:24): it hovers, spins and sweeps through every direction. */
public class StationedSwordRenderer extends GeoEntityRenderer<StationedSwordEntity> {
    private static final ResourceLocation MODEL = FiatLux.id("geo/item/flaming_sword_of_cherubim.geo.json");
    private static final ResourceLocation TEXTURE = FiatLux.id("textures/item/flaming_sword_of_cherubim.png");
    private static final ResourceLocation ANIM = FiatLux.id("animations/item/flaming_sword_of_cherubim.animation.json");

    public StationedSwordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(StationedSwordEntity animatable) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(StationedSwordEntity animatable) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(StationedSwordEntity animatable) {
                return ANIM;
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.3f;
    }

    @Override
    protected void applyRotations(StationedSwordEntity sword, PoseStack ps, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        float t = sword.tickCount + partialTick;
        ps.translate(0, 0.35 + Mth.sin(t * 0.08f) * 0.12, 0);
        ps.mulPose(Axis.YP.rotationDegrees(t * 9f));                 // turning...
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.05f) * 24f)); // ...every way
        ps.mulPose(Axis.XP.rotationDegrees(Mth.cos(t * 0.037f) * 16f));
        ps.scale(0.85f, 0.85f, 0.85f);
    }

    @Override
    public RenderType getRenderType(StationedSwordEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
