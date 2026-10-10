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
 * A Dominion: governs the offices of the lower choirs. Every few seconds it raises its sceptre
 * over the beings of lower rank on its side (strength and swiftness for their work) and lays a
 * decree of heaviness on the enemies about it; it judges with a pillar of light.
 */
public class DominionEntity extends ChoirBeing {
    public DominionEntity(EntityType<? extends DominionEntity> type, Level level) {
        super(type, level, "dominion");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 180).add(Attributes.ARMOR, 14)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 56);
    }

    @Override
    public Choir choir() {
        return Choir.DOMINIONS;
    }

    @Override
    protected int officeInterval() {
        return 160;
    }

    /** Governance: strengthens the lower choirs of its side, burdens its enemies. */
    @Override
    protected void office(ServerLevel level) {
        int governed = 0;
        for (LivingEntity ally : alliesWithin(16)) {
            if (ally instanceof HeavenlyBeing b && !choir().outranks(b.choir())) continue;
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            sendFx(level, FxKind.BLESSING, ally.position(), ally.getBbHeight(), 0xFF6A8CFF, 16);
            governed++;
        }
        for (LivingEntity enemy : enemiesWithin(12)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            governed++;
        }
        if (governed > 0) {
            playJudgmentAnimation();
            sendFx(level, FxKind.SHOCKWAVE, position(), 16f, 0xFF5A7CFF, 24);
        }
    }

    @Override
    public double judgmentRange() {
        return 14;
    }

    @Override
    public int judgmentInterval() {
        return 45;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        target.hurt(damageSources().indirectMagic(this, this), 20f);
        sendFx(level, FxKind.CELESTIAL_PILLAR, target.position(), 30f, 0xFF7A9CFF, 20);
    }
}
