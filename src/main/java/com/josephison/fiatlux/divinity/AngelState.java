package com.josephison.fiatlux.divinity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * The inner life of one of the Host, carried as a data attachment: pride ("Thou wast perfect in
 * thy ways ... till iniquity was found in thee", Ezek 28:15; "thine heart was lifted up because of
 * thy beauty", 28:17) and loyalty to the throne. Pride grows with the moral climate of creation;
 * when an angel's pride overtakes its loyalty as war breaks out, it joins the rebellion.
 */
public record AngelState(float pride, float loyalty) {
    /** Not yet rolled: a freshly created being draws its temperament on its first tick. */
    public static final AngelState UNSET = new AngelState(-1f, -1f);
    public static final Codec<AngelState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("pride", -1f).forGetter(AngelState::pride),
            Codec.FLOAT.optionalFieldOf("loyalty", -1f).forGetter(AngelState::loyalty)
    ).apply(i, AngelState::new));

    public boolean isSet() {
        return pride >= 0;
    }

    public static AngelState roll(RandomSource random) {
        return new AngelState(random.nextFloat() * 0.35f, 0.6f + random.nextFloat() * 0.4f);
    }

    /** Pride drifts toward the climate of the cosmos; loyalty erodes as pride grows. */
    public AngelState drift(double cosmicPride, double rate) {
        float p = (float) Math.min(1.0, pride + (cosmicPride - pride) * rate);
        float l = (float) Math.max(0.0, Math.min(1.0, loyalty - (p - 0.5f) * rate * 0.5));
        return new AngelState(p, l);
    }

    /** Would this angel take up arms against the throne in a war of the given cosmic pride? */
    public boolean wouldRebel(double cosmicPride) {
        return pride + cosmicPride * 0.5 > loyalty;
    }

    public AngelState asRebel() {
        return new AngelState(Math.max(pride, 0.9f), 0f);
    }
}
