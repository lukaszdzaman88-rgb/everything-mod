package net.hehex.everythingmod.client;

import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.hehex.everythingmod.grindstone.EnchantGrindstoneMenu;
import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.hehex.everythingmod.grindstone.GrindstoneSelectPayload;
import net.hehex.everythingmod.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Ekran Grindstone'a: po lewej slot na przedmiot, strzałka w dół i slot wyniku;
 * po prawej lista enchantów (przyciski) z suwakiem jak w trybie kreatywnym;
 * pod spodem ekwipunek. Cały wygląd jest rysowany w kodzie w stylu vanilla,
 * więc nie potrzeba własnej tekstury.
 */
public class EnchantGrindstoneScreen extends AbstractContainerScreen<EnchantGrindstoneMenu> {

    /* ---- układ okna (piksele GUI, względem lewego górnego rogu okna) ---- */
    private static final int WINDOW_WIDTH = 208;

    private static final int COLUMN_X = 8;
    private static final int COLUMN_Y = 16;
    private static final int COLUMN_W = 24;
    private static final int COLUMN_H = 56;

    private static final int LIST_X = 36;
    private static final int LIST_Y = 16;
    private static final int LIST_W = 164;
    private static final int LIST_H = 56;

    private static final int ROW_X = 38;
    private static final int ROW_Y = 18;
    private static final int ROW_W = 146;
    private static final int ROW_H = 12;
    private static final int ROW_STEP = 13;
    private static final int VISIBLE_ROWS = 4;

    private static final int TRACK_X = 186;
    private static final int TRACK_Y = 18;
    private static final int TRACK_W = 12;
    private static final int TRACK_H = 52;
    private static final int HANDLE_W = 12;
    private static final int HANDLE_H = 15;

    private static final int BUTTON_RIGHT = 200;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_H = 12;
    private static final int BUTTON_PAD = 4;

    private static final int CHECKBOX = 8;

    /* ---- kolory (ARGB), paleta vanillowego GUI ---- */
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;

    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int RECESS_FILL = 0xFFB5B5B5;

    private static final int ROW_NORMAL = 0xFFA6A6A6;
    private static final int ROW_SELECTED = 0xFF86D986;
    private static final int ROW_LIGHT = 0xFFFFFFFF;
    private static final int ROW_DARK = 0xFF555555;

    // Dokładnie jak podświetlenie slotu w vanilla (biel, 50% krycia); klątwa - czerwień.
    private static final int HOVER_TINT = 0x80FFFFFF;
    private static final int HOVER_CURSE_TINT = 0x80FF4040;

    private static final int BUTTON_NORMAL = 0xFF8B8B8B;
    private static final int BUTTON_HOVER = 0xFFA0A0A0;
    private static final int BUTTON_ACTIVE = 0xFF7FD67F;
    private static final int BUTTON_ACTIVE_HOVER = 0xFF9AEB9A;
    private static final int BUTTON_DISABLED = 0xFFA8A8A8;

    private static final int TEXT_NORMAL = 0xFF202020;
    private static final int TEXT_SELECTED = 0xFF083B08;
    private static final int TEXT_CURSE = 0xFF8A1F1F;
    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_HINT = 0xFF6A6A6A;
    private static final int CHECK_FILL = 0xFFF2FFF2;
    private static final int CHECK_COLOR = 0xFF083B08;

    private static final Identifier SCROLLER =
            Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED =
            Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    /** Przewinięcie listy 0..1 (jak w ekranie kreatywnym). */
    private float scrollOffs = 0.0F;
    private boolean scrolling = false;

    private int lastSignature = 0;

    public EnchantGrindstoneScreen(EnchantGrindstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        // Szersze okno (lista z dłuższymi nazwami). Pole ustawiamy przez accessor (@Mutable).
        ((AbstractContainerScreenAccessor) this).everything$setImageWidth(WINDOW_WIDTH);
    }

    // ------------------------------------------------------------------ przewijanie

    private static int maxScroll(int entries) {
        return Math.max(0, entries - VISIBLE_ROWS);
    }

    private int firstRow(int entries) {
        return (int) (this.scrollOffs * maxScroll(entries) + 0.5F);
    }

    /** Nowy przedmiot w slocie => lista od góry. */
    private void resetScrollOnItemChange() {
        int signature = this.menu.getSignatureHash();

        if (signature != this.lastSignature) {
            this.lastSignature = signature;
            this.scrollOffs = 0.0F;
            this.scrolling = false;
        }
    }

    private void setScrollFromMouse(double mouseY) {
        float offs = (float) ((mouseY - (this.topPos + TRACK_Y) - HANDLE_H / 2.0) / (TRACK_H - HANDLE_H));

        this.scrollOffs = Math.max(0.0F, Math.min(1.0F, offs));
    }

    // ------------------------------------------------------------------ rysowanie

