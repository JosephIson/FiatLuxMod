package com.josephison.fiatlux.client.screen;

import com.josephison.fiatlux.network.LineagePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * The Book of Generations (Genesis 5:1): a family drawn on a parchment page across five generations -
 * grandparents, parents, the person and their spouse, children and grandchildren - each on a card
 * with their age, trade, world, faith and, for the dead, where their soul went. Click anyone to turn
 * the page to their family; "Back" returns to the page before, "The First Parents" to the beginning.
 * The faiths of the cosmos and how many follow each are kept in the margin.
 */
public class GenerationsScreen extends Screen {
    private static final int INK = 0xFF3A2410, INK_SOFT = 0xFF7A5A3A, INK_FAINT = 0xFFA08860, GOLD = 0xFFC8901E, RED = 0xFF9A2A1A;
    private static final String[] ROWS = {"Grandparents", "Parents", "", "Children", "Grandchildren"};
    private LineagePayloads.View view;
    private final Deque<UUID> history = new ArrayDeque<>();
    private final Map<UUID, int[]> boxes = new HashMap<>();
    private int[] backButton, firstButton;
    private UUID hovered;
    private float turn = 1f;
    private List<Component> tooltip;

    public GenerationsScreen(LineagePayloads.View view) {
        super(Component.literal("The Book of Generations"));
        this.view = view;
    }

    public void show(LineagePayloads.View view) {
        this.view = view;
        turn = 0f;   // the page turns
    }

