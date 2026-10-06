package net.hehex.everythingmod.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.hehex.everythingmod.component.ModComponents;
import net.hehex.everythingmod.itemid.ModItemIDs;
import net.hehex.everythingmod.registries.SpawnerSoulRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static Item register(
            net.minecraft.resources.ResourceKey<Item> itemKey,
            Function<Item.Properties, Item> itemFactory,
            Item.Properties properties
    ) {
        Item item = itemFactory.apply(
                properties.setId(itemKey)
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                item
        );

        return item;
    }

    public static final Item XP_TALISMAN = register(
            ModItemIDs.XP_TALISMAN,
            XPTalismanItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .durability(10)
    );
    public static final Item SPAWNER_SHARD = register(
            ModItemIDs.SPAWNER_SHARD,
            Item::new,
            new Item.Properties()
                    .stacksTo(64)
    );
    public static final Item SOUL_OF_THE_ZOMBIE = register(
            ModItemIDs.SOUL_OF_THE_ZOMBIE,
            SpawnerSoulItem::new,
            new Item.Properties()
                    .stacksTo(64)
    );
    public static final Item SOUL_OF_THE_SKELETON = register(
            ModItemIDs.SOUL_OF_THE_SKELETON,
            SpawnerSoulItem::new,
            new Item.Properties()
                    .stacksTo(64)
    );
    public static final Item SOUL_OF_THE_SPIDER = register(
            ModItemIDs.SOUL_OF_THE_SPIDER,
            SpawnerSoulItem::new,
            new Item.Properties()
                    .stacksTo(64)
    );
    public static final Item SOUL_OF_THE_CREEPER = register(
            ModItemIDs.SOUL_OF_THE_CREEPER,
            SpawnerSoulItem::new,
            new Item.Properties()
                    .stacksTo(64)
    );

    public static void initialize() {

        SpawnerSoulRegistry.register(
                SOUL_OF_THE_ZOMBIE,
                EntityTypes.ZOMBIE
        );
        SpawnerSoulRegistry.register(
                SOUL_OF_THE_SKELETON,
                EntityTypes.SKELETON
        );
        SpawnerSoulRegistry.register(
                SOUL_OF_THE_SPIDER,
                EntityTypes.SPIDER
        );
        SpawnerSoulRegistry.register(
                SOUL_OF_THE_CREEPER,
                EntityTypes.CREEPER
        );


        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ModItems.XP_TALISMAN));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> { entries.accept(ModItems.SOUL_OF_THE_ZOMBIE);
                    entries.accept(ModItems.SPAWNER_SHARD);
                entries.accept(ModItems.SOUL_OF_THE_SKELETON);
                entries.accept(ModItems.SOUL_OF_THE_SPIDER);
                entries.accept(ModItems.SOUL_OF_THE_CREEPER);});
    }



    public static void registerModItems() {
    }
}