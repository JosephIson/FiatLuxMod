package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A Principality: a prince set over a people (Daniel 10:13, 20-21). It guards the mortals of the
 * place it keeps - every hostile creature, not only the corrupt, is its enemy - and periodically
 * raises its standard over the people about it, shielding them.
 */
public class PrincipalityEntity extends ChoirBeing {
    public PrincipalityEntity(EntityType<? extends PrincipalityEntity> type, Level level) {
        super(type, level, "principality");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 120).add(Attributes.ARMOR, 12)
                .add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    public Choir choir() {
        return Choir.PRINCIPALITIES;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> !isRebel() && e instanceof Enemy && !(e instanceof HeavenlyBeing)));
    }

    @Override
    protected int officeInterval() {
        return 200;
    }

    /** Raises the standard: the people under its keeping are shielded. */
    @Override
    protected void office(ServerLevel level) {
        if (isRebel()) return;
        var people = level.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(24),
                e -> e instanceof AbstractVillager || e instanceof net.minecraft.world.entity.player.Player);
        for (LivingEntity p : people) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
            sendFx(level, FxKind.BLESSING, p.position(), p.getBbHeight(), 0xFFFFD27A, 16);
        }
        if (!people.isEmpty()) playJudgmentAnimation();
    }

    @Override
    public double judgmentRange() {
        return 10;
    }

    @Override
    public int judgmentInterval() {
        return 35;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        target.hurt(damageSources().indirectMagic(this, this), 18f);
        sendFx(level, FxKind.CELESTIAL_PILLAR, target.position(), 24f, 0xFFFFD27A, 16);
    }
}