    @Nullable
    private UUID focusId() {
        for (LineagePayloads.Node n : view.nodes()) if (n.focus()) return n.id();
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        turn = Math.min(1f, turn + partialTick * 0.12f);
        int left = 10, top = 8, right = width - 10, bottom = height - 8;
        Ui.parchment(g, left, top, right - left, bottom - top);
        int margin = Mth.clamp(width / 4, 120, 180);
        int pageR = right - margin;
        int cx = (left + pageR) / 2;

        // the title, in ink, with flourishes
        Component title = Component.literal("The Book of Generations").withStyle(s -> s.withBold(true));
        int tw = font.width(title);
        g.drawString(font, title, cx - tw / 2, top + 12, INK, false);
        Ui.hGradient(g, cx - tw / 2 - 60, top + 16, cx - tw / 2 - 8, top + 17, 0x7A5228, 0, 0xC0);
        Ui.hGradient(g, cx + tw / 2 + 8, top + 16, cx + tw / 2 + 60, top + 17, 0x7A5228, 0xC0, 0);
        Ui.diamond(g, cx - tw / 2 - 5, top + 16, 2, INK_SOFT);
        Ui.diamond(g, cx + tw / 2 + 4, top + 16, 2, INK_SOFT);
        int pageW = pageR - left - 24;
        String verseText = "\"This is the book of the generations of Adam\" - " + view.living() + " living, " + view.total() + " written";
        if (font.width(verseText) > pageW) verseText = view.living() + " living · " + view.total() + " written";
        Component verse = Component.literal(verseText).withStyle(s -> s.withItalic(true));
        g.drawString(font, verse, cx - font.width(verse) / 2, top + 25, INK_SOFT, false);

        // the buttons, on their own line under the title
        backButton = history.isEmpty() ? null : button(g, left + 12, top + 37, "< Back", mouseX, mouseY);
        firstButton = button(g, pageR - 12 - font.width("The First Parents") - 10, top + 37, "The First Parents", mouseX, mouseY);

        tooltip = null;
        drawMargin(g, pageR, top, right, bottom, mouseX, mouseY);

        if (view.nodes().isEmpty()) {
            g.drawCenteredString(font, "No mortal has yet been given the breath of life.", cx, height / 2, INK_SOFT);
            return;
        }
        // lay the generations out in rows
        boxes.clear();
        boolean labels = pageW > 380;
        int treeL = labels ? left + 22 + font.width("Grandchildren") + 8 : left + 14, treeR = pageR - 12, treeW = treeR - treeL;
        Map<Integer, List<LineagePayloads.Node>> rows = new HashMap<>();
        for (LineagePayloads.Node n : view.nodes()) rows.computeIfAbsent(n.gen(), k -> new ArrayList<>()).add(n);
        // only the generations there are take up the page
        int minGen = rows.keySet().stream().min(Integer::compare).orElse(0), maxGen = rows.keySet().stream().max(Integer::compare).orElse(0);
        int count = maxGen - minGen + 1;
        int rowTop = top + 54, rowH = Math.max(24, Math.min(70, (bottom - 28 - rowTop) / count));
        rowTop += Math.max(0, (bottom - 28 - top - 54 - rowH * count) / 2);
        int boxH = Math.max(16, Math.min(34, rowH - 10));
        for (int gen = minGen; gen <= maxGen; gen++) {
            int y = rowTop + (gen - minGen) * rowH;
            String label = ROWS[gen + 2];
            if (!label.isEmpty() && labels) g.drawString(font, Component.literal(label).withStyle(s -> s.withItalic(true)), left + 14, y + boxH / 2 - 4, INK_FAINT, false);
            Ui.hGradient(g, left + 14, y + rowH - 4, treeR, y + rowH - 3, 0x7A5228, 0x18, 0x00);
            List<LineagePayloads.Node> row = rows.getOrDefault(gen, List.of());
            int n = row.size();
            if (n == 0) continue;
            int boxW = Math.min(124, treeW / n - 6);
            for (int i = 0; i < n; i++) {
                int bx = treeL + (int) ((i + 0.5) * treeW / n);
                boxes.put(row.get(i).id(), new int[]{bx - boxW / 2, y, bx, y + boxH, boxW});
            }
        }
        hovered = null;
        for (var e : boxes.entrySet()) {
            int[] b = e.getValue();
            if (mouseX >= b[0] && mouseX < b[0] + b[4] && mouseY >= b[1] && mouseY < b[3]) hovered = e.getKey();
        }
        // the lines from each child up to its parents
        for (LineagePayloads.Node n : view.nodes()) {
            int[] b = boxes.get(n.id());
            for (Optional<UUID> parent : List.of(n.father(), n.mother())) {
                int[] p = parent.map(boxes::get).orElse(null);
                if (p == null) continue;
                int midY = (p[3] + b[1]) / 2;
                int col = n.id().equals(hovered) || parent.get().equals(hovered) ? GOLD : INK_SOFT;
                g.fill(p[2], p[3], p[2] + 2, midY, col);
                g.fill(Math.min(p[2], b[2]), midY, Math.max(p[2], b[2]) + 2, midY + 2, col);
                g.fill(b[2], midY, b[2] + 2, b[1], col);
            }
        }
        // husband and wife, joined
        UUID focus = focusId();
        for (LineagePayloads.Node n : view.nodes()) {
            if (n.gen() != 0 || n.focus() || focus == null) continue;
            int[] a = boxes.get(focus), b = boxes.get(n.id());
            if (a == null || b == null) continue;
            int x0 = Math.min(a[0] + a[4], b[0] + b[4]), x1 = Math.max(a[0], b[0]);
            int y = a[1] + (a[3] - a[1]) / 2;
            if (x1 > x0) {
                g.fill(x0, y - 2, x1, y - 1, GOLD);
                g.fill(x0, y + 1, x1, y + 2, GOLD);
                Ui.diamond(g, (x0 + x1) / 2, y, 3, GOLD);
            }
        }
        // the people
        float slide = Ui.easeOut(turn);
        for (LineagePayloads.Node n : view.nodes()) card(g, n, boxes.get(n.id()), n.id().equals(hovered), slide);
        // a hint at the foot of the page
        String hint = hovered != null ? "Click to turn to the family of " + nameOf(hovered) : "+ in Heaven     - in the Underworld     click anyone to see their family";
        if (font.width(hint) > pageW) hint = hovered != null ? "Open " + nameOf(hovered) : "Click anyone to see their family";
        g.drawCenteredString(font, Component.literal(font.plainSubstrByWidth(hint, pageW)).withStyle(s -> s.withItalic(true)), cx, bottom - 18, INK_FAINT);
        if (tooltip != null) g.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    private String nameOf(UUID id) {
        for (LineagePayloads.Node n : view.nodes()) if (n.id().equals(id)) return n.name();
        return "";
    }

    /** A card for one person: a coloured band above (blue for men, rose for women), their soul's mark, name, life and faith. */
    private void card(GuiGraphics g, LineagePayloads.Node n, int[] b, boolean hover, float slide) {
        int w = b[4], h = b[3] - b[1];
        int x = b[0], y = b[1] + (int) ((1 - slide) * 6) - (hover ? 1 : 0);
        int alpha = (int) (255 * slide);
        if (alpha < 8) return;
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, Ui.withAlpha(0x5A3A1A, alpha * 0x50 / 255));   // shadow
        int fill = n.focus() ? 0xFFFBF3DA : hover ? 0xFFF8EED4 : 0xFFF2E6C6;
        g.fill(x, y, x + w, y + h, Ui.withAlpha(fill, alpha));
        int band = n.male() ? 0x5A7AA0 : 0xA05A7A;
        g.fill(x, y, x + w, y + 3, Ui.withAlpha(band, alpha));
        int border = switch (n.soul()) {
            case "heaven" -> 0xD8A830;
            case "underworld" -> 0x8A2020;
            default -> 0x8A6A3A;
        };
        g.renderOutline(x, y, w, h, Ui.withAlpha(border, alpha));
        if (n.focus() || hover) g.renderOutline(x - 1, y - 1, w + 2, h + 2, Ui.withAlpha(n.focus() ? 0xC8901E : 0xB08040, alpha));
        // the soul's mark
        int mx = x + w - 7, my = y + 9;
        switch (n.soul()) {
            case "heaven" -> {
                g.fill(mx - 3, my - 3, mx + 4, my - 2, Ui.withAlpha(0xD8A830, alpha));
                g.fill(mx - 3, my - 1, mx + 4, my, Ui.withAlpha(0xD8A830, alpha));
            }
            case "underworld" -> Ui.diamond(g, mx, my, 2, Ui.withAlpha(0x9A2A1A, alpha));
            case "" -> Ui.diamond(g, mx, my, 1, Ui.withAlpha(0x4A8A3A, alpha));
            default -> Ui.diamond(g, mx, my, 1, Ui.withAlpha(0x7A6A5A, alpha));
        }
        int tx = x + 4, tw = w - 14;
        Component name = Component.literal(font.plainSubstrByWidth(n.name(), tw)).withStyle(s -> s.withBold(n.focus()));
        g.drawString(font, name, tx, y + 5, Ui.withAlpha(INK, alpha), false);
        if (h >= 24) g.drawString(font, font.plainSubstrByWidth(n.info(), w - 8), tx, y + 15, Ui.withAlpha(INK_SOFT, alpha), false);
        if (h >= 33) g.drawString(font, font.plainSubstrByWidth(n.faith().isEmpty() ? "no faith" : n.faith(), w - 8), tx, y + 24,
                Ui.withAlpha(0x8A6A3A, alpha), false);
    }

