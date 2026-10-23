package net.hehex.everythingmod.defusion;

/**
 * Wszystkie liczby Defusion Table w jednym miejscu - tu zmieniasz koszt i progi.
 */
public final class DefusionConfig {

    private DefusionConfig() {
    }

    /** Bazowy koszt jednego "defusion" w poziomach doświadczenia. */
    public static final int BASE_LEVEL_COST = 7;

    /** Minimalna liczba Runic Bookshelf wokół stołu, żeby w ogóle działał. */
    public static final int MIN_SHELVES = 5;

    /** Od tylu regałów koszt jest niższy o TIER1_DISCOUNT_PERCENT %. */
    public static final int TIER1_SHELVES = 10;
    public static final int TIER1_DISCOUNT_PERCENT = 10;

    /** Od tylu regałów koszt jest niższy o TIER2_DISCOUNT_PERCENT %. */
    public static final int TIER2_SHELVES = 15;
    public static final int TIER2_DISCOUNT_PERCENT = 20;

    /** Wysokość dźwięku stołu do enchantowania (vanilla gra ~0.9-1.0, tu niżej = głębiej). */
    public static final float SOUND_PITCH = 0.55F;

    /** Zniżka w % dla danej liczby regałów. */
    public static int discountPercent(int shelves) {
        if (shelves >= TIER2_SHELVES) {
            return TIER2_DISCOUNT_PERCENT;
        }

        if (shelves >= TIER1_SHELVES) {
            return TIER1_DISCOUNT_PERCENT;
        }

        return 0;
    }

    /**
     * Koszt w poziomach dla danej liczby regałów (zaokrąglany w dół, min. 1).
     * Przy bazie 7: 7 / 6 (-10%) / 5 (-20%).
     */
    public static int levelCost(int shelves) {
        return Math.max(1, BASE_LEVEL_COST * (100 - discountPercent(shelves)) / 100);
    }
}
