package net.hehex.everythingmod.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.hehex.everythingmod.registries.SpawnerSoulRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class SpawnerSoulLoot {

    public static void initialize() {

        LootTableEvents.MODIFY.register(
                (key, tableBuilder, source, registries) -> {

                    if (!source.isBuiltin()) {
                        return;
                    }

                    for (var entry : SpawnerSoulRegistry.getAll().entrySet()) {

                        var soulItem = entry.getKey();
                        EntityType<?> entityType = entry.getValue();

                        /*
                         * getDefaultLootTable() zwraca:
                         *
                         * Optional<ResourceKey<LootTable>>
                         *
                         * Musimy wyjąć z niego ResourceKey.
                         */
                        var lootTableKey =
                                entityType.getDefaultLootTable().orElse(null);

                        if (lootTableKey == null) {
                            continue;
                        }

                        // Czy to loot table tego moba?
                        if (!lootTableKey.equals(key)) {
                            continue;
                        }

                        LootPool.Builder pool =
                                LootPool.lootPool()
                                        .setRolls(
                                                ConstantValue.exactly(1)
                                        )
                                        .add(
                                                LootItem.lootTableItem(
                                                        soulItem
                                                )
                                        )
                                        .when(
                                                LootItemRandomChanceWithEnchantedBonusCondition
                                                        .randomChanceAndLootingBoost(
                                                                registries,
                                                                0.005F,
                                                                0.005F
                                                        )
                                        );

                        tableBuilder.withPool(pool);

                        // Znaleźliśmy właściwego moba.
                        break;
                    }
                }
        );
    }
}