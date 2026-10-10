package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, FiatLux.MODID);

    /** Twinkling golden spark that rises and fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HOLY_SPARK = PARTICLES.register("holy_spark", () -> new SimpleParticleType(false));
    /** Animated holy fire tongue (8-frame sprite sheet). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIVINE_FLAME = PARTICLES.register("divine_flame", () -> new SimpleParticleType(false));
    /** Soft drifting mote of light - Heaven's ambient particle. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHT_MOTE = PARTICLES.register("light_mote", () -> new SimpleParticleType(true));
    /** A falling, swaying feather. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FEATHER = PARTICLES.register("feather", () -> new SimpleParticleType(false));
    /** A burning coal ember (Ezekiel 1:13). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER = PARTICLES.register("ember", () -> new SimpleParticleType(false));
    /** A large soft glow used for radiance bursts and auras. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RADIANCE = PARTICLES.register("radiance", () -> new SimpleParticleType(true));

    /** A tumbling shard of a shattered halo (the Fall). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HALO_SHARD = PARTICLES.register("halo_shard", () -> new SimpleParticleType(false));
    /** A feather torn from a falling angel's wing, singed dark. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TORN_FEATHER = PARTICLES.register("torn_feather", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
