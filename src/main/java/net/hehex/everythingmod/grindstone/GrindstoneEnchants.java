package net.hehex.everythingmod.grindstone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wspólna logika (klient + serwer) dla ulepszonego Grindstone'a.
 *
 * WAŻNE: klient i serwer MUSZĄ widzieć enchanty w tej samej kolejności,
 * bo zaznaczenie jest przesyłane jako maska bitowa (bit i = i-ty enchant).
 * Dlatego lista jest sortowana po kluczu rejestru (a nie po kolejności
 * z mapy, która zależy od hashy i może być inna u klienta i serwera).
 */
public final class GrindstoneEnchants {

    private GrindstoneEnchants() {
    }

    /**
     * Czy klątwy też można usuwać? W vanilla Grindstone zostawia klątwy,
     * więc domyślnie false. Zmień na true, jeśli chcesz je usuwać.
     */
    public static final boolean ALLOW_CURSE_REMOVAL = false;

    /** Maska idzie w jednym DataSlot (16 bitów ze znakiem) => max 15 enchantów. */
    public static final int MAX_ENTRIES = 15;

    /**
     * Logowanie diagnostyczne (klik klienta, odbiór na serwerze, budowa wyniku).
     * Po sprawdzeniu, że wszystko działa, możesz ustawić na false.
     */
    public static final boolean DEBUG_LOG = true;

    private static final Logger LOGGER = LoggerFactory.getLogger("everything-mod/grindstone");

    /** Log diagnostyczny (tylko gdy DEBUG_LOG = true). Format jak w slf4j: "{}" */
    public static void debug(String message, Object... args) {
        if (DEBUG_LOG) {
            LOGGER.info(message, args);
        }
    }

    /** Ile % maksymalnej wytrzymałości zabiera odczarowanie. */
    public static final int DURABILITY_PENALTY_PERCENT = 10;

    public record Entry(Holder<Enchantment> holder, int level, boolean curse) {

        public boolean removable() {
            return ALLOW_CURSE_REMOVAL || !curse;
        }
    }

    private static DataComponentType<ItemEnchantments> componentFor(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS
                : DataComponents.ENCHANTMENTS;
    }

    private static String sortKey(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(Object::toString).orElse("");
    }

    /** Wszystkie enchanty itemu (bez limitu), posortowane deterministycznie. */
    public static List<Entry> allEntries(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        ItemEnchantments enchantments =
                stack.getOrDefault(componentFor(stack), ItemEnchantments.EMPTY);

        List<Entry> entries = new ArrayList<>();

        for (Holder<Enchantment> holder : enchantments.keySet()) {
            entries.add(new Entry(
                    holder,
                    enchantments.getLevel(holder),
                    holder.is(EnchantmentTags.CURSE)
            ));
        }

        entries.sort(Comparator.comparing((Entry entry) -> sortKey(entry.holder())));

        return entries;
    }

    /** Enchanty widoczne w GUI (max MAX_ENTRIES). */
    public static List<Entry> entriesOf(ItemStack stack) {
        List<Entry> entries = allEntries(stack);

        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }

