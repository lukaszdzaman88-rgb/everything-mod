package net.hehex.everythingmod;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.hehex.everythingmod.datagen.ModBlockTagsProvider;
import net.hehex.everythingmod.datagen.ModModelProvider;
import net.hehex.everythingmod.datagen.ModRecipeProvider;

public class EverythingModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		var pack = fabricDataGenerator.createPack();



		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModBlockTagsProvider::new);
		pack.addProvider(ModRecipeProvider::new);

	}
}
