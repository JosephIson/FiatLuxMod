package com.josephison.fiatlux.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Dynamic traits of a divine or infernal weapon, stored as an item data component (never raw NBT).
 *
 * @param smitten  corrupt beings this weapon has struck down
 * @param kindled  holy-fire level, 0..5; grows with use and strengthens the fire wave
 */
public record WeaponTraits(int smitten, int kindled) {
    public static final WeaponTraits FRESH = new WeaponTraits(0, 1);
    public static final Codec<WeaponTraits> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("smitten", 0).forGetter(WeaponTraits::smitten),
            Codec.INT.optionalFieldOf("kindled", 1).forGetter(WeaponTraits::kindled)
    ).apply(i, WeaponTraits::new));
    public static final StreamCodec<ByteBuf, WeaponTraits> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WeaponTraits::smitten,
            ByteBufCodecs.VAR_INT, WeaponTraits::kindled,
            WeaponTraits::new);

    public WeaponTraits recordSmite() {
        int s = smitten + 1;
        return new WeaponTraits(s, Math.min(5, 1 + s / 10));
    }
}
