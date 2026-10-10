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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A cherub - one of the four living creatures (Ezekiel 1 and 10), guardian of the way to the tree
 * of life (Genesis 3:24). Fire goes up and down among them and lightning goes forth (1:13): its
 * judgment is a ring of fire and lightning around itself.
 */
public class CherubEntity extends HeavenlyBeing {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cherub.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.cherub.move");

    public CherubEntity(EntityType<? extends CherubEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 320).add(Attributes.ARMOR, 20)
                .add(Attributes.ATTACK_DAMAGE, 20).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FLYING_SPEED, 0.55).add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    public double judgmentRange() {
        return 10;
    }

    @Override
    public int judgmentInterval() {
        return 60;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        // "the fire was bright, and out of the fire went forth lightning" (Ezekiel 1:13)
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(judgmentRange()),
                ModTags::isCorrupt)) {
            e.hurt(damageSources().indirectMagic(this, this), 18f);
            e.igniteForSeconds(8);
            Vec3 push = e.position().subtract(position()).normalize().scale(1.4);
            e.push(push.x, 0.4, push.z);
        }
        sendFx(level, FxKind.SHOCKWAVE, position(), (float) judgmentRange(), 0xFFFF8A30, 24);
        sendFx(level, FxKind.CELESTIAL_PILLAR, target.position(), 18f, 0xFFE8F0FF, 10);
    }

    @Override
    protected void spawnAura() {
        super.spawnAura();
        // coals of fire going up and down among the living creatures
        level().addParticle(ModParticles.EMBER.get(), getX() + (random.nextDouble() - 0.5) * 2.0,
                getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * 2.0,
                0, (random.nextDouble() - 0.4) * 0.08, 0);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return MOVE;
    }

    @Override
    protected RawAnimation judgmentAnimation() {
        return MOVE;
    }
}
