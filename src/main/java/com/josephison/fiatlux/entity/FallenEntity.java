package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One of the angels "which kept not their first estate" (Jude 1:6): a rebel cast out of heaven.
 * Its flesh is scorched where the lightning struck, its halo broken, its wings torn into skeletal
 * bat wings. It is born mid-fall: it plunges through the sky-fracture (no collision, trailing fire
 * and ash), is carried into the Underworld, and strikes the ground in a crater-wave of crimson fire.
 * Thereafter it dwells in the Underworld - hostile to mortals, to the loyal Host and to its Creator.
 */
public class FallenEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fallen.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fallen.fly");
    private static final RawAnimation FALL = RawAnimation.begin().thenLoop("animation.fallen.fall");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("animation.fallen.claw");
    /** Plunge phase: 0 = dwelling; 1 = falling through the sky-fracture; 2 = falling through the Underworld sky. */
    private static final EntityDataAccessor<Integer> DATA_PLUNGE = SynchedEntityData.defineId(FallenEntity.class, EntityDataSerializers.INT);
    /** Ticks of the first phase before the fallen one passes through the fracture. */
    public static final int FRACTURE_TICKS = 30;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String formerOrder = "";
    private int plungeTicks;

    public FallenEntity(EntityType<? extends FallenEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_DAMAGE, 9).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLUNGE, 0);
    }

    public void setFormerOrder(String order) {
        this.formerOrder = order;
    }

    public String formerOrder() {
        return formerOrder;
    }

    public boolean isPlunging() {
        return entityData.get(DATA_PLUNGE) != 0;
    }

    /** Starts the plunge from heaven. */
    public void beginPlunge() {
        entityData.set(DATA_PLUNGE, 1);
        plungeTicks = 0;
        setPersistenceRequired();
    }

    // ------------------------------------------------------------------ flight & goals

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HeavenlyBeing.class, 10, true, false,
                e -> e instanceof HeavenlyBeing b && !b.isRebel()));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof FallenEntity || other instanceof HeavenlyBeing b && b.isRebel()) return true;
        return super.isAlliedTo(other);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("attack", "claw");
        boolean hit = super.doHurtTarget(target);
        if (hit) target.igniteForSeconds(3);
        return hit;
    }

    // ------------------------------------------------------------------ the plunge

    @Override
    public void aiStep() {
        super.aiStep();
        if (!isPlunging()) {
            if (level().isClientSide && tickCount % 4 == 0) {
                level().addParticle(random.nextBoolean() ? ParticleTypes.SMOKE : ModParticles.EMBER.get(),
                        getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (level().isClientSide) {
            spawnTrail();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        plungeTicks++;
        for (var flag : net.minecraft.world.entity.ai.goal.Goal.Flag.values()) goalSelector.disableControlFlag(flag);
        int phase = entityData.get(DATA_PLUNGE);
        setTarget(null);
        if (phase == 1) {
            ServerLevel underworld = level.getServer().getLevel(FiatLuxDimensions.UNDERWORLD);
            if (underworld == null || underworld == level) {   // nowhere further to fall: come down here
                entityData.set(DATA_PLUNGE, 2);
                plungeTicks = 0;
                return;
            }
            noPhysics = true;
            setDeltaMovement(getLookAngle().x * 0.2, -1.4, getLookAngle().z * 0.2);
            if (plungeTicks >= FRACTURE_TICKS || getY() < level.getMinBuildHeight() + 2) {
                passIntoUnderworld(underworld);
            }
        } else {
            noPhysics = false;
            setDeltaMovement(getDeltaMovement().x * 0.8, -1.1, getDeltaMovement().z * 0.8);
            if (onGround() || verticalCollisionBelow || plungeTicks > 200 || getY() < level.getMinBuildHeight() + 2) {
                impact(level);
            }
        }
    }

    private void passIntoUnderworld(ServerLevel underworld) {
        int x = getBlockX(), z = getBlockZ();
        underworld.getChunk(x >> 4, z >> 4);   // generate the landing site first
        int ground = underworld.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        Vec3 sky = new Vec3(x + 0.5, Math.min(underworld.getMaxBuildHeight() - 4, ground + 36), z + 0.5);
        entityData.set(DATA_PLUNGE, 2);
        plungeTicks = 0;
        noPhysics = false;
        Fx.send(underworld, FxKind.SKY_FRACTURE, sky, 1f, 0xFFFF2A1A, 60);
        changeDimension(new DimensionTransition(underworld, sky, new Vec3(0, -1.1, 0), getYRot(), 0, DimensionTransition.DO_NOTHING));
    }

    private void impact(ServerLevel level) {
        entityData.set(DATA_PLUNGE, 0);
        for (var flag : net.minecraft.world.entity.ai.goal.Goal.Flag.values()) goalSelector.enableControlFlag(flag);
        plungeTicks = 0;
        noPhysics = false;
        setDeltaMovement(Vec3.ZERO);
        Fx.send(level, FxKind.FALLEN_IMPACT, position(), 9f, 0xFFFF3A1A, 40);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 0.6f);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, getBoundingBox().inflate(4), e -> e != this)) {
            e.hurt(damageSources().mobAttack(this), 6f);
            e.igniteForSeconds(4);
        }
    }

    private void spawnTrail() {
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        for (int i = 0; i < 4; i++) {
            level().addParticle(ParticleTypes.FLAME, c.x + random.nextGaussian() * 0.4, c.y + random.nextGaussian() * 0.6,
                    c.z + random.nextGaussian() * 0.4, random.nextGaussian() * 0.03, 0.25, random.nextGaussian() * 0.03);
            level().addParticle(ModParticles.EMBER.get(), c.x + random.nextGaussian() * 0.5, c.y + random.nextGaussian(),
                    c.z + random.nextGaussian() * 0.5, 0, 0.3, 0);
        }
        level().addParticle(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1.2, c.z, random.nextGaussian() * 0.04, 0.3, random.nextGaussian() * 0.04);
        if (random.nextInt(3) == 0) {
            level().addParticle(ModParticles.TORN_FEATHER.get(), c.x + random.nextGaussian() * 0.6, c.y, c.z + random.nextGaussian() * 0.6,
                    random.nextGaussian() * 0.05, 0.2, random.nextGaussian() * 0.05);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isPlunging() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f + random.nextFloat() * 0.1f;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FormerOrder", formerOrder);
        tag.putInt("Plunge", entityData.get(DATA_PLUNGE));
        tag.putInt("PlungeTicks", plungeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        formerOrder = tag.getString("FormerOrder");
        entityData.set(DATA_PLUNGE, tag.getInt("Plunge"));
        plungeTicks = tag.getInt("PlungeTicks");
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 5, state ->
                state.setAndContinue(isPlunging() ? FALL : state.isMoving() ? FLY : IDLE)));
        controllers.add(new AnimationController<>(this, "attack", 2, state -> PlayState.STOP).triggerableAnim("claw", CLAW));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
