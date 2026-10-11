package com.josephison.fiatlux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a being says, drawn above it as a speech bubble: a solid rounded panel with a border and a
 * tail pointing down at the speaker, its name in the header, the words wrapped beneath. Bubbles pop
 * in when something new is said and fade with distance, and always face the viewer.
 */
public final class SpeechBubble {
    /** The colours of a bubble: panel, border, words and the speaker's name. */
    public record Palette(int fill, int border, int text, int header) {}

    public static final Palette PARCHMENT = new Palette(0xF2F6EEDC, 0xFF6B4E2E, 0xFF2A1E12, 0xFF8A5A1E);
    public static final Palette HEAVENLY = new Palette(0xE8FFFBEA, 0xFFD8B04A, 0xFF5A4818, 0xFFB08420);
    public static final Palette DAMNED = new Palette(0xE82A1414, 0xFF8A2A1E, 0xFFF0D8CC, 0xFFE07050);
    public static final Palette SERPENT = new Palette(0xEA142818, 0xFF4E8A3A, 0xFFDDF2C0, 0xFF9CD070);
    public static final Palette POSSESSED = new Palette(0xEE1E1418, 0xFF5A1A2A, 0xFFE8C8D0, 0xFFB04060);

    private static final int MAX_WIDTH = 140, MAX_LINES = 5, PAD_X = 5, PAD_Y = 4, TAIL = 5;
    private static final float SCALE = 0.025f;
    /** When each speaker began its current words, to pop the bubble in. */
    private static final Map<Integer, Said> SAID = new HashMap<>();

    private record Said(String words, float since) {}

    private SpeechBubble() {}

    /**
     * Draws `words` above `speaker` at `height` blocks (the tip of the tail), visible up to `range` blocks.
     * `name` may be empty for no header.
     */
    public static void render(Entity speaker, String words, String name, double height, double range, Palette palette,
                              PoseStack pose, MultiBufferSource buffers, EntityRenderDispatcher dispatcher, Font font, float partialTick) {
        if (words.isEmpty()) {
            SAID.remove(speaker.getId());
            return;
        }
        double dist = Math.sqrt(dispatcher.distanceToSqr(speaker));
        if (dist > range) return;
        float now = speaker.tickCount + partialTick;
        Said said = SAID.get(speaker.getId());
        if (said == null || !said.words.equals(words) || said.since > now) {
            said = new Said(words, now);
            SAID.put(speaker.getId(), said);
            if (SAID.size() > 512) SAID.clear();
        }
        float age = now - said.since;
        float pop = Mth.clamp(age / 5f, 0f, 1f);
        float grow = 0.7f + 0.3f * easeOutBack(pop);
        float alpha = Mth.clamp(pop * 2f, 0f, 1f) * (float) Mth.clamp((range - dist) / 6.0, 0.0, 1.0);
        if (alpha < 0.04f) return;

        // lay out the words
        List<FormattedCharSequence> lines = font.split(Component.literal(words), MAX_WIDTH);
        if (lines.size() > MAX_LINES) {
            lines = new java.util.ArrayList<>(lines.subList(0, MAX_LINES));
            String last = toString(lines.get(MAX_LINES - 1));
            lines.set(MAX_LINES - 1, Component.literal(font.plainSubstrByWidth(last, MAX_WIDTH - font.width("...")) + "...").getVisualOrderText());
        }
        Component header = name.isEmpty() ? null : Component.literal(name).withStyle(Style.EMPTY.withBold(true));
        int textW = 0;
        for (FormattedCharSequence l : lines) textW = Math.max(textW, font.width(l));
        if (header != null) textW = Math.max(textW, font.width(header));
        int headerH = header == null ? 0 : 11;
        int w = textW + PAD_X * 2, h = headerH + lines.size() * 10 - 1 + PAD_Y * 2;
        int x0 = -w / 2, x1 = x0 + w, y1 = -TAIL, y0 = y1 - h;

        pose.pushPose();
        pose.translate(0, height, 0);
        pose.mulPose(dispatcher.cameraOrientation());
        float s = SCALE * grow * (float) Mth.clamp(0.75 + dist / 40.0, 0.75, 1.25);   // a little larger far off, to stay legible
        pose.scale(s, -s, s);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackground());
        int light = 0xF000F0;
        int border = fade(palette.border, alpha), fill = fade(palette.fill, alpha);
        int shadow = fade(0x50000000, alpha);
        // a soft shadow, the border, then the panel - each a step further back from the viewer than the words
        rounded(vc, m, x0 + 1, y0 + 2, x1 + 1, y1 + 2, -1.5f, shadow, light);
        rounded(vc, m, x0 - 1, y0 - 1, x1 + 1, y1 + 1, -1.0f, border, light);
        rounded(vc, m, x0, y0, x1, y1, -0.5f, fill, light);
        // the tail, pointing down at the speaker
        for (int i = 0; i < TAIL; i++) {
            int half = TAIL - i;
            quad(vc, m, -half, y1 + i, half, y1 + i + 1, -1.0f, border, light);
            if (half > 1) quad(vc, m, -half + 1, y1 + i, half - 1, y1 + i + 1, -0.5f, fill, light);
        }
        // a thin rule under the name
        if (header != null) quad(vc, m, x0 + PAD_X, y0 + PAD_Y + 9, x1 - PAD_X, y0 + PAD_Y + 10, -0.25f, fade(palette.header & 0x00FFFFFF | 0x60000000, alpha), light);

        int ty = y0 + PAD_Y;
        int textCol = fade(palette.text, alpha);
        if (header != null) {
            font.drawInBatch(header, -font.width(header) / 2f, ty, fade(palette.header, alpha), false, m, buffers, Font.DisplayMode.NORMAL, 0, light);
            ty += headerH;
        }
        for (FormattedCharSequence l : lines) {
            font.drawInBatch(l, -font.width(l) / 2f, ty, textCol, false, m, buffers, Font.DisplayMode.NORMAL, 0, light);
            ty += 10;
        }
        pose.popPose();
    }

    /** Forgets speakers no longer in the world (called once a second). */
    public static void forgetAbsent() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            SAID.clear();
            return;
        }
        SAID.keySet().removeIf(id -> level.getEntity(id) == null);
    }

    private static String toString(FormattedCharSequence seq) {
        StringBuilder b = new StringBuilder();
        seq.accept((i, style, cp) -> {
            b.appendCodePoint(cp);
            return true;
        });
        return b.toString();
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private static int fade(int argb, float alpha) {
        int a = Math.max(5, Math.round((argb >>> 24) * alpha));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** A rectangle with its corners cut by a pixel. */
    private static void rounded(VertexConsumer vc, Matrix4f m, int x0, int y0, int x1, int y1, float z, int argb, int light) {
        quad(vc, m, x0 + 1, y0, x1 - 1, y0 + 1, z, argb, light);
        quad(vc, m, x0, y0 + 1, x1, y1 - 1, z, argb, light);
        quad(vc, m, x0 + 1, y1 - 1, x1 - 1, y1, z, argb, light);
    }

    /** One quad, wound both ways so it shows whichever way it is culled. */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb, int light) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, a).setLight(light);
    }
}
