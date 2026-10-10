package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.josephison.fiatlux.divinity.AngelState;
import com.josephison.fiatlux.registry.ModAttachments;
import com.josephison.fiatlux.world.RebellionWar;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;

import java.util.UUID;

/**
 * Shared behaviour of the Heavenly Host: weightless flight, guarding their Creator, smiting the
 * corrupt, and a radiance that surrounds them ("the glory of the Lord shone round about them",
 * Luke 2:9). Subclasses define their animations, their act of judgment and their aura.
 *
 * Every member of the Host carries an {@link AngelState} (pride and loyalty). When war breaks out
 * in heaven the proud may rebel: a rebel turns on the loyal Host and on its Creator, is counted
 * among the corrupt, and cannot die - when it is defeated (or the Creator casts it out) it is
 * struck by lightning, its radiance shatters, and it is cast down (see {@link #castOut}).
 */
public abstract class HeavenlyBeing extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID creator;
    private int judgmentCooldown;
    private static final EntityDataAccessor<Boolean> DATA_REBEL = SynchedEntityData.defineId(HeavenlyBeing.class, EntityDataSerializers.BOOLEAN);
    /** Ticks into the cast-out sequence (0 = not being cast out); negative = waiting to be struck. */
    private static final EntityDataAccessor<Integer> DATA_CAST_OUT = SynchedEntityData.defineId(HeavenlyBeing.class, EntityDataSerializers.INT);
    /** Ticks from the lightning strike to the mutation into a fallen one. */
    public static final int STRIKE_TICKS = 24;
    private static final net.minecraft.resources.ResourceLocation REBEL_MIGHT = com.josephison.fiatlux.FiatLux.id("rebel_might");

    protected HeavenlyBeing(EntityType<? extends HeavenlyBeing> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REBEL, false);
        builder.define(DATA_CAST_OUT, 0);
    }

    // ------------------------------------------------------------------ the rebellion

    public AngelState angelState() {
        AngelState s = getData(ModAttachments.ANGEL_STATE);
        if (!s.isSet()) {
            s = AngelState.roll(random);
            setData(ModAttachments.ANGEL_STATE, s);
        }
        return s;
    }

    public void setAngelState(AngelState state) {
        setData(ModAttachments.ANGEL_STATE, state);
    }

    public boolean isRebel() {
        return entityData.get(DATA_REBEL);
    }

    /** Joins (or is reconciled from) the rebellion. Rebels serve no Creator. */
    public void setRebel(boolean rebel) {
        entityData.set(DATA_REBEL, rebel);
        var health = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (rebel) {
            creator = null;
            setAngelState(angelState().asRebel());
            setTarget(null);
            // the might of pride: a rebel withstands the loyal Host until the Creator casts it out
            if (health != null && !health.hasModifier(REBEL_MIGHT)) {
                health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(REBEL_MIGHT, 2.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                setHealth(getMaxHealth());
            }
        } else if (health != null) {
            health.removeModifier(REBEL_MIGHT);
        }
    }

    public boolean isBeingCastOut() {
        return entityData.get(DATA_CAST_OUT) != 0;
    }

    /** Ticks since the lightning struck (0 while not being cast out or still waiting). */
    public int castOutTicks() {
        return Math.max(0, entityData.get(DATA_CAST_OUT));
    }

    /**
     * "I beheld Satan as lightning fall from heaven" (Luke 10:18). Begins the Fall of a rebel after
     * {@code delay} ticks: lightning strikes, the halo and radiance shatter, the being is lifted up
     * convulsing, and its wings tear and mutate as it becomes a fallen one and plunges through a
     * fracture in the sky. Returns false if this being is not a rebel or is already falling.
     */
    public boolean castOut(int delay) {
        if (!isRebel() || isBeingCastOut() || level().isClientSide) return false;
        entityData.set(DATA_CAST_OUT, delay > 0 ? -delay : 1);
        freeze();
        if (level() instanceof ServerLevel sl) RebellionWar.onCastOut(sl.getServer(), this);
        return true;
    }

    /** Held by the judgment: no more will of its own. */
    private void freeze() {
        setTarget(null);
        getNavigation().stop();
        for (var flag : net.minecraft.world.entity.ai.goal.Goal.Flag.values()) {
            goalSelector.disableControlFlag(flag);
            targetSelector.disableControlFlag(flag);
        }
    }

    private void tickCastOut(ServerLevel level) {
        int t = entityData.get(DATA_CAST_OUT);
        if (t < 0) {                          // awaiting the stroke: held fast in the air
            entityData.set(DATA_CAST_OUT, t + 1 == 0 ? 1 : t + 1);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        entityData.set(DATA_CAST_OUT, t + 1);
        setDeltaMovement(0, 0.06, 0);          // lifted up before the throne to be judged
        Vec3 head = position().add(0, getBbHeight() * 0.9, 0);
        if (t == 1) {
            net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(position());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            sendFx(level, FxKind.HALO_SHATTER, head, getBbWidth() + 1.5f, 0xFFFFD86A, 30);
            level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.5f);
            level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 3.0f, 0.8f);
        }
        if (t == STRIKE_TICKS / 2) {
            sendFx(level, FxKind.SKY_FRACTURE, position(), 1f, 0xFFFF2A1A, 70);
            level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 0.6f);
        }
        if (t >= STRIKE_TICKS) {
            FallenEntity fallen = com.josephison.fiatlux.registry.ModEntities.FALLEN.get().create(level);
            if (fallen != null) {
                fallen.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                fallen.setFormerOrder(getType().getDescription().getString());
                if (hasCustomName()) fallen.setCustomName(getCustomName());
                fallen.beginPlunge();
                level.addFreshEntity(fallen);
            }
            sendFx(level, FxKind.HALO_SHATTER, head, getBbWidth() + 0.5f, 0xFF8A1A1A, 20);
            discard();
        }
    }

    // ------------------------------------------------------------------ identity

    public void setCreator(UUID uuid) {
        this.creator = uuid;
    }

    public boolean hasCreator() {
        return creator != null;
    }

    @Nullable
    public Player getCreator() {
        return creator == null ? null : level().getPlayerByUUID(creator);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof HeavenlyBeing being) return being.isRebel() == isRebel();
        if (isRebel()) return false;
        if (other.getType().is(ModTags.HEAVENLY_HOST)) return true;
        if (creator != null && creator.equals(other.getUUID())) return true;
        return super.isAlliedTo(other);
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
        goalSelector.addGoal(2, new JudgeGoal(this));
        goalSelector.addGoal(4, new FollowCreatorGoal(this, 1.1, 14f, 5f));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // the loyal judge the corrupt (rebels among them); rebels war against the loyal Host and the Creator
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> isRebel() ? (e instanceof HeavenlyBeing b && !b.isRebel()) || e instanceof Player
                        : e != getCreator() && ModTags.isCorrupt(e)));
    }

    /** Distance at which this being executes judgment on its target. */
    public abstract double judgmentRange();

    /** Ticks between acts of judgment. */
    public abstract int judgmentInterval();

    /** Performs this being's act of judgment on a corrupt target (server side). */
    protected abstract void judge(ServerLevel level, LivingEntity target);

    /** Called by JudgeGoal: if ready and in range, judge. */
    void tryJudge(LivingEntity target) {
        if (judgmentCooldown > 0 || !(level() instanceof ServerLevel sl)) return;
        if (distanceToSqr(target) > judgmentRange() * judgmentRange() || !hasLineOfSight(target)) return;
        judgmentCooldown = judgmentInterval();
        judge(sl, target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isBeingCastOut()) {
            if (level() instanceof ServerLevel sl) tickCastOut(sl);
            else spawnCastOutParticles();
            return;
        }
        if (judgmentCooldown > 0) judgmentCooldown--;
        if (level() instanceof ServerLevel sl && tickCount % 20 == 7) RebellionWar.tickBeing(sl, this);
        if (level().isClientSide && tickCount % 3 == 0 && FiatLuxConfig.CLIENT.hostAuras.get()) {
            spawnAura();
        }
    }

    /** Client: shards of radiance, sparks and torn feathers while being struck down. */
    private void spawnCastOutParticles() {
        if (castOutTicks() <= 0) return;
        Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
        for (int i = 0; i < 3; i++) {
            level().addParticle(ModParticles.TORN_FEATHER.get(), c.x + random.nextGaussian() * 0.6, c.y + random.nextGaussian() * 0.6,
                    c.z + random.nextGaussian() * 0.6, random.nextGaussian() * 0.05, -0.02, random.nextGaussian() * 0.05);
            level().addParticle(ModParticles.EMBER.get(), c.x + random.nextGaussian() * 0.4, c.y + random.nextGaussian() * 0.8,
                    c.z + random.nextGaussian() * 0.4, 0, 0.03, 0);
        }
        level().addParticle(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, random.nextGaussian() * 0.02, 0.05, random.nextGaussian() * 0.02);
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, c.x + random.nextGaussian() * 0.5, c.y + random.nextGaussian(),
                    c.z + random.nextGaussian() * 0.5, 0, 0, 0);
        }
    }

    /** Client-side radiance particles. */
    protected void spawnAura() {
        if (isRebel()) {   // the glory departs: embers and smoke where radiance was
            Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
            double r = getBbWidth() * 0.9;
            level().addParticle(random.nextBoolean() ? ModParticles.EMBER.get() : ParticleTypes.SMOKE,
                    c.x + (random.nextDouble() - 0.5) * r * 2, c.y + (random.nextDouble() - 0.5) * getBbHeight(),
                    c.z + (random.nextDouble() - 0.5) * r * 2, 0, 0.02, 0);
            return;
        }
        Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
        double r = getBbWidth() * 0.9;
        level().addParticle(ModParticles.RADIANCE.get(), c.x + (random.nextDouble() - 0.5) * r * 2,
                c.y + (random.nextDouble() - 0.5) * getBbHeight(), c.z + (random.nextDouble() - 0.5) * r * 2, 0, 0.01, 0);
    }

    // ------------------------------------------------------------------ immortal-ish physiology

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBeingCastOut() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!isRebel() && source.getEntity() != null && source.getEntity().getUUID().equals(creator)) {
            return false; // the Host does not strive against its Creator
        }
        return super.hurt(source, amount);
    }

    /** A defeated rebel does not die: it falls. */
    @Override
    public void die(DamageSource source) {
        if (isRebel() && !isBeingCastOut() && !isRemoved() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setHealth(1f);
            castOut(0);
            return;
        }
        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return !isBeingCastOut() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (creator != null) tag.putUUID("Creator", creator);
        tag.putBoolean("Rebel", isRebel());
        tag.putInt("CastOut", entityData.get(DATA_CAST_OUT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Creator")) creator = tag.getUUID("Creator");
        entityData.set(DATA_REBEL, tag.getBoolean("Rebel"));
        entityData.set(DATA_CAST_OUT, tag.getInt("CastOut"));
        if (isBeingCastOut()) freeze();
    }

    // ------------------------------------------------------------------ GeckoLib

    protected abstract RawAnimation idleAnimation();

    protected abstract RawAnimation moveAnimation();

    protected abstract RawAnimation judgmentAnimation();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 6, state ->
                state.setAndContinue(state.isMoving() ? moveAnimation() : idleAnimation())));
        controllers.add(new AnimationController<>(this, "judgment", 2, state -> PlayState.STOP)
                .triggerableAnim("judge", judgmentAnimation()));
    }

    protected void playJudgmentAnimation() {
        triggerAnim("judgment", "judge");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    protected static void sendFx(ServerLevel level, FxKind kind, Vec3 pos, float radius, int color, int duration) {
        Fx.send(level, kind, pos, radius, color, duration);
    }
}
