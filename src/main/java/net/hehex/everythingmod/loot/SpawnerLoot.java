package net.hehex.everythingmod.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.hehex.everythingmod.item.ModItems;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class SpawnerLoot {

    public static void initialize() {

        LootTableEvents.MODIFY.register(
                (key, tableBuilder, source, registries) -> {

                    // Tylko zwykły Minecraftowy spawner.
                    // Trial Spawner ma inną loot table, więc go to nie dotknie.
                    if (!source.isBuiltin()
                            || !Blocks.SPAWNER.getLootTable()
                            .orElseThrow()
                            .equals(key)) {
                        return;
                    }

                    LootPool.Builder pool = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))

                            // 1-2 Spawner Shards
                            .add(
                                    LootItem.lootTableItem(
                                                    ModItems.SPAWNER_SHARD
                                            )
                                            .apply(
                                                    SetItemCountFunction.setCount(
                                                            new UniformGenerator(
                                                                    ConstantValue.exactly(1),
                                                                    ConstantValue.exactly(2)
                                                            )
                                                    )
                                            )

                                            // Tylko żelazny, diamentowy
                                            // lub netheritowy kilof
                                            .when(
                                                    MatchTool.toolMatches(
                                                            ItemPredicate.Builder
                                                                    .item()
                                                                    .of(
                                                                            registries.lookupOrThrow(
                                                                                    Registries.ITEM
                                                                            ),
                                                                            Items.IRON_PICKAXE,
                                                                            Items.DIAMOND_PICKAXE,
                                                                            Items.NETHERITE_PICKAXE
                                                                    )
                                                    )
                                            )
                            );

                    tableBuilder.withPool(pool);
                }
        );
    }
}