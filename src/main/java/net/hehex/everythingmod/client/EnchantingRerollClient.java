package net.hehex.everythingmod.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.hehex.everythingmod.mixin.client.AbstractContainerScreenAccessor;
import net.hehex.everythingmod.mixin.client.SlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Rozszerzone GUI stołu do enchantowania z przyciskiem Reroll.
 *
 * <pre>
 * Układ tekstury (176 x 194, w pliku PNG 256 x 256):
 *
 *   y =   0 ..  71   vanilla: tytuł, sloty itemu/lapisu, 3 przyciski enchantów
 *   y =  72 .. 110   WOLNE MIEJSCE (28 px dodane przez moda) - tu jest Reroll
 *   y = 111 .. 193   ekwipunek + hotbar (przesunięte o 28 px w dół)
 * </pre>
 *
 * Górną część (0-71) rysuje vanilla, więc nazwy enchantów, ikony poziomów i
 * podświetlenie działają normalnie. My dorysowujemy wyłącznie dolną część
 * tekstury (od y = 72), która przykrywa stare, nieprzesunięte tło ekwipunku.
 */
public class EnchantingRerollClient implements ClientModInitializer {

    private static final int REROLL_BUTTON_ID = 3;
    private static final int REROLL_COST = 3;

    /* Rozmiar naszej tekstury w GUI. */
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 194;

    /* Prawdziwy rozmiar pliku PNG (WAŻNE dla blit - nie 176x194!). */
    private static final int TEXTURE_SIZE = 256;

    /* Vanilla: 166 px. My: 194 px => +28 px. */
    private static final int EXTRA_HEIGHT = 28;

    /* Od tego wiersza tekstury zaczyna się nasza część (wszystko wyżej = vanilla). */
    private static final int SEAM_Y = 72;

    /* Vanilla: napis "Inventory" jest na y = 72. */
    private static final int VANILLA_INVENTORY_LABEL_Y = 72;

    /*
     * Przycisk Reroll (współrzędne względem lewego górnego rogu GUI).
     *
     * Przyciski enchantów mają 108 px szerokości i kończą się na x = 168,
     * więc Reroll (połowa szerokości = 54 px) wyrównujemy do ich prawej krawędzi.
     * Pionowo siedzi w wolnych 28 px pod przyciskami enchantów.
     */
    private static final int BUTTON_WIDTH = 54;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_X = 168 - BUTTON_WIDTH; // 114
    private static final int BUTTON_Y = 76;

    private static final Identifier ENCHANTING_TABLE_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "everything-mod",
                    "textures/gui/enchanting_table_extended.png"
            );

    private static final Set<EnchantmentScreen> REGISTERED_SCREENS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static final WeakHashMap<EnchantmentScreen, Button> REROLL_BUTTONS =
            new WeakHashMap<>();

    /*
     * Menu, którym już przesunęliśmy sloty ekwipunku.
     * Bez tego każdy resize okna (ponowny init) przesuwałby sloty kolejny raz.
     */
    private static final Set<EnchantmentMenu> SHIFTED_MENUS =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void onInitializeClient() {

        /*
         * Przed vanillowym init() ustawiamy wysokość 194 px,
         * dzięki czemu vanilla sama wyśrodkuje całe GUI (leftPos/topPos).
         */
        ScreenEvents.BEFORE_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof EnchantmentScreen)) {
                        return;
                    }

                    AbstractContainerScreenAccessor accessor =
                            (AbstractContainerScreenAccessor) screen;

                    accessor.everything$setImageHeight(GUI_HEIGHT);
                    accessor.everything$setInventoryLabelY(
                            VANILLA_INVENTORY_LABEL_Y + EXTRA_HEIGHT
                    );
                }
        );

        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof EnchantmentScreen enchantmentScreen)) {
                        return;
                    }

                    if (client.player == null
                            || !(client.player.containerMenu
                            instanceof EnchantmentMenu menu)) {
                        return;
                    }

                    shiftInventorySlots(menu);

                    AbstractContainerScreenAccessor accessor =
                            (AbstractContainerScreenAccessor) enchantmentScreen;

                    int guiLeft = accessor.everything$getLeftPos();
                    int guiTop = accessor.everything$getTopPos();

                    Button rerollButton =
                            Button.builder(
                                            Component.translatable(
                                                    "gui.everything_mod.enchanting.reroll"
                                            ),
                                            button -> {

                                                if (client.player == null
                                                        || client.gameMode == null) {
                                                    return;
                                                }

                                                client.gameMode
                                                        .handleInventoryButtonClick(
                                                                client.player
                                                                        .containerMenu
                                                                        .containerId,
                                                                REROLL_BUTTON_ID
                                                        );
                                            }
                                    )
                                    .bounds(
                                            guiLeft + BUTTON_X,
                                            guiTop + BUTTON_Y,
                                            BUTTON_WIDTH,
                                            BUTTON_HEIGHT
                                    )
                                    .build();

                    rerollButton.active = canReroll(menu);

                    Screens.getWidgets(enchantmentScreen).add(rerollButton);
                    REROLL_BUTTONS.put(enchantmentScreen, rerollButton);

                    /*
                     * Zdarzenia renderowania rejestrujemy tylko raz na ekran
                     * (init wywołuje się ponownie np. przy zmianie rozmiaru okna).
                     */
                    if (REGISTERED_SCREENS.add(enchantmentScreen)) {
                        registerRenderEvents(client, enchantmentScreen);
                    }
                }
        );
    }

    private static void registerRenderEvents(
            Minecraft client,
            EnchantmentScreen enchantmentScreen
    ) {

        /*
         * DOLNA CZĘŚĆ TŁA + KOSZT REROLLA.
         *
         * Vanilla w tym momencie już narysowała swoje tło oraz przyciski
         * enchantów. Rysujemy tylko wiersze tekstury od SEAM_Y w dół, żeby
         * nie zakryć vanillowej góry (nazw enchantów, ikon poziomów itd.).
         *
         * Ostatnie dwa argumenty to rozmiar PLIKU PNG (256x256).
         */
        ScreenEvents.afterBackground(enchantmentScreen).register(
                (currentScreen, graphics, mouseX, mouseY, tickProgress) -> {

                    AbstractContainerScreenAccessor accessor =
                            (AbstractContainerScreenAccessor) currentScreen;

                    int left = accessor.everything$getLeftPos();
                    int top = accessor.everything$getTopPos();

                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            ENCHANTING_TABLE_TEXTURE,

                            left,
                            top + SEAM_Y,

                            0,
                            SEAM_Y,

                            GUI_WIDTH,
                            GUI_HEIGHT - SEAM_Y,

                            TEXTURE_SIZE,
                            TEXTURE_SIZE
                    );

                    /*
                     * KOSZT REROLLA: "3 [lapis]" po lewej stronie przycisku.
                     *
                     * Rysujemy to TUTAJ (na etapie tła), a nie w afterExtract.
                     * afterExtract odpala się dopiero po złożeniu tooltipów, więc
                     * ikona i cyfra wyświetlały się nad tooltipem itemu.
                     * Na etapie tła są pod slotami, widgetami i tooltipami.
                     */
                    if (client.player == null
                            || !(client.player.containerMenu
                            instanceof EnchantmentMenu menu)) {
                        return;
                    }

                    int buttonX = left + BUTTON_X;
                    int buttonY = top + BUTTON_Y;

                    // Ikona lapisa (16x16), tuż przy przycisku.
                    int iconX = buttonX - 18;
                    int iconY = buttonY + (BUTTON_HEIGHT - 16) / 2;

                    graphics.item(
                            new ItemStack(Items.LAPIS_LAZULI),
                            iconX,
                            iconY
                    );

                    // Cyfra "3" po lewej stronie ikony.
                    graphics.text(
                            client.font,
                            String.valueOf(REROLL_COST),
                            iconX - 8,
                            buttonY + (BUTTON_HEIGHT - 8) / 2,
                            hasEnoughLapis(menu) ? 0xFF404040 : 0xFFA03030,
                            false
                    );
                }
        );

        /*
         * AKTYWNOŚĆ PRZYCISKU: item w stole + min. 3 lapisy + są jakieś oferty.
         */
        ScreenEvents.afterTick(enchantmentScreen).register(
                currentScreen -> {

                    if (client.player == null
                            || !(client.player.containerMenu
                            instanceof EnchantmentMenu menu)) {
                        return;
                    }

                    Button button = REROLL_BUTTONS.get(enchantmentScreen);

                    if (button != null) {
                        button.active = canReroll(menu);
                    }
                }
        );
    }

    /**
     * Przesuwa sloty ekwipunku gracza w dół o EXTRA_HEIGHT.
     * Robi to tylko raz na dane menu.
     */
    private static void shiftInventorySlots(EnchantmentMenu menu) {

        if (!SHIFTED_MENUS.add(menu)) {
            return;
        }

        // 0 = item, 1 = lapis, 2+ = ekwipunek i hotbar gracza.
        for (int i = 2; i < menu.slots.size(); i++) {

            Slot slot = menu.slots.get(i);

            ((SlotAccessor) slot).everything$setY(slot.y + EXTRA_HEIGHT);
        }
    }

    private static boolean hasEnoughLapis(EnchantmentMenu menu) {
        return menu.getSlot(1).getItem().getCount() >= REROLL_COST;
    }

    private static boolean hasOffers(EnchantmentMenu menu) {
        return menu.costs[0] > 0 || menu.costs[1] > 0 || menu.costs[2] > 0;
    }

    private static boolean canReroll(EnchantmentMenu menu) {
        return !menu.getSlot(0).getItem().isEmpty()
                && hasOffers(menu)
                && hasEnoughLapis(menu);
    }
}
