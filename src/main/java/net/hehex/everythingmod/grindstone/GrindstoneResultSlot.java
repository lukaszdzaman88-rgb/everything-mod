package net.hehex.everythingmod.grindstone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Slot wyniku Grindstone'a. Zachowuje się jak vanillowy, ale XP liczy
 * tylko za USUNIĘTE enchanty (vanilla liczyłaby za wszystkie).
 */
public class GrindstoneResultSlot extends Slot {

    private final GrindstoneSelection menu;
    private final Container inputSlots;
    private final ContainerLevelAccess access;

    public GrindstoneResultSlot(
            GrindstoneSelection menu,
            Container resultSlots,
            Container inputSlots,
            ContainerLevelAccess access,
            int x,
            int y
    ) {
        super(resultSlots, 2, x, y); // 2 - tak samo jak w vanilla
        this.menu = menu;
        this.inputSlots = inputSlots;
        this.access = access;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack carried) {
        // XP musi być policzone PRZED wyczyszczeniem slotów wejściowych.
        int rawExperience = this.menu.everything$getRawExperience();

        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                ExperienceOrb.award(
                        serverLevel,
                        Vec3.atCenterOf(pos),
                        GrindstoneEnchants.finalExperience(rawExperience, player.getRandom())
                );
            }

            level.levelEvent(1042, pos, 0); // dźwięk Grindstone'a
        });

        this.inputSlots.setItem(0, ItemStack.EMPTY);
        this.inputSlots.setItem(1, ItemStack.EMPTY);
    }
}
