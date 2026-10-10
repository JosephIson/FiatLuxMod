package com.josephison.fiatlux.client.screen;

import com.josephison.fiatlux.client.ClientDivinity;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.network.SelectPowerPayload;
import com.josephison.fiatlux.network.SyncDivinityPayload;
import com.josephison.fiatlux.power.DivinePower;
import com.josephison.fiatlux.power.ModPowers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Radial power wheel: point toward a power and click to make it the power you will speak. */
public class PowerWheelScreen extends Screen {
    private final List<ResourceLocation> powers = new ArrayList<>();
    private int hovered = -1;
    private float open;

    public PowerWheelScreen() {
        super(Component.translatable("screen.fiatlux.power_wheel"));
        powers.addAll(ModPowers.REGISTRY.keySet());
        powers.sort(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        open = Math.min(1f, open + partialTick * 0.2f);
        int cx = width / 2, cy = height / 2;
        float radius = 96 * (0.7f + 0.3f * open);
        g.fillGradient(0, 0, width, height, 0x60100A04, 0x90100A04);
        float dx = mouseX - cx, dy = mouseY - cy;
        hovered = -1;
        if (dx * dx + dy * dy > 24 * 24 && !powers.isEmpty()) {
            double a = Math.atan2(dy, dx) + Math.PI / 2;
            if (a < 0) a += Math.PI * 2;
            hovered = (int) Math.floor(a / (Math.PI * 2) * powers.size() + 0.5) % powers.size();
        }
        SyncDivinityPayload s = ClientDivinity.get();
        for (int i = 0; i < powers.size(); i++) {
            DivinePower p = ModPowers.REGISTRY.get(powers.get(i));
            if (p == null) continue;
            double a = Math.PI * 2 * i / powers.size() - Math.PI / 2;
            int px = cx + (int) (Math.cos(a) * radius), py = cy + (int) (Math.sin(a) * radius);
            boolean locked = s.mode() == PlayMode.ASCENSION && p.tier() > s.tier();
            boolean sel = powers.get(i).equals(s.state().selectedPower());
            int size = i == hovered ? 15 : 11;
            int col = locked ? 0xFF4A4640 : (0xFF000000 | p.color());
            if (i == hovered || sel) {
                g.fill(px - size - 3, py - size - 3, px + size + 3, py + size + 3, sel ? 0xC0FFF2C0 : 0x80FFFFFF);
            }
            g.fill(px - size, py - size, px + size, py + size, 0xE0201810);
            g.fill(px - size + 2, py - size + 2, px + size - 2, py + size - 2, col);
            String glyph = p.name().getString().substring(0, 1);
            g.drawCenteredString(font, glyph, px, py - 4, locked ? 0xFF8A8478 : 0xFF201810);
        }
        // centre: the hovered (or selected) power's name, verse-like description and cost
        ResourceLocation focus = hovered >= 0 ? powers.get(hovered) : s.state().selectedPower();
        DivinePower fp = ModPowers.REGISTRY.get(focus);
        if (fp != null) {
            g.drawCenteredString(font, fp.name().copy().withColor(fp.color()), cx, cy - 14, 0xFFFFFFFF);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(fp.description(), 150);
            for (int k = 0; k < Math.min(3, lines.size()); k++) {
                g.drawCenteredString(font, lines.get(k), cx, cy - 2 + k * 10, 0xFFE8D7B0);
            }
            g.drawCenteredString(font, Component.translatable("screen.fiatlux.power_cost", (int) fp.cost(), fp.tier()), cx, cy + 32, 0xFFB8A888);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered >= 0) {
            PacketDistributor.sendToServer(new SelectPowerPayload(powers.get(hovered)));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // the wheel draws its own veil
    }

    @SuppressWarnings("unused")
    private static float ease(float t) {
        return Mth.clamp(1f - (1f - t) * (1f - t), 0f, 1f);
    }
}
