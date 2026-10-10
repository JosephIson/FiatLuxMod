package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Power: the warrior choir that holds the fallen in check (Jude 1:6, Rev 20:1-2). It strikes
 * with the sword of the Spirit and binds what it strikes in chains; against the fallen and the
 * rebel angels its stroke falls twice as heavily.
 */
public class PowerEntity extends ChoirBeing {
    public PowerEntity(EntityType<? extends PowerEntity> type, Level level) {
        super(type, level, "power");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.ARMOR, 20)
                .add(Attributes.ARMOR_TOUGHNESS, 6).add(Attributes.ATTACK_DAMAGE, 20).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.7).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public Choir choir() {
        return Choir.POWERS;
    }

    @Override
    public double judgmentRange() {
        return 4.5;
    }

    @Override
    public int judgmentInterval() {
        return 22;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        boolean fallen = target instanceof FallenEntity || target instanceof HeavenlyBeing b && b.isRebel();
        target.hurt(damageSources().mobAttack(this), fallen ? 40f : 22f);
        // "reserved in everlasting chains" (Jude 1:6): the struck are bound
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        sendFx(level, FxKind.SHOCKWAVE, target.position(), 3.5f, 0xFFDDE4FF, 16);
        sendFx(level, FxKind.FIRE_WAVE, target.position().add(0, 1, 0), 2.5f, 0xFFFFF0C0, 10);
    }
}
