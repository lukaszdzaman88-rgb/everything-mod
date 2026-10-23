package net.hehex.everythingmod.defusion;

import java.util.List;

import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Menu Defusion Table.
 *
 *   slot 0: zaklęty przedmiot (zostanie ZNISZCZONY)
 *   slot 1: pusta książka (zostanie zużyta)
 *   slot 2: wynik = zaklęta książka z JEDNYM wybranym zaklęciem (też klątwą)
 *
 * Wybór (indeks na liście zaklęć) jest trzymany po obu stronach; klient wysyła go
 * pakietem {@link DefusionSelectPayload}.
 */
public class DefusionTableMenu extends AbstractContainerMenu {

    public static final int ITEM_SLOT = 0;
    public static final int BOOK_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    private static final int INV_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int INV_END = 39;

    private final Container inputSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            DefusionTableMenu.this.slotsChanged(this);
        }
    };

    private final Container resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;

    /** Indeks wybranego zaklęcia na liście albo -1. */
    private int selection = -1;

    private String signature = "";

    /** Konstruktor klienta. */
    public DefusionTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    /** Konstruktor serwera. */
    public DefusionTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(DefusionTable.MENU, containerId);
        this.access = access;

        this.addSlot(new Slot(this.inputSlots, ITEM_SLOT, 31, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !GrindstoneEnchants.allEntries(stack).isEmpty();
            }
        });

        this.addSlot(new Slot(this.inputSlots, BOOK_SLOT, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }
        });

        this.addSlot(new Slot(this.resultSlots, RESULT_SLOT, 126, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                DefusionTableMenu.this.consumeInputs();
            }
        });

        this.addStandardInventorySlots(inventory, 8, 84);
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
        ItemStack item = this.inputSlots.getItem(ITEM_SLOT);
        ItemStack book = this.inputSlots.getItem(BOOK_SLOT);

        // Inny przedmiot / inny zestaw zaklęć => wybór od zera.
        String newSignature = GrindstoneEnchants.signature(item);

        if (!newSignature.equals(this.signature)) {
            this.signature = newSignature;
            this.selection = -1;
        }

        ItemStack result = ItemStack.EMPTY;
        List<GrindstoneEnchants.Entry> entries = this.getEntries();

        if (!item.isEmpty()
                && book.is(Items.BOOK)
                && this.selection >= 0
                && this.selection < entries.size()) {

            GrindstoneEnchants.Entry entry = entries.get(this.selection);

            ItemEnchantments.Mutable enchantments =
                    new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(entry.holder(), entry.level());

            result = new ItemStack(Items.ENCHANTED_BOOK);
            result.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        }

        this.resultSlots.setItem(0, result);
        this.broadcastChanges();
    }

    /** Zabranie wyniku: niszczy JEDEN przedmiot i zużywa JEDNĄ książkę. */
    private void consumeInputs() {
        this.inputSlots.removeItem(ITEM_SLOT, 1);
        this.inputSlots.removeItem(BOOK_SLOT, 1);

        this.access.execute((level, pos) -> level.levelEvent(1042, pos, 0)); // dźwięk
    }

    // ------------------------------------------------------------ stan wyboru

    /** Zaklęcia na przedmiocie w slocie (pusta lista = nic do wyboru). */
    public List<GrindstoneEnchants.Entry> getEntries() {
        return GrindstoneEnchants.entriesOf(this.inputSlots.getItem(ITEM_SLOT));
    }

    public boolean hasBook() {
        return this.inputSlots.getItem(BOOK_SLOT).is(Items.BOOK);
    }

    public int getSelection() {
        return this.selection;
    }

    public int getSignatureHash() {
        return GrindstoneEnchants.signature(this.inputSlots.getItem(ITEM_SLOT)).hashCode();
    }

    /** KLIENT: ustaw wybór lokalnie (-1 = brak) i przelicz podgląd wyniku. */
    public void setSelectionLocal(int index) {
        this.selection = (index >= 0 && index < this.getEntries().size()) ? index : -1;
        this.slotsChanged(this.inputSlots);
    }

    /** SERWER: przyjmij wybór od klienta (odrzuca nieaktualny podpis). */
    public boolean applyRemoteSelection(int index, int signatureHash) {
        if (this.getEntries().isEmpty() || signatureHash != this.getSignatureHash()) {
            return false;
        }

        this.selection = (index >= 0 && index < this.getEntries().size()) ? index : -1;
        this.slotsChanged(this.inputSlots);

        return true;
    }

    // ------------------------------------------------------------ vanilla

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, DefusionTable.BLOCK);
    }

    /** Zamknięcie okna: oddaj graczowi to, co leżało w slotach wejściowych. */
    @Override
    public void removed(Player player) {
        super.removed(player);

        this.access.execute((level, pos) -> {
            for (int i = 0; i < 2; i++) {
                ItemStack stack = this.inputSlots.getItem(i);

                if (!stack.isEmpty()) {
                    this.inputSlots.setItem(i, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(stack);
                }
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
            } else if (index == ITEM_SLOT || index == BOOK_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.BOOK)) {
                if (!this.moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!GrindstoneEnchants.allEntries(stack).isEmpty()) {
                if (!this.moveItemStackTo(stack, ITEM_SLOT, ITEM_SLOT + 1, false)) {
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
