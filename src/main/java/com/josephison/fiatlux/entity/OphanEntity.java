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

    /** The four quarters of the high rim ("their rings were so high that they were dreadful", Ezek 1:18). */
    private final OphanPart[] rims;

    public OphanEntity(EntityType<? extends OphanEntity> type, Level level) {
        super(type, level);
        this.rims = new OphanPart[]{
                new OphanPart(this, "rim_top", 1.6f, 0.9f), new OphanPart(this, "rim_bottom", 1.6f, 0.9f),
                new OphanPart(this, "rim_fore", 0.9f, 1.6f), new OphanPart(this, "rim_aft", 0.9f, 1.6f)};
        // parts take the ids immediately after the parent, as NeoForge expects (cf. MC-158205)
        this.setId(ENTITY_COUNTER.getAndAdd(rims.length + 1) + 1);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (rims != null) {
            for (int i = 0; i < rims.length; i++) rims[i].setId(id + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return rims;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // keep the rim quarters on the rolling wheel's circumference (radius ~1.4 blocks at render scale 1.5)
        double cy = getY() + 1.7;
        float yaw = getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        double fx = -net.minecraft.util.Mth.sin(yaw), fz = net.minecraft.util.Mth.cos(yaw);
        place(rims[0], getX(), cy + 1.25, getZ());
        place(rims[1], getX(), cy - 1.7, getZ());
        place(rims[2], getX() + fx * 1.35, cy - 0.8, getZ() + fz * 1.35);
        place(rims[3], getX() - fx * 1.35, cy - 0.8, getZ() - fz * 1.35);
    }

    private static void place(OphanPart part, double x, double y, double z) {
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
        part.xOld = part.xo;
        part.yOld = part.yo;
        part.zOld = part.zo;
        part.setPos(x, y, z);
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
