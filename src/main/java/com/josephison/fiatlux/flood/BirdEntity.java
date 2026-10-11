package com.josephison.fiatlux.flood;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The birds Noah sent out of the window of the ark (Genesis 8:6-12): the raven, "which went forth to
 * and fro, until the waters were dried up", and the dove, which "found no rest for the sole of her
 * foot" and came back; came again "and, lo, in her mouth was an olive leaf pluckt off"; and the third
 * time "returned not again unto him any more". Each flies a set course out from the window and away
 * or home, and is gone.
 */
public class BirdEntity extends Mob implements GeoEntity {
    public static final int RAVEN = 0, DOVE = 1;
    /** Where the bird goes: out and back, out and away. */
    public static final int RETURNS = 0, AWAY = 1;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LEAF = SynchedEntityData.defineId(BirdEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.bird.fly");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Vec3> course = new ArrayList<>();
    private int leg;
    private boolean leafOnReturn;

    public BirdEntity(EntityType<? extends BirdEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, DOVE);
        builder.define(LEAF, false);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public boolean carriesLeaf() {
        return entityData.get(LEAF);
    }

    /**
     * Sends the bird from the window: out over the waters to `far`, wheeling there, then back to the
     * window (with an olive leaf, if `leaf`) or on and away out of sight.
     */
    public void send(int kind, Vec3 window, Vec3 far, int way, boolean leaf) {
        entityData.set(KIND, kind);
        moveTo(window.x, window.y, window.z, 0, 0);
        course.clear();
        Vec3 out = far.subtract(window);
        Vec3 side = new Vec3(-out.z, 0, out.x).normalize().scale(10);
        course.add(window.add(out.scale(0.5)).add(0, 8, 0));
        course.add(far.add(side));
        course.add(far.add(0, 4, 0));
        course.add(far.subtract(side));
        if (kind == RAVEN) {   // "to and fro"
            course.add(window.add(side.scale(2)).add(0, 12, 0));
            course.add(far.add(side.scale(-2)).add(0, 6, 0));
        }
        if (way == RETURNS) course.add(window);
        else course.add(far.add(out.normalize().scale(120)).add(0, 30, 0));
        leafOnReturn = leaf;
        leg = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (course.isEmpty() || leg >= course.size()) {
            discard();   // home in the ark, or gone beyond sight
            return;
        }
        Vec3 to = course.get(leg);
        Vec3 d = to.subtract(position());
        double dist = d.length();
        if (dist < 1.0) {
            leg++;
            if (leafOnReturn && leg == 3) entityData.set(LEAF, true);   // "an olive leaf pluckt off"
            return;
        }
        Vec3 step = d.scale(Math.min(1, 0.55 / dist));
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        setDeltaMovement(Vec3.ZERO);
        float yaw = (float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        if (tickCount > 2400) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;   // the Creator's messengers
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "wings", 3, state -> state.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
