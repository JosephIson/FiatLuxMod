package com.josephison.fiatlux.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cosmic Expanse (/fiatlux cosmos): the whole creation at a glance, in five pages - the universe,
 * every world with its living people, towns and faiths, Heaven and the Underworld, the hierarchy of
 * the fallen, and the Chronicle. Scroll with the mouse wheel.
 */
public class CosmosScreen extends Screen {
    private static final String[] TABS = {"Universe", "Worlds", "Heaven & Hell", "The Fallen", "Chronicle"};
    private static final int GOLD = 0xFFF5D77A, TEXT = 0xFFE6E0D2, DIM = 0xFF9A94A8, HEAVEN = 0xFFFFF0C0, HELL = 0xFFFF7A5A;
    private CompoundTag data;
    private int tab;
    private double scroll;
    private final List<Line> lines = new ArrayList<>();

    private record Line(Component text, int color, int indent, int kind, float value) {
        static final int TEXT = 0, HEADING = 1, BAR = 2, SPACE = 3;
    }

    public CosmosScreen(CompoundTag data) {
        super(Component.literal("The Cosmic Expanse"));
        this.data = data;
    }

    public void show(CompoundTag data) {
        this.data = data;
        rebuild();
    }

    @Override
    protected void init() {
        int w = Math.min(width - 24, 460);
        int x0 = (width - w) / 2;
        int bw = w / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> {
                tab = t;
                scroll = 0;
                rebuild();
            }).bounds(x0 + i * bw, 30, bw - 2, 18).build());
        }
        rebuild();
    }

    // ------------------------------------------------------------------ content

    private void heading(String s, int color) {
        if (!lines.isEmpty()) lines.add(new Line(Component.empty(), 0, 0, Line.SPACE, 0));
        lines.add(new Line(Component.literal(s), color, 0, Line.HEADING, 0));
    }

    private void text(String s, int color, int indent) {
        lines.add(new Line(Component.literal(s), color, indent, Line.TEXT, 0));
    }

    private void text(String s) {
        text(s, TEXT, 0);
    }

    private void bar(String label, double v, int color) {
        lines.add(new Line(Component.literal(label), color, 0, Line.BAR, (float) Mth.clamp(v, 0, 1)));
    }

    private static String num(double v) {
        return com.josephison.fiatlux.network.CosmosReport.num(v);
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    private void rebuild() {
        lines.clear();
        CompoundTag u = data.getCompound("universe"), m = data.getCompound("moral"), uw = data.getCompound("underworld");
        switch (tab) {
            case 0 -> {
                heading("Creation", GOLD);
                text(u.getBoolean("bigBang") ? "The light has been spoken: the universe is " + String.format("%.1f", u.getDouble("age")) + " days old."
                        : "Darkness is upon the face of the deep. Speak Let There Be Light in the Void.");
                text(plural(u.getInt("galaxies"), "galaxy", "galaxies") + " · " + plural(u.getInt("blackHoles"), "black hole", "black holes")
                        + " · " + plural(u.getInt("worlds"), "world", "worlds") + " (" + u.getInt("living") + " with life)");
                text("Glory " + num(u.getLong("glory")) + " · Ascension tier " + u.getInt("tier"));
                text("One mortal year passes every " + timeText(u.getInt("yearSeconds")) + " (simulation speed ×" + num(u.getDouble("speed")) + ").", DIM, 0);
                heading("The garden of Eden", 0xFF9CE38C);
                text(u.getString("eden"));
                heading("The living", GOLD);
                text(plural(data.getInt("totalLiving"), "mortal is", "mortals are") + " alive, and " + plural(data.getInt("totalWritten"), "life is", "lives are")
                        + " written in the Book of Generations.");
                if (data.getInt("earthLiving") > 0) text(data.getInt("earthLiving") + " of them live on the earth.");
                text(plural(data.getInt("faithCount"), "faith has", "faiths have") + " been founded.");
                heading("The moral climate", GOLD);
                bar("Corruption of creation " + pct(m.getDouble("corruption")), m.getDouble("corruption"), HELL);
                bar("Pride among the Host " + pct(m.getDouble("pride")), m.getDouble("pride"), 0xFFE0A050);
                text(m.getBoolean("rebellion") ? "There is war in Heaven." : m.getDouble("fallen") > 0 ? "Heaven is at peace since the Fall."
                        : "Heaven is at peace; no angel has fallen.");
            }
            case 1 -> {
                var worlds = data.getList("worlds", Tag.TAG_COMPOUND);
                if (worlds.isEmpty()) text("No world has yet been formed. Use Planetary Genesis in the Void.");
                for (int i = 0; i < worlds.size(); i++) {
                    CompoundTag w = worlds.getCompound(i);
                    heading(w.getString("name"), 0xFF000000 | w.getInt("color") | 0x404040);
                    text(cap(w.getString("type")) + " world, " + String.format("%.2f", w.getDouble("orbit")) + " AU from the sun", DIM, 0);
                    if (!w.getBoolean("life")) {
                        text("Without life. Breathe the Breath of Life upon it.");
                        continue;
                    }
                    text("People: " + w.getInt("living") + " living mortals you can meet; " + num(w.getDouble("population")) + " in all, in the "
                            + w.getString("era") + " age.");
                    if (!w.getString("giver").isEmpty()) text("Life was breathed into it by " + w.getString("giver") + ".", DIM, 0);
                    bar("Faith " + pct(w.getDouble("faith")), w.getDouble("faith"), HEAVEN);
                    bar("Sin " + pct(w.getDouble("sin")), w.getDouble("sin"), HELL);
                    var towns = w.getList("towns", Tag.TAG_COMPOUND);
                    text("Towns (" + towns.size() + "):", GOLD, 0);
                    for (int t = 0; t < towns.size(); t++) {
                        CompoundTag town = towns.getCompound(t);
                        int people = town.getInt("people");
                        String who = people >= 0 ? people + " people, " + town.getInt("houses") + " houses, food " + town.getInt("food")
                                : "far from any Creator (not loaded)";
                        String petitions = town.getInt("petitions") > 0 ? " · " + town.getInt("petitions") + " prayers awaiting an answer" : "";
                        text(town.getString("name") + ": " + who + petitions, TEXT, 8);
                        if (!town.getString("founders").isEmpty()) text("founded by " + town.getString("founders"), DIM, 16);
                    }
                    var faiths = w.getList("faiths", Tag.TAG_COMPOUND);
                    if (!faiths.isEmpty()) text("Faiths:", GOLD, 0);
                    for (int f = 0; f < faiths.size(); f++) {
                        CompoundTag fa = faiths.getCompound(f);
                        text(fa.getString("name") + " - " + fa.getInt("followers") + " following " + fa.getString("deity"),
                                0xFF000000 | fa.getInt("color"), 8);
                    }
                }
            }
            case 2 -> {
                heading("Heaven", HEAVEN);
                text(num(m.getDouble("heaven")) + " souls have been taken up into Heaven.");
                heading("The Underworld", HELL);
                if (m.getDouble("fallen") <= 0) text("No angel has yet fallen; the deep is empty and dark.");
                text("It has reached the " + uw.getString("era") + " era, with " + num(uw.getDouble("knowledge")) + " knowledge gathered.");
                text(num(uw.getDouble("demons")) + " fallen angels are at large below.");
                text(num(uw.getDouble("souls")) + " sinner souls are held there, yielding " + num(uw.getDouble("soulPower")) + " soul power.");
                text(num(uw.getDouble("contracts")) + " soul contracts have been signed with the living.");
                text(uw.getInt("lordsRisen") + " of the " + uw.getInt("lordsAll") + " lords of the hierarchy have risen, commanding "
                        + num(uw.getLong("legions")) + " legions.");
            }
            case 3 -> {
                heading("The hierarchy of the fallen", HELL);
                text("\"For we wrestle not against flesh and blood, but against principalities, against powers, against the rulers of the "
                        + "darkness of this world, against spiritual wickedness in high places.\" (Ephesians 6:12)", DIM, 0);
                var lords = uw.getList("lords", Tag.TAG_COMPOUND);
                if (lords.isEmpty()) text("No angel has yet fallen. The hierarchy of darkness is empty.");
                for (int i = 0; i < lords.size(); i++) {
                    CompoundTag l = lords.getCompound(i);
                    String legions = l.getInt("legions") > 0 ? ", " + l.getInt("legions") + " legions" : "";
                    String sin = l.getString("sin").isEmpty() ? "" : " - over " + l.getString("sin");
                    text(l.getString("title") + legions + sin, i == 0 ? HELL : TEXT, 0);
                    text(l.getString("office") + "  [" + l.getString("source") + "]", DIM, 10);
                }
            }
            default -> {
                heading("The Chronicle", GOLD);
                var ch = data.getList("chronicle", Tag.TAG_STRING);
                if (ch.isEmpty()) text("Nothing has yet been written.");
                for (int i = ch.size() - 1; i >= 0; i--) text(ch.getString(i));
            }
        }
    }

    private static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String timeText(int seconds) {
        return seconds % 60 == 0 ? seconds / 60 + (seconds == 60 ? " minute" : " minutes") : seconds + " seconds";
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int w = Math.min(width - 24, 460);
        int x0 = (width - w) / 2, top = 52, bottom = height - 12;
        g.fillGradient(0, 0, width, height, 0xF0060814, 0xF0120A1C);
        // a few fixed stars
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 90; i++) {
            int sx = r.nextInt(Math.max(1, width)), sy = r.nextInt(Math.max(1, height));
            g.fill(sx, sy, sx + 1, sy + 1, 0x60FFFFFF | (r.nextInt(80) << 24));
        }
        g.drawCenteredString(font, Component.literal("The Cosmic Expanse").withStyle(s -> s.withBold(true)), width / 2, 10, GOLD);
        g.fill(x0, top - 2, x0 + w, bottom, 0x60000000);
        g.renderOutline(x0, top - 2, w, bottom - top + 2, 0x70F5D77A);
        super.render(g, mouseX, mouseY, partialTick);
        // the page, scrolled
        int innerW = w - 20;
        List<Object[]> rows = new ArrayList<>();   // {FormattedCharSequence or Line, color, indent, kind, value}
        for (Line l : lines) {
            if (l.kind == Line.SPACE) {
                rows.add(new Object[]{null, 0, 0, Line.SPACE, 0f});
            } else if (l.kind == Line.BAR) {
                rows.add(new Object[]{l.text.getVisualOrderText(), l.color, 0, Line.BAR, l.value});
            } else {
                for (FormattedCharSequence seq : font.split(l.text, innerW - l.indent)) rows.add(new Object[]{seq, l.color, l.indent, l.kind, 0f});
            }
        }
        int contentH = 0;
        for (Object[] row : rows) contentH += rowHeight((int) row[3]);
        int viewH = bottom - top - 8;
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentH - viewH));
        g.enableScissor(x0 + 1, top, x0 + w - 1, bottom - 1);
        int y = top + 4 - (int) scroll;
        for (Object[] row : rows) {
            int kind = (int) row[3];
            int h = rowHeight(kind);
            if (y + h >= top && y <= bottom && row[0] != null) {
                int x = x0 + 10 + (int) row[2];
                int color = (int) row[1];
                FormattedCharSequence seq = (FormattedCharSequence) row[0];
                if (kind == Line.HEADING) {
                    g.drawString(font, seq, x, y + 2, color, true);
                    g.fill(x, y + 12, x0 + w - 10, y + 13, (color & 0x00FFFFFF) | 0x50000000);
                } else if (kind == Line.BAR) {
                    int bw = 140, bx = x0 + w - 10 - bw;
                    g.drawString(font, seq, x, y + 1, TEXT, false);
                    g.fill(bx, y + 2, bx + bw, y + 8, 0x40FFFFFF);
                    g.fill(bx, y + 2, bx + (int) (bw * (float) row[4]), y + 8, color);
                } else {
                    g.drawString(font, seq, x, y, color, false);
                }
            }
            y += h;
        }
        g.disableScissor();
        if (contentH > viewH) {
            int barH = Math.max(16, viewH * viewH / contentH);
            int barY = top + (int) ((viewH - barH) * (scroll / Math.max(1, contentH - viewH)));
            g.fill(x0 + w - 4, barY, x0 + w - 2, barY + barH, 0x90F5D77A);
        }
    }

    private static int rowHeight(int kind) {
        return switch (kind) {
            case Line.HEADING -> 16;
            case Line.SPACE -> 6;
            case Line.BAR -> 11;
            default -> 10;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        scroll -= dy * 20;
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
