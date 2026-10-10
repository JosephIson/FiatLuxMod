package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * A Virtue: "spirits through whom signs and miracles are done". Its office is the miracle of
 * healing - wounds close, afflictions are lifted from those about it - and it judges with a
 * burst of light that burns the corrupt around its target.
 */
public class VirtueEntity extends ChoirBeing {
    public VirtueEntity(EntityType<? extends VirtueEntity> type, Level level) {
        super(type, level, "virtue");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 150).add(Attributes.ARMOR, 12)
                .add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.65)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    public Choir choir() {
        return Choir.VIRTUES;
    }

    @Override
    protected int officeInterval() {
        return 100;
    }

    /** A sign: the wounded on its side are healed and their afflictions lifted. */
    @Override
    protected void office(ServerLevel level) {
        boolean healed = false;
        for (LivingEntity ally : alliesWithin(12)) {
            if (ally.getHealth() >= ally.getMaxHealth() && ally.getActiveEffects().stream().noneMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)) continue;
            ally.heal(8f);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            for (var effect : new ArrayList<>(ally.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) ally.removeEffect(effect.getEffect());
            }
            sendFx(level, FxKind.BLESSING, ally.position(), ally.getBbHeight(), 0xFFFFF0B0, 20);
            healed = true;
        }
        if (healed) {
            playJudgmentAnimation();
            sendFx(level, FxKind.GLORY_FLASH, position().add(0, 2, 0), 6f, 0xFFFFF0C0, 20);
        }
    }

    @Override
    public double judgmentRange() {
        return 12;
    }

    @Override
    public int judgmentInterval() {
        return 40;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        target.hurt(damageSources().indirectMagic(this, this), 16f);
        for (LivingEntity e : enemiesWithin(16)) {
            if (e != target && e.distanceToSqr(target) < 16) e.hurt(damageSources().indirectMagic(this, this), 8f);
        }
        sendFx(level, FxKind.FIAT_LUX, target.position().add(0, 1, 0), 3.5f, 0xFFFFF4C8, 20);
    }
}
