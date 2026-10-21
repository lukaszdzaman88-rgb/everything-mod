package net.hehex.everythingmod.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
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
 * Panel wyboru enchantów przy Grindstone.
 *
 * Panel jest rysowany OBOK okna Grindstone'a (po prawej, a gdy brak miejsca - po lewej).
 * Nie powiększamy imageWidth, bo vanillowa tekstura ma 256 px i przy szerszym GUI
 * vanilla rysowałaby ją poza krawędzią. Zamiast widgetów rysujemy wszystko ręcznie
 * (bez własnych tekstur) i sami obsługujemy kliknięcia.
 */
public class GrindstoneSelectClient implements ClientModInitializer {

    /* ---- wymiary (piksele GUI) ---- */
    private static final int VANILLA_GUI_WIDTH = 176;
    private static final int GAP = 4;

    private static final int PANEL_WIDTH = 150;
    private static final int PADDING = 4;
    private static final int HEADER_HEIGHT = 22;
    private static final int ROW_STEP = 14;
    private static final int ROW_HEIGHT = 13;
    private static final int FOOTER_HEIGHT = 12;

    private static final int SELECT_BUTTON_WIDTH = 70;
    private static final int SELECT_BUTTON_HEIGHT = 14;

    /* ---- kolory (ARGB) w stylu vanillowego GUI ---- */
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;

    private static final int WIDGET_DARK = 0xFF373737;
    private static final int WIDGET_LIGHT = 0xFFFFFFFF;

    private static final int ROW_NORMAL = 0xFF8B8B8B;
    private static final int ROW_HOVER = 0xFFA0A0A0;
    private static final int ROW_SELECTED = 0xFF8FC68F;
    private static final int ROW_SELECTED_HOVER = 0xFFA6DBA6;
    private static final int ROW_DISABLED = 0xFF9A9A9A;

    private static final int TEXT_NORMAL = 0xFF1A1A1A;
    private static final int TEXT_DISABLED = 0xFF8A1F1F;
    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_HINT = 0xFF7F3F3F;
    private static final int CHECK_COLOR = 0xFF1E8E1E;

