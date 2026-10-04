package net.hehex.everythingmod.component;

import com.mojang.serialization.Codec;
import net.hehex.everythingmod.EverythingMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public class ModComponents {

    public static DataComponentType<Integer> STORED_XP;

    public static void initialize() {

        STORED_XP = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(
                        EverythingMod.MOD_ID,
                        "stored_xp"
                ),
                DataComponentType.<Integer>builder()
                        .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                        .build()
        );
    }
}

