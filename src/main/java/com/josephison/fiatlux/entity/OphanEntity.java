package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * An ophan - the wheel within a wheel (Ezekiel 1:15-21). "Whithersoever the spirit was to go, they
 * went": it follows the cherubim and its Creator, and its judgment is the crushing roll of the
 * wheels over the corrupt ("O wheel", 10:13).
 */
public class OphanEntity extends HeavenlyBeing {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ophan.idle");
    private static final RawAnimation WHIRL = RawAnimation.begin().thenLoop("animation.ophan.whirl");

    public OphanEntity(EntityType<? extends OphanEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260).add(Attributes.ARMOR, 24)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FLYING_SPEED, 0.75).add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    public double judgmentRange() {
        return 3.0;
    }

    @Override
    public int judgmentInterval() {
        return 12;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.0), ModTags::isCorrupt)) {
            e.hurt(damageSources().indirectMagic(this, this), 12f);
        }
        level.sendParticles(ModParticles.EMBER.get(), target.getX(), target.getY() + 0.5, target.getZ(), 16, 0.5, 0.3, 0.5, 0.08);
        sendFx(level, FxKind.SHOCKWAVE, target.position(), 3f, 0xFFD6E296, 10);
    }

    @Override
    protected void spawnAura() {
        super.spawnAura();
        if (random.nextInt(3) == 0) {
            level().addParticle(ModParticles.EMBER.get(), getX() + (random.nextDouble() - 0.5),
                    getY() + getBbHeight() / 2, getZ() + (random.nextDouble() - 0.5), 0, 0.02, 0);
        }
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return WHIRL;
    }

    @Override
    protected RawAnimation judgmentAnimation() {
        return WHIRL;
    }
}
