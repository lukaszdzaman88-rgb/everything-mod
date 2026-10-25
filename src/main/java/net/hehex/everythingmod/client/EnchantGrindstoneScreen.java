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
 * Ekran Grindstone'a (okno 176 px jak w vanilla, margines 7 px):
 *  - po lewej ramka jak w vanillowym Grindstone'ie ze slotem przedmiotu, strzałką w dół i slotem wyniku,
 *  - po prawej lista enchantów jako przyciski w rozmiarze ze stołu do enchantowania (108 x 19),
 *    zaznaczony przycisk jest "wciśnięty" (sprite wyłączonego slotu stołu),
 *  - suwak z trybu kreatywnego,
 *  - "Select all" w linii tytułu.
 * Wygląd rysujemy w kodzie i z vanillowych sprite'ów, więc nie potrzeba własnej tekstury.
 */
public class EnchantGrindstoneScreen extends AbstractContainerScreen<EnchantGrindstoneMenu> {

    /* ---- układ okna (piksele GUI, względem lewego górnego rogu okna) ---- */
    private static final int WINDOW_HEIGHT = 172;

    // Ramka vanillowego Grindstone'a: 34 px szerokości, sloty oddalone o 6 px od bocznych krawędzi.
    private static final int COLUMN_X = 7;
    private static final int COLUMN_Y = 18;
    private static final int COLUMN_W = 34;
    private static final int COLUMN_H = 57;

    // Strzałka w dół między slotami (11 px wysokości, wycentrowana w wolnym miejscu 39..53).
    private static final int ARROW_SHAFT_X = COLUMN_X + 15;
    private static final int ARROW_SHAFT_W = 4;
    private static final int ARROW_SHAFT_Y = 41;
    private static final int ARROW_SHAFT_H = 5;
    private static final int ARROW_HEAD_X = COLUMN_X + 11;
    private static final int ARROW_HEAD_Y = 46;
    private static final int ARROW_HEAD_W = 12;
    private static final int ARROW_HEAD_ROWS = 6;

    // Przyciski enchantów: dokładnie jak w stole do enchantowania (108 x 19).
    private static final int ROW_X = 45;
    private static final int ROW_Y = 18;
    private static final int ROW_W = 108;
    private static final int ROW_H = 19;
    private static final int ROW_STEP = 19;
    private static final int VISIBLE_ROWS = 3;
    private static final int ROW_TEXT_PAD = 6;

    // Suwak (sprite 12 x 15 z trybu kreatywnego); prawa krawędź = prawy margines 7 px.
    private static final int TRACK_X = 157;
    private static final int TRACK_Y = 18;
    private static final int TRACK_W = 12;
    private static final int TRACK_H = 57;
    private static final int HANDLE_W = 12;
    private static final int HANDLE_H = 15;

    // "Select all": linia tytułu, wyrównany do prawej krawędzi suwaka.
    private static final int BUTTON_RIGHT = TRACK_X + TRACK_W;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_H = 13;
    private static final int BUTTON_PAD = 6;

    private static final int INVENTORY_LABEL_Y = 78;

    /* ---- kolory (ARGB), paleta vanillowego GUI ---- */
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;

    private static final int RIM_DARK = 0xFF373737;
    private static final int RIM_INNER = 0xFF8B8B8B;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    // Dokładnie jak podświetlenie slotu w vanilla (biel, 50% krycia); klątwa - czerwień.
    private static final int HOVER_TINT = 0x80FFFFFF;
    private static final int HOVER_PRESSED_TINT = 0x40FFFFFF;
    private static final int HOVER_CURSE_TINT = 0x80FF4040;

    private static final int TEXT_ROW = 0xFF2A2214;
    private static final int TEXT_ROW_PRESSED = 0xFFF2EAD0;
    private static final int TEXT_CURSE = 0xFF7A1010;
    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_HINT = 0xFF6A6A6A;
    private static final int TEXT_BUTTON = 0xFFFFFFFF;
    private static final int TEXT_BUTTON_DISABLED = 0xFFA0A0A0;

    /* ---- sprite'y z vanilla ---- */
    private static final Identifier ROW_SPRITE =
            Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot");
    private static final Identifier ROW_SPRITE_PRESSED =
            Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_disabled");

    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_SPRITE_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier BUTTON_SPRITE_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

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

        // Okno jest wyższe niż w vanilla (lista przycisków 19 px); pole ustawiamy przez accessor (@Mutable).
        ((AbstractContainerScreenAccessor) this).everything$setImageHeight(WINDOW_HEIGHT);
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

    /** Etykiety rysujemy sami w tle (inne rozmieszczenie niż w vanilla), więc vanillowe wyłączamy. */
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

        // Etykiety (pozycje jak w vanilla: tytuł x = 8, ekwipunek x = 8).
        graphics.text(this.font, this.title.getString(), x + 8, y + 6, TEXT_TITLE, false);
        graphics.text(
                this.font,
                Component.translatable("container.inventory").getString(),
                x + EnchantGrindstoneMenu.INVENTORY_X, y + INVENTORY_LABEL_Y,
                TEXT_TITLE, false
        );

        this.drawColumn(graphics, x, y);
        this.drawSelectAllButton(graphics, mouseX, mouseY, removable, mask);

        if (entries.isEmpty()) {
            String hint = Component.translatable("gui.everything_mod.grindstone.hint_empty").getString();

            graphics.text(
                    this.font, hint,
                    x + ROW_X + (ROW_W - this.font.width(hint)) / 2,
                    y + ROW_Y + (VISIBLE_ROWS * ROW_STEP - 8) / 2,
                    TEXT_HINT, false
            );
        } else {
            this.drawRows(graphics, mouseX, mouseY, entries, mask);
        }

