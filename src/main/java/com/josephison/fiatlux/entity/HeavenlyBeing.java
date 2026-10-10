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

import java.util.UUID;

/**
 * Shared behaviour of the Heavenly Host: weightless flight, guarding their Creator, smiting the
 * corrupt, and a radiance that surrounds them ("the glory of the Lord shone round about them",
 * Luke 2:9). Subclasses define their animations, their act of judgment and their aura.
 */
public abstract class HeavenlyBeing extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID creator;
    private int judgmentCooldown;

    protected HeavenlyBeing(EntityType<? extends HeavenlyBeing> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    // ------------------------------------------------------------------ identity

    public void setCreator(UUID uuid) {
        this.creator = uuid;
    }

    @Nullable
    public Player getCreator() {
        return creator == null ? null : level().getPlayerByUUID(creator);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
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
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> e != getCreator() && ModTags.isCorrupt(e)));
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
        if (judgmentCooldown > 0) judgmentCooldown--;
        if (level().isClientSide && tickCount % 3 == 0 && FiatLuxConfig.CLIENT.hostAuras.get()) {
            spawnAura();
        }
    }

    /** Client-side radiance particles. */
    protected void spawnAura() {
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
        if (source.getEntity() != null && source.getEntity().getUUID().equals(creator)) {
            return false; // the Host does not strive against its Creator
        }
        return super.hurt(source, amount);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Creator")) creator = tag.getUUID("Creator");
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