    private static final Set<Screen> REGISTERED_SCREENS =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void onInitializeClient() {

        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof GrindstoneScreen)) {
                        return;
                    }

                    // init() wywołuje się ponownie np. przy zmianie rozmiaru okna.
                    if (!REGISTERED_SCREENS.add(screen)) {
                        return;
                    }

                    /*
                     * afterBackground: nad tłem okna, ale pod tekstem, itemami
                     * i tooltipami (tak jak vanillowe tło), więc tooltip itemu
                     * poprawnie przykrywa nasz panel.
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

    private record Panel(int x, int y, int rows, boolean hasCurse) {

        int width() {
            return PANEL_WIDTH;
        }

        int height() {
            return HEADER_HEIGHT + rows * ROW_STEP + PADDING
                    + (hasCurse ? FOOTER_HEIGHT : 0);
        }

        int rowX() {
            return x + PADDING;
        }

        int rowWidth() {
            return PANEL_WIDTH - 2 * PADDING;
        }

        int rowY(int index) {
            return y + HEADER_HEIGHT + index * ROW_STEP;
        }

        int buttonX() {
            return x + PANEL_WIDTH - PADDING - SELECT_BUTTON_WIDTH;
        }

        int buttonY() {
            return y + PADDING;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width() && my >= y && my < y + height();
        }
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

    private static Panel layoutFor(Screen screen, List<GrindstoneEnchants.Entry> entries) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;

        int guiLeft = accessor.everything$getLeftPos();
        int guiTop = accessor.everything$getTopPos();

        int x = guiLeft + VANILLA_GUI_WIDTH + GAP;

        // Brak miejsca po prawej => po lewej stronie okna.
        if (x + PANEL_WIDTH > screen.width && guiLeft - GAP - PANEL_WIDTH >= 0) {
            x = guiLeft - GAP - PANEL_WIDTH;
        }

        boolean hasCurse = false;
        for (GrindstoneEnchants.Entry entry : entries) {
            if (!entry.removable()) {
                hasCurse = true;
                break;
            }
        }

        return new Panel(x, guiTop, entries.size(), hasCurse);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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

        Panel panel = layoutFor(screen, entries);

        if (!panel.contains(mx, my)) {
            return false;
        }

        // Reagujemy tylko na lewy przycisk myszy (0), reszta jest po prostu pochłaniana.
        if (mouseButton != 0 || client.gameMode == null) {
            return true;
        }

        int removableMask = GrindstoneEnchants.removableMask(entries);

        if (inside(mx, my, panel.buttonX(), panel.buttonY(), SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT)) {
            if (removableMask != 0) {
                press(client, menu, GrindstoneEnchants.SELECT_ALL_BUTTON_ID);
            }
            return true;
        }

        for (int i = 0; i < entries.size(); i++) {
            if (inside(mx, my, panel.rowX(), panel.rowY(i), panel.rowWidth(), ROW_HEIGHT)) {
                if (entries.get(i).removable()) {
                    press(client, menu, i);
                }
                return true;
            }
        }

        return true;
    }

    private static void press(Minecraft client, GrindstoneMenu menu, int buttonId) {
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F)
        );

        client.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
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

        int mask = selection.everything$getSelectionMask();
        int removableMask = GrindstoneEnchants.removableMask(entries);

        Panel panel = layoutFor(screen, entries);

        // Tło panelu.
        graphics.fill(panel.x() - 1, panel.y() - 1,
                panel.x() + panel.width() + 1, panel.y() + panel.height() + 1, OUTLINE);
        bevel(graphics, panel.x(), panel.y(), panel.width(), panel.height(),
                PANEL_FILL, PANEL_LIGHT, PANEL_DARK);

        // Tytuł (po lewej) i przycisk "Select all" (w prawym górnym rogu panelu).
        graphics.text(
                client.font,
                Component.translatable("gui.everything_mod.grindstone.title").getString(),
                panel.x() + PADDING + 1,
                panel.y() + PADDING + 3,
                TEXT_TITLE,
                false
        );

        boolean allSelected = removableMask != 0 && (mask & removableMask) == removableMask;
        boolean buttonHover = removableMask != 0 && inside(
                mouseX, mouseY, panel.buttonX(), panel.buttonY(),
                SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT
        );

        String buttonLabel = Component.translatable(
                allSelected
                        ? "gui.everything_mod.grindstone.deselect_all"
                        : "gui.everything_mod.grindstone.select_all"
        ).getString();

        drawRowBox(
                graphics,
                panel.buttonX(), panel.buttonY(), SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT,
                removableMask == 0 ? ROW_DISABLED
                        : allSelected ? (buttonHover ? ROW_SELECTED_HOVER : ROW_SELECTED)
                        : (buttonHover ? ROW_HOVER : ROW_NORMAL)
        );

        graphics.text(
                client.font,
                buttonLabel,
                panel.buttonX() + (SELECT_BUTTON_WIDTH - client.font.width(buttonLabel)) / 2,
                panel.buttonY() + 3,
                removableMask == 0 ? TEXT_DISABLED : TEXT_NORMAL,
                false
        );

        // Wiersze z enchantami.
        for (int i = 0; i < entries.size(); i++) {
            GrindstoneEnchants.Entry entry = entries.get(i);

            int rowX = panel.rowX();
            int rowY = panel.rowY(i);

            boolean selected = (mask & (1 << i)) != 0 && entry.removable();
            boolean hover = entry.removable()
                    && inside(mouseX, mouseY, rowX, rowY, panel.rowWidth(), ROW_HEIGHT);

            drawRowBox(
                    graphics, rowX, rowY, panel.rowWidth(), ROW_HEIGHT,
                    !entry.removable() ? ROW_DISABLED
                            : selected ? (hover ? ROW_SELECTED_HOVER : ROW_SELECTED)
                            : (hover ? ROW_HOVER : ROW_NORMAL)
            );

            drawCheckbox(graphics, rowX + 2, rowY + 2, selected, entry.removable());

            String name = Enchantment.getFullname(entry.holder(), entry.level()).getString();
            int maxTextWidth = panel.rowWidth() - 15 - 3;

            graphics.text(
                    client.font,
                    fit(client, name, maxTextWidth),
                    rowX + 15,
                    rowY + 3,
                    entry.removable() ? TEXT_NORMAL : TEXT_DISABLED,
                    false
            );
        }

        // Podpowiedź o klątwach.
        if (panel.hasCurse()) {
            graphics.text(
                    client.font,
                    fit(
                            client,
                            Component.translatable("gui.everything_mod.grindstone.curse_hint").getString(),
                            panel.width() - 2 * PADDING
                    ),
                    panel.x() + PADDING,
                    panel.y() + panel.height() - FOOTER_HEIGHT + 2,
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

    /** Płaski prostokąt z fazowaniem (jasna góra/lewo, ciemny dół/prawo). */
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

    /** "Wgłębiony" prostokąt jak slot w ekwipunku. */
    private static void drawRowBox(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            int fill
    ) {
        bevel(graphics, x, y, w, h, fill, WIDGET_DARK, WIDGET_LIGHT);
    }

    /** Checkbox 9x9 narysowany z prostokątów, ptaszek jako pixel-art. */
    private static void drawCheckbox(
            GuiGraphicsExtractor graphics,
            int x, int y,
            boolean checked,
            boolean enabled
    ) {
        graphics.fill(x, y, x + 9, y + 9, WIDGET_DARK);
        graphics.fill(x + 1, y + 1, x + 8, y + 8, enabled ? PANEL_FILL : 0xFF6B6B6B);

        if (!checked) {
            return;
        }

        // Ptaszek w obszarze wnętrza (7x7 od x+1, y+1).
        int[][] pixels = {
                {1, 3}, {2, 4}, {3, 3}, {4, 2}, {5, 1},
                {1, 4}, {2, 5}, {3, 4}, {4, 3}, {5, 2}
        };

        for (int[] p : pixels) {
            graphics.fill(x + 1 + p[0], y + 1 + p[1], x + 2 + p[0], y + 2 + p[1], CHECK_COLOR);
        }
    }
}
