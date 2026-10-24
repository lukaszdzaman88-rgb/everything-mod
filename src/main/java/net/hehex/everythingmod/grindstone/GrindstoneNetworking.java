package net.hehex.everythingmod.grindstone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Blocks;

/**
 * Wejście (main) Grindstone'a:
 *  - rejestracja własnego menu,
 *  - przejęcie prawego kliknięcia na Grindstone (otwiera nasze menu zamiast vanillowego),
 *  - pakiet z zaznaczeniem enchantów i jego odbiornik na serwerze.
 */
public class GrindstoneNetworking implements ModInitializer {

    @Override
    public void onInitialize() {

        GrindstoneRegistry.init();

        /*
         * Zachowujemy zasady vanilla: Shift + coś w ręce = zwykłe użycie przedmiotu
         * (np. postawienie bloku), a nie otwarcie menu. Bez eventu Fabric nie musimy
         * nic wstrzykiwać w kod bloku.
         */
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {

            if (hand != InteractionHand.MAIN_HAND
                    || !level.getBlockState(hitResult.getBlockPos()).is(Blocks.GRINDSTONE)) {
                return InteractionResult.PASS;
            }

            boolean holdingItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();

            if (player.isSecondaryUseActive() && holdingItem) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> new EnchantGrindstoneMenu(
                                containerId,
                                inventory,
                                ContainerLevelAccess.create(level, hitResult.getBlockPos())
                        ),
                        GrindstoneRegistry.TITLE
                ));
            }

            return InteractionResult.SUCCESS;
        });

        PayloadTypeRegistry.serverboundPlay().register(
                GrindstoneSelectPayload.TYPE,
                GrindstoneSelectPayload.CODEC
        );

        // Odbiornik działa na wątku serwera (gwarantuje to Fabric API).
        ServerPlayNetworking.registerGlobalReceiver(
                GrindstoneSelectPayload.TYPE,
                (payload, context) -> {

                    ServerPlayer player = context.player();

                    if (!(player.containerMenu instanceof EnchantGrindstoneMenu menu)) {
                        debug("odrzucono: gracz nie ma otwartego Grindstone'a");
                        return;
                    }

                    if (player.isSpectator() || !menu.stillValid(player)) {
                        debug("odrzucono: menu nieważne albo spectator");
                        return;
                    }

                    boolean accepted = menu.applyRemoteSelection(payload.mask(), payload.signature());

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
