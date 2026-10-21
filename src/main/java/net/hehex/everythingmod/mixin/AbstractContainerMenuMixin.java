package net.hehex.everythingmod.mixin;

import net.hehex.everythingmod.grindstone.GrindstoneSelection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GrindstoneMenu sam nie nadpisuje clickMenuButton, więc nie da się wpiąć
 * bezpośrednio w niego. Wpinamy się w klasę bazową i reagujemy tylko wtedy,
 * gdy menu to Grindstone (czyli implementuje GrindstoneSelection).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void everything$grindstoneButton(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof GrindstoneSelection selection) {
            cir.setReturnValue(selection.everything$handleButton(buttonId));
        }
    }
}
