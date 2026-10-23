package net.hehex.everythingmod.defusion;

import java.util.List;

import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Menu Defusion Table.
 *
 *   slot 0: zaklęty przedmiot (zostanie ZNISZCZONY)
 *   slot 1: pusta książka (zostanie zużyta)
 *   slot 2: wynik = zaklęta książka z JEDNYM wybranym zaklęciem (też klątwą)
 *
 * Wymagania: min. {@link DefusionConfig#MIN_SHELVES} Runic Bookshelf wokół stołu
 * (układ jak przy stole do enchantowania) oraz poziomy doświadczenia
 * ({@link DefusionConfig#levelCost}), niższe przy 10 i 15 regałach.
 *
 * Wybór (indeks na liście zaklęć) jest trzymany po obu stronach; klient wysyła go
 * pakietem {@link DefusionSelectPayload}. Liczba regałów jest liczona na serwerze
 * i synchronizowana do klienta przez DataSlot.
 */
public class DefusionTableMenu extends AbstractContainerMenu {

    public static final int ITEM_SLOT = 0;
    public static final int BOOK_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    private static final int INV_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int INV_END = 39;

    /** Dźwięk stołu do enchantowania (zagrany z niższą tonacją). */
    private static final SoundEvent TABLE_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("minecraft", "block.enchantment_table.use")
    );

    private final Container inputSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            DefusionTableMenu.this.slotsChanged(this);
        }
    };

    private final Container resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;

    /** Liczba Runic Bookshelf wokół stołu (liczona na serwerze, synchronizowana do klienta). */
    private final DataSlot shelves = DataSlot.standalone();

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

        this.addSlot(new Slot(this.inputSlots, ITEM_SLOT, 31, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !GrindstoneEnchants.allEntries(stack).isEmpty();
            }
        });

        this.addSlot(new Slot(this.inputSlots, BOOK_SLOT, 62, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }
        });

        this.addSlot(new Slot(this.resultSlots, RESULT_SLOT, 126, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            /** Bez wystarczającej liczby poziomów nie da się zabrać wyniku. */
            @Override
            public boolean mayPickup(Player player) {
                return DefusionTableMenu.this.canAfford(player);
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                DefusionTableMenu.this.finishCrafting(player);
            }
        });

        this.addStandardInventorySlots(inventory, 8, 84);

        this.addDataSlot(this.shelves);
        this.updateShelves();
    }

    // ------------------------------------------------------------ regały i koszt

    /** Liczy Runic Bookshelf w tym samym układzie co vanillowy stół do enchantowania. */
    private int countShelves() {
        int[] count = {0};

        this.access.execute((level, pos) -> {
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                boolean isShelf = level.getBlockState(pos.offset(offset)).is(RunicBookshelf.TAG);

                // Blok pomiędzy stołem a regałem musi przepuszczać "moc" (np. powietrze).
                boolean clearPath = level.getBlockState(
                        pos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)
                ).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);

                if (isShelf && clearPath) {
                    count[0]++;
                }
            }
        });

        return count[0];
    }

    /** Tylko serwer ma prawdziwy ContainerLevelAccess; klient dostaje wartość z DataSlot. */
    private void updateShelves() {
        if (this.access != ContainerLevelAccess.NULL) {
            this.shelves.set(this.countShelves());
        }
    }

    public int getShelfCount() {
        return this.shelves.get();
    }

    public boolean hasEnoughShelves() {
        return this.getShelfCount() >= DefusionConfig.MIN_SHELVES;
    }

    public int getDiscountPercent() {
        return DefusionConfig.discountPercent(this.getShelfCount());
    }

    /** Koszt w poziomach przy aktualnej liczbie regałów. */
    public int getLevelCost() {
        return DefusionConfig.levelCost(this.getShelfCount());
    }

    public boolean canAfford(Player player) {
        return player.hasInfiniteMaterials() || player.experienceLevel >= this.getLevelCost();
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
        this.updateShelves();

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

        // Na kliencie liczba regałów przychodzi z opóźnieniem; wynik i tak ustala serwer.
        if (!item.isEmpty()
                && book.is(Items.BOOK)
                && this.hasEnoughShelves()
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

    /**
     * Zabranie wyniku: pobiera poziomy, niszczy JEDEN przedmiot, zużywa JEDNĄ książkę
     * i gra dźwięk stołu do enchantowania z niższą tonacją.
     */
    private void finishCrafting(Player player) {
        // Koszt liczymy PRZED zużyciem składników.
        int cost = this.getLevelCost();

        if (!player.hasInfiniteMaterials()) {
            player.giveExperienceLevels(-cost);
        }

        this.inputSlots.removeItem(ITEM_SLOT, 1);
        this.inputSlots.removeItem(BOOK_SLOT, 1);

        this.access.execute((level, pos) -> level.playSound(
                null,
                pos,
                TABLE_SOUND,
                SoundSource.BLOCKS,
                1.0F,
                DefusionConfig.SOUND_PITCH + level.getRandom().nextFloat() * 0.1F
        ));
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
                // Shift-klik na wynik też wymaga poziomów.
                if (!slot.mayPickup(player)) {
                    return ItemStack.EMPTY;
                }

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
