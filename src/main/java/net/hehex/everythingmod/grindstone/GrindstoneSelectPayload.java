package net.hehex.everythingmod.grindstone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Pakiet klient -> serwer: "zaznaczyłem takie enchanty w Grindstone".
 *
 * @param mask      maska bitowa zaznaczonych enchantów
 * @param signature skrót podpisu itemu, którego dotyczy zaznaczenie
 */
public record GrindstoneSelectPayload(int mask, int signature) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GrindstoneSelectPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("everything-mod", "grindstone_select")
            );

    public static final StreamCodec<FriendlyByteBuf, GrindstoneSelectPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GrindstoneSelectPayload::mask,
                    ByteBufCodecs.INT, GrindstoneSelectPayload::signature,
                    GrindstoneSelectPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
