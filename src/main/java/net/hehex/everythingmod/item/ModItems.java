package net.hehex.everythingmod.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.hehex.everythingmod.component.ModComponents;
import net.hehex.everythingmod.itemid.ModItemIDs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public static void initialize() {
        // Dodanie do zakładki Tools.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(XP_TALISMAN));
    }

    public static void registerModItems() {
    }
}