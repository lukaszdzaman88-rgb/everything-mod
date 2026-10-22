package net.hehex.everythingmod.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.hehex.everythingmod.grindstone.GrindstoneSelectPayload;
import net.hehex.everythingmod.grindstone.GrindstoneSelection;
import net.hehex.everythingmod.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Kompaktowy panel wyboru enchantów przy Grindstone.
 *
 * Panel jest rysowany tuż obok okna Grindstone'a (po prawej, a gdy brak miejsca - po lewej).
 * Nie powiększamy imageWidth, bo vanillowa tekstura ma 256 px i przy szerszym GUI
 * vanilla rysowałaby ją poza krawędzią. Wszystko jest rysowane w kodzie (bez własnych
 * tekstur), a kliknięcia obsługujemy sami.
 */
public class GrindstoneSelectClient implements ClientModInitializer {

    /* ---- wymiary (piksele GUI) ---- */
    private static final int VANILLA_GUI_WIDTH = 176;
    private static final int GAP = 2;

    private static final int PAD = 4;
    private static final int HEADER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 12;
    private static final int FOOTER_HEIGHT = 11;

    private static final int MIN_WIDTH = 96;
    private static final int MAX_WIDTH = 176;

    private static final int CHECKBOX = 8;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_PAD = 4;

    /* ---- kolory (ARGB), paleta vanillowego GUI ---- */
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;

    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    private static final int HOVER_TINT = 0x33000000;
    private static final int SELECTED_TINT = 0x4455AA55;

    private static final int BUTTON_NORMAL = 0xFF8B8B8B;
    private static final int BUTTON_HOVER = 0xFFA0A0A0;
    private static final int BUTTON_ACTIVE = 0xFF86BC86;
    private static final int BUTTON_ACTIVE_HOVER = 0xFF9DD09D;
    private static final int BUTTON_DISABLED = 0xFFA8A8A8;

    private static final int TEXT_NORMAL = 0xFF303030;
    private static final int TEXT_SELECTED = 0xFF1B5E1B;
    private static final int TEXT_CURSE = 0xFF9A2A2A;
    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_HINT = 0xFF7A4A4A;
    private static final int CHECK_COLOR = 0xFF1E8E1E;

    /** Klik poza wierszami ("Select all" ma własny kod). */
    private static final int NO_HIT = -1;
    private static final int HIT_SELECT_ALL = -2;

    @Override
    public void onInitializeClient() {

        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof GrindstoneScreen)) {
                        return;
                    }

                    /*
                     * Fabric tworzy zdarzenia ekranu OD NOWA przy każdym init/resize,
                     * więc rejestrujemy je przy każdym AFTER_INIT (bez strażnika
                     * "tylko raz" - inaczej po zmianie rozmiaru okna panel by zniknął).
                     *
                     * afterBackground: nad tłem okna, ale pod tekstem, itemami
                     * i tooltipami, więc tooltip itemu poprawnie przykrywa panel.
                     */
                    ScreenEvents.afterBackground(screen).register(
                            (currentScreen, graphics, mouseX, mouseY, tickProgress) ->
                                    drawPanel(client, currentScreen, graphics, mouseX, mouseY)
                    );

                    /*
                     * Kliknięcia w obszar panelu obsługujemy sami i "połykamy"
                     * (return false). Inaczej vanilla uznałaby je za kliknięcie
                     * poza oknem i wyrzuciłaby trzymany przedmiot.
                     */
                    ScreenMouseEvents.allowMouseClick(screen).register(
                            (currentScreen, event) ->
                                    !handleClick(client, currentScreen, event.x(), event.y(), event.button())
                    );
                }
        );
    }

    // ------------------------------------------------------------------ geometria

    private record Layout(
            int x, int y, int w, int h,
            int buttonX, int buttonY, int buttonW,
            List<String> names,
            boolean hasCurse
    ) {

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

        boolean overButton(double mx, double my) {
            return inside(mx, my, buttonX, buttonY, buttonW, BUTTON_HEIGHT);
        }

        /** Indeks wiersza pod kursorem albo NO_HIT. */
        int rowAt(double mx, double my, int rows) {
            for (int i = 0; i < rows; i++) {
                if (inside(mx, my, rowX(), rowY(i), rowWidth(), ROW_HEIGHT)) {
                    return i;
                }
            }
            return NO_HIT;
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Menu Grindstone'a gracza albo null. */
    private static GrindstoneMenu menuOf(Minecraft client) {
        return client.player != null
                && client.player.containerMenu instanceof GrindstoneMenu menu
                ? menu
                : null;
    }

    private static GrindstoneSelection selectionOf(GrindstoneMenu menu) {
        return (GrindstoneSelection) (Object) menu;
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static Layout layout(
            Minecraft client,
            Screen screen,
            List<GrindstoneEnchants.Entry> entries
    ) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;

        int guiLeft = accessor.everything$getLeftPos();
        int guiTop = accessor.everything$getTopPos();

        List<String> names = new ArrayList<>();
        int namesWidth = 0;
        boolean hasCurse = false;

        for (GrindstoneEnchants.Entry entry : entries) {
            String name = Enchantment.getFullname(entry.holder(), entry.level()).getString();
            names.add(name);
            namesWidth = Math.max(namesWidth, client.font.width(name));

            if (!entry.removable()) {
                hasCurse = true;
            }
        }

        int buttonW = Math.max(
                client.font.width(text("gui.everything_mod.grindstone.select_all")),
                client.font.width(text("gui.everything_mod.grindstone.deselect_all"))
        ) + 2 * BUTTON_PAD;

        int headerWidth = client.font.width(text("gui.everything_mod.grindstone.title")) + 8 + buttonW;
        int rowsWidth = CHECKBOX + 4 + namesWidth;
        int hintWidth = hasCurse ? client.font.width(text("gui.everything_mod.grindstone.curse_hint")) : 0;

        int contentWidth = Math.max(Math.max(headerWidth, rowsWidth), hintWidth);
        int w = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, contentWidth + 2 * PAD));
        int h = HEADER_HEIGHT + entries.size() * ROW_HEIGHT + PAD + (hasCurse ? FOOTER_HEIGHT : 0);

        int x = guiLeft + VANILLA_GUI_WIDTH + GAP;

        // Brak miejsca po prawej => po lewej stronie okna.
        if (x + w > screen.width && guiLeft - GAP - w >= 0) {
            x = guiLeft - GAP - w;
        }

        return new Layout(
                x, guiTop, w, h,
                x + w - PAD - buttonW, guiTop + 3, buttonW,
                names, hasCurse
        );
    }

    // ------------------------------------------------------------------ kliknięcia

    /** @return true, jeśli kliknięcie wypadło w panel (i ma zostać pochłonięte). */
    private static boolean handleClick(
            Minecraft client,
            Screen screen,
            double mx,
            double my,
            int mouseButton
    ) {
        GrindstoneMenu menu = menuOf(client);

        if (menu == null) {
            return false;
        }

        GrindstoneSelection selection = selectionOf(menu);
        List<GrindstoneEnchants.Entry> entries = selection.everything$getEntries();

        if (entries.isEmpty()) {
            return false;
        }

        Layout layout = layout(client, screen, entries);

        if (!layout.contains(mx, my)) {
            return false;
        }

        // Reagujemy tylko na lewy przycisk myszy (0), reszta jest po prostu pochłaniana.
        if (mouseButton != 0) {
            return true;
        }

        int removable = GrindstoneEnchants.removableMask(entries);
        int mask = selection.everything$getSelectionMask() & removable;

        if (layout.overButton(mx, my)) {
            if (removable != 0) {
                // Wszystko zaznaczone => odznacz; w przeciwnym razie zaznacz wszystko.
                apply(client, selection, mask == removable ? 0 : removable);
            }
            return true;
        }

        int row = layout.rowAt(mx, my, entries.size());

        if (row != NO_HIT && entries.get(row).removable()) {
            apply(client, selection, mask ^ (1 << row));
        }

        return true;
    }

    /** Ustawia zaznaczenie lokalnie i wysyła je do serwera. */
    private static void apply(Minecraft client, GrindstoneSelection selection, int newMask) {
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F)
        );

        selection.everything$setSelectionMask(newMask);

        int mask = selection.everything$getSelectionMask();
        int signature = selection.everything$getSignatureHash();

        GrindstoneEnchants.debug("[Grindstone][klient] wysyłam maska={} podpis={}", mask, signature);

        ClientPlayNetworking.send(new GrindstoneSelectPayload(mask, signature));
    }

    // ------------------------------------------------------------------ rysowanie

    private static void drawPanel(
            Minecraft client,
            Screen screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        GrindstoneMenu menu = menuOf(client);

        if (menu == null) {
            return;
        }

        GrindstoneSelection selection = selectionOf(menu);
        List<GrindstoneEnchants.Entry> entries = selection.everything$getEntries();

        if (entries.isEmpty()) {
            return;
        }

        int removable = GrindstoneEnchants.removableMask(entries);
        int mask = selection.everything$getSelectionMask() & removable;

        Layout layout = layout(client, screen, entries);

        // Tło panelu (obrys + fazowanie jak w vanillowym kontenerze).
        graphics.fill(layout.x() - 1, layout.y() - 1,
                layout.x() + layout.w() + 1, layout.y() + layout.h() + 1, OUTLINE);
        bevel(graphics, layout.x(), layout.y(), layout.w(), layout.h(),
                PANEL_FILL, PANEL_LIGHT, PANEL_DARK);

        // Tytuł po lewej.
        graphics.text(
                client.font,
                text("gui.everything_mod.grindstone.title"),
                layout.x() + PAD,
                layout.y() + 5,
                TEXT_TITLE,
                false
        );

        // "Select all" w prawym górnym rogu panelu.
        boolean allSelected = removable != 0 && mask == removable;
        boolean buttonHover = removable != 0 && layout.overButton(mouseX, mouseY);

        bevel(
                graphics,
                layout.buttonX(), layout.buttonY(), layout.buttonW(), BUTTON_HEIGHT,
                removable == 0 ? BUTTON_DISABLED
                        : allSelected ? (buttonHover ? BUTTON_ACTIVE_HOVER : BUTTON_ACTIVE)
                        : (buttonHover ? BUTTON_HOVER : BUTTON_NORMAL),
                SLOT_LIGHT, SLOT_DARK
        );

        String buttonLabel = text(
                allSelected
                        ? "gui.everything_mod.grindstone.deselect_all"
                        : "gui.everything_mod.grindstone.select_all"
        );

        graphics.text(
                client.font,
                buttonLabel,
                layout.buttonX() + (layout.buttonW() - client.font.width(buttonLabel)) / 2,
                layout.buttonY() + 2,
                removable == 0 ? TEXT_CURSE : TEXT_NORMAL,
                false
        );

        // Wiersze z enchantami.
        for (int i = 0; i < entries.size(); i++) {
            GrindstoneEnchants.Entry entry = entries.get(i);

            int rowX = layout.rowX();
            int rowY = layout.rowY(i);
            int rowW = layout.rowWidth();

            boolean selected = (mask & (1 << i)) != 0;
            boolean hover = entry.removable() && inside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT);

            if (selected) {
                graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, SELECTED_TINT);
            }

            if (hover) {
                graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, HOVER_TINT);
            }

            drawCheckbox(graphics, rowX + 1, rowY + (ROW_HEIGHT - CHECKBOX) / 2, selected, entry.removable());

            graphics.text(
                    client.font,
                    fit(client, layout.names().get(i), rowW - CHECKBOX - 5),
                    rowX + CHECKBOX + 4,
                    rowY + (ROW_HEIGHT - 8) / 2 + 1,
                    !entry.removable() ? TEXT_CURSE : selected ? TEXT_SELECTED : TEXT_NORMAL,
                    false
            );
        }

        // Podpowiedź o klątwach.
        if (layout.hasCurse()) {
            graphics.text(
                    client.font,
                    fit(client, text("gui.everything_mod.grindstone.curse_hint"), layout.w() - 2 * PAD),
                    layout.x() + PAD,
                    layout.y() + layout.h() - FOOTER_HEIGHT + 1,
                    TEXT_HINT,
                    false
            );
        }
    }

    /** Przycina tekst, żeby mieścił się w podanej szerokości (dodaje "..."). */
    private static String fit(Minecraft client, String text, int maxWidth) {
        if (client.font.width(text) <= maxWidth) {
            return text;
        }

        String cut = text;

        while (cut.length() > 1 && client.font.width(cut + "...") > maxWidth) {
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

    /** Checkbox 8x8 jak "wgłębiony" slot, ptaszek jako pixel-art. */
    private static void drawCheckbox(
            GuiGraphicsExtractor graphics,
            int x, int y,
            boolean checked,
            boolean enabled
    ) {
        bevel(graphics, x, y, CHECKBOX, CHECKBOX,
                enabled ? SLOT_FILL : 0xFFA0A0A0, SLOT_DARK, SLOT_LIGHT);

        if (!checked) {
            return;
        }

        // Wnętrze ma 6x6 px (od x+1, y+1).
        int[][] pixels = {
                {0, 3}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0},
                {0, 4}, {1, 5}, {2, 4}, {3, 3}, {4, 2}, {5, 1}
        };

        for (int[] p : pixels) {
            graphics.fill(x + 1 + p[0], y + 1 + p[1], x + 2 + p[0], y + 2 + p[1], CHECK_COLOR);
        }
    }
}
