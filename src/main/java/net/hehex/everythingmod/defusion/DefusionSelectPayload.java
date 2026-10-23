package net.hehex.everythingmod.defusion;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Pakiet klient -> serwer: "wybrałem zaklęcie o tym indeksie" (-1 = brak wyboru).
 *
 * @param index     indeks zaklęcia na liście
 * @param signature skrót podpisu przedmiotu, którego dotyczy wybór
 */
public record DefusionSelectPayload(int index, int signature) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DefusionSelectPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("everything-mod", "defusion_select")
            );

    public static final StreamCodec<FriendlyByteBuf, DefusionSelectPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DefusionSelectPayload::index,
                    ByteBufCodecs.INT, DefusionSelectPayload::signature,
                    DefusionSelectPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
