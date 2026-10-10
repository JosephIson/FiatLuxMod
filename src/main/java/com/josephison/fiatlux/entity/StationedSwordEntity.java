package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.weapon.DivineWeaponRegistry;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModDataComponents;
import com.josephison.fiatlux.registry.ModItems;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModTags;
import com.josephison.fiatlux.weapon.WeaponTraits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * "So he drove out the man; and he placed at the east of the garden of Eden Cherubims, and a
 * flaming sword which turned every way, to keep the way of the tree of life." (Genesis 3:24)
 *
 * The Flaming Sword stationed as an autonomous guardian: it hovers, turns every way, and drives
 * back and burns the corrupt that approach. Its owner takes it back with a right-click.
 */
public class StationedSwordEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Integer> KINDLED = SynchedEntityData.defineId(StationedSwordEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.flaming_sword_of_cherubim.idle");
    public static final double GUARD_RADIUS = 7.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID owner;
    private WeaponTraits traits = WeaponTraits.FRESH;

    public StationedSwordEntity(EntityType<? extends StationedSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(UUID owner, WeaponTraits traits) {
        this.owner = owner;
        this.traits = traits;
        this.entityData.set(KINDLED, traits.kindled());
    }

    public int kindled() {
        return entityData.get(KINDLED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KINDLED, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // the turning blade sheds holy fire in every direction
            double a = tickCount * 0.35;
            for (int i = 0; i < 2; i++) {
                double ang = a + i * Math.PI;
                level().addParticle(ModParticles.DIVINE_FLAME.get(), getX() + Math.cos(ang) * 0.9, getY() + 1.2 + random.nextDouble(),
                        getZ() + Math.sin(ang) * 0.9, Math.cos(ang) * 0.03, 0.02, Math.sin(ang) * 0.03);
            }
            return;
        }
        if (tickCount % 10 == 0 && level() instanceof ServerLevel sl) {
            boolean struck = false;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(GUARD_RADIUS), ModTags::isCorrupt)) {
                e.hurt(damageSources().magic(), 6f + 2f * kindled());
                e.igniteForSeconds(6);
                Vec3 away = e.position().subtract(position()).normalize().scale(1.2);
                e.push(away.x, 0.35, away.z);
                e.hurtMarked = true;
                struck = true;
            }
            if (struck) {
                Fx.send(sl, FxKind.FIRE_WAVE, position().add(0, 1, 0), (float) GUARD_RADIUS, 0xFFFFA040, 14);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (owner != null && !owner.equals(player.getUUID()) && !player.isCreative()) return InteractionResult.FAIL;
        ItemStack sword = new ItemStack(DivineWeaponRegistry.FLAMING_SWORD.get());
        sword.set(ModDataComponents.WEAPON_TRAITS.get(), traits);
        if (!player.addItem(sword)) player.drop(sword, false);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        traits = new WeaponTraits(tag.getInt("Smitten"), Math.max(1, tag.getInt("Kindled")));
        entityData.set(KINDLED, traits.kindled());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Smitten", traits.smitten());
        tag.putInt("Kindled", traits.kindled());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fire", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