    /** Etykiety rysujemy sami w tle (okno ma nietypową szerokość), więc vanillowe wyłączamy. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        this.resetScrollOnItemChange();

        int x = this.leftPos;
        int y = this.topPos;

        List<GrindstoneEnchants.Entry> entries = this.menu.getEntries();
        int removable = GrindstoneEnchants.removableMask(entries);
        int mask = this.menu.getSelectionMask() & removable;

        // Okno.
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, OUTLINE);
        bevel(graphics, x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2,
                PANEL_FILL, PANEL_LIGHT, PANEL_DARK);

        // Etykiety.
        graphics.text(this.font, this.title.getString(), x + 8, y + 6, TEXT_TITLE, false);
        graphics.text(
                this.font,
                Component.translatable("container.inventory").getString(),
                x + EnchantGrindstoneMenu.INVENTORY_X, y + 74,
                TEXT_TITLE, false
        );

        this.drawColumn(graphics, x, y);
        this.drawSelectAllButton(graphics, mouseX, mouseY, removable, mask);

        // Pojemnik listy.
        bevel(graphics, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, RECESS_FILL, SLOT_DARK, SLOT_LIGHT);

        if (entries.isEmpty()) {
            String hint = Component.translatable("gui.everything_mod.grindstone.hint_empty").getString();

            graphics.text(
                    this.font, hint,
                    x + LIST_X + (LIST_W - this.font.width(hint)) / 2,
                    y + LIST_Y + (LIST_H - 8) / 2,
                    TEXT_HINT, false
            );
            return;
        }

        this.drawRows(graphics, mouseX, mouseY, entries, mask);
        this.drawScrollbar(graphics, entries.size());
    }

    /** Lewa kolumna: slot przedmiotu, strzałka w dół, slot wyniku. */
    private void drawColumn(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x + COLUMN_X, y + COLUMN_Y, COLUMN_W, COLUMN_H, RECESS_FILL, SLOT_DARK, SLOT_LIGHT);

        // Tła wszystkich slotów (wgłębione kwadraty jak w vanilla).
        for (Slot slot : this.menu.slots) {
            bevel(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);
        }

        // Strzałka w dół (od slotu przedmiotu do slotu wyniku).
        graphics.fill(x + 17, y + 40, x + 21, y + 45, SLOT_FILL);

