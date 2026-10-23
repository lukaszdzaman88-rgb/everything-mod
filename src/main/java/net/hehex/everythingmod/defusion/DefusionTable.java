package net.hehex.everythingmod.defusion;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Rejestr Defusion Table: blok, przedmiot (BlockItem) i typ menu.
 *
 * Nazwę bloku zmienisz w jednym miejscu: stała {@link #NAME}
 * (plus pliki zasobów o tej samej nazwie, patrz README w odpowiedzi).
 */
public final class DefusionTable {

    private DefusionTable() {
    }

    public static final String MOD_ID = "everything-mod";
    public static final String NAME = "defusion_table";

    public static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, NAME);

    public static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ID);
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);

    public static final Block BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            BLOCK_KEY,
            new DefusionTableBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CARTOGRAPHY_TABLE).setId(BLOCK_KEY)
            )
    );

    public static final Item ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ITEM_KEY,
            new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY))
    );

    public static final MenuType<DefusionTableMenu> MENU = Registry.register(
            BuiltInRegistries.MENU,
            ID,
            new MenuType<>(DefusionTableMenu::new, FeatureFlags.VANILLA_SET)
    );

    /** Wywołanie wymusza załadowanie klasy (a więc rejestrację) w odpowiednim momencie. */
    public static void init() {
    }
}
