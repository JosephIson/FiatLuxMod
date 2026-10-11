package com.josephison.fiatlux.client.screen;

import com.josephison.fiatlux.network.LineagePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Book of Generations (Genesis 5:1): a family drawn on parchment across five generations -
 * grandparents, parents, the person and their spouse, children and grandchildren - each with their
 * age, trade, world, faith and, for the dead, where their soul went. Click anyone to turn the page to
 * their family. The faiths of the cosmos and their followers are listed in the margin.
 */
public class GenerationsScreen extends Screen {
    private static final int BOX_W = 112, BOX_H = 34;
    private LineagePayloads.View view;
    private final Map<UUID, int[]> boxes = new HashMap<>();

    public GenerationsScreen(LineagePayloads.View view) {
        super(Component.literal("The Book of Generations"));
        this.view = view;
    }

    public void show(LineagePayloads.View view) {
        this.view = view;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = 12, top = 10, right = width - 12, bottom = height - 10;
        int margin = Math.min(170, width / 4);
        // parchment
        g.fill(left, top, right, bottom, 0xF0E8DCC0);
        g.renderOutline(left, top, right - left, bottom - top, 0xFF8A6A3A);
        g.renderOutline(left + 3, top + 3, right - left - 6, bottom - top - 6, 0xFFB8965A);
        g.drawCenteredString(font, Component.literal("The Book of Generations").withStyle(s -> s.withBold(true)), (left + right - margin) / 2, top + 8, 0xFF5A3A1A);
        g.drawCenteredString(font, Component.literal("\"This is the book of the generations of Adam\" - " + view.living() + " living, "
                + view.total() + " written"), (left + right - margin) / 2, top + 20, 0xFF7A5A3A);
        // the margin: faiths
        int mx = right - margin + 4;
        g.fill(mx - 6, top + 6, mx - 5, bottom - 6, 0xFFB8965A);
        g.drawString(font, Component.literal("Faiths").withStyle(s -> s.withBold(true)), mx, top + 10, 0xFF5A3A1A, false);
        int fy = top + 24;
        for (LineagePayloads.Faith f : view.faiths()) {
            if (fy > bottom - 24) break;
            g.fill(mx, fy + 1, mx + 6, fy + 7, f.color() | 0xFF000000);
            g.drawString(font, font.plainSubstrByWidth(f.name(), margin - 20), mx + 9, fy, 0xFF3A2A1A, false);
            g.drawString(font, font.plainSubstrByWidth(f.followers() + " follow " + f.deity(), margin - 20), mx + 9, fy + 9, 0xFF7A6A5A, false);
            fy += 22;
        }
        if (view.nodes().isEmpty()) {
            g.drawCenteredString(font, "No mortal has yet been given the breath of life.", (left + right - margin) / 2, height / 2, 0xFF5A3A1A);
            return;
        }
        // lay the generations out in rows
        boxes.clear();
        int treeW = right - margin - left - 20;
        int rowGap = Math.max(BOX_H + 10, (bottom - top - 60) / 5);
        Map<Integer, List<LineagePayloads.Node>> rows = new HashMap<>();
        for (LineagePayloads.Node n : view.nodes()) rows.computeIfAbsent(n.gen(), k -> new ArrayList<>()).add(n);
        for (int gen = -2; gen <= 2; gen++) {
            List<LineagePayloads.Node> row = rows.getOrDefault(gen, List.of());
            int y = top + 36 + (gen + 2) * rowGap;
            int n = row.size();
            for (int i = 0; i < n; i++) {
                int cx = left + 10 + (int) ((i + 0.5) * treeW / Math.max(1, n));
                boxes.put(row.get(i).id(), new int[]{cx - BOX_W / 2, y, cx, y + BOX_H});
            }
        }
        // lines from each child up to its parents
        for (LineagePayloads.Node n : view.nodes()) {
            int[] b = boxes.get(n.id());
            for (Optional<UUID> parent : List.of(n.father(), n.mother())) {
                int[] p = parent.map(boxes::get).orElse(null);
                if (p == null) continue;
                int midY = (p[3] + b[1]) / 2;
                g.fill(p[2], p[3], p[2] + 1, midY, 0xFF8A6A3A);
                g.fill(Math.min(p[2], b[2]), midY, Math.max(p[2], b[2]) + 1, midY + 1, 0xFF8A6A3A);
                g.fill(b[2], midY, b[2] + 1, b[1], 0xFF8A6A3A);
            }
        }
        // the people
        for (LineagePayloads.Node n : view.nodes()) {
            int[] b = boxes.get(n.id());
            boolean hover = mouseX >= b[0] && mouseX < b[0] + BOX_W && mouseY >= b[1] && mouseY < b[1] + BOX_H;
            int fill = n.focus() ? 0xFFF8EFD0 : hover ? 0xFFF2E6C8 : 0xFFEADBB8;
            int border = switch (n.soul()) {
                case "heaven" -> 0xFFD8A830;
                case "underworld" -> 0xFF8A2020;
                default -> n.male() ? 0xFF5A7AA0 : 0xFFA05A7A;
            };
            g.fill(b[0], b[1], b[0] + BOX_W, b[1] + BOX_H, fill);
            g.renderOutline(b[0], b[1], BOX_W, BOX_H, border);
            if (n.focus()) g.renderOutline(b[0] - 1, b[1] - 1, BOX_W + 2, BOX_H + 2, border);
            String fate = n.soul().equals("heaven") ? " +" : n.soul().equals("underworld") ? " -" : "";
            g.drawString(font, font.plainSubstrByWidth(n.name() + fate, BOX_W - 6), b[0] + 3, b[1] + 3, 0xFF2A1A0A, false);
            g.drawString(font, font.plainSubstrByWidth(n.info(), BOX_W - 6), b[0] + 3, b[1] + 13, 0xFF6A5A4A, false);
            g.drawString(font, font.plainSubstrByWidth(n.faith().isEmpty() ? "no faith" : n.faith(), BOX_W - 6), b[0] + 3, b[1] + 23, 0xFF8A6A3A, false);
        }
        g.drawString(font, "+ in Heaven   - in the Underworld   click anyone to see their family", left + 8, bottom - 12, 0xFF8A7A6A, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var e : boxes.entrySet()) {
            int[] b = e.getValue();
            if (mouseX >= b[0] && mouseX < b[0] + BOX_W && mouseY >= b[1] && mouseY < b[1] + BOX_H) {
                PacketDistributor.sendToServer(new LineagePayloads.Request(Optional.of(e.getKey())));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
