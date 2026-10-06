package net.hehex.everythingmod.tags;

import net.hehex.everythingmod.EverythingMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {

        public static final TagKey<Item> IRON_OR_BETTER_PICKAXES =
                TagKey.create(
                        Registries.ITEM,
                        Identifier.fromNamespaceAndPath(
                                EverythingMod.MOD_ID,
                                "iron_or_better_pickaxes"
                        )
                );
    }
}
