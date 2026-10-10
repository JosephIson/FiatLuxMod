package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renders any member of the Host from its GeckoLib model, with a translucent pass (beryl and
 * crystal surfaces) and an AutoGlowingGeoLayer that lights every _glowmask texel at full
 * brightness - holy fire, burning coals, lightning faces and watching eyes.
 */
public class HostRenderer<T extends HeavenlyBeing> extends GeoEntityRenderer<T> {
    public HostRenderer(EntityRendererProvider.Context ctx, String name, float scale, float shadow, boolean turnsHead) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id(name), turnsHead));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(scale);
        this.shadowRadius = shadow;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
