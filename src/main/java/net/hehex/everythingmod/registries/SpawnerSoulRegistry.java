package net.hehex.everythingmod.registries;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class SpawnerSoulRegistry {

    private static final Map<Item, EntityType<?>> SOULS =
            new IdentityHashMap<>();

    private SpawnerSoulRegistry() {
    }

    public static void register(
            Item soulItem,
            EntityType<?> entityType
    ) {
        SOULS.put(soulItem, entityType);
    }

    public static EntityType<?> getEntityType(ItemStack stack) {
        return SOULS.get(stack.getItem());
    }

    public static Map<Item, EntityType<?>> getAll() {
        return Collections.unmodifiableMap(SOULS);
    }
}