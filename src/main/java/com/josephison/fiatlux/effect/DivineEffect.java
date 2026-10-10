package com.josephison.fiatlux.effect;

import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModTags;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The five spiritual states. Their stat changes are attribute modifiers registered in ModEffects;
 * the tick behaviour here handles what attributes cannot express.
 */
public class DivineEffect extends MobEffect {
    public enum Kind { BLESSED, SANCTIFIED, CURSED, CORRUPTED, SPIRIT_FILLED }

    private final Kind kind;

    public DivineEffect(Kind kind, MobEffectCategory category, int color, ParticleOptions particle) {
        super(category, color, particle);
        this.kind = kind;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        switch (kind) {
            case SANCTIFIED -> {
                // holiness burns away what is corrupt in the bearer and wounds the corrupt
                if (ModTags.isCorrupt(entity)) {
                    entity.hurt(entity.damageSources().magic(), 2f + amplifier);
                } else {
                    entity.heal(1f);
                }
            }
            case CORRUPTED -> {
                if (!ModTags.isCorrupt(entity) && entity.getHealth() > 2f) {
                    entity.hurt(entity.damageSources().wither(), 1f);
                }
            }
            case SPIRIT_FILLED -> {
                entity.heal(0.5f);
                if (entity.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.HOLY_SPARK.get(), entity.getX(), entity.getY() + entity.getBbHeight() + 0.3,
                            entity.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                }
            }
            default -> {
            }
        }
        return true;
    }
}
