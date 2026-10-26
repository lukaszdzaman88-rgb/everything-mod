package net.hehex.everythingmod.grindstone;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Menu Grindstone'a z wyborem enchantów do usunięcia.
 *
 *   slot 0: przedmiot z enchantami
 *   slot 1: wynik = ten sam przedmiot bez zaznaczonych enchantów (-10% wytrzymałości)
 *
 * Zaznaczenie (maska bitowa po liście enchantów) jest trzymane po obu stronach;
 * klient zmienia je lokalnie i wysyła serwerowi pakiet {@link GrindstoneSelectPayload}.
 */
public class EnchantGrindstoneMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;

    private static final int INV_START = 2;
    private static final int HOTBAR_START = 29;
    private static final int INV_END = 38;

    /** Pozycja ekwipunku w oknie: standardowy margines vanilla (x = 8), y przesunięte pod listę. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 90;

    private final Container inputSlots = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantGrindstoneMenu.this.slotsChanged(this);
        }
    };

    private final Container resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;

    private int selectionMask = 0;
    private String signature = "";

    /** Konstruktor klienta. */
    public EnchantGrindstoneMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    /** Konstruktor serwera. */
    public EnchantGrindstoneMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(GrindstoneRegistry.MENU, containerId);
        this.access = access;

        this.addSlot(new Slot(this.inputSlots, 0, 20, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !GrindstoneEnchants.allEntries(stack).isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addSlot(new Slot(this.resultSlots, 0, 20, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                EnchantGrindstoneMenu.this.finishGrinding(player);
            }
        });

        this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    // ------------------------------------------------------------ logika

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (container == this.inputSlots) {
            this.createResult();
        }
    }

    private void createResult() {
        ItemStack input = this.inputSlots.getItem(INPUT_SLOT);

        // Inny przedmiot / inny zestaw enchantów => zaznaczenie od zera.
        String newSignature = GrindstoneEnchants.signature(input);

        if (!newSignature.equals(this.signature)) {
            this.signature = newSignature;
            this.selectionMask = 0;
        }

        ItemStack result = input.isEmpty()
                ? ItemStack.EMPTY
                : GrindstoneEnchants.buildResult(input, this.selectionMask);

        if (this.access != ContainerLevelAccess.NULL) {
            GrindstoneEnchants.debug(
                    "[Grindstone][serwer] wynik: maska={} -> {}",
                    this.selectionMask,
                    result.isEmpty() ? "pusty" : result
            );
        }

        this.resultSlots.setItem(0, result);
        this.broadcastChanges();
    }

    /** Zabranie wyniku: XP tylko za usunięte enchanty, dźwięk Grindstone'a, przedmiot znika. */
    private void finishGrinding(Player player) {
        // XP musi być policzone PRZED wyczyszczeniem slotu wejściowego.
        int rawExperience = GrindstoneEnchants.selectedCost(this.getEntries(), this.selectionMask);

        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                ExperienceOrb.award(
                        serverLevel,
                        Vec3.atCenterOf(pos),
                        GrindstoneEnchants.finalExperience(rawExperience, player.getRandom())
                );
            }

            level.levelEvent(1042, pos, 0); // dźwięk Grindstone'a
        });

        this.inputSlots.setItem(INPUT_SLOT, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------ stan wyboru

    /** Enchanty przedmiotu w slocie (pusta lista = brak listy). */
    public List<GrindstoneEnchants.Entry> getEntries() {
        return GrindstoneEnchants.entriesOf(this.inputSlots.getItem(INPUT_SLOT));
    }

    public int getSelectionMask() {
        return this.selectionMask;
    }

    public int getSignatureHash() {
        return GrindstoneEnchants.signatureHash(this.inputSlots.getItem(INPUT_SLOT));
    }

    /** KLIENT: ustaw zaznaczenie lokalnie i przelicz podgląd wyniku. */
    public void setSelectionLocal(int mask) {
        this.selectionMask = mask & GrindstoneEnchants.removableMask(this.getEntries());
        this.slotsChanged(this.inputSlots);
    }

    /** SERWER: przyjmij zaznaczenie od klienta (odrzuca nieaktualny podpis). */
    public boolean applyRemoteSelection(int mask, int signatureHash) {
        List<GrindstoneEnchants.Entry> entries = this.getEntries();

        if (entries.isEmpty() || signatureHash != this.getSignatureHash()) {
            return false;
        }

        this.selectionMask = mask & GrindstoneEnchants.removableMask(entries);
        this.slotsChanged(this.inputSlots);

        return true;
    }

    // ------------------------------------------------------------ vanilla

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.GRINDSTONE);
    }

    /** Zamknięcie okna: oddaj graczowi przedmiot ze slotu wejściowego. */
    @Override
    public void removed(Player player) {
        super.removed(player);

        this.access.execute((level, pos) -> {
            ItemStack stack = this.inputSlots.getItem(INPUT_SLOT);

            if (!stack.isEmpty()) {
                this.inputSlots.setItem(INPUT_SLOT, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(stack);
            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(stack, clicked);
            } else if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!GrindstoneEnchants.allEntries(stack).isEmpty()) {
                if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_START) {
                if (!this.moveItemStackTo(stack, HOTBAR_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return clicked;
    }
}
