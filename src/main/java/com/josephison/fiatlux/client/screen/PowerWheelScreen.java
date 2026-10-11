package com.josephison.fiatlux.client.screen;

import com.josephison.fiatlux.client.ClientDivinity;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.network.SelectPowerPayload;
import com.josephison.fiatlux.network.SyncDivinityPayload;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerRegistry;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The radial power wheel: a ring of the Creator's powers, each a coloured segment with its sign.
 * Point toward one and it rises out of the ring and its name, words and cost appear in the medallion
 * at the centre; click to make it the power you will speak. The wheel opens with a turn and a bloom.
 */
public class PowerWheelScreen extends Screen {
    private static final float INNER = 50, OUTER = 112;
    private final List<ResourceLocation> powers = new ArrayList<>();
    private final float[] lift;
    private final long openedAt = Util.getMillis();
    private int hovered = -1;

    public PowerWheelScreen() {
        super(Component.translatable("screen.fiatlux.power_wheel"));
        powers.addAll(PowerRegistry.REGISTRY.keySet());
        powers.sort(null);
        lift = new float[powers.size()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float opening() {
        return Ui.easeOut((Util.getMillis() - openedAt) / 260f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float open = opening();
        float time = Util.getMillis() / 1000f;
        int cx = width / 2, cy = height / 2;
        // the veil: darker toward the edges
        g.fillGradient(0, 0, width, height, Ui.withAlpha(0x0A0610, (int) (0x70 * open)), Ui.withAlpha(0x0A0610, (int) (0xB0 * open)));
        Ui.ring(g, cx, cy, 0, OUTER + 60, Ui.withAlpha(0xF5D77A, (int) (0x22 * open)), 0x00F5D77A);

        SyncDivinityPayload s = ClientDivinity.get();
        int n = powers.size();
        float dx = mouseX - cx, dy = mouseY - cy;
        hovered = -1;
        if (n > 0 && dx * dx + dy * dy > (INNER - 6) * (INNER - 6) && dx * dx + dy * dy < (OUTER + 50) * (OUTER + 50)) {
            double a = Math.atan2(dx, -dy);
            if (a < 0) a += Math.PI * 2;
            hovered = (int) Math.floor(a / (Math.PI * 2) * n + 0.5) % n;
        }
        float spin = (1 - open) * -0.6f;   // the ring turns into place as it opens
        float scale = 0.8f + 0.2f * open;
        float r0 = INNER * scale, r1 = OUTER * scale;
        double slice = Math.PI * 2 / Math.max(1, n);
        for (int i = 0; i < n; i++) {
            AbstractDivinePower p = PowerRegistry.REGISTRY.get(powers.get(i));
            if (p == null) continue;
            lift[i] = Mth.lerp(0.35f, lift[i], i == hovered ? 1f : 0f);
            boolean locked = s.mode() == PlayMode.ASCENSION && p.tier() > s.tier();
            boolean sel = powers.get(i).equals(s.state().selectedPower());
            double mid = i * slice + spin;
            double a0 = mid - slice / 2 + 0.012, a1 = mid + slice / 2 - 0.012;
            float out = r1 + lift[i] * 10;
            int col = locked ? 0xFF3A3640 : 0xFF000000 | p.color();
            int alpha = (int) (255 * open);
            // the segment: deep at its root, its own colour at the rim
            Ui.sector(g, cx, cy, r0, out, a0, a1, Ui.withAlpha(Ui.shade(col, 0.22f), alpha * 230 / 255),
                    Ui.withAlpha(Ui.shade(col, 0.55f + lift[i] * 0.35f), alpha * 235 / 255));
            // its rim
            Ui.sector(g, cx, cy, out - 3, out, a0, a1, Ui.withAlpha(Ui.shade(col, 1.25f), alpha), Ui.withAlpha(Ui.shade(col, 1.5f), alpha));
            if (sel) Ui.sector(g, cx, cy, out + 2, out + 4, a0, a1, Ui.withAlpha(0xF5D77A, alpha), Ui.withAlpha(0xFFF6D0, alpha));
            // the power's sign
            float gr = (r0 + out) / 2;
            int gx = cx + (int) (Math.sin(mid) * gr), gy = cy - (int) (Math.cos(mid) * gr);
            String glyph = sign(p.name().getString());
            Component sign = Component.literal(glyph).withStyle(st -> st.withBold(true));
            int glyphCol = locked ? 0xFF7A7480 : i == hovered ? 0xFFFFFFFF : Ui.shade(0xFF000000 | p.color(), 1.6f);
            g.pose().pushPose();
            g.pose().translate(gx, gy, 0);
            float gs = (glyph.length() > 1 ? 1.0f : 1.4f) + lift[i] * 0.35f;
            g.pose().scale(gs, gs, 1);
            g.drawString(font, sign, -font.width(sign) / 2, -4, glyphCol, true);
            g.pose().popPose();
            if (locked) Ui.diamond(g, gx, gy + 11, 1, 0xFF8A8478);
        }
        // the medallion
        Ui.ring(g, cx, cy, 0, r0 - 4, Ui.withAlpha(0x1A1428, (int) (250 * open)), Ui.withAlpha(0x0C0A14, (int) (250 * open)));
        Ui.ring(g, cx, cy, r0 - 5, r0 - 3, Ui.withAlpha(0xB89A50, (int) (255 * open)), Ui.withAlpha(0xF5D77A, (int) (255 * open)));
        // a slow ring of light around it
        double glow = time * 0.8;
        Ui.sector(g, cx, cy, r0 - 3, r0 - 1, glow, glow + 1.2, 0x00FFF0C0, Ui.withAlpha(0xFFF0C0, (int) (0x90 * open)));
        ResourceLocation focus = hovered >= 0 ? powers.get(hovered) : s.state().selectedPower();
        AbstractDivinePower fp = PowerRegistry.REGISTRY.get(focus);
        if (fp != null && open > 0.5f) {
            boolean locked = s.mode() == PlayMode.ASCENSION && fp.tier() > s.tier();
            Component name = fp.name().copy().withStyle(st -> st.withBold(true));
            int nameCol = locked ? 0xFF9A94A0 : Ui.shade(0xFF000000 | fp.color(), 1.3f);
            g.drawString(font, name, cx - font.width(name) / 2, cy - 26, nameCol, true);
            Ui.rule(g, cx - 34, cx + 34, cy - 15, Ui.withAlpha(0xF5D77A, 0xA0));
            g.drawCenteredString(font, Component.literal((int) fp.cost() + " essence"), cx, cy - 7, 0xFFE8D7B0);
            Component tierText = Component.literal(locked ? "locked: tier " + fp.tier() : "tier " + fp.tier());
            int cw = font.width(tierText) + 8;
            Ui.chip(g, font, tierText, cx - cw / 2, cy + 6, locked ? 0xFFC08070 : 0xFFF5D77A);
            // its words, on a panel beneath the wheel
            int pw = Math.min(width - 20, 300);
            var lines = font.split(fp.description(), pw - 16);
            int ph = Math.min(4, lines.size()) * 10 + 10;
            int py = Math.min(height - ph - 4, cy + (int) (r1 + 16));
            Ui.panel(g, cx - pw / 2, py, pw, ph, open);
            Ui.centredLines(g, font, lines, cx, py + 6, 0xFFE8D7B0, 4);
        }
        // the name of the hovered power, outside the ring
        if (hovered >= 0 && open > 0.9f) {
            AbstractDivinePower hp = PowerRegistry.REGISTRY.get(powers.get(hovered));
            if (hp != null) {
                double mid = hovered * slice;
                int lx = cx + (int) (Math.sin(mid) * (r1 + 26)), ly = cy - (int) (Math.cos(mid) * (r1 + 26)) - 4;
                int w = font.width(hp.name());
                lx = Mth.clamp(lx - w / 2, 4, width - w - 4);
                g.fill(lx - 4, ly - 3, lx + w + 4, ly + 11, 0xC0100A14);
                g.renderOutline(lx - 4, ly - 3, w + 8, 14, 0xA0B89A50);
                g.drawString(font, hp.name(), lx, ly, Ui.shade(0xFF000000 | hp.color(), 1.3f), false);
            }
        }
        Ui.title(g, font, Component.literal("Powers of the Creator"), cx, Math.max(6, cy - (int) (r1 + 40)), Ui.GOLD);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** A power's sign on the wheel: the first letters of its chief words ("Breath of Life" - "BL"). */
    private static String sign(String name) {
        StringBuilder b = new StringBuilder();
        for (String w : name.split("[\\s-]+")) {
            if (w.isEmpty() || List.of("of", "the", "a", "an", "and", "in", "upon", "from", "to").contains(w.toLowerCase(java.util.Locale.ROOT))) continue;
            b.append(Character.toUpperCase(w.charAt(0)));
            if (b.length() == 2) break;
        }
        return b.isEmpty() ? name.substring(0, 1) : b.toString();
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
}
