package com.josephison.fiatlux.infernal;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A rift from the Underworld: a jagged tear standing on the ground of a living world, dark at its
 * heart and burning at its edges, through which unclean spirits come up "out of the bottomless pit"
 * (Rev 9:2) to go about seeking whom they may enter. It stays open a while, sending them up one by
 * one, and closes; a holy angel standing by seals it, and so does its Creator's stroke - "the gates of
 * hell shall not prevail" (Matt 16:18).
 */
public class RiftEntity extends Entity {
    /** Ticks a rift stands open, and the ticks it takes to close. */
    public static final int OPEN = 900, CLOSING = 40;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLOSED_AT = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private int spirits = 3;
    private String lord = "";
    private Affliction affliction = Affliction.DISCORD;
    private boolean legion;

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AGE, 0);
        builder.define(CLOSED_AT, -1);
    }

    /** What comes up through it: how many spirits, of whose legions, and what they do to those they enter. */
    public void carry(int spirits, String lord, Affliction affliction, boolean legion) {
        this.spirits = spirits;
        this.lord = lord;
        this.affliction = affliction;
        this.legion = legion;
    }

    public int age() {
        return entityData.get(AGE);
    }

    /** How open it is, 0..1 (it tears open, stands, and closes). */
    public float openness(float partial) {
        float a = age() + partial;
        int closed = entityData.get(CLOSED_AT);
        float o = Math.min(1f, a / 30f);
        if (closed >= 0) o *= Math.max(0f, 1f - (a - closed) / CLOSING);
        return o;
    }

    public boolean closing() {
        return entityData.get(CLOSED_AT) >= 0;
    }

    /** "Upon this rock I will build my church; and the gates of hell shall not prevail against it" (Matt 16:18). */
    public void seal(String by) {
        if (closing() || !(level() instanceof ServerLevel sl)) return;
        entityData.set(CLOSED_AT, age());
        sl.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5f, 1.6f);
        sl.sendParticles(ModParticles.HOLY_SPARK.get(), getX(), getY() + 2.5, getZ(), 60, 0.4, 1.6, 0.4, 0.2);
        if (!by.isEmpty()) {
            InfernalData d = InfernalData.get(sl.getServer());
            d.sealed++;
            d.setDirty();
            Component line = Component.literal("The rift is sealed by " + by + ".").withStyle(s -> s.withColor(0xF5D77A));
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(64))) p.sendSystemMessage(line);
        }
    }

    @Override
    public void tick() {
        super.tick();
        int age = age() + 1;
        entityData.set(AGE, age);
        if (level().isClientSide) {
            float o = openness(0);
            if (o > 0.2f) {
                for (int i = 0; i < 2; i++) {
                    level().addParticle(random.nextBoolean() ? ModParticles.EMBER.get() : ParticleTypes.LARGE_SMOKE,
                            getX() + (random.nextDouble() - 0.5) * 0.8, getY() + random.nextDouble() * 4.5, getZ() + (random.nextDouble() - 0.5) * 0.8,
                            (random.nextDouble() - 0.5) * 0.04, 0.04, (random.nextDouble() - 0.5) * 0.04);
                }
            }
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (age == 1) {
            sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.4f);
            sl.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
        if (age % 90 == 0 && !closing()) sl.playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 1f, 0.4f);
        // one by one they come up out of the pit
        if (!closing() && spirits > 0 && age >= 60 && age % 120 == 60) {
            release(sl);
            spirits--;
        }
        // a holy angel standing by seals it
        if (!closing() && age % 20 == 0) {
            for (HeavenlyBeing b : sl.getEntitiesOfClass(HeavenlyBeing.class, getBoundingBox().inflate(7), b -> !b.isRebel())) {
                seal(b.getName().getString());
                break;
            }
        }
        if (!closing() && age >= OPEN) seal("");
        if (closing() && age - entityData.get(CLOSED_AT) >= CLOSING) discard();
    }

    /** One spirit comes up through the rift. */
    public SpiritEntity release(ServerLevel level) {
        SpiritEntity s = ModEntities.SPIRIT.get().create(level);
        if (s == null) return null;
        s.moveTo(getX(), getY() + 2.2, getZ(), random.nextFloat() * 360f, 0);
        String name = legion ? "Legion" : !lord.isEmpty() && random.nextInt(3) == 0 ? lord : InfernalHierarchy.spiritName(random);
        s.become(name, lord, legion ? Affliction.VIOLENCE : affliction);
        level.addFreshEntity(s);
        level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2.2, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.5f, 0.4f);
        return s;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer p && !Divinity.get(p).pilgrim()) {
            seal(p.getGameProfile().getName());
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(AGE, tag.getInt("Age"));
        spirits = tag.getInt("Spirits");
        lord = tag.getString("Lord");
        legion = tag.getBoolean("Legion");
        try {
            affliction = Affliction.valueOf(tag.getString("Affliction"));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age());
        tag.putInt("Spirits", spirits);
        tag.putString("Lord", lord);
        tag.putBoolean("Legion", legion);
        tag.putString("Affliction", affliction.name());
    }
}
