package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An Archangel: captain of the Host, "the voice of the archangel, and ... the trump of God"
 * (1 Thess 4:16). Its judgment is the trumpet: a blast that strikes every enemy about it and
 * hurls them back. Michael, who leads the loyal Host against the rebellion, is of this choir.
 */
public class ArchangelEntity extends ChoirBeing {
    public ArchangelEntity(EntityType<? extends ArchangelEntity> type, Level level) {
        super(type, level, "archangel");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 170).add(Attributes.ARMOR, 16)
                .add(Attributes.ATTACK_DAMAGE, 18).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    public Choir choir() {
        return Choir.ARCHANGELS;
    }

    @Override
    public double judgmentRange() {
        return 10;
    }

    @Override
    public int judgmentInterval() {
        return 70;
    }

    /** The trumpet of God: every enemy within range is struck and thrown back. */
    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        level.playSound(null, blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3f, 1.25f);
        for (LivingEntity e : enemiesWithin(judgmentRange())) {
            e.hurt(damageSources().indirectMagic(this, this), e == target ? 22f : 14f);
            Vec3 push = e.position().subtract(position()).normalize().scale(1.4);
            e.push(push.x, 0.5, push.z);
            e.hurtMarked = true;
        }
        sendFx(level, FxKind.SHOCKWAVE, position(), (float) judgmentRange(), 0xFFFFE6A0, 24);
        sendFx(level, FxKind.GLORY_FLASH, position().add(0, 2.5, 0), 8f, 0xFFFFF0C0, 16);
    }
}
