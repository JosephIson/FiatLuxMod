package com.josephison.fiatlux.divinity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-player divine state, stored as a NeoForge data attachment (copied on death).
 *
 * @param essence      Divine Essence available to spend on powers
 * @param faith        faith offered to this Creator by mortals (feeds Glory)
 * @param corruption   0..1, rises with destructive or infernal acts; tints the HUD and powers
 * @param pilgrim      true when this player is a Mortal Pilgrim and cannot wield divine power
 * @param visitedHeaven whether the player has been brought to Heaven on first join
 * @param selectedPower the power cast by the scepter / cast key
 */
public record DivineState(float essence, float faith, float corruption, boolean pilgrim, boolean visitedHeaven,
                          ResourceLocation selectedPower) {
    public static final ResourceLocation DEFAULT_POWER = ResourceLocation.fromNamespaceAndPath("fiatlux", "fiat_lux");
    public static final DivineState DEFAULT = new DivineState(1000f, 0f, 0f, false, false, DEFAULT_POWER);

    public static final Codec<DivineState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("essence", 1000f).forGetter(DivineState::essence),
            Codec.FLOAT.optionalFieldOf("faith", 0f).forGetter(DivineState::faith),
            Codec.FLOAT.optionalFieldOf("corruption", 0f).forGetter(DivineState::corruption),
            Codec.BOOL.optionalFieldOf("pilgrim", false).forGetter(DivineState::pilgrim),
            Codec.BOOL.optionalFieldOf("visited_heaven", false).forGetter(DivineState::visitedHeaven),
            ResourceLocation.CODEC.optionalFieldOf("selected_power", DEFAULT_POWER).forGetter(DivineState::selectedPower)
    ).apply(i, DivineState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DivineState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DivineState::essence,
            ByteBufCodecs.FLOAT, DivineState::faith,
            ByteBufCodecs.FLOAT, DivineState::corruption,
            ByteBufCodecs.BOOL, DivineState::pilgrim,
            ByteBufCodecs.BOOL, DivineState::visitedHeaven,
            ResourceLocation.STREAM_CODEC, DivineState::selectedPower,
            DivineState::new);

    public DivineState withEssence(float v) {
        return new DivineState(v, faith, corruption, pilgrim, visitedHeaven, selectedPower);
    }

    public DivineState withFaith(float v) {
        return new DivineState(essence, v, corruption, pilgrim, visitedHeaven, selectedPower);
    }

    public DivineState withPilgrim(boolean v) {
        return new DivineState(essence, faith, corruption, v, visitedHeaven, selectedPower);
    }

    public DivineState withVisitedHeaven(boolean v) {
        return new DivineState(essence, faith, corruption, pilgrim, v, selectedPower);
    }

    public DivineState withSelectedPower(ResourceLocation v) {
        return new DivineState(essence, faith, corruption, pilgrim, visitedHeaven, v);
    }
}
