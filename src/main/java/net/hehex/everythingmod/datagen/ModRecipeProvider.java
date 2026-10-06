package net.hehex.everythingmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.hehex.everythingmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                shaped(RecipeCategory.TOOLS, ModItems.XP_TALISMAN)
                        .pattern(" G ")
                        .pattern("GEG")
                        .pattern(" G ")
                        .define('G', Items.GOLD_INGOT)
                        .define('E', Items.EMERALD)
                        .unlockedBy(getHasName(Items.GOLD_INGOT), has(Items.GOLD_INGOT))
                        .save(output);
                shaped(RecipeCategory.MISC, Items.SPAWNER)
                        .pattern(" S ")
                        .pattern("S S")
                        .pattern(" S ")
                        .define('S', ModItems.SPAWNER_SHARD)
                        .unlockedBy(
                                getHasName(ModItems.SPAWNER_SHARD),
                                has(ModItems.SPAWNER_SHARD))
                        .save(output);







            }};}













    @Override
    public String getName() {
        return "EverythingMod Recipies";
    }
}
