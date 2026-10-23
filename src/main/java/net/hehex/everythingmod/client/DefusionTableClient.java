package net.hehex.everythingmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.hehex.everythingmod.defusion.DefusionTable;
import net.minecraft.client.gui.screens.MenuScreens;

/** Rejestracja ekranu Defusion Table po stronie klienta. */
public class DefusionTableClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(DefusionTable.MENU, DefusionTableScreen::new);
    }
}
