package net.hehex.everythingmod.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.hehex.everythingmod.defusion.DefusionSelectPayload;
import net.hehex.everythingmod.defusion.DefusionTableMenu;
import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Ekran Defusion Table. Tło (panel, sloty, strzałka) jest rysowane w kodzie w stylu
 * vanillowego GUI, więc nie potrzeba własnej tekstury. Obok okna jest panel z listą
 * zaklęć - można wybrać dokładnie jedno (klątwy też).
 */
public class DefusionTableScreen extends AbstractContainerScreen<DefusionTableMenu> {

    /* ---- wymiary panelu z listą (piksele GUI) ---- */
    private static final int GAP = 2;
    private static final int PAD = 4;
    private static final int HEADER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 12;
    private static final int FOOTER_HEIGHT = 11;
    private static final int MIN_WIDTH = 96;
    private static final int MAX_WIDTH = 176;
    private static final int CHECKBOX = 8;

    /* ---- kolory (ARGB), paleta vanillowego GUI ---- */
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;

    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    private static final int HOVER_TINT = 0x66FFFFFF;
    private static final int SELECTED_TINT = 0x8050E050;

    private static final int TEXT_NORMAL = 0xFF303030;
    private static final int TEXT_SELECTED = 0xFF083B08;
    private static final int TEXT_CURSE = 0xFF9A2A2A;
    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_HINT = 0xFF7A4A4A;
    private static final int CHECK_FILL = 0xFF6FE06F;
    private static final int CHECK_COLOR = 0xFF083B08;

    public DefusionTableScreen(DefusionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // ------------------------------------------------------------------ tło okna

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        drawContainer(graphics);
        drawPanel(graphics, mouseX, mouseY);
    }

