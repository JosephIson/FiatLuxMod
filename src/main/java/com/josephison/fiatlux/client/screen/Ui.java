package com.josephison.fiatlux.client.screen;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The look shared by every Fiat Lux screen and the HUD: illuminated panels edged in gold with
 * ornaments at the corners, rules with a diamond at their centre, framed bars with a moving glint,
 * and rings and sectors for the power wheel.
 */
public final class Ui {
    public static final int GOLD = 0xFFF5D77A, GOLD_DIM = 0xFFB89A50, GOLD_DEEP = 0xFF8A6A2A, INK = 0xFF0C0A14,
            TEXT = 0xFFEDE6D6, MUTED = 0xFFA8A0B4, HEAVEN = 0xFFFFF0C0, HELL = 0xFFFF7A5A;

    private Ui() {}

    // ------------------------------------------------------------------ panels

    /** A dark panel edged in gold, with a soft shadow and ornaments at its corners. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        int a = (int) (alpha * 255);
        // shadow
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, (int) (alpha * 0x60) << 24);
        // body: a deep gradient of night
        g.fillGradient(x, y, x + w, y + h, withAlpha(0x161226, a * 235 / 255), withAlpha(0x0A0812, a * 245 / 255));
        // a faint inner light along the top
        g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(18, h / 3), withAlpha(0xF5D77A, a / 9), withAlpha(0xF5D77A, 0));
        // double border
        g.renderOutline(x, y, w, h, withAlpha(0xB89A50, a));
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, withAlpha(0x8A6A2A, a * 150 / 255));
        // corner ornaments
        int c = withAlpha(0xF5D77A, a);
        corner(g, x, y, 1, 1, c);
        corner(g, x + w - 1, y, -1, 1, c);
        corner(g, x, y + h - 1, 1, -1, c);
        corner(g, x + w - 1, y + h - 1, -1, -1, c);
    }

    /** A light parchment page with ink-brown double border and corner ornaments. */
    public static void parchment(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 4, y + 4, x + w + 4, y + h + 4, 0x70000000);
        g.fillGradient(x, y, x + w, y + h, 0xFFF1E6CB, 0xFFE3D2AE);
        // darkened, age-worn edges
        int e = 10;
        g.fillGradient(x, y, x + w, y + e, 0x40704A20, 0x00704A20);
        g.fillGradient(x, y + h - e, x + w, y + h, 0x00704A20, 0x40704A20);
        for (int i = 0; i < e; i++) {
            int al = (int) (0x40 * (1 - i / (float) e));
            g.fill(x + i, y, x + i + 1, y + h, al << 24 | 0x704A20);
            g.fill(x + w - i - 1, y, x + w - i, y + h, al << 24 | 0x704A20);
        }
        g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0xFF7A5228);
        g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0xA07A5228);
        int c = 0xFF7A5228;
        corner(g, x + 3, y + 3, 1, 1, c);
        corner(g, x + w - 4, y + 3, -1, 1, c);
        corner(g, x + 3, y + h - 4, 1, -1, c);
        corner(g, x + w - 4, y + h - 4, -1, -1, c);
    }

    /** An L-shaped flourish with a diamond, pointing into the panel from a corner. */
    private static void corner(GuiGraphics g, int x, int y, int dx, int dy, int color) {
        for (int i = 0; i < 9; i++) {
            px(g, x + dx * i, y, color);
            px(g, x, y + dy * i, color);
        }
        diamond(g, x + dx * 4, y + dy * 4, 2, color);
    }

    // ------------------------------------------------------------------ ornaments and text

    public static void px(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }

    /** A small diamond (radius r) centred on (cx, cy). */
    public static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int half = r - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
        }
    }

    /** A rule across [x0, x1] at y, fading at its ends, with a diamond at its centre. */
    public static void rule(GuiGraphics g, int x0, int x1, int y, int color) {
        int mid = (x0 + x1) / 2;
        int rgb = color & 0xFFFFFF, a = color >>> 24;
        hGradient(g, x0, y, mid - 4, y + 1, rgb, 0, a);
        hGradient(g, mid + 4, y, x1, y + 1, rgb, a, 0);
        diamond(g, mid, y, 2, color);
    }

    /** A horizontal alpha gradient (drawn in short steps). */
    public static void hGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int rgb, int a0, int a1) {
        int w = x1 - x0;
        if (w <= 0) return;
        int step = Math.max(1, w / 24);
        for (int x = x0; x < x1; x += step) {
            float t = (x - x0 + step / 2f) / w;
            int a = Math.round(a0 + (a1 - a0) * t);
            g.fill(x, y0, Math.min(x1, x + step), y1, a << 24 | rgb);
        }
    }

    /** A title in gold, bold, with a rule and diamond on either side. */
    public static void title(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        Component bold = text.copy().withStyle(s -> s.withBold(true));
        int w = font.width(bold);
        g.drawString(font, bold, cx - w / 2, y, color, true);
        int a = 0xC0;
        hGradient(g, cx - w / 2 - 70, y + 4, cx - w / 2 - 8, y + 5, color & 0xFFFFFF, 0, a);
        hGradient(g, cx + w / 2 + 8, y + 4, cx + w / 2 + 70, y + 5, color & 0xFFFFFF, a, 0);
        diamond(g, cx - w / 2 - 5, y + 4, 2, color);
        diamond(g, cx + w / 2 + 4, y + 4, 2, color);
    }

    /** A framed bar filled to `frac` with a gradient and a glint that travels along it. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int from, int to, float time) {
        frac = Mth.clamp(frac, 0f, 1f);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
        g.renderOutline(x - 2, y - 2, w + 4, h + 4, 0xA0B89A50);
        g.fill(x, y, x + w, y + h, 0x60302838);
        int fw = (int) (w * frac);
        if (fw > 0) {
            hGradientColor(g, x, y, x + fw, y + h, from, to);
            g.fill(x, y, x + fw, y + 1, 0x60FFFFFF);   // a highlight along the top
            int gx = x + (int) ((time % 1f) * (w + 20)) - 10;
            for (int i = 0; i < 6; i++) {
                int sx = gx + i;
                if (sx >= x && sx < x + fw) g.fill(sx, y, sx + 1, y + h, (0x70 - Math.abs(i - 3) * 0x18) << 24 | 0xFFFFFF);
            }
        }
        // quarter ticks
        for (int q = 1; q < 4; q++) g.fill(x + w * q / 4, y + h - 2, x + w * q / 4 + 1, y + h, 0x50000000);
    }

    /** A horizontal colour gradient between two opaque colours. */
    public static void hGradientColor(GuiGraphics g, int x0, int y0, int x1, int y1, int from, int to) {
        int w = x1 - x0;
        if (w <= 0) return;
        int steps = Math.min(w, 32);
        for (int i = 0; i < steps; i++) {
            int a = x0 + w * i / steps, b = x0 + w * (i + 1) / steps;
            g.fill(a, y0, b, y1, lerpColor(from, to, (i + 0.5f) / steps));
        }
    }

    /** A rounded "chip" behind a short label. */
    public static int chip(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int w = font.width(text) + 8;
        g.fill(x + 1, y, x + w - 1, y + 11, withAlpha(color & 0xFFFFFF, 0x40));
        g.fill(x, y + 1, x + w, y + 10, withAlpha(color & 0xFFFFFF, 0x40));
        g.renderOutline(x, y, w, 11, withAlpha(color & 0xFFFFFF, 0xA0));
        g.drawString(font, text, x + 4, y + 2, color, false);
        return w;
    }

    /** Draws wrapped lines centred on cx; returns the y below them. */
    public static int centredLines(GuiGraphics g, Font font, java.util.List<FormattedCharSequence> lines, int cx, int y, int color, int max) {
        for (int i = 0; i < Math.min(max, lines.size()); i++) {
            g.drawString(font, lines.get(i), cx - font.width(lines.get(i)) / 2, y, color, false);
            y += 10;
        }
        return y;
    }

    // ------------------------------------------------------------------ rings and sectors

    /**
     * A ring sector between radii r0 and r1 from angle a0 to a1 (radians, clockwise from the top),
     * coloured `inner` at r0 and `outer` at r1.
     */
    public static void sector(GuiGraphics g, float cx, float cy, float r0, float r1, double a0, double a1, int inner, int outer) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        int steps = Math.max(2, (int) Math.ceil((a1 - a0) / (Math.PI / 48)));
        for (int i = 0; i < steps; i++) {
            double b0 = a0 + (a1 - a0) * i / steps, b1 = a0 + (a1 - a0) * (i + 1) / steps;
            float s0 = (float) Math.sin(b0), c0 = (float) -Math.cos(b0), s1 = (float) Math.sin(b1), c1 = (float) -Math.cos(b1);
            float[] xs = {cx + s0 * r0, cx + s1 * r0, cx + s1 * r1, cx + s0 * r1};
            float[] ys = {cy + c0 * r0, cy + c1 * r0, cy + c1 * r1, cy + c0 * r1};
            int[] cs = {inner, inner, outer, outer};
            // wind it as GuiGraphics.fill does, so it is not culled
            float cross = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (ys[1] - ys[0]) * (xs[2] - xs[0]);
            if (r0 <= 0.01f) cross = (xs[2] - xs[0]) * (ys[3] - ys[0]) - (ys[2] - ys[0]) * (xs[3] - xs[0]);
            for (int k = 0; k < 4; k++) {
                int j = cross < 0 ? k : 3 - k;
                vertex(vc, m, xs[j], ys[j], cs[j]);
            }
        }
        g.flush();
    }

    /** A full ring (or disc, with r0 = 0). */
    public static void ring(GuiGraphics g, float cx, float cy, float r0, float r1, int inner, int outer) {
        sector(g, cx, cy, r0, r1, 0, Math.PI * 2, inner, outer);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, int argb) {
        vc.addVertex(m, x, y, 0).setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    // ------------------------------------------------------------------ colour

    public static int withAlpha(int rgb, int a) {
        return Mth.clamp(a, 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    /** The colour darkened (f < 1) or lightened toward white (f > 1). */
    public static int shade(int argb, float f) {
        int a = argb >>> 24, r = argb >> 16 & 255, gr = argb >> 8 & 255, b = argb & 255;
        if (f <= 1) return a << 24 | (int) (r * f) << 16 | (int) (gr * f) << 8 | (int) (b * f);
        float t = Math.min(1, f - 1);
        return a << 24 | (int) (r + (255 - r) * t) << 16 | (int) (gr + (255 - gr) * t) << 8 | (int) (b + (255 - b) * t);
    }

    public static float easeOut(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }
}
