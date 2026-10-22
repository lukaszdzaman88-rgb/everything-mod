package net.hehex.everythingmod.grindstone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.GrindstoneMenu;

/**
 * Rejestracja pakietu zaznaczenia i jego odbiornika na serwerze.
 * Uruchamiany jako osobny entrypoint "main" (po obu stronach: klient i serwer).
 */
public class GrindstoneNetworking implements ModInitializer {

    @Override
    public void onInitialize() {

        PayloadTypeRegistry.serverboundPlay().register(
                GrindstoneSelectPayload.TYPE,
                GrindstoneSelectPayload.CODEC
        );

        // Odbiornik działa na wątku serwera (gwarantuje to Fabric API).
        ServerPlayNetworking.registerGlobalReceiver(
                GrindstoneSelectPayload.TYPE,
                (payload, context) -> {

                    ServerPlayer player = context.player();

                    if (!(player.containerMenu instanceof GrindstoneMenu menu)) {
                        debug("odrzucono: gracz nie ma otwartego Grindstone'a");
                        return;
                    }

                    if (player.isSpectator() || !menu.stillValid(player)) {
                        debug("odrzucono: menu nieważne albo spectator");
                        return;
                    }

                    boolean accepted = ((GrindstoneSelection) (Object) menu)
                            .everything$applyRemoteSelection(payload.mask(), payload.signature());

                    debug("odebrano maska=" + payload.mask()
                            + " podpis=" + payload.signature()
                            + " -> " + (accepted ? "ZAAKCEPTOWANO" : "odrzucono (nieaktualny podpis / brak enchantów)"));
                }
        );
    }

    private static void debug(String message) {
        GrindstoneEnchants.debug("[Grindstone][serwer] {}", message);
    }
}
