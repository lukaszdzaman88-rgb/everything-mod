package net.hehex.everythingmod.item;

import net.hehex.everythingmod.registries.SpawnerSoulRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerSoulItem extends Item {

    public SpawnerSoulItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Klient tylko wyświetla poprawną animację/interakcję.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Soul można użyć wyłącznie na zwykłym spawnerze.
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof SpawnerBlockEntity spawner)) {
            return InteractionResult.PASS;
        }

        // Sprawdź, jaki mob odpowiada tej duszy.
        var entityType = SpawnerSoulRegistry.getEntityType(stack);

        if (entityType == null) {
            return InteractionResult.PASS;
        }

        // Ustaw moba spawnera.
        spawner.setEntityId(
                entityType,
                RandomSource.create()
        );

        spawner.setChanged();

        // Natychmiastowa aktualizacja klienta.
        level.sendBlockUpdated(
                context.getClickedPos(),
                level.getBlockState(context.getClickedPos()),
                level.getBlockState(context.getClickedPos()),
                3
        );

        // Zużyj jedną duszę.
        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }
}
