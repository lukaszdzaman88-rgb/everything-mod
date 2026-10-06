package net.hehex.everythingmod.itemid;

import net.hehex.everythingmod.EverythingMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIDs {

    public static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(
                        EverythingMod.MOD_ID,
                        name
                )
        );
    }

    public static final ResourceKey<Item> XP_TALISMAN = create("xp_talisman");

    public static final ResourceKey<Item> SPAWNER_SHARD = create("spawner_shard");
    public static final ResourceKey<Item> SOUL_OF_THE_ZOMBIE = create("soul_of_the_zombie");
    public static final ResourceKey<Item> SOUL_OF_THE_SKELETON = create("soul_of_the_skeleton");
    public static final ResourceKey<Item> SOUL_OF_THE_SPIDER = create("soul_of_the_spider");
    public static final ResourceKey<Item> SOUL_OF_THE_CREEPER = create("soul_of_the_creeper");
}