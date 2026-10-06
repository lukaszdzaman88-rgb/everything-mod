package net.hehex.everythingmod.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.hehex.everythingmod.mixin.client.AbstractContainerScreenAccessor;
import net.hehex.everythingmod.mixin.client.SlotAccessor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnchantingRerollClient implements ClientModInitializer {

    private static final int REROLL_BUTTON_ID = 3;
    private static final int REROLL_COST = 3;

    /*
     * TWOJA TEKSTURA:
     *
     * 176 x 194
     */
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 194;

    /*
     * Vanilla:
     *
     * 176 x 166
     */
    private static final int VANILLA_GUI_HEIGHT = 166;

    /*
     * 194 - 166 = 28
     */
    private static final int EXTRA_HEIGHT = 28;

    /*
     * Ekwipunek przesuwamy dokładnie o tyle,
     * ile dodaliśmy do tekstury.
     */
    private static final int INVENTORY_SHIFT = 28;

    private static final Identifier ENCHANTING_TABLE_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "everything-mod",
                    "textures/gui/enchanting_table_extended.png"
            );

    private static final Set<EnchantmentScreen> REGISTERED_SCREENS =
            Collections.newSetFromMap(
                    new WeakHashMap<>()
            );

    private static final WeakHashMap<EnchantmentScreen, Button> REROLL_BUTTONS =
            new WeakHashMap<>();

    @Override
    public void onInitializeClient() {

        /*
         * =========================================
         * ZMIANA WYSOKOŚCI GUI
         * =========================================
         *
         * Przed normalnym init Minecrafta ustawiamy
         * wysokość na 194 px.
         */
        ScreenEvents.BEFORE_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof EnchantmentScreen)) {
                        return;
                    }

                    AbstractContainerScreenAccessor accessor =
                            (AbstractContainerScreenAccessor) screen;

                    accessor.everything$setImageHeight(
                            GUI_HEIGHT
                    );

                    accessor.everything$setInventoryLabelY(
                            72 + INVENTORY_SHIFT
                    );
                }
        );

        /*
         * =========================================
         * DODANIE REROLL + PRZESUNIĘCIE SLOTÓW
         * =========================================
         */
        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {

                    if (!(screen instanceof EnchantmentScreen enchantmentScreen)) {
                        return;
                    }

                    if (!(client.player.containerMenu
                            instanceof EnchantmentMenu menu)) {
                        return;
                    }

                    AbstractContainerScreenAccessor screenAccessor =
                            (AbstractContainerScreenAccessor) enchantmentScreen;

                    /*
                     * UWAGA:
                     *
                     * Vanilla GUI jest centrowane względem 166 px.
                     *
                     * Chcemy zachować jego oryginalną górę
                     * i dołożyć 28 px WYŁĄCZNIE W DÓŁ.
                     */
                    int vanillaTop =
                            (scaledHeight - VANILLA_GUI_HEIGHT) / 2;

                    screenAccessor.everything$setTopPos(
                            vanillaTop
                    );

                    /*
                     * =================================
                     * PRZESUWAMY EKWIPUNEK
                     * =================================
                     *
                     * Sloty:
                     *
                     * 0 = przedmiot
                     * 1 = lapis
                     * 2+ = inventory
                     */
                    for (int i = 2; i < menu.slots.size(); i++) {

                        Slot slot = menu.slots.get(i);

                        SlotAccessor accessor =
                                (SlotAccessor) slot;

                        accessor.everything$setY(
                                slot.y + INVENTORY_SHIFT
                        );
                    }

                    /*
                     * =================================
                     * POZYCJA GUI
                     * =================================
                     */
                    int guiLeft =
                            (scaledWidth - GUI_WIDTH) / 2;

                    int guiTop =
                            vanillaTop;

                    /*
                     * =================================
                     * REROLL BUTTON
                     * =================================
                     *
                     * Dodatkowe 28 px zaczyna się
                     * zaraz po vanilla GUI.
                     *
                     * 166 -> 194
                     *
                     * Przyciskiem zajmujemy 20 px.
                     */
                    int buttonX =
                            guiLeft + 108;

                    int buttonY =
                            guiTop + 170;

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
                                            buttonX,
                                            buttonY,
                                            54,
                                            20
                                    )
                                    .build();

                    Screens.getWidgets(
                            enchantmentScreen
                    ).add(rerollButton);

                    REROLL_BUTTONS.put(
                            enchantmentScreen,
                            rerollButton
                    );

                    /*
                     * =================================
                     * EVENTY RENDEROWANIA
                     * =================================
                     */
                    if (REGISTERED_SCREENS.add(
                            enchantmentScreen
                    )) {

                        /*
                         * =================================
                         * WŁASNE TŁO 176x194
                         * =================================
                         *
                         * Vanilla najpierw rysuje swoje GUI.
                         *
                         * Następnie my rysujemy NASZĄ CAŁĄ
                         * teksturę 176x194 w dokładnie tym
                         * samym miejscu.
                         *
                         * Dzięki temu na ekranie zostaje
                         * tylko jeden layout.
                         */
                        ScreenEvents.afterBackground(
                                enchantmentScreen
                        ).register(
                                (
                                        currentScreen,
                                        graphics,
                                        mouseX,
                                        mouseY,
                                        tickProgress
                                ) -> {

                                    int left =
                                            (currentScreen.width
                                                    - GUI_WIDTH)
                                                    / 2;

                                    /*
                                     * Najważniejsze:
                                     *
                                     * NIE używamy:
                                     *
                                     * (height - 194) / 2
                                     *
                                     * bo wtedy całe GUI
                                     * przesunęłoby się do góry.
                                     *
                                     * Używamy pozycji vanilla.
                                     */
                                    int top =
                                            (currentScreen.height
                                                    - VANILLA_GUI_HEIGHT)
                                                    / 2;

                                    graphics.blit(
                                            RenderPipelines.GUI_TEXTURED,
                                            ENCHANTING_TABLE_TEXTURE,

                                            left,
                                            top,

                                            0,
                                            0,

                                            GUI_WIDTH,
                                            GUI_HEIGHT,

                                            GUI_WIDTH,
                                            GUI_HEIGHT
                                    );
                                }
                        );

                        /*
                         * =================================
                         * KOSZT REROLLA
                         * =================================
                         */
                        ScreenEvents.afterExtract(
                                enchantmentScreen
                        ).register(
                                (
                                        currentScreen,
                                        graphics,
                                        mouseX,
                                        mouseY,
                                        tickProgress
                                ) -> {

                                    if (!(client.player.containerMenu
                                            instanceof EnchantmentMenu enchantmentMenu)) {
                                        return;
                                    }

                                    Button currentButton =
                                            REROLL_BUTTONS.get(
                                                    enchantmentScreen
                                            );

                                    if (currentButton == null) {
                                        return;
                                    }

                                    /*
                                     * Czy jest item?
                                     */
                                    boolean hasItem =
                                            !enchantmentMenu
                                                    .getSlot(0)
                                                    .getItem()
                                                    .isEmpty();

                                    /*
                                     * Czy jest >=3 lapisu?
                                     */
                                    boolean enoughLapis =
                                            enchantmentMenu
                                                    .getSlot(1)
                                                    .getItem()
                                                    .getCount()
                                                    >= REROLL_COST;

                                    currentButton.active =
                                            hasItem && enoughLapis;

                                    int left =
                                            (currentScreen.width
                                                    - GUI_WIDTH)
                                                    / 2;

                                    int top =
                                            (currentScreen.height
                                                    - VANILLA_GUI_HEIGHT)
                                                    / 2;

                                    int currentButtonX =
                                            left + 108;

                                    int currentButtonY =
                                            top + 170;

                                    /*
                                     * 3
                                     */
                                    graphics.text(
                                            client.font,
                                            "3",

                                            currentButtonX - 22,
                                            currentButtonY + 6,

                                            enoughLapis
                                                    ? 0xFFFFFFFF
                                                    : 0xFF777777,

                                            true
                                    );

                                    /*
                                     * Lapis
                                     */
                                    graphics.item(
                                            new ItemStack(
                                                    Items.LAPIS_LAZULI
                                            ),

                                            currentButtonX - 12,
                                            currentButtonY + 2
                                    );
                                }
                        );

                        /*
                         * =================================
                         * AKTUALIZACJA BUTTONA
                         * =================================
                         */
                        ScreenEvents.afterTick(
                                enchantmentScreen
                        ).register(
                                currentScreen -> {

                                    if (!(client.player.containerMenu
                                            instanceof EnchantmentMenu enchantmentMenu)) {
                                        return;
                                    }

                                    Button currentButton =
                                            REROLL_BUTTONS.get(
                                                    enchantmentScreen
                                            );

                                    if (currentButton == null) {
                                        return;
                                    }

                                    boolean hasItem =
                                            !enchantmentMenu
                                                    .getSlot(0)
                                                    .getItem()
                                                    .isEmpty();

                                    boolean enoughLapis =
                                            enchantmentMenu
                                                    .getSlot(1)
                                                    .getItem()
                                                    .getCount()
                                                    >= REROLL_COST;

                                    currentButton.active =
                                            hasItem && enoughLapis;
                                }
                        );
                    }
                }
        );
    }
}