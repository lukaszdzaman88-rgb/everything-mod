package net.hehex.everythingmod.grindstone;

import java.util.List;

/**
 * Interfejs doklejany do GrindstoneMenu przez mixin.
 * Dzięki niemu klient (ekran) i slot wyniku mogą czytać stan zaznaczenia.
 */
public interface GrindstoneSelection {

    /** Bit i = zaznaczony i-ty enchant z listy {@link #everything$getEntries()}. */
    int everything$getSelectionMask();

    /** Enchanty itemu w trybie odczarowania (pusta lista = brak panelu). */
    List<GrindstoneEnchants.Entry> everything$getEntries();

    /** Obsługa kliknięcia przycisku (przełącznik enchantu albo "Select all"). */
    boolean everything$handleButton(int buttonId);

    /** Suma kosztów enchantów, za które należy się XP (przed losowaniem). */
    int everything$getRawExperience();
}
