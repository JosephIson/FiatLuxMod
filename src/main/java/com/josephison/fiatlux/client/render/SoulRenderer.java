package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.SoulEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * A soul of the dead: the mortal it was, in its own face, but made of light in Heaven - glowing,
 * half-transparent and lightly floating - or of ash with smouldering embers in the Underworld.
 */
public class SoulRenderer extends GeoEntityRenderer<SoulEntity> {
    private static final String[] VARIANTS = {"", "_b", "_c", "_d", "_e", "_f", "_g", "_h"};
    private static final String[] ERAS = {"hunter", "agrarian", "classical", "medieval", "industrial", "spacefaring"};

    public SoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("mortal"), true) {
            @Override
            public ResourceLocation getTextureResource(SoulEntity soul) {
                return FiatLux.id("textures/entity/soul_" + (soul.inHeaven() ? "heaven" : "underworld")
                        + VARIANTS[Math.floorMod(soul.variant(), VARIANTS.length)] + ".png");
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(0.95f);
        this.shadowRadius = 0.2f;
    }

    @Override
    public void preRender(PoseStack poseStack, SoulEntity soul, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        for (String era : ERAS) {
            model.getBone("era_" + era).ifPresent(b -> b.setHidden(true));
            model.getBone("tool_" + era).ifPresent(b -> b.setHidden(true));
        }
        model.getBone("era_spacefaring_helm").ifPresent(b -> b.setHidden(true));
        model.getBone("crown").ifPresent(b -> b.setHidden(true));
        model.getBone("beard").ifPresent(b -> b.setHidden(!soul.isMale()));
        model.getBone("long_hair").ifPresent(b -> b.setHidden(soul.isMale()));
        super.preRender(poseStack, soul, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(SoulEntity soul, float partialTick, int packedLight) {
        return soul.inHeaven() ? Color.ofARGB(205, 255, 255, 255) : Color.ofARGB(225, 255, 255, 255);
    }

    @Override
    public void render(SoulEntity soul, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        if (soul.inHeaven()) poseStack.translate(0, 0.15 + Mth.sin((soul.tickCount + partialTick) * 0.06f) * 0.06, 0);   // lightly floating
        super.render(soul, yaw, partialTick, poseStack, buffers, soul.inHeaven() ? 0xF000F0 : light);
        poseStack.popPose();
        SpeechBubble.render(soul, soul.speech(), soul.getName().getString(), 2.4, 24, soul.inHeaven() ? SpeechBubble.HEAVENLY : SpeechBubble.DAMNED,
                poseStack, buffers, entityRenderDispatcher, getFont(), partialTick);
    }

    @Override
    public RenderType getRenderType(SoulEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
