package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.FallenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renders a fallen angel: translucent pass for the torn wing membranes, and an AutoGlowingGeoLayer
 * for the smouldering cracks in its scorched flesh, the glowing breaks of its halo and its red eyes.
 */
public class FallenRenderer extends GeoEntityRenderer<FallenEntity> {
    public FallenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("fallen"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.7f;
    }

    @Override
    public RenderType getRenderType(FallenEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
