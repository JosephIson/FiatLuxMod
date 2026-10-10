package com.josephison.fiatlux.client;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.network.SyncDivinityPayload;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The Creator's HUD: Divine Essence, Glory, play mode and the power ready to be spoken. */
public final class DivinityHud {
    private DivinityHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!FiatLuxConfig.CLIENT.showHud.get() || mc.options.hideGui || mc.player == null) return;
        SyncDivinityPayload s = ClientDivinity.get();
        Font font = mc.font;
        int x = FiatLuxConfig.CLIENT.hudX.get(), y = FiatLuxConfig.CLIENT.hudY.get();
        if (s.state().pilgrim()) {
            g.drawString(font, Component.translatable("hud.fiatlux.pilgrim"), x, y, 0xFFC8C0B0, true);
            return;
        }
        float frac = Math.min(1f, s.state().essence() / Math.max(1f, s.maxEssence()));
        int barW = 110;
        g.drawString(font, Component.translatable("hud.fiatlux.essence", (int) s.state().essence(), (int) s.maxEssence()), x, y, 0xFFFFE9A8, true);
        g.fill(x - 1, y + 10, x + barW + 1, y + 16, 0xA0201810);
        g.fillGradient(x, y + 11, x + (int) (barW * frac), y + 15, 0xFFFFF6D0, 0xFFE0A63A);
        // a moving glint along the bar
        int glint = (int) ((mc.level.getGameTime() % 60) / 60f * barW * frac);
        g.fill(x + glint, y + 11, x + Math.min(glint + 3, (int) (barW * frac)), y + 15, 0x90FFFFFF);
        String mode = "hud.fiatlux.mode." + s.mode().getSerializedName();
        g.drawString(font, Component.translatable("hud.fiatlux.glory", s.glory(), s.tier()).append(" · ").append(Component.translatable(mode)),
                x, y + 19, 0xFFE8D7B0, true);
        AbstractDivinePower p = PowerRegistry.REGISTRY.get(s.state().selectedPower());
        if (p != null) {
            boolean locked = s.mode() == PlayMode.ASCENSION && p.tier() > s.tier();
            Component name = p.name().copy().withColor(locked ? 0xFF8A8478 : p.color());
            g.drawString(font, Component.translatable("hud.fiatlux.power").append(name), x, y + 30, 0xFFFFFFFF, true);
        }
    }
}
