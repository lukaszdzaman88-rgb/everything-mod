package net.hehex.everythingmod.defusion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Blok Defusion Table - prawy klik otwiera menu (bez BlockEntity: sloty są chwilowe). */
public class DefusionTableBlock extends Block {

    public static final Component TITLE =
            Component.translatable("container.everything_mod.defusion_table");

    public DefusionTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) ->
                            new DefusionTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                    TITLE
            ));
        }

        return InteractionResult.SUCCESS;
    }
}