        for (int i = 0; i < 5; i++) {
            graphics.fill(x + 13 + i, y + 45 + i, x + 25 - i, y + 46 + i, SLOT_FILL);
        }
    }

    private int buttonWidth() {
        return Math.max(
                this.font.width(Component.translatable("gui.everything_mod.grindstone.select_all").getString()),
                this.font.width(Component.translatable("gui.everything_mod.grindstone.deselect_all").getString())
        ) + 2 * BUTTON_PAD;
    }

    /**
     * "Select all" w linii tytułu, wyrównany do prawej krawędzi listy: nad listą, której dotyczy,
     * w wolnym miejscu obok tytułu.
     */
    private void drawSelectAllButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int removable, int mask) {
        int bw = this.buttonWidth();
        int bx = this.leftPos + BUTTON_RIGHT - bw;
        int by = this.topPos + BUTTON_Y;

        boolean allSelected = removable != 0 && mask == removable;
        boolean hover = removable != 0 && inside(mouseX, mouseY, bx, by, bw, BUTTON_H);

        bevel(
                graphics, bx, by, bw, BUTTON_H,
                removable == 0 ? BUTTON_DISABLED
                        : allSelected ? (hover ? BUTTON_ACTIVE_HOVER : BUTTON_ACTIVE)
                        : (hover ? BUTTON_HOVER : BUTTON_NORMAL),
                ROW_LIGHT, ROW_DARK
        );

        String label = Component.translatable(
                allSelected
                        ? "gui.everything_mod.grindstone.deselect_all"
                        : "gui.everything_mod.grindstone.select_all"
        ).getString();

        graphics.text(
                this.font, label,
                bx + (bw - this.font.width(label)) / 2, by + 2,
                removable == 0 ? TEXT_HINT : TEXT_NORMAL, false
        );
    }

    private void drawRows(
            GuiGraphicsExtractor graphics,
            int mouseX, int mouseY,
            List<GrindstoneEnchants.Entry> entries,
            int mask
    ) {
        int first = this.firstRow(entries.size());

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = first + i;

            if (index >= entries.size()) {
                break;
            }

            GrindstoneEnchants.Entry entry = entries.get(index);

            int rx = this.leftPos + ROW_X;
            int ry = this.topPos + ROW_Y + i * ROW_STEP;

            boolean selected = (mask & (1 << index)) != 0;
            boolean hover = inside(mouseX, mouseY, rx, ry, ROW_W, ROW_H) && !this.scrolling;

            // Przycisk (wypukły).
            bevel(graphics, rx, ry, ROW_W, ROW_H, selected ? ROW_SELECTED : ROW_NORMAL, ROW_LIGHT, ROW_DARK);

            // Podświetlenie po najechaniu jak na slocie (pod napisem, żeby tekst nie blakł);
            // klątwa świeci na czerwono i nie reaguje na klik.
            if (hover) {
                graphics.fill(rx + 1, ry + 1, rx + ROW_W - 1, ry + ROW_H - 1,
                        entry.removable() ? HOVER_TINT : HOVER_CURSE_TINT);
            }

            // Checkbox i nazwa.
            drawCheckbox(graphics, rx + 2, ry + (ROW_H - CHECKBOX) / 2, selected, entry.removable());

            String name = Enchantment.getFullname(entry.holder(), entry.level()).getString();

            graphics.text(
                    this.font,
                    this.fit(name, ROW_W - CHECKBOX - 9),
                    rx + CHECKBOX + 6, ry + (ROW_H - 8) / 2 + 1,
                    !entry.removable() ? TEXT_CURSE : selected ? TEXT_SELECTED : TEXT_NORMAL,
                    false
            );
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int entryCount) {
        int tx = this.leftPos + TRACK_X;
        int ty = this.topPos + TRACK_Y;

        bevel(graphics, tx, ty, TRACK_W, TRACK_H, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);

        boolean canScroll = maxScroll(entryCount) > 0;
        int handleY = ty + (int) ((TRACK_H - HANDLE_H - 2) * (canScroll ? this.scrollOffs : 0.0F));

        // Ten sam sprite suwaka co w ekranie trybu kreatywnego.
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                canScroll ? SCROLLER : SCROLLER_DISABLED,
                tx, handleY, HANDLE_W, HANDLE_H
        );
    }

    // ------------------------------------------------------------------ mysz

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.handleClick(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.scrolling) {
            this.setScrollFromMouse(event.y());
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.scrolling) {
            this.scrolling = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll(this.menu.getEntries().size());

        if (max > 0 && inside(mouseX, mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, LIST_W, LIST_H)) {
            this.scrollOffs = Math.max(0.0F, Math.min(1.0F, this.scrollOffs - (float) (scrollY / max)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** @return true, jeśli kliknięcie wypadło w element listy / przycisk (i ma zostać pochłonięte). */
    private boolean handleClick(double mx, double my, int mouseButton) {
        List<GrindstoneEnchants.Entry> entries = this.menu.getEntries();

        int removable = GrindstoneEnchants.removableMask(entries);
        int mask = this.menu.getSelectionMask() & removable;

        // "Select all".
        int bw = this.buttonWidth();
        int bx = this.leftPos + BUTTON_RIGHT - bw;
        int by = this.topPos + BUTTON_Y;

        if (inside(mx, my, bx, by, bw, BUTTON_H)) {
            if (mouseButton == 0 && removable != 0) {
                // Wszystko zaznaczone => odznacz; w przeciwnym razie zaznacz wszystko.
                this.apply(mask == removable ? 0 : removable);
            }
            return true;
        }

        if (!inside(mx, my, this.leftPos + LIST_X, this.topPos + LIST_Y, LIST_W, LIST_H)) {
            return false;
        }

        if (mouseButton != 0 || entries.isEmpty()) {
            return true;
        }

        // Suwak.
        if (maxScroll(entries.size()) > 0
                && inside(mx, my, this.leftPos + TRACK_X, this.topPos + TRACK_Y, TRACK_W, TRACK_H)) {
            this.scrolling = true;
            this.setScrollFromMouse(my);
            return true;
        }

        // Wiersze.
        int first = this.firstRow(entries.size());

        for (int i = 0; i < VISIBLE_ROWS && first + i < entries.size(); i++) {
            if (inside(mx, my, this.leftPos + ROW_X, this.topPos + ROW_Y + i * ROW_STEP, ROW_W, ROW_H)) {
                int index = first + i;

                // Klątwa: czerwone podświetlenie, ale bez reakcji na klik.
                if (entries.get(index).removable()) {
                    this.apply(mask ^ (1 << index));
                }

                return true;
            }
        }

        return true;
    }

    /** Ustawia zaznaczenie lokalnie i wysyła je do serwera. */
    private void apply(int newMask) {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F)
        );

        this.menu.setSelectionLocal(newMask);

        int mask = this.menu.getSelectionMask();
        int signature = this.menu.getSignatureHash();

        GrindstoneEnchants.debug("[Grindstone][klient] wysyłam maska={} podpis={}", mask, signature);

        ClientPlayNetworking.send(new GrindstoneSelectPayload(mask, signature));
    }

    // ------------------------------------------------------------------ pomocnicze

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

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

    /** Prostokąt z fazowaniem (kolor "light" góra/lewo, "dark" dół/prawo). */
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

    /** Checkbox 8x8 jak "wgłębiony" slot, ptaszek jako pixel-art (jasne wnętrze, ciemny ptaszek). */
    private static void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked, boolean enabled) {
        bevel(graphics, x, y, CHECKBOX, CHECKBOX, enabled ? SLOT_FILL : 0xFF9A9A9A, SLOT_DARK, SLOT_LIGHT);

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
