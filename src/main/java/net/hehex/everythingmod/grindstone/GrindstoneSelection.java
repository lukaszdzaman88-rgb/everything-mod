package net.hehex.everythingmod.grindstone;

import java.util.List;

/**
 * Interfejs doklejany do GrindstoneMenu przez mixin.
 * Dzięki niemu klient (ekran), sieć i slot wyniku mogą czytać/zmieniać zaznaczenie.
 *
 * Zaznaczenie jest trzymane po OBU stronach (klient i serwer) jako maska bitowa:
 * bit i = i-ty enchant z {@link #everything$getEntries()}. Klient zmienia je lokalnie
 * i wysyła do serwera pakiet {@link GrindstoneSelectPayload}.
 */
public interface GrindstoneSelection {

    /** Enchanty itemu w trybie odczarowania (pusta lista = brak panelu). */
    List<GrindstoneEnchants.Entry> everything$getEntries();

    /** Aktualna maska zaznaczenia. */
    int everything$getSelectionMask();

    /** Podpis zestawu enchantów (do odrzucania nieaktualnych kliknięć). */
    int everything$getSignatureHash();

    /** KLIENT: ustaw zaznaczenie lokalnie i przelicz podgląd wyniku. */
    void everything$setSelectionMask(int mask);

    /**
     * SERWER: przyjmij zaznaczenie od klienta.
     *
     * @return true, jeśli zaakceptowano
     */
    boolean everything$applyRemoteSelection(int mask, int signatureHash);

    /** Suma kosztów enchantów, za które należy się XP (przed losowaniem). */
    int everything$getRawExperience();
}