        this.drawScrollbar(graphics, entries.size());
    }

    /** Ramka jak w vanillowym Grindstone'ie + tła slotów + strzałka w dół. */
    private void drawColumn(GuiGraphicsExtractor graphics, int x, int y) {
        drawRim(graphics, x + COLUMN_X, y + COLUMN_Y, COLUMN_W, COLUMN_H);

        // Tła wszystkich slotów (wgłębione kwadraty 18 x 18 jak w vanilla).
        for (Slot slot : this.menu.slots) {
            bevel(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_FILL, SLOT_DARK, SLOT_LIGHT);
        }

        // Strzałka w dół: trzon + grot (zwężający się co wiersz o 1 px z każdej strony).
        graphics.fill(
                x + ARROW_SHAFT_X, y + ARROW_SHAFT_Y,
                x + ARROW_SHAFT_X + ARROW_SHAFT_W, y + ARROW_SHAFT_Y + ARROW_SHAFT_H,
                SLOT_FILL
        );

        for (int i = 0; i < ARROW_HEAD_ROWS; i++) {
            graphics.fill(
                    x + ARROW_HEAD_X + i, y + ARROW_HEAD_Y + i,
                    x + ARROW_HEAD_X + ARROW_HEAD_W - i, y + ARROW_HEAD_Y + i + 1,
                    SLOT_FILL
            );
        }
    }

    private int buttonWidth() {
        return Math.max(
                this.font.width(Component.translatable("gui.everything_mod.grindstone.select_all").getString()),
                this.font.width(Component.translatable("gui.everything_mod.grindstone.deselect_all").getString())
        ) + 2 * BUTTON_PAD;
    }

    /** "Select all": w linii tytułu, wyrównany do prawej krawędzi listy (suwaka), vanillowy przycisk. */
    private void drawSelectAllButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int removable, int mask) {
        int bw = this.buttonWidth();
        int bx = this.leftPos + BUTTON_RIGHT - bw;
        int by = this.topPos + BUTTON_Y;

        boolean enabled = removable != 0;
        boolean hover = enabled && inside(mouseX, mouseY, bx, by, bw, BUTTON_H);

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                !enabled ? BUTTON_SPRITE_DISABLED : hover ? BUTTON_SPRITE_HOVER : BUTTON_SPRITE,
                bx, by, bw, BUTTON_H
        );

        // Wszystko zaznaczone => przycisk odznacza.
        String label = Component.translatable(
                enabled && mask == removable
                        ? "gui.everything_mod.grindstone.deselect_all"
                        : "gui.everything_mod.grindstone.select_all"
        ).getString();

        graphics.text(
                this.font, label,
                bx + (bw - this.font.width(label)) / 2, by + (BUTTON_H - 8) / 2,
                enabled ? TEXT_BUTTON : TEXT_BUTTON_DISABLED, enabled
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

            boolean pressed = (mask & (1 << index)) != 0;
            boolean hover = inside(mouseX, mouseY, rx, ry, ROW_W, ROW_H) && !this.scrolling;

            // Przycisk ze stołu do enchantowania; zaznaczony = sprite "wciśniętego" (ciemnego) slotu.
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, pressed ? ROW_SPRITE_PRESSED : ROW_SPRITE,
                    rx, ry, ROW_W, ROW_H);

            // Podświetlenie po najechaniu jak na slocie (pod napisem, żeby tekst nie blakł);
            // klątwa świeci na czerwono i nie reaguje na klik.
            if (hover) {
                graphics.fill(rx + 1, ry + 1, rx + ROW_W - 1, ry + ROW_H - 1,
                        !entry.removable() ? HOVER_CURSE_TINT : pressed ? HOVER_PRESSED_TINT : HOVER_TINT);
            }

            Component fullName = Enchantment.getFullname(entry.holder(), entry.level());
            String name = fullName.getString();
            String shown = this.fit(name, ROW_W - 2 * ROW_TEXT_PAD);

            // Wciśnięty przycisk: napis lekko przesunięty w dół i w prawo.
            int offset = pressed ? 1 : 0;

            graphics.text(
                    this.font, shown,
                    rx + ROW_TEXT_PAD + offset, ry + (ROW_H - 8) / 2 + offset,
                    !entry.removable() ? TEXT_CURSE : pressed ? TEXT_ROW_PRESSED : TEXT_ROW,
                    pressed
            );

            // Za długa nazwa została skrócona => pełna nazwa w podpowiedzi.
            if (hover && !shown.equals(name)) {
                graphics.setTooltipForNextFrame(this.font, fullName, mouseX, mouseY);
            }
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

        if (max > 0 && this.insideList(mouseX, mouseY)) {
            this.scrollOffs = Math.max(0.0F, Math.min(1.0F, this.scrollOffs - (float) (scrollY / max)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Obszar listy: przyciski + suwak. */
    private boolean insideList(double mx, double my) {
        return inside(
                mx, my,
                this.leftPos + ROW_X, this.topPos + ROW_Y,
                TRACK_X + TRACK_W - ROW_X, VISIBLE_ROWS * ROW_STEP
        );
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

        if (!this.insideList(mx, my)) {
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

        // Przyciski.
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

    /**
     * Ramka dokładnie jak w vanillowym Grindstone'ie: ciemny obrys dookoła, a w środku drugi,
     * 1-pikselowy pasek (szary na górze i po lewej, biały na dole i po prawej).
     */
    private static void drawRim(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, RIM_DARK);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, RIM_INNER);
        graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, WHITE);
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, WHITE);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL_FILL);
    }
}
