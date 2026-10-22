package net.hehex.everythingmod.mixin;

import java.util.List;

import net.hehex.everythingmod.grindstone.GrindstoneEnchants;
import net.hehex.everythingmod.grindstone.GrindstoneResultSlot;
import net.hehex.everythingmod.grindstone.GrindstoneSelection;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Selektywne odczarowywanie w Grindstone:
 *  - zaznaczenie (maska bitowa) trzymane po obu stronach; klient wysyła je pakietem,
 *  - wynik = item bez zaznaczonych enchantów, -10% wytrzymałości,
 *  - slot wyniku podmieniony na własny (XP tylko za usunięte enchanty).
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu implements GrindstoneSelection {

    @Shadow
    @Final
    private Container repairSlots;

    @Shadow
    @Final
    private Container resultSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Unique
    private int everything$selectionMask = 0;

    @Unique
    private String everything$signature = "";

    @Unique
    private boolean everything$initDone = false;

    // Konstruktor wymagany tylko po to, żeby klasa się skompilowała (Mixin go odrzuca).
    protected GrindstoneMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    /*
     * Wpinamy się w KAŻDY konstruktor (GrindstoneMenu ma dwa: 2-argumentowy
     * deleguje do 3-argumentowego), dlatego strażnik "już zrobione".
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void everything$init(CallbackInfo ci) {
        if (this.everything$initDone) {
            return;
        }

        this.everything$initDone = true;

        // Podmień vanillowy slot wyniku (indeks 2) na nasz.
        Slot vanilla = this.slots.get(2);

        GrindstoneResultSlot replacement = new GrindstoneResultSlot(
                this,
                this.resultSlots,
                this.repairSlots,
                this.access,
                vanilla.x,
                vanilla.y
        );

        replacement.index = vanilla.index;
        this.slots.set(2, replacement);
    }

    /*
     * Dla jednego odczarowywanego itemu liczymy wynik sami.
     * Dla pozostałych przypadków (np. łączenie dwóch itemów) działa vanilla.
     */
    @Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
    private void everything$computeResult(
            ItemStack input,
            ItemStack additional,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack single = GrindstoneEnchants.singleInput(input, additional);

        // Inny item / inny zestaw enchantów => zaznaczenie od zera.
        String signature = GrindstoneEnchants.signature(single);

        if (!signature.equals(this.everything$signature)) {
            this.everything$signature = signature;
            this.everything$selectionMask = 0;
        }

        if (single.isEmpty()) {
            return;
        }

        ItemStack result = GrindstoneEnchants.buildResult(single, this.everything$selectionMask);

        if (!this.everything$isClient()) {
            GrindstoneEnchants.debug(
                    "[Grindstone][serwer] wynik: maska={} -> {}",
                    this.everything$selectionMask,
                    result.isEmpty() ? "pusty" : result
            );
        }

        cir.setReturnValue(result);
    }

    // ------------------------------------------------ GrindstoneSelection

    @Override
    public List<GrindstoneEnchants.Entry> everything$getEntries() {
        ItemStack single = GrindstoneEnchants.singleInput(
                this.repairSlots.getItem(0),
                this.repairSlots.getItem(1)
        );

        return single.isEmpty() ? List.of() : GrindstoneEnchants.entriesOf(single);
    }

    @Override
    public int everything$getSelectionMask() {
        return this.everything$selectionMask;
    }

    @Override
    public int everything$getSignatureHash() {
        return GrindstoneEnchants.signatureHash(GrindstoneEnchants.singleInput(
                this.repairSlots.getItem(0),
                this.repairSlots.getItem(1)
        ));
    }

    @Override
    public void everything$setSelectionMask(int mask) {
        List<GrindstoneEnchants.Entry> entries = this.everything$getEntries();

        this.everything$selectionMask = mask & GrindstoneEnchants.removableMask(entries);

        // Natychmiastowy podgląd wyniku (serwer i tak przyśle swój).
        this.slotsChanged(this.repairSlots);
    }

    @Override
    public boolean everything$applyRemoteSelection(int mask, int signatureHash) {
        List<GrindstoneEnchants.Entry> entries = this.everything$getEntries();

        if (entries.isEmpty() || signatureHash != this.everything$getSignatureHash()) {
            return false;
        }

        this.everything$selectionMask = mask & GrindstoneEnchants.removableMask(entries);

        // Przelicz wynik (createResult -> computeResult -> broadcastChanges).
        this.slotsChanged(this.repairSlots);

        return true;
    }

    @Override
    public int everything$getRawExperience() {
        ItemStack top = this.repairSlots.getItem(0);
        ItemStack bottom = this.repairSlots.getItem(1);

        ItemStack single = GrindstoneEnchants.singleInput(top, bottom);

        if (!single.isEmpty()) {
            // Odczarowanie: XP tylko za zaznaczone enchanty.
            return GrindstoneEnchants.selectedCost(
                    GrindstoneEnchants.entriesOf(single),
                    this.everything$selectionMask
            );
        }

        // Łączenie dwóch itemów: jak w vanilla (wszystkie enchanty oprócz klątw).
        return GrindstoneEnchants.nonCurseCost(top) + GrindstoneEnchants.nonCurseCost(bottom);
    }

    /** Klient ma ContainerLevelAccess.NULL, serwer prawdziwy. */
    @Unique
    private boolean everything$isClient() {
        return this.access == ContainerLevelAccess.NULL;
    }
}
