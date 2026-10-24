package net.hehex.everythingmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.hehex.everythingmod.grindstone.GrindstoneRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** Rejestracja ekranu Grindstone'a po stronie klienta. */
public class GrindstoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(GrindstoneRegistry.MENU, EnchantGrindstoneScreen::new);
    }
}
