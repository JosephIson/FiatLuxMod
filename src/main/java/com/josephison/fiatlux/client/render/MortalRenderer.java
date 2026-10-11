package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.MortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renders a mortal: one of eight skin, hair and garment textures over one skeleton, showing only
 * the bones of the mortal's sex and of the age its people have reached.
 */
public class MortalRenderer extends GeoEntityRenderer<MortalEntity> {
    private static final String[] ERAS = {"hunter", "agrarian", "classical", "medieval", "industrial", "spacefaring"};
    private static final String[] VARIANTS = {"", "_b", "_c", "_d", "_e", "_f", "_g", "_h"};

    public MortalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(FiatLux.id("mortal"), true) {
            @Override
            public ResourceLocation getTextureResource(MortalEntity mortal) {
                return FiatLux.id("textures/entity/mortal" + VARIANTS[Math.floorMod(mortal.variant(), VARIANTS.length)] + ".png");
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(0.95f);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void preRender(PoseStack poseStack, MortalEntity mortal, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        int era = mortal.era().ordinal();
        boolean innocent = mortal.looksInnocent();   // plain linen in the garden: the coats of skins come later (Gen 3:21)
        for (int i = 0; i < ERAS.length; i++) {
            boolean shown = i == era && !innocent;
            model.getBone("era_" + ERAS[i]).ifPresent(b -> b.setHidden(!shown));
            boolean held = mortal.act() == MortalEntity.CRUCIFIED || mortal.act() == MortalEntity.STOCKS || mortal.act() == MortalEntity.EXORCISE;
            model.getBone("tool_" + ERAS[i]).ifPresent(b -> b.setHidden(!shown || mortal.isChild() || held));
        }
        boolean helmet = ERAS[Math.min(era, ERAS.length - 1)].equals("spacefaring") && !innocent;
        model.getBone("era_spacefaring_helm").ifPresent(b -> b.setHidden(!helmet));
        String title = mortal.title();
        boolean crowned = title.equals("King") || title.equals("Queen") || title.equals("Emperor") || title.equals("Empress")
                || title.equals("Chief") || title.equals("Chieftess");
        model.getBone("crown").ifPresent(b -> b.setHidden(!crowned || helmet));
        model.getBone("beard").ifPresent(b -> b.setHidden(!mortal.isMale() || helmet));
        model.getBone("long_hair").ifPresent(b -> b.setHidden(mortal.isMale() || helmet));
        super.preRender(poseStack, mortal, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** Children are drawn small, elders a little stooped; whatever a mortal says floats above it. */
    @Override
    public void render(MortalEntity mortal, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float scale = mortal.isChild() ? 0.6f : mortal.isElder() ? 0.92f : 0.95f;
        this.scaleWidth = scale;
        this.scaleHeight = scale;
        int glory = mortal.glory();
        super.render(mortal, yaw, partialTick, poseStack, buffers, glory >= 2 ? 0xF000F0 : light);   // the risen and transfigured shine
        if (glory > 0) halo(mortal, poseStack, buffers, partialTick, glory, scale);
        String name = mortal.getName().getString() + (mortal.title().isEmpty() ? "" : ", " + mortal.title());
        SpeechBubble.render(mortal, mortal.speech(), name, 1.95 * scale / 0.95 * mortal.getScale() + 0.45, 24,
                mortal.isPossessed() ? SpeechBubble.POSSESSED : SpeechBubble.PARCHMENT, poseStack, buffers, entityRenderDispatcher, getFont(), partialTick);
    }

    /** A ring of light over his head, turning slowly; brighter when he is glorified. */
    private void halo(MortalEntity mortal, PoseStack pose, MultiBufferSource buffers, float partialTick, int glory, float scale) {
        pose.pushPose();
        pose.translate(0, 2.08 * scale / 0.95 * mortal.getScale(), 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((mortal.tickCount + partialTick) * 1.5f));
        org.joml.Matrix4f m = pose.last().pose();
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float r0 = 0.24f, r1 = 0.31f;
        int a = glory >= 2 ? 230 : 150;
        for (int i = 0; i < 24; i++) {
            float t0 = (float) (Math.PI * 2 * i / 24), t1 = (float) (Math.PI * 2 * (i + 1) / 24);
            float c0 = net.minecraft.util.Mth.cos(t0), s0 = net.minecraft.util.Mth.sin(t0), c1 = net.minecraft.util.Mth.cos(t1), s1 = net.minecraft.util.Mth.sin(t1);
            vc.addVertex(m, c0 * r0, 0, s0 * r0).setColor(255, 236, 160, a);
            vc.addVertex(m, c0 * r1, 0, s0 * r1).setColor(255, 214, 110, a / 3);
            vc.addVertex(m, c1 * r1, 0, s1 * r1).setColor(255, 214, 110, a / 3);
            vc.addVertex(m, c1 * r0, 0, s1 * r0).setColor(255, 236, 160, a);
            vc.addVertex(m, c1 * r0, 0, s1 * r0).setColor(255, 236, 160, a);
            vc.addVertex(m, c1 * r1, 0, s1 * r1).setColor(255, 214, 110, a / 3);
            vc.addVertex(m, c0 * r1, 0, s0 * r1).setColor(255, 214, 110, a / 3);
            vc.addVertex(m, c0 * r0, 0, s0 * r0).setColor(255, 236, 160, a);
        }
        pose.popPose();
    }

    /** One with an unclean spirit in it is drawn as if a shadow lay on it. */
    @Override
    public software.bernie.geckolib.util.Color getRenderColor(MortalEntity mortal, float partialTick, int packedLight) {
        if (mortal.isPossessed()) return software.bernie.geckolib.util.Color.ofRGBA(0.62f, 0.5f, 0.5f, 1f);
        return super.getRenderColor(mortal, partialTick, packedLight);
    }

    @Override
    public RenderType getRenderType(MortalEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
