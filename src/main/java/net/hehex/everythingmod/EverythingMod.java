package net.hehex.everythingmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.hehex.everythingmod.component.ModComponents;
import net.hehex.everythingmod.item.ModItems;
import net.hehex.everythingmod.loot.SpawnerLoot;
import net.hehex.everythingmod.loot.SpawnerSoulLoot;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EverythingMod implements ModInitializer {
	public static final String MOD_ID = "everything-mod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModItems.initialize();
		ModComponents.initialize();
		SpawnerLoot.initialize();
		SpawnerSoulLoot.initialize();

	}

}
