package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A messenger of the Lord (Daniel 10:5-6). Wingless - angels "ascending and descending" (Gen 28:12)
 * glide without wings. Strikes the corrupt with a touch of lightning from its countenance.
 */
public class AngelEntity extends HeavenlyBeing {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.angel.idle");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.angel.glide");
    private static final RawAnimation SMITE = RawAnimation.begin().thenPlay("animation.angel.smite");

    public AngelEntity(EntityType<? extends AngelEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    public double judgmentRange() {
        return 3.5;
    }

    @Override
    public int judgmentInterval() {
        return 20;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        target.hurt(damageSources().indirectMagic(this, this), 14f);
        target.igniteForSeconds(4);
        sendFx(level, FxKind.BLESSING, target.position(), target.getBbHeight(), 0xFFE8F0FF, 12);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return GLIDE;
    }

    @Override
    protected RawAnimation judgmentAnimation() {
        return SMITE;
    }

    @Override
    public Choir choir() {
        return Choir.ANGELS;
    }
}
