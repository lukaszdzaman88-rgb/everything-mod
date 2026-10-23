package net.hehex.everythingmod.defusion;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Runic Bookshelf - regał liczony przez Defusion Table.
 *
 * Stół liczy bloki z tagu {@link #TAG} (everything-mod:runic_bookshelf), więc
 * kolejne rodzaje runicznych regałów wystarczy dopisać do tego tagu.
 */
public final class RunicBookshelf {

    private RunicBookshelf() {
    }

    public static final String NAME = "runic_bookshelf";

    public static final Identifier ID = Identifier.fromNamespaceAndPath(DefusionTable.MOD_ID, NAME);

    public static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ID);
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);

    /** Tag bloków, które Defusion Table uznaje za runiczne regały. */
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, ID);

    public static final Block BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            BLOCK_KEY,
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).setId(BLOCK_KEY))
    );

    public static final Item ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ITEM_KEY,
            new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY))
    );

    /** Wywołanie wymusza załadowanie klasy (a więc rejestrację) w odpowiednim momencie. */
    public static void init() {
    }
}
