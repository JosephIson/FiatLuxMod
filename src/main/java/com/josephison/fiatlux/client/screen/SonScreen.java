package com.josephison.fiatlux.client.screen;

import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.SonPayloads;
import com.josephison.fiatlux.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * "In the fulness of the time": the Creator chooses the Son - his name, his likeness, the town of his
 * birth and his mother - on an illuminated page, with his likeness turning beside it.
 */
public class SonScreen extends Screen {
    private static final String[] NAMES = {"Immanuel", "Yeshua", "Joshua", "Emmanuel", "Elior", "Adonai", "Shiloh", "Ariel", "Elijah", "Gabriel"};
    private final SonPayloads.Setup setup;
    private EditBox name;
    private int variant, town, mother = -1, suggestion;
    @Nullable private MortalEntity likeness;
    private final java.util.List<int[]> buttons = new java.util.ArrayList<>();
    private final java.util.List<Runnable> actions = new java.util.ArrayList<>();

    public SonScreen(SonPayloads.Setup setup) {
        super(Component.literal("The Fulness of the Time"));
        this.setup = setup;
    }

    @Override
    protected void init() {
        int w = Math.min(width - 20, 420), x0 = (width - w) / 2;
        name = new EditBox(font, x0 + w / 2 + 10, 72, w / 2 - 30, 16, Component.literal("Name"));
        name.setMaxLength(24);
        name.setValue(name.getValue().isEmpty() ? NAMES[0] : name.getValue());
        addRenderableWidget(name);
        if (Minecraft.getInstance().level != null) {
            likeness = ModEntities.MORTAL.get().create(Minecraft.getInstance().level);
            if (likeness != null) {
                likeness.setMaleLikeness(true);
                likeness.setVariant(variant);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xE0060410, 0xF0100818);
        int w = Math.min(width - 20, 420), h = Math.min(height - 20, 250);
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        Ui.panel(g, x0, y0, w, h, 1f);
        Ui.title(g, font, Component.literal("The Fulness of the Time"), width / 2, y0 + 10, Ui.GOLD);
        Component verse = Component.literal("\"God sent forth his Son, made of a woman\" - Galatians 4:4").withStyle(s -> s.withItalic(true));
        g.drawCenteredString(font, verse, width / 2, y0 + 24, Ui.MUTED);
        name.setY(y0 + 52);
        name.setX(x0 + w / 2 + 6);
        name.setWidth(w / 2 - 22);
        // his likeness
        int px = x0 + 16, pw = w / 2 - 30, py = y0 + 42, ph = h - 70;
        g.fillGradient(px, py, px + pw, py + ph, 0x40F5D77A, 0x10F5D77A);
        g.renderOutline(px, py, pw, ph, 0x80B89A50);
        Ui.ring(g, px + pw / 2f, py + 26, 10, 12, 0xA0FFE9A8, 0x40FFE9A8);   // the halo
        if (likeness != null) {
            likeness.setVariant(variant);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph, (int) (ph * 0.42), 0.0625f, mouseX, mouseY, likeness);
        }
        buttons.clear();
        actions.clear();
        button(g, px + 4, py + ph - 18, "<", mouseX, mouseY, () -> variant = Math.floorMod(variant - 1, MortalEntity.VARIANTS));
        button(g, px + pw - 18, py + ph - 18, ">", mouseX, mouseY, () -> variant = Math.floorMod(variant + 1, MortalEntity.VARIANTS));
        g.drawCenteredString(font, "Likeness " + (variant + 1) + " of " + MortalEntity.VARIANTS, px + pw / 2, py + ph - 14, Ui.TEXT);

        int rx = x0 + w / 2 + 6, rw = w / 2 - 22;
        g.drawString(font, Component.literal("His name").withStyle(s -> s.withBold(true)), rx, y0 + 40, Ui.GOLD, false);
        button(g, rx + rw - font.width("Suggest") - 8, y0 + 38, "Suggest", mouseX, mouseY, () -> {
            suggestion = (suggestion + 1) % NAMES.length;
            name.setValue(NAMES[suggestion]);
        });
        int y = y0 + 78;
        g.drawString(font, Component.literal("To be born in").withStyle(s -> s.withBold(true)), rx, y, Ui.GOLD, false);
        y += 12;
        if (setup.towns().isEmpty()) {
            g.drawString(font, "No town of " + setup.world() + " is near.", rx, y, 0xFFE08060, false);
        } else {
            SonPayloads.Town t = setup.towns().get(Math.floorMod(town, setup.towns().size()));
            button(g, rx, y - 2, "<", mouseX, mouseY, () -> {
                town = Math.floorMod(town - 1, setup.towns().size());
                mother = -1;
            });
            g.drawCenteredString(font, t.name() + " on " + setup.world(), rx + rw / 2, y, Ui.TEXT);
            button(g, rx + rw - 14, y - 2, ">", mouseX, mouseY, () -> {
                town = Math.floorMod(town + 1, setup.towns().size());
                mother = -1;
            });
            y += 20;
            g.drawString(font, Component.literal("Of the woman").withStyle(s -> s.withBold(true)), rx, y, Ui.GOLD, false);
            y += 12;
            int count = t.mothers().size();
            button(g, rx, y - 2, "<", mouseX, mouseY, () -> mother = mother <= -1 ? count - 1 : mother - 1);
            String who = mother < 0 || mother >= count ? "Chosen by your providence" : t.mothers().get(mother).name();
            g.drawCenteredString(font, who, rx + rw / 2, y, Ui.TEXT);
            button(g, rx + rw - 14, y - 2, ">", mouseX, mouseY, () -> mother = mother + 1 >= count ? -1 : mother + 1);
            y += 12;
            String info = mother < 0 || mother >= count ? "the most devout and fitting of the women of the town" : t.mothers().get(mother).info();
            for (var line : font.split(Component.literal(info), rw)) {
                g.drawCenteredString(font, line, rx + rw / 2, y, Ui.MUTED);
                y += 10;
            }
        }
        // let it be so
        int bw = 140, bx = width / 2 - bw / 2, by = y0 + h - 24;
        boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
        g.fillGradient(bx, by, bx + bw, by + 16, hover ? 0xFF5A4418 : 0xFF3A2C10, hover ? 0xFF3A2C10 : 0xFF241A08);
        g.renderOutline(bx, by, bw, 16, hover ? Ui.GOLD : Ui.GOLD_DIM);
        g.drawCenteredString(font, Component.literal("Let it be so").withStyle(s -> s.withBold(true)), width / 2, by + 4, Ui.GOLD);
        buttons.add(new int[]{bx, by, bw, 16});
        actions.add(this::send);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void button(GuiGraphics g, int x, int y, String text, int mouseX, int mouseY, Runnable action) {
        int w = font.width(text) + 8, h = 12;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hover ? 0xC05A4418 : 0xA0241A08);
        g.renderOutline(x, y, w, h, hover ? Ui.GOLD : 0x80B89A50);
        g.drawString(font, text, x + 4, y + 2, hover ? Ui.GOLD : Ui.TEXT, false);
        buttons.add(new int[]{x, y, w, h});
        actions.add(action);
    }

    private void send() {
        if (setup.towns().isEmpty()) return;
        SonPayloads.Town t = setup.towns().get(Math.floorMod(town, setup.towns().size()));
        Optional<java.util.UUID> m = mother >= 0 && mother < t.mothers().size() ? Optional.of(t.mothers().get(mother).id()) : Optional.empty();
        PacketDistributor.sendToServer(new SonPayloads.Choose(name.getValue(), variant, t.centre(), m));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < buttons.size(); i++) {
            int[] b = buttons.get(i);
            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                actions.get(i).run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
