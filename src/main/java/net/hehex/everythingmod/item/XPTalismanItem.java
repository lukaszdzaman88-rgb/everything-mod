package net.hehex.everythingmod.item;

import net.hehex.everythingmod.component.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Properties;
import java.util.function.Consumer;

public class XPTalismanItem extends Item {

    public XPTalismanItem(Properties properties) {
        super(properties);
    }

    // ==========================================
    // PPM - zabieranie / oddawanie XP
    // ==========================================

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        // Logikę wykonujemy tylko na serwerze.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int storedXp = stack.getOrDefault(
                ModComponents.STORED_XP,
                0
        );

        // ==========================================
        // ODDAJ XP Z TALIZMANU
        // ==========================================

        if (storedXp > 0) {

            player.giveExperiencePoints(storedXp);

            stack.set(
                    ModComponents.STORED_XP,
                    0
            );

            return InteractionResult.SUCCESS;
        }

        // ==========================================
        // ZABIERZ XP OD GRACZA
        // ==========================================

        int currentXp = getCurrentXp(player);

        if (currentXp > 0) {

            stack.set(
                    ModComponents.STORED_XP,
                    currentXp
            );

            player.giveExperiencePoints(-currentXp);
        }

        return InteractionResult.SUCCESS;
    }

    // ==========================================
    // TOOLTIP
    // ==========================================

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> textConsumer,
            TooltipFlag type
    ) {
        int storedXp = stack.getOrDefault(
                ModComponents.STORED_XP,
                0
        );

        textConsumer.accept(
                Component.translatable(
                        "tooltip.xptalisman.stored_xp",
                        storedXp
                ).withStyle(ChatFormatting.GREEN)
        );
    }

    // ==========================================
    // OBLICZANIE CAŁKOWITEGO XP
    // ==========================================

    private static int getCurrentXp(Player player) {

        int level = player.experienceLevel;

        int xpForLevel;

        if (level <= 16) {

            xpForLevel =
                    level * level
                            + 6 * level;

        } else if (level <= 31) {

            xpForLevel = Mth.floor(
                    2.5F * level * level
                            - 40.5F * level
                            + 360.0F
            );

        } else {

            xpForLevel = Mth.floor(
                    4.5F * level * level
                            - 162.5F * level
                            + 2220.0F
            );
        }

        int progressXp = Mth.floor(
                player.experienceProgress
                        * player.getXpNeededForNextLevel()
        );

        return xpForLevel + progressXp;
    }
}
