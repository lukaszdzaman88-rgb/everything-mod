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
}