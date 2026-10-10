package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Screen-space effects: glory flashes and the soft golden vignette of Heaven. */
public final class FxOverlay {
    private FxOverlay() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        if (mc.level != null && mc.level.dimension() == FiatLuxDimensions.HEAVEN && FiatLuxConfig.CLIENT.heavenVignette.get()) {
            int edge = 0x55FFE7A8;
            g.fillGradient(0, 0, w, h / 3, edge, 0x00FFE7A8);
            g.fillGradient(0, h - h / 3, w, h, 0x00FFE7A8, 0x66FFD98A);
        }
        float flash = FxManager.flashAlpha();
        if (flash > 0.004f) {
            int c = FxManager.flashColor() & 0xFFFFFF;
            int a = Mth.clamp((int) (flash * 255), 0, 255);
            g.fill(0, 0, w, h, a << 24 | c);
        }
    }
}
