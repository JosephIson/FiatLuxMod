package com.josephison.fiatlux.client.render;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.PunishmentPropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The cross and the stocks, with the charge written on them: the board above the cross and the
 * placard on the stocks carry the condemned's name and crime, lettered in dark ink.
 */
public class PunishmentPropRenderer extends GeoEntityRenderer<PunishmentPropEntity> {
    /** Where each board's face is, in model units (front faces -z), and how wide it is. */
    private static final float[][] BOARDS = {
            // centre y, face z, width, height
            {49.75f, 1.1f, 15f, 6.5f},       // the titulus of the cross
            {29.6f, -7.86f, 15f, 5.0f},      // the placard of the stocks
    };

    public PunishmentPropRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            private String name(PunishmentPropEntity e) {
                return e.kind() == PunishmentPropEntity.STOCKS ? "stocks" : "cross";
            }

            @Override
            public ResourceLocation getModelResource(PunishmentPropEntity e) {
                return FiatLux.id("geo/entity/" + name(e) + ".geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PunishmentPropEntity e) {
                return FiatLux.id("textures/entity/" + name(e) + ".png");
            }

            @Override
            public ResourceLocation getAnimationResource(PunishmentPropEntity e) {
                return FiatLux.id("animations/entity/" + name(e) + ".animation.json");
            }
        });
        this.shadowRadius = 0.6f;
    }

    /** GeckoLib turns only living entities by their yaw; a prop is turned by its own. */
    @Override
    protected void applyRotations(PunishmentPropEntity prop, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - prop.getYRot()));
    }

    @Override
    public void render(PunishmentPropEntity prop, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(prop, yaw, partialTick, poseStack, buffers, light);
        String text = prop.inscription();
        if (text.isEmpty() || entityRenderDispatcher.distanceToSqr(prop) > 32 * 32) return;
        float[] board = BOARDS[Math.min(prop.kind(), BOARDS.length - 1)];
        Font font = getFont();
        String[] lines = text.split("\n|\\|");
        float maxW = (board[2] - 1.8f) / 16f, maxH = (board[3] - 1.4f) / 16f;
        float lineH = maxH / lines.length;   // each line as large as fits its share of the board
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - prop.getYRot()));   // as GeckoLib turns the model
        poseStack.translate(0, board[0] / 16f, (board[1] - 0.06f) / 16f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));                     // the lettering faces out, the way the board faces
        for (int i = 0; i < lines.length; i++) {
            float s = Math.min(lineH / 9f, maxW / Math.max(1, font.width(lines[i])));
            poseStack.pushPose();
            poseStack.translate(0, maxH / 2f - lineH * (i + 0.5f), 0);
            poseStack.scale(s, -s, s);
            font.drawInBatch(lines[i], -font.width(lines[i]) / 2f, -4f, 0xFF2A1A0A, false, poseStack.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
