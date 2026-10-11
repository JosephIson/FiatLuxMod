package com.josephison.fiatlux.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An instrument of human justice standing where a people punishes its condemned: a cross on the rise
 * outside the town, or the stocks in its square. Each carries the charge written on it - the name of
 * the condemned and the crime, as the title set over the cross (John 19:19) - which stays after the
 * sentence is done. A player in creative mode can strike it down.
 */
public class PunishmentPropEntity extends Entity implements GeoEntity {
    public static final int CROSS = 0, STOCKS = 1;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PunishmentPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> INSCRIPTION = SynchedEntityData.defineId(PunishmentPropEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PunishmentPropEntity(EntityType<? extends PunishmentPropEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public void setKind(int kind) {
        entityData.set(KIND, kind);
    }

    /** The charge: lines separated by '\n' (the name of the condemned, then the crime). */
    public String inscription() {
        return entityData.get(INSCRIPTION);
    }

    public void setInscription(String text) {
        entityData.set(INSCRIPTION, text);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, CROSS);
        builder.define(INSCRIPTION, "");
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            if (!level().isClientSide) discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(tag.getInt("Kind"));
        setInscription(tag.getString("Inscription"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind());
        tag.putString("Inscription", inscription());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
