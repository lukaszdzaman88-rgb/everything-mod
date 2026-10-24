package net.hehex.everythingmod.grindstone;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Rejestr własnego menu Grindstone'a (zastępuje vanillowe po prawym kliknięciu na blok). */
public final class GrindstoneRegistry {

    private GrindstoneRegistry() {
    }

    public static final Identifier ID = Identifier.fromNamespaceAndPath("everything-mod", "grindstone");

    public static final Component TITLE = Component.translatable("container.everything_mod.grindstone");

    public static final MenuType<EnchantGrindstoneMenu> MENU = Registry.register(
            BuiltInRegistries.MENU,
            ID,
            new MenuType<>(EnchantGrindstoneMenu::new, FeatureFlags.VANILLA_SET)
    );

    /** Wywołanie wymusza załadowanie klasy (a więc rejestrację) w odpowiednim momencie. */
    public static void init() {
    }
}
