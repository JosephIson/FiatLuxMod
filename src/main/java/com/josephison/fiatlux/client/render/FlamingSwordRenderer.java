package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.weapon.FlamingSwordItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 3D GeckoLib renderer for the Flaming Sword: translucent crystal blade, emissive holy fire. */
public class FlamingSwordRenderer extends GeoItemRenderer<FlamingSwordItem> {
    public FlamingSwordRenderer() {
        super(new DefaultedItemGeoModel<>(FiatLux.id("flaming_sword_of_cherubim")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    public static FlamingSwordRenderer create() {
        return new FlamingSwordRenderer();
    }

    @Override
    public RenderType getRenderType(FlamingSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
