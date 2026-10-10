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
import software.bernie.geckolib.util.Color;

/**
 * Renders any member of the Host from its GeckoLib model, with a translucent pass (beryl and
 * crystal surfaces) and an AutoGlowingGeoLayer that lights every _glowmask texel at full
 * brightness - holy fire, burning coals, lightning faces and watching eyes.
 *
 * A rebel is drawn darkened toward blood and ash ("thy brightness" corrupted, Ezek 28:17); while
 * being cast out it flickers between the white of the lightning that struck it and that darkness.
 */
public class HostRenderer<T extends HeavenlyBeing> extends GeoEntityRenderer<T> {
    public HostRenderer(EntityRendererProvider.Context ctx, String name, float scale, float shadow, boolean turnsHead) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id(name), turnsHead));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(scale);
        this.shadowRadius = shadow;
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        if (animatable.isBeingCastOut() && animatable.castOutTicks() > 0) {
            float t = animatable.castOutTicks() + partialTick;
            float flicker = (float) Math.abs(Math.sin(t * 1.7)) * Math.max(0f, 1f - t / HeavenlyBeing.STRIKE_TICKS);
            return Color.ofRGBA(0.55f + 0.45f * flicker, 0.3f + 0.7f * flicker, 0.3f + 0.7f * flicker, 1f);
        }
        if (animatable.isRebel()) return Color.ofRGBA(0.72f, 0.42f, 0.4f, 1f);
        return super.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