        return entries;
    }

    /**
     * Tryb "jeden przedmiot do odczarowania": dokładnie jeden z dwóch slotów
     * ma przedmiot (1 szt.) i ma on jakieś enchanty. Inaczej zwraca EMPTY
     * i działa zwykła logika vanilla (np. łączenie dwóch przedmiotów).
     */
    public static ItemStack singleInput(ItemStack top, ItemStack bottom) {
        boolean hasTop = !top.isEmpty();
        boolean hasBottom = !bottom.isEmpty();

        if (hasTop == hasBottom) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = hasTop ? top : bottom;

        if (stack.getCount() != 1 || allEntries(stack).isEmpty()) {
            return ItemStack.EMPTY;
        }

        return stack;
    }

    /** Podpis zestawu enchantów - gdy się zmieni, zaznaczenie jest zerowane. */
    public static String signature(ItemStack single) {
        if (single.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Entry entry : allEntries(single)) {
            builder.append(sortKey(entry.holder()))
                    .append('=')
                    .append(entry.level())
                    .append(';');
        }

        return builder.toString();
    }

    /**
     * Skrót podpisu - klient wysyła go razem z zaznaczeniem, a serwer odrzuca
     * zaznaczenie, jeśli item w międzyczasie się zmienił (opóźnienie sieci).
     * String.hashCode() jest ściśle zdefiniowany, więc jest taki sam po obu stronach.
     */
    public static int signatureHash(ItemStack single) {
        return signature(single).hashCode();
    }

    /** Maska wszystkich enchantów, które wolno usunąć. */
    public static int removableMask(List<Entry> entries) {
        int mask = 0;

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).removable()) {
                mask |= 1 << i;
            }
        }

        return mask;
    }

    /**
     * Buduje wynik: kopia itemu bez zaznaczonych enchantów + kara wytrzymałości.
     * Zwraca EMPTY, jeśli nic nie zaznaczono.
     */
    public static ItemStack buildResult(ItemStack input, int selectionMask) {
        List<Entry> entries = entriesOf(input);
        int mask = selectionMask & removableMask(entries);

        if (mask == 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = input.copy();
        DataComponentType<ItemEnchantments> type = componentFor(result);

        ItemEnchantments.Mutable mutable =
                new ItemEnchantments.Mutable(result.getOrDefault(type, ItemEnchantments.EMPTY));

        for (int i = 0; i < entries.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                mutable.set(entries.get(i).holder(), 0); // poziom 0 = usuń
            }
        }

        ItemEnchantments remaining = mutable.toImmutable();
        result.set(type, remaining);

        // Książka bez enchantów wraca do zwykłej książki (jak w vanilla).
        if (result.is(Items.ENCHANTED_BOOK) && remaining.size() == 0) {
            result = result.transmuteCopy(Items.BOOK);
        }

        // Koszt naprawy w kowadle - tak jak w vanilla (zależy od liczby pozostałych enchantów).
        int repairCost = 0;
        for (int i = 0; i < remaining.size(); i++) {
            repairCost = (int) Math.min(repairCost * 2L + 1L, Integer.MAX_VALUE);
        }
        result.set(DataComponents.REPAIR_COST, repairCost);

        applyDurabilityPenalty(result);

        return result;
    }

    /**
     * Zabiera 10% maksymalnej wytrzymałości. Jeśli zostało mniej (lub tyle samo),
     * item zostaje na 1 wytrzymałości - nigdy się nie niszczy.
     */
    private static void applyDurabilityPenalty(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }

        int max = stack.getMaxDamage();
        int penalty = Math.max(1, Math.round(max * DURABILITY_PENALTY_PERCENT / 100f));
        int left = max - stack.getDamageValue();
        int newLeft = left > penalty ? left - penalty : 1;

        stack.setDamageValue(max - newLeft);
    }

    // ------------------------------------------------------------------ XP

    private static int minCost(Entry entry) {
        return entry.holder().value().definition().minCost().calculate(entry.level());
    }

    /** Suma "kosztów" zaznaczonych (usuwanych) enchantów. */
    public static int selectedCost(List<Entry> entries, int selectionMask) {
        int mask = selectionMask & removableMask(entries);
        int total = 0;

        for (int i = 0; i < entries.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                total += minCost(entries.get(i));
            }
        }

        return total;
    }

    /** Vanilla: suma kosztów wszystkich enchantów oprócz klątw (tryb łączenia dwóch itemów). */
    public static int nonCurseCost(ItemStack stack) {
        int total = 0;

        for (Entry entry : allEntries(stack)) {
            if (!entry.curse()) {
                total += minCost(entry);
            }
        }

        return total;
    }

    /** Ten sam wzór co vanilla: połowa kosztu (w górę) + losowa wartość z tego zakresu. */
    public static int finalExperience(int totalCost, RandomSource random) {
        if (totalCost <= 0) {
            return 0;
        }

        int half = (int) Math.ceil(totalCost / 2.0);

        return half + random.nextInt(half);
    }
}
