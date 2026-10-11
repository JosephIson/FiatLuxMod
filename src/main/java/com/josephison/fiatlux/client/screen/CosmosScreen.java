package com.josephison.fiatlux.client.screen;

import net.minecraft.client.gui.GuiGraphics;
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

    private float shownScroll;
    private int[][] tabRects = new int[0][];

    @Override
    protected void init() {
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
                if (u.contains("flood")) {
                    heading("The days of Noah", 0xFF9AB8E8);
                    text(u.getString("flood"));
                }
                if (u.contains("spirits")) {
                    heading("Unclean spirits and the guardians", 0xFFD08070);
                    text(u.getString("spirits"));
                }
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
                    if (w.contains("government")) {
                        CompoundTag gov = w.getCompound("government");
                        text("The " + gov.getString("name") + ", ruled by " + gov.getString("ruler") + " from " + gov.getString("capital")
                                + (gov.getInt("judged") > 0 ? "; " + gov.getInt("judged") + " judged under its law" : "") + ".", 0xFFE8C890, 0);
                        var laws = gov.getList("laws", Tag.TAG_STRING);
                        for (int l = 0; l < laws.size(); l++) text(laws.getString(l), DIM, 8);
                        var an = gov.getList("annals", Tag.TAG_STRING);
                        for (int l = 0; l < an.size(); l++) text(an.getString(l), 0xFFB8B0C8, 8);
                    }
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
                        text(fa.getString("name") + " - " + fa.getInt("followers") + " following " + fa.getString("deity")
                                        + (fa.getString("leader").isEmpty() ? "" : ", led by " + fa.getString("leader"))
                                        + (fa.getString("parent").isEmpty() ? "" : " (split from the " + fa.getString("parent") + ")"),
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
        int w = Math.min(width - 24, 480);
        int x0 = (width - w) / 2, top = 56, bottom = height - 12;
        float time = net.minecraft.Util.getMillis() / 1000f;
        // the heavens: a deep gradient, a faint nebula and twinkling stars
        g.fillGradient(0, 0, width, height, 0xF8060814, 0xF8120A1C);
        Ui.ring(g, width * 0.78f, height * 0.25f, 0, Math.max(width, height) * 0.45f, 0x283A2A6A, 0x003A2A6A);
        Ui.ring(g, width * 0.2f, height * 0.8f, 0, Math.max(width, height) * 0.35f, 0x205A2A3A, 0x005A2A3A);
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 140; i++) {
            int sx = r.nextInt(Math.max(1, width)), sy = r.nextInt(Math.max(1, height));
            float tw = 0.5f + 0.5f * Mth.sin(time * (0.8f + r.nextFloat() * 2f) + r.nextFloat() * 6.28f);
            int a = (int) (40 + 160 * tw * r.nextFloat());
            g.fill(sx, sy, sx + 1, sy + 1, a << 24 | (r.nextInt(4) == 0 ? 0xC8D8FF : 0xFFFFFF));
            if (r.nextInt(18) == 0) {   // a few bright ones, with a cross of light
                g.fill(sx - 1, sy, sx + 2, sy + 1, (a / 2) << 24 | 0xFFFFFF);
                g.fill(sx, sy - 1, sx + 1, sy + 2, (a / 2) << 24 | 0xFFFFFF);
            }
        }
        Ui.title(g, font, Component.literal("The Cosmic Expanse"), width / 2, 12, GOLD);
        // the tabs
        tabRects = new int[TABS.length][];
        int tw = w / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + i * tw, ty = 30, th = 18;
            boolean sel = i == tab, hover = mouseX >= tx && mouseX < tx + tw - 2 && mouseY >= ty && mouseY < ty + th;
            tabRects[i] = new int[]{tx, ty, tw - 2, th};
            g.fillGradient(tx, ty, tx + tw - 2, ty + th, sel ? 0xF0302440 : hover ? 0xD0221A30 : 0xB0140F1E, sel ? 0xF0181222 : 0xB00C0A14);
            g.renderOutline(tx, ty, tw - 2, th, sel ? 0xFFF5D77A : hover ? 0xC0B89A50 : 0x808A6A2A);
            if (sel) g.fill(tx + 1, ty + th - 2, tx + tw - 3, ty + th, GOLD);
            String label = font.plainSubstrByWidth(TABS[i], tw - 8);
            g.drawCenteredString(font, label, tx + (tw - 2) / 2, ty + 5, sel ? GOLD : hover ? TEXT : DIM);
        }
        Ui.panel(g, x0, top - 2, w, bottom - top + 2, 1f);
        // the page, scrolled smoothly
        int innerW = w - 24;
        List<Object[]> rows = new ArrayList<>();   // {FormattedCharSequence, color, indent, kind, value}
        for (Line l : lines) {
            if (l.kind == Line.SPACE) {
                rows.add(new Object[]{null, 0, 0, Line.SPACE, 0f});
            } else if (l.kind == Line.BAR) {
                rows.add(new Object[]{l.text.getVisualOrderText(), l.color, 0, Line.BAR, l.value});
            } else {
                for (FormattedCharSequence seq : font.split(l.text, innerW - l.indent - (l.kind == Line.HEADING ? 10 : 0))) {
                    rows.add(new Object[]{seq, l.color, l.indent, l.kind, 0f});
                }
            }
        }
        int contentH = 0;
        for (Object[] row : rows) contentH += rowHeight((int) row[3]);
        int viewH = bottom - top - 10;
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentH - viewH));
        shownScroll = Mth.lerp(Math.min(1f, partialTick * 0.5f + 0.25f), shownScroll, (float) scroll);
        g.enableScissor(x0 + 3, top + 2, x0 + w - 3, bottom - 3);
        int y = top + 6 - (int) shownScroll;
        for (Object[] row : rows) {
            int kind = (int) row[3];
            int h = rowHeight(kind);
            if (y + h >= top && y <= bottom && row[0] != null) {
                int x = x0 + 12 + (int) row[2];
                int color = (int) row[1];
                FormattedCharSequence seq = (FormattedCharSequence) row[0];
                if (kind == Line.HEADING) {
                    Ui.diamond(g, x + 3, y + 6, 3, color);
                    g.drawString(font, seq, x + 11, y + 2, color, true);
                    Ui.hGradient(g, x, y + 13, x0 + w - 12, y + 14, color & 0xFFFFFF, 0xA0, 0x10);
                } else if (kind == Line.BAR) {
                    int bw = Math.min(160, innerW / 2), bx = x0 + w - 14 - bw;
                    g.drawString(font, seq, x, y + 1, TEXT, false);
                    Ui.bar(g, bx, y + 2, bw, 5, (float) row[4], Ui.shade(color, 0.6f), color, time * 0.3f);
                } else {
                    if ((int) row[2] > 0) Ui.px(g, x - 5, y + 3, Ui.withAlpha(color & 0xFFFFFF, 0x90));
                    g.drawString(font, seq, x, y, color, false);
                }
            }
            y += h;
        }
        g.disableScissor();
        // fade the page at its top and bottom edges
        g.fillGradient(x0 + 3, top + 1, x0 + w - 3, top + 8, 0xC00C0A14, 0x000C0A14);
        g.fillGradient(x0 + 3, bottom - 9, x0 + w - 3, bottom - 2, 0x000C0A14, 0xC00C0A14);
        if (contentH > viewH) {
            int barH = Math.max(16, viewH * viewH / contentH);
            int barY = top + 2 + (int) ((viewH - barH) * (shownScroll / Math.max(1, contentH - viewH)));
            g.fill(x0 + w - 6, top + 2, x0 + w - 5, bottom - 3, 0x30F5D77A);
            g.fill(x0 + w - 7, barY, x0 + w - 4, barY + barH, 0xC0F5D77A);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < tabRects.length; i++) {
            int[] t = tabRects[i];
            if (t != null && mouseX >= t[0] && mouseX < t[0] + t[2] && mouseY >= t[1] && mouseY < t[1] + t[3]) {
                if (tab != i) {
                    tab = i;
                    scroll = 0;
                    shownScroll = 0;
                    rebuild();
                    net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1f));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
            tab = Math.floorMod(tab + (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT ? 1 : -1), TABS.length);
            scroll = 0;
            shownScroll = 0;
            rebuild();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private static int rowHeight(int kind) {
        return switch (kind) {
            case Line.HEADING -> 18;
            case Line.SPACE -> 6;
            case Line.BAR -> 13;
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
