package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModParticles;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

public class ModParticleProvider extends ParticleDescriptionProvider {
    public ModParticleProvider(PackOutput out, ExistingFileHelper files) {
        super(out, files);
    }

    @Override
    protected void addDescriptions() {
        spriteSet(ModParticles.HOLY_SPARK.get(), FiatLux.id("holy_spark"), 4, false);
        spriteSet(ModParticles.DIVINE_FLAME.get(), FiatLux.id("divine_flame"), 8, false);
        spriteSet(ModParticles.LIGHT_MOTE.get(), FiatLux.id("light_mote"), 3, false);
        spriteSet(ModParticles.FEATHER.get(), FiatLux.id("feather"), 3, false);
        spriteSet(ModParticles.EMBER.get(), FiatLux.id("ember"), 3, false);
        spriteSet(ModParticles.RADIANCE.get(), FiatLux.id("radiance"), 2, false);
        spriteSet(ModParticles.HALO_SHARD.get(), FiatLux.id("halo_shard"), 3, false);
        spriteSet(ModParticles.TORN_FEATHER.get(), FiatLux.id("feather"), 3, false);
    }
}
