package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A seraph - "burning one" (Isaiah 6). Six wings; it cries "Holy, holy, holy" and purges with the
 * live coal from the altar (6:6-7): corrupt beings are set ablaze from afar.
 */
public class SeraphEntity extends HeavenlyBeing {
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.seraph.hover");
    private static final RawAnimation HOLY = RawAnimation.begin().thenPlay("animation.seraph.holy");
    private int cryTimer = 200;

    public SeraphEntity(EntityType<? extends SeraphEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 220).add(Attributes.ARMOR, 16)
                .add(Attributes.ATTACK_DAMAGE, 18).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    public double judgmentRange() {
        return 16;
    }

    @Override
    public int judgmentInterval() {
        return 50;
    }

    @Override
    protected void judge(ServerLevel level, LivingEntity target) {
        playJudgmentAnimation();
        target.hurt(damageSources().indirectMagic(this, this), 22f);
        target.igniteForSeconds(10);
        sendFx(level, FxKind.CELESTIAL_PILLAR, target.position(), 24f, 0xFFFF9A30, 20);
        level.sendParticles(ModParticles.DIVINE_FLAME.get(), target.getX(), target.getY() + 1, target.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getTarget() == null && --cryTimer <= 0) {
            // "And one cried unto another, and said, Holy, holy, holy, is the LORD of hosts" (Isaiah 6:3)
            cryTimer = 400 + random.nextInt(400);
            playJudgmentAnimation();
            level().playSound(null, blockPosition(), ModSounds.HOLY.get(), SoundSource.NEUTRAL, 1.6f, 1f);
            if (level() instanceof ServerLevel sl) {
                sendFx(sl, FxKind.GLORY_FLASH, position().add(0, 2, 0), 10f, 0xFFFFD27A, 30);
            }
        }
    }

    @Override
    protected void spawnAura() {
        super.spawnAura();
        Vec3 c = position();
        level().addParticle(ModParticles.DIVINE_FLAME.get(), c.x + (random.nextDouble() - 0.5) * 2.4,
                c.y + random.nextDouble() * getBbHeight(), c.z + (random.nextDouble() - 0.5) * 2.4, 0, 0.04, 0);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return HOVER;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return HOVER;
    }

    @Override
    protected RawAnimation judgmentAnimation() {
        return HOLY;
    }

    @Override
    public Choir choir() {
        return Choir.SERAPHIM;
    }
}
