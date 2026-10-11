package com.josephison.fiatlux.eden;

import com.josephison.fiatlux.divinity.Divinity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * "Now the serpent was more subtil than any beast of the field" (Genesis 3:1) - "that old serpent,
 * called the Devil, and Satan" (Revelation 12:9). He comes into the garden once Satan has fallen,
 * and his words are given him by the Eden script ({@link EdenEvents}). He cannot be slain; a Creator
 * who strikes him drives him off for a while. Cursed, he loses his legs and goes upon his belly.
 */
public class SerpentEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> CURSED = SynchedEntityData.defineId(SerpentEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(SerpentEntity.class, EntityDataSerializers.STRING);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.serpent.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.serpent.move");
    private static final RawAnimation SPEAK = RawAnimation.begin().thenLoop("animation.serpent.speak");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int speechTicks;

    public SerpentEntity(EntityType<? extends SerpentEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURSED, false);
        builder.define(SPEECH, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
    }

    public boolean cursed() {
        return entityData.get(CURSED);
    }

    public void curse() {
        entityData.set(CURSED, true);
    }

    public String speech() {
        return entityData.get(SPEECH);
    }

    public void say(String words) {
        entityData.set(SPEECH, words);
        speechTicks = 120;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && speechTicks > 0 && --speechTicks == 0) entityData.set(SPEECH, "");
        if (level().isClientSide && random.nextInt(10) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + 0.2, getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level() instanceof ServerLevel sl && source.getEntity() instanceof ServerPlayer p && !Divinity.get(p).pilgrim()) {
            EdenEvents.serpentDrivenOff(sl, this, p);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Cursed", cursed());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CURSED, tag.getBoolean("Cursed"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 6, state -> state.setAndContinue(
                !speech().isEmpty() ? SPEAK : state.isMoving() ? MOVE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