    private void drawContainer(GuiGraphicsExtractor graphics) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel okna (obrys + fazowanie).
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, OUTLINE);
        bevel(graphics, x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2,
                PANEL_FILL, PANEL_LIGHT, PANEL_DARK);

        // Tła wszystkich slotów (wgłębione kwadraty jak w vanilla).
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;

            if (slot.index == DefusionTableMenu.RESULT_SLOT) {
                bevel(graphics, sx - 5, sy - 5, 26, 26, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);
            } else {
                bevel(graphics, sx - 1, sy - 1, 18, 18, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);
            }
        }

        // "+" między przedmiotem a książką.
        graphics.fill(x + 51, y + 42, x + 58, y + 44, PANEL_DARK);
        graphics.fill(x + 54, y + 39, x + 56, y + 47, PANEL_DARK);

        // Strzałka w stronę wyniku.
        graphics.fill(x + 84, y + 42, x + 102, y + 44, SLOT_FILL);
        for (int i = 0; i < 6; i++) {
            graphics.fill(x + 102 + i, y + 43 - (5 - i), x + 103 + i, y + 44 + (5 - i), SLOT_FILL);
        }
    }

    // ------------------------------------------------------------------ panel z listą

    private record Layout(int x, int y, int w, int h, List<String> names, String footer) {

        int rowX() {
            return x + PAD;
        }

        int rowWidth() {
            return w - 2 * PAD;
        }

        int rowY(int index) {
            return y + HEADER_HEIGHT + index * ROW_HEIGHT;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        /** Indeks wiersza pod kursorem albo -1. */
        int rowAt(double mx, double my, int rows) {
            for (int i = 0; i < rows; i++) {
                if (inside(mx, my, rowX(), rowY(i), rowWidth(), ROW_HEIGHT)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    /** Układ panelu albo null, gdy nie ma czego wybierać. */
    private Layout layout(List<GrindstoneEnchants.Entry> entries) {
        if (entries.isEmpty()) {
            return null;
        }

        List<String> names = new ArrayList<>();
        int namesWidth = 0;

        for (GrindstoneEnchants.Entry entry : entries) {
            String name = Enchantment.getFullname(entry.holder(), entry.level()).getString();
            names.add(name);
            namesWidth = Math.max(namesWidth, this.font.width(name));
        }

        String footer = !this.menu.hasBook()
                ? text("gui.everything_mod.defusion.need_book")
                : this.menu.getSelection() < 0
                ? text("gui.everything_mod.defusion.pick_one")
                : "";

        int headerWidth = this.font.width(text("gui.everything_mod.defusion.title"));
        int rowsWidth = CHECKBOX + 4 + namesWidth;
        int footerWidth = this.font.width(footer);

        int contentWidth = Math.max(Math.max(headerWidth, rowsWidth), footerWidth);
        int w = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, contentWidth + 2 * PAD));
        int h = HEADER_HEIGHT + entries.size() * ROW_HEIGHT + PAD + (footer.isEmpty() ? 0 : FOOTER_HEIGHT);

        int x = this.leftPos + this.imageWidth + GAP;

        // Brak miejsca po prawej => po lewej stronie okna.
        if (x + w > this.width && this.leftPos - GAP - w >= 0) {
            x = this.leftPos - GAP - w;
        }

        return new Layout(x, this.topPos, w, h, names, footer);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<GrindstoneEnchants.Entry> entries = this.menu.getEntries();
        Layout layout = this.layout(entries);

        if (layout == null) {
            return;
        }

        int selection = this.menu.getSelection();

        graphics.fill(layout.x() - 1, layout.y() - 1,
                layout.x() + layout.w() + 1, layout.y() + layout.h() + 1, OUTLINE);
        bevel(graphics, layout.x(), layout.y(), layout.w(), layout.h(),
                PANEL_FILL, PANEL_LIGHT, PANEL_DARK);

        graphics.text(
                this.font,
                text("gui.everything_mod.defusion.title"),
                layout.x() + PAD,
                layout.y() + 5,
                TEXT_TITLE,
                false
        );

        for (int i = 0; i < entries.size(); i++) {
            GrindstoneEnchants.Entry entry = entries.get(i);

            int rowX = layout.rowX();
            int rowY = layout.rowY(i);
            int rowW = layout.rowWidth();

            boolean selected = i == selection;
            boolean hover = inside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT);

            if (selected) {
                graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, SELECTED_TINT);
            }

            if (hover) {
                graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, HOVER_TINT);
            }

            drawCheckbox(graphics, rowX + 1, rowY + (ROW_HEIGHT - CHECKBOX) / 2, selected);

            // Klątwy są wybieralne (tu można je przenieść na książkę), ale podświetlone na czerwono.
            graphics.text(
                    this.font,
                    fit(layout.names().get(i), rowW - CHECKBOX - 5),
                    rowX + CHECKBOX + 4,
                    rowY + (ROW_HEIGHT - 8) / 2 + 1,
                    selected ? TEXT_SELECTED : entry.curse() ? TEXT_CURSE : TEXT_NORMAL,
                    false
            );
        }

        if (!layout.footer().isEmpty()) {
            graphics.text(
                    this.font,
                    fit(layout.footer(), layout.w() - 2 * PAD),
                    layout.x() + PAD,
                    layout.y() + layout.h() - FOOTER_HEIGHT + 1,
                    TEXT_HINT,
                    false
            );
        }
    }

    // ------------------------------------------------------------------ kliknięcia

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.handlePanelClick(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    /** @return true, jeśli kliknięcie wypadło w panel (i ma zostać pochłonięte). */
    private boolean handlePanelClick(double mx, double my, int mouseButton) {
        List<GrindstoneEnchants.Entry> entries = this.menu.getEntries();
        Layout layout = this.layout(entries);

        if (layout == null || !layout.contains(mx, my)) {
            return false;
        }

        // Reagujemy tylko na lewy przycisk myszy (0), reszta jest po prostu pochłaniana.
        if (mouseButton != 0) {
            return true;
        }

        int row = layout.rowAt(mx, my, entries.size());

        if (row >= 0) {
            // Ponowny klik w wybrane zaklęcie odznacza je.
            this.apply(row == this.menu.getSelection() ? -1 : row);
        }

        return true;
    }

    /** Ustawia wybór lokalnie i wysyła go do serwera. */
    private void apply(int newSelection) {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F)
        );

        this.menu.setSelectionLocal(newSelection);

        int index = this.menu.getSelection();
        int signature = this.menu.getSignatureHash();

        GrindstoneEnchants.debug("[Defusion][klient] wysyłam indeks={} podpis={}", index, signature);

        ClientPlayNetworking.send(new DefusionSelectPayload(index, signature));
    }

    // ------------------------------------------------------------------ rysowanie pomocnicze

    /** Przycina tekst, żeby mieścił się w podanej szerokości (dodaje "..."). */
    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String cut = text;

        while (cut.length() > 1 && this.font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }

        return cut + "...";
    }

    /** Płaski prostokąt z fazowaniem (kolor "light" góra/lewo, "dark" dół/prawo). */
    private static void bevel(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            int fill, int light, int dark
    ) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w - 1, y + 1, light);
        graphics.fill(x, y, x + 1, y + h - 1, light);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, dark);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, dark);
    }

    /** Checkbox 8x8 jak "wgłębiony" slot, ptaszek jako pixel-art (jasne wypełnienie, ciemny ptaszek). */
    private static void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        bevel(graphics, x, y, CHECKBOX, CHECKBOX, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);

        if (!checked) {
            return;
        }

        graphics.fill(x + 1, y + 1, x + CHECKBOX - 1, y + CHECKBOX - 1, CHECK_FILL);

        int[][] pixels = {
                {0, 3}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0},
                {0, 4}, {1, 5}, {2, 4}, {3, 3}, {4, 2}, {5, 1}
        };

        for (int[] p : pixels) {
            graphics.fill(x + 1 + p[0], y + 1 + p[1], x + 2 + p[0], y + 2 + p[1], CHECK_COLOR);
        }
    }
}