    /** The margin: the faiths of the cosmos, each with its colour and a bar for its followers. */
    private void drawMargin(GuiGraphics g, int x0, int top, int right, int bottom, int mouseX, int mouseY) {
        g.fill(x0, top + 10, x0 + 1, bottom - 10, 0x907A5228);
        int mx = x0 + 8, w = right - mx - 12;
        g.drawString(font, Component.literal("Faiths").withStyle(s -> s.withBold(true)), mx, top + 14, INK, false);
        Ui.hGradient(g, mx, top + 24, mx + w, top + 25, 0x7A5228, 0xA0, 0);
        int most = 1;
        for (LineagePayloads.Faith f : view.faiths()) most = Math.max(most, f.followers());
        int fy = top + 30;
        for (LineagePayloads.Faith f : view.faiths()) {
            if (fy > bottom - 30) break;
            int col = 0xFF000000 | f.color();
            // a small shield in the faith's colour
            g.fill(mx, fy + 1, mx + 7, fy + 6, col);
            g.fill(mx + 1, fy + 6, mx + 6, fy + 7, col);
            g.fill(mx + 2, fy + 7, mx + 5, fy + 8, col);
            boolean hover = mouseX >= mx && mouseX < mx + w && mouseY >= fy && mouseY < fy + 24;
            String name = f.name();
            g.drawString(font, font.plainSubstrByWidth(name, w - 12), mx + 11, fy, hover ? GOLD : INK, false);
            g.drawString(font, font.plainSubstrByWidth(f.followers() + " follow " + f.deity(), w - 12), mx + 11, fy + 9, INK_SOFT, false);
            int bw = (int) ((w - 12) * (f.followers() / (float) most));
            g.fill(mx + 11, fy + 19, mx + 11 + w - 12, fy + 21, 0x30704A20);
            g.fill(mx + 11, fy + 19, mx + 11 + bw, fy + 21, Ui.withAlpha(f.color(), 0xC0));
            if (hover) tooltip = List.of(Component.literal(name), Component.literal(f.followers() + " living follow " + f.deity())
                    .withStyle(s -> s.withColor(0xFFC8B890)));
            fy += 27;
        }
    }

    private int[] button(GuiGraphics g, int x, int y, String text, int mouseX, int mouseY) {
        int w = font.width(text) + 10, h = 13;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hover ? 0xFFE8D2A0 : 0xFFEADBB8);
        g.renderOutline(x, y, w, h, hover ? GOLD : INK_SOFT);
        g.drawString(font, text, x + 5, y + 3, hover ? RED : INK, false);
        return new int[]{x, y, w, h};
    }

    private static boolean inside(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inside(backButton, mouseX, mouseY) && !history.isEmpty()) {
            PacketDistributor.sendToServer(new LineagePayloads.Request(Optional.of(history.pop())));
            return true;
        }
        if (inside(firstButton, mouseX, mouseY)) {
            UUID f = focusId();
            if (f != null) history.push(f);
            PacketDistributor.sendToServer(new LineagePayloads.Request(Optional.empty()));
            return true;
        }
        for (var e : boxes.entrySet()) {
            int[] b = e.getValue();
            if (mouseX >= b[0] && mouseX < b[0] + b[4] && mouseY >= b[1] && mouseY < b[3]) {
                UUID f = focusId();
                if (e.getKey().equals(f)) return true;   // already open at this one
                if (f != null) history.push(f);
                PacketDistributor.sendToServer(new LineagePayloads.Request(Optional.of(e.getKey())));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !history.isEmpty()) {
            PacketDistributor.sendToServer(new LineagePayloads.Request(Optional.of(history.pop())));
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
