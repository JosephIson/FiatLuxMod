package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.infernal.SpiritEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * An unclean spirit: the form of a fallen angel, drawn as a half-seen shadow - dark and translucent,
 * with only its eyes and the cracks of its scorched flesh burning - that fades away when it departs.
 */
public class SpiritRenderer extends GeoEntityRenderer<SpiritEntity> {
    public SpiritRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("fallen"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(0.85f);
        this.shadowRadius = 0f;
    }

    @Override
    public Color getRenderColor(SpiritEntity spirit, float partialTick, int packedLight) {
        float fade = Math.max(0f, 1f - (spirit.fade() + partialTick) / 40f);
        return Color.ofRGBA(0.16f, 0.1f, 0.12f, 0.55f * (spirit.fade() > 0 ? fade : 1f));
    }

    @Override
    public RenderType getRenderType(SpiritEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
