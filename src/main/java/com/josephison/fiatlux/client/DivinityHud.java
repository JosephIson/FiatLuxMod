package com.josephison.fiatlux.client;

import com.josephison.fiatlux.client.screen.Ui;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.network.SyncDivinityPayload;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerRegistry;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Creator's HUD: a small illuminated panel with a halo that fills toward the next tier of
 * Ascension, the bar of Divine Essence, the play mode and the power ready to be spoken.
 */
public final class DivinityHud {
    private static float shownEssence = -1;

    private DivinityHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!FiatLuxConfig.CLIENT.showHud.get() || mc.options.hideGui || mc.player == null || mc.level == null) return;
        SyncDivinityPayload s = ClientDivinity.get();
        Font font = mc.font;
        int x = FiatLuxConfig.CLIENT.hudX.get(), y = FiatLuxConfig.CLIENT.hudY.get();
        float time = Util.getMillis() / 1000f;
        if (s.state().pilgrim()) {
            Component text = Component.translatable("hud.fiatlux.pilgrim");
            int w = font.width(text) + 16;
            Ui.panel(g, x - 2, y - 2, w, 16, 0.85f);
            g.drawString(font, text, x + 6, y + 2, 0xFFC8C0B0, false);
            return;
        }
        float max = Math.max(1f, s.maxEssence());
        float essence = s.state().essence();
        shownEssence = shownEssence < 0 ? essence : Mth.lerp(0.15f, shownEssence, essence);   // the bar eases toward the true value
        int w = 176, h = 50;
        Ui.panel(g, x - 2, y - 2, w, h, 0.9f);

        // the halo: glory toward the next tier
        int hx = x + 20, hy = y + 22;
        long per = Math.max(1, FiatLuxConfig.COMMON.gloryPerTier.get());
        float toNext = s.tier() >= 10 ? 1f : (s.glory() % per) / (float) per;
        Ui.ring(g, hx, hy, 0, 17, 0xF0201828, 0xF0100C18);
        Ui.ring(g, hx, hy, 13, 16, 0x50F5D77A, 0x50F5D77A);
        if (toNext > 0) Ui.sector(g, hx, hy, 13, 16, 0, Math.PI * 2 * toNext, 0xFFE0A63A, 0xFFFFF0B0);
        float pulse = 0.5f + 0.5f * Mth.sin(time * 2.2f);
        Ui.ring(g, hx, hy, 16, 19, Ui.withAlpha(0xF5D77A, (int) (40 + 50 * pulse)), 0x00F5D77A);
        Component tier = Component.literal(String.valueOf(s.tier())).withStyle(st -> st.withBold(true));
        g.drawString(font, tier, hx - font.width(tier) / 2, hy - 4, 0xFFFFE9A8, true);

        // essence
        int bx = x + 44, bw = w - 54;
        Component label = Component.literal("Essence");
        g.drawString(font, label, bx, y + 3, 0xFFFFE9A8, true);
        String num = (int) essence + " / " + (int) max;
        g.drawString(font, num, bx + bw - font.width(num), y + 3, 0xFFC8B890, false);
        Ui.bar(g, bx, y + 14, bw, 5, shownEssence / max, 0xFFE0A63A, 0xFFFFF6D0, time * 0.45f);

        // glory and mode
        String mode = "hud.fiatlux.mode." + s.mode().getSerializedName();
        Component glory = Component.literal("Glory " + glory(s.glory()));
        g.drawString(font, glory, bx, y + 24, 0xFFE8D7B0, false);
        Component modeText = Component.translatable(mode);
        Ui.chip(g, font, modeText, bx + bw - font.width(modeText) - 8, y + 22, s.mode() == PlayMode.ASCENSION ? 0xFFB8D8FF : 0xFFF5D77A);

        // the power ready to be spoken
        AbstractDivinePower p = PowerRegistry.REGISTRY.get(s.state().selectedPower());
        if (p != null) {
            boolean locked = s.mode() == PlayMode.ASCENSION && p.tier() > s.tier();
            boolean affordable = essence >= p.cost();
            int col = locked ? 0xFF8A8478 : 0xFF000000 | p.color();
            Ui.diamond(g, bx + 3, y + 38, 3, col);
            Component name = p.name().copy().withColor(locked ? 0xFF8A8478 : Ui.shade(col, 1.3f));
            g.drawString(font, name, bx + 10, y + 34, 0xFFFFFFFF, true);
            String cost = (int) p.cost() + "";
            g.drawString(font, cost, bx + bw - font.width(cost), y + 34, affordable ? 0xFFB8A888 : 0xFFE07060, false);
        }
    }

    private static String glory(long v) {
        return v >= 1_000_000 ? String.format("%.1fM", v / 1e6) : v >= 10_000 ? String.format("%.1fk", v / 1e3) : String.valueOf(v);
    }
}
