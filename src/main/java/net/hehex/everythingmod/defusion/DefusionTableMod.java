package net.hehex.everythingmod.defusion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;

/** Wejście (main) Defusion Table: rejestracje, zakładka kreatywna i sieć. */
public class DefusionTableMod implements ModInitializer {

    @Override
    public void onInitialize() {

        DefusionTable.init();  // blok + przedmiot + typ menu
        RunicBookshelf.init(); // runiczny regał

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> {
                    entries.accept(DefusionTable.ITEM);
                    entries.accept(RunicBookshelf.ITEM);
                });

        PayloadTypeRegistry.serverboundPlay().register(
                DefusionSelectPayload.TYPE,
                DefusionSelectPayload.CODEC
        );

        // Odbiornik działa na wątku serwera (gwarantuje to Fabric API).
        ServerPlayNetworking.registerGlobalReceiver(
                DefusionSelectPayload.TYPE,
                (payload, context) -> {

                    ServerPlayer player = context.player();

                    if (!(player.containerMenu instanceof DefusionTableMenu menu)
                            || player.isSpectator()
                            || !menu.stillValid(player)) {
                        GrindstoneEnchants.debug("[Defusion][serwer] odrzucono: brak ważnego menu");
                        return;
                    }

                    boolean accepted = menu.applyRemoteSelection(payload.index(), payload.signature());

                    GrindstoneEnchants.debug(
                            "[Defusion][serwer] odebrano indeks={} podpis={} -> {}",
                            payload.index(), payload.signature(),
                            accepted ? "ZAAKCEPTOWANO" : "odrzucono (nieaktualny podpis)"
                    );
                }
        );
    }
}
