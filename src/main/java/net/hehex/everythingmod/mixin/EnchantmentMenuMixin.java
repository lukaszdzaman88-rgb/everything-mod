package net.hehex.everythingmod.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Unique
    private static final int REROLL_BUTTON_ID = 3;
    @Unique
    private static final int REROLL_LAPIS_COST = 3;

    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private DataSlot enchantmentSeed;

    @Inject(
            method = "clickMenuButton",
            at = @At("HEAD"),
            cancellable = true
    )
    private void everythingMod$reroll(
            Player player,
            int id,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (id != REROLL_BUTTON_ID) {
            return;
        }

        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;

        // Musi być przedmiot do enchantowania.
        if (menu.getSlot(0).getItem().isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        // Przedmiot musi mieć jakiekolwiek oferty (inaczej nie zabieramy lapisu na darmo).
        if (menu.costs[0] <= 0 && menu.costs[1] <= 0 && menu.costs[2] <= 0) {
            cir.setReturnValue(false);
            return;
        }

        // Muszą być co najmniej 3 lapisy.
        if (menu.getSlot(1).getItem().getCount() < REROLL_LAPIS_COST) {
            cir.setReturnValue(false);
            return;
        }

        // Zabierz 3 lapisy (tryb kreatywny, tak jak w vanilla, nic nie kosztuje).
        if (!player.hasInfiniteMaterials()) {
            menu.getSlot(1).remove(REROLL_LAPIS_COST);
        }

        // Wylosuj nowe ziarno enchantingu.
        enchantmentSeed.set(RandomSource.create().nextInt());

        // Ponownie wygeneruj oferty (serwer wyśle je klientowi przy broadcastChanges).
        menu.slotsChanged(enchantSlots);

        cir.setReturnValue(true);
    }
}
