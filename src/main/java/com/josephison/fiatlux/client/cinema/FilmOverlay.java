package com.josephison.fiatlux.client.cinema;

import com.josephison.fiatlux.cinema.Film;
import com.josephison.fiatlux.client.screen.Ui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * What a film shows on the screen: the tint over the world, the black bars, the fades, title cards,
 * captions, narration along the bottom, flashes, and the hint that holding Jump skips it.
 */
public final class FilmOverlay {
    private FilmOverlay() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        if (FilmPlayer.film == null) {
            if (FilmPlayer.afterglow > 0) {
                float a = (FilmPlayer.afterglow - delta.getGameTimeDeltaPartialTick(false)) / 16f;
                fill(g, 0, 0, w, h, FilmPlayer.afterglowColor, a * a);
            }
            return;
        }
        float t = FilmPlayer.tick + (mc.isPaused() ? 0 : delta.getGameTimeDeltaPartialTick(false));
        Font font = mc.font;

        fill(g, 0, 0, w, h, FilmPlayer.GRADE.color(t), FilmPlayer.GRADE.value(t));

        // the bars, to a wide frame
        int full = (int) Math.max(h * 0.11f, (h - w / 2.39f) / 2f);
        int bar = Math.round(full * FilmPlayer.LETTERBOX.value(t));
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }

        fill(g, 0, 0, w, h, FilmPlayer.FADE.color(t), FilmPlayer.FADE.value(t));

        float[] fl = FilmPlayer.flash;
        if (fl != null) {
            float left = 1 - (t - fl[0]) / fl[2];
            if (left > 0) fill(g, 0, 0, w, h, (int) fl[3], fl[1] * left * left);
        }

        for (FilmPlayer.Shown s : FilmPlayer.SHOWN) {
            float a = envelope(t, s.start(), s.until(), s.cue().kind() == Film.Cue.TITLE ? 22 : 12);
            if (a <= 0.02f) continue;
            switch (s.cue().kind()) {
                case Film.Cue.TITLE -> title(g, font, s, t, a, w, h);
                case Film.Cue.CAPTION -> caption(g, font, s.cue(), a, bar, h);
                case Film.Cue.NARRATE -> narration(g, font, s.cue(), a, w, h, bar);
                default -> {
                }
            }
        }

        // hold Jump to skip
        float hint = Mth.clamp((t - 30) / 20f, 0, 1) * (FilmPlayer.skipHeld > 0 ? 1f : 0.55f);
        if (hint > 0.05f) {
            String key = mc.options.keyJump.getTranslatedKeyMessage().getString();
            Component text = Component.literal("Hold " + key + " to skip");
            int tw = font.width(text), x = w - tw - 10, y = Math.max(4, bar / 2 - 5);   // in the top bar, clear of the words below
            text(g, font, text, x, y, 0xA8A0B4, hint, false);
            if (FilmPlayer.skipHeld > 0) {
                float p = Math.min(1, FilmPlayer.skipHeld / 24f);
                g.fill(x, y + 10, x + tw, y + 11, 0x40FFFFFF);
                g.fill(x, y + 10, x + (int) (tw * p), y + 11, 0xFFF5D77A);
            }
        }
    }

    /** Fades in over `edge` ticks from `start` and out over `edge` ticks before `end`. */
    private static float envelope(float t, float start, float end, float edge) {
        float in = Mth.clamp((t - start) / edge, 0, 1), out = Mth.clamp((end - t) / edge, 0, 1);
        return Math.min(in, out);
    }

    private static void title(GuiGraphics g, Font font, FilmPlayer.Shown s, float t, float a, int w, int h) {
        Film.Cue c = s.cue();
        float life = Mth.clamp((t - s.start()) / Math.max(1, c.length()), 0, 1);
        float scale = Math.max(1, c.a()) * (1 + life * 0.06f);   // a slow push in
        Component text = Component.literal(c.text());
        int tw = font.width(text);
        float cy = h * 0.42f;
        int rgb = c.color() & 0xFFFFFF;
        float light = ((rgb >> 16 & 255) * 0.3f + (rgb >> 8 & 255) * 0.59f + (rgb & 255) * 0.11f) / 255f;
        if (light > 0.5f) {
            // a soft dark band behind light lettering, so it reads over a bright world
            int top = (int) (cy - 10 * scale), bottom = (int) (cy + 8 * scale + (c.sub().isEmpty() ? 12 : 26)), alpha = (int) (120 * a);
            for (int i = 0; i < 2; i++) {
                int y0 = i == 0 ? top : (top + bottom) / 2, y1 = i == 0 ? (top + bottom) / 2 : bottom;
                int a0 = i == 0 ? 0 : alpha, a1 = i == 0 ? alpha : 0;
                g.fillGradient(0, y0, w, y1, a0 << 24, a1 << 24);
            }
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(w / 2f, cy, 0);
        pose.scale(scale, scale, 1);
        text(g, font, text, -tw / 2, -4, c.color() & 0xFFFFFF, a, true);
        pose.popPose();
        // the rules on either side widen as it shows
        int half = (int) (tw * scale / 2), reach = (int) (60 * Math.min(1, a * 1.3f));
        int ry = (int) (cy + 4 * scale + 8), col = c.color() & 0xFFFFFF;
        if (reach > 2) {
            Ui.hGradient(g, w / 2 - half - reach, ry, w / 2 - half / 3, ry + 1, col, 0, (int) (200 * a));
            Ui.hGradient(g, w / 2 + half / 3, ry, w / 2 + half + reach, ry + 1, col, (int) (200 * a), 0);
            if (a > 0.3f) Ui.diamond(g, w / 2, ry, 2, Ui.withAlpha(col, (int) (255 * a)));
        }
        if (!c.sub().isEmpty()) {
            Component sub = Component.literal(c.sub()).withStyle(st -> st.withItalic(true));
            pose.pushPose();
            pose.translate(w / 2f, ry + 8, 0);
            float ss = Math.max(1, scale * 0.4f);
            pose.scale(ss, ss, 1);
            text(g, font, sub, -font.width(sub) / 2, 0, c.color() & 0xFFFFFF, a * 0.8f, true);
            pose.popPose();
        }
    }

    private static void caption(GuiGraphics g, Font font, Film.Cue c, float a, int bar, int h) {
        int y = Math.max(bar + 10, 12);
        Component text = Component.literal(c.text()).withStyle(st -> st.withItalic(true));
        Ui.hGradient(g, 14, y + 11, 14 + font.width(text) + 30, y + 12, c.color() & 0xFFFFFF, (int) (180 * a), 0);
        text(g, font, text, 16, y, c.color() & 0xFFFFFF, a, true);
    }

    private static void narration(GuiGraphics g, Font font, Film.Cue c, float a, int w, int h, int bar) {
        // the speaker, the words, and the reference all run together so that two lines fit within the bar
        boolean reference = c.sub().matches(".*\\d+:\\d+.*");
        String speaker = !reference && !c.sub().isEmpty() ? c.sub() : null;
        MutableComponent line = Component.empty();
        if (speaker != null) line.append(Component.literal(speaker + "  ").withStyle(st -> st.withBold(true).withColor(0xF5D77A)));
        line.append(Component.literal(c.text()).withStyle(st -> st.withItalic(speaker == null).withColor(c.color() & 0xFFFFFF)));
        if (reference) line.append(Component.literal("   " + c.sub()).withStyle(st -> st.withItalic(false).withColor(0x9A8A6A)));
        int max = Math.min((int) (w * 0.8f), 460);
        List<FormattedCharSequence> lines = font.split(line, max);
        int lh = 11, n = Math.min(lines.size(), 4), block = n * lh - 2;
        boolean inBar = bar >= block + 6;
        int y = inBar ? h - bar + (bar - block) / 2 : h - block - Math.max(bar, 0) - 12;
        if (!inBar) g.fillGradient(0, y - 10, w, y + block + 10, 0x00000000, Ui.withAlpha(0x000000, (int) (150 * a)));
        int alpha = (int) (255 * Mth.clamp(a, 0, 1));
        if (alpha < 8) return;
        for (int i = 0; i < n; i++) {
            FormattedCharSequence l = lines.get(i);
            g.drawString(font, l, w / 2 - font.width(l) / 2, y, alpha << 24 | 0xFFFFFF, true);
            y += lh;
        }
    }

    /** Text at an alpha (skipped when almost unseen: the font draws faint alphas as opaque). */
    private static void text(GuiGraphics g, Font font, Component text, int x, int y, int rgb, float a, boolean shadow) {
        int alpha = (int) (255 * Mth.clamp(a, 0, 1));
        if (alpha < 8) return;
        g.drawString(font, text, x, y, alpha << 24 | rgb & 0xFFFFFF, shadow);
    }

    private static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int rgb, float a) {
        int alpha = (int) (255 * Mth.clamp(a, 0, 1));
        if (alpha <= 1) return;
        g.fill(x0, y0, x1, y1, alpha << 24 | rgb & 0xFFFFFF);
    }
}
