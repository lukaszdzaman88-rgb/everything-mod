package net.hehex.everythingmod.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("imageHeight")
    @Mutable
    void everything$setImageHeight(int height);

    @Accessor("inventoryLabelY")
    void everything$setInventoryLabelY(int y);

    @Accessor("topPos")
    @Mutable
    void everything$setTopPos(int top);
}