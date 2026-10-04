package net.hehex.everythingmod.item;

import net.hehex.everythingmod.component.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class XPTalismanItem extends Item {

    private static final int COOLDOWN_TICKS = 15;

    public XPTalismanItem(Properties properties) {
        super(properties);
    }

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

        // Cooldown
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS;
        }

        int storedXp = stack.getOrDefault(
                ModComponents.STORED_XP,
                0
        );

        // ==========================================
        // ODDAWANIE XP Z TALIZMANU
        // ==========================================

        if (storedXp > 0) {

            player.giveExperiencePoints(storedXp);

            // Usuwamy komponent.
            // Dzięki temu talizman wraca do tekstury inactive.
            stack.remove(ModComponents.STORED_XP);

            // Zużycie 1 durability.
            EquipmentSlot slot =
                    hand == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND;

            stack.hurtAndBreak(
                    1,
                    player,
                    slot
            );

            // 15 ticków cooldownu.
            player.getCooldowns().addCooldown(
                    stack,
                    COOLDOWN_TICKS
            );

            return InteractionResult.SUCCESS;
        }

        // ==========================================
        // ZABIERANIE XP OD GRACZA
        // ==========================================

        int currentXp = getCurrentXp(player);

        if (currentXp > 0) {

            // Zapisujemy XP w komponencie.
            // Od tego momentu model przełączy się na ACTIVE.
            stack.set(
                    ModComponents.STORED_XP,
                    currentXp
            );

            player.giveExperiencePoints(-currentXp);

            // Dźwięk podnoszenia XP.
            player.playSound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    1.0F,
                    1.0F
            );

            // Cooldown 15 ticków.
            player.getCooldowns().addCooldown(
                    stack,
                    COOLDOWN_TICKS
            );
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

        int storedLevel = getLevelFromXp(storedXp);

        textConsumer.accept(
                Component.translatable(
                        "tooltip.xptalisman.stored_xp",
                        storedLevel,
                        storedXp
                ).withStyle(ChatFormatting.GREEN)
        );
    }

    // ==========================================
    // CAŁKOWITE XP GRACZA
    // ==========================================

    private static int getCurrentXp(Player player) {

        int level = player.experienceLevel;

        long xpForLevel = getXpForLevel(level);

        long progressXp = Math.round(
                player.experienceProgress
                        * player.getXpNeededForNextLevel()
        );

        long totalXp = xpForLevel + progressXp;

        return (int) Math.min(
                totalXp,
                Integer.MAX_VALUE
        );
    }

    // ==========================================
    // XP POTRZEBNE DO OSIĄGNIĘCIA POZIOMU
    // ==========================================

    private static long getXpForLevel(int level) {

        if (level <= 16) {
            return (long) level * level
                    + 6L * level;
        }

        if (level <= 31) {
            return Math.round(
                    2.5D * level * level
                            - 40.5D * level
                            + 360.0D
            );
        }

        return Math.round(
                4.5D * level * level
                        - 162.5D * level
                        + 2220.0D
        );
    }

    // ==========================================
    // POZIOM ODPOWIADAJĄCY ILOŚCI XP
    // ==========================================

    private static int getLevelFromXp(int xp) {

        int low = 0;
        int high = 30000;

        while (low <= high) {

            int middle =
                    low + (high - low) / 2;

            long xpForLevel =
                    getXpForLevel(middle);

            if (xpForLevel <= xp) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        return high;
    }
}