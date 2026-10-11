package com.josephison.fiatlux.infernal;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModParticles;
import net.minecraft.core.BlockPos;
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
 * A rift from the Underworld: a jagged tear standing upright on the ground of a living world, burning
 * at its edges, through which unclean spirits come up "out of the bottomless pit" (Rev 9:2) to go about
 * seeking whom they may enter. Through it the Underworld itself can be seen - its ash plains, brimstone
 * and the lights of its cities, in true perspective as one moves (see {@link com.josephison.fiatlux.portal.Portals}) -
 * and its twin stands on the far side, looking back into the world; whoever steps through one comes
 * out of the other. It stays open a while, sending the spirits up one by one, and closes; a holy angel
 * standing by seals it, and so does its Creator's stroke - "the gates of hell shall not prevail"
 * (Matt 16:18).
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
    /** How long a holy angel has stood by it; at SEAL_TICKS it is sealed. */
    private int holyTicks;
    public static final int SEAL_TICKS = 160;
    /** The far side: its dimension, the base of the twin rift there, and the twin itself. */
    private String farDim = "";
    private BlockPos farBase = BlockPos.ZERO;
    @org.jetbrains.annotations.Nullable private java.util.UUID twin;
    private boolean isTwin;
    /** How wide the tear is at its widest, and how tall. */
    public static final float HEIGHT = 5.2f, HALF_WIDTH = 1.2f;
    private static final int UNDERWORLD_FOG = 0x3A0E0A, WORLD_FOG = 0xA8C8E8;

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
        com.josephison.fiatlux.portal.Portals.close(this);
        RiftEntity other = twin();
        if (other != null) other.seal("");
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
        if (age == 1 && !isTwin && farDim.isEmpty()) openOnto(sl);
        if (!closing() && age > 30 && !farDim.isEmpty()) {
            ServerLevel far = sl.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(farDim)));
            if (far != null) {
                if (!com.josephison.fiatlux.portal.Portals.isOpen(this) && age % 40 == 0) {   // after a reload, the view is captured again
                    com.josephison.fiatlux.portal.Portals.open(this, far, farBase, getYRot(), "rift", isTwin ? WORLD_FOG : UNDERWORLD_FOG, 28, 10, 22);
                }
                float half = HALF_WIDTH * openness(0) * 0.8f;
                com.josephison.fiatlux.portal.Portals.cross(this, half, HEIGHT - 1, far, net.minecraft.world.phys.Vec3.atBottomCenterOf(farBase), getYRot());
            }
        }
        if (isTwin) {   // the far side only looks back; it sends no one up, and closes with its rift
            if (age >= OPEN + 200) seal("");
            if (closing() && age - entityData.get(CLOSED_AT) >= CLOSING) discard();
            return;
        }
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
        // a holy angel standing by seals it, if it stands there long enough
        if (!closing() && age % 20 == 0) {
            HeavenlyBeing holy = sl.getEntitiesOfClass(HeavenlyBeing.class, getBoundingBox().inflate(7), b -> !b.isRebel()).stream().findFirst().orElse(null);
            holyTicks = holy == null ? Math.max(0, holyTicks - 20) : holyTicks + 20;
            if (holy != null) sl.sendParticles(ModParticles.HOLY_SPARK.get(), getX(), getY() + 2.5, getZ(), 6, 0.3, 1.4, 0.3, 0.05);
            if (holy != null && holyTicks >= SEAL_TICKS) seal(holy.getName().getString());
        }
        if (!closing() && age >= OPEN) seal("");
        if (closing() && age - entityData.get(CLOSED_AT) >= CLOSING) discard();
    }

    /**
     * Tears through to the Underworld: finds the ground there beneath the same place, sets the twin
     * rift on it facing the same way, and opens the view each way.
     */
    private void openOnto(ServerLevel sl) {
        ServerLevel under = sl.getServer().getLevel(com.josephison.fiatlux.world.FiatLuxDimensions.UNDERWORLD);
        if (under == null || under == sl) return;
        int x = getBlockX(), z = getBlockZ();
        under.getChunk(x >> 4, z >> 4);
        int y = under.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos base = new BlockPos(x, Math.max(under.getMinBuildHeight() + 2, y), z);
        farDim = under.dimension().location().toString();
        farBase = base;
        RiftEntity t = com.josephison.fiatlux.registry.ModEntities.RIFT.get().create(under);
        if (t != null) {
            t.isTwin = true;
            t.spirits = 0;
            t.farDim = sl.dimension().location().toString();
            t.farBase = blockPosition();
            t.twin = getUUID();
            t.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, getYRot(), 0);
            under.addFreshEntity(t);
            twin = t.getUUID();
            com.josephison.fiatlux.portal.Portals.open(t, sl, blockPosition(), getYRot(), "rift", WORLD_FOG, 28, 10, 22);
        }
        com.josephison.fiatlux.portal.Portals.open(this, under, base, getYRot(), "rift", UNDERWORLD_FOG, 28, 10, 22);
    }

    @org.jetbrains.annotations.Nullable
    private RiftEntity twin() {
        if (twin == null || farDim.isEmpty() || !(level() instanceof ServerLevel sl)) return null;
        ServerLevel far = sl.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(farDim)));
        return far != null && far.getEntity(twin) instanceof RiftEntity r ? r : null;
    }

    /**
     * The shape of the tear: a jagged spine up the middle (x offset per segment) and its half-width
     * per segment, the same for every client (from the entity's id). Widest in the middle, closed at the ends.
     */
    public static float[][] outline(int id, float open) {
        int n = 14;
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(id * 31L);
        float[] x = new float[n + 1], w = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            x[i] = (r.nextFloat() - 0.5f) * 0.6f;
            float bulge = (float) Math.sin(Math.PI * k);
            w[i] = (0.1f + HALF_WIDTH * (float) Math.pow(bulge, 0.8) * (0.7f + r.nextFloat() * 0.3f)) * open;
        }
        return new float[][]{x, w};
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
    public void remove(RemovalReason reason) {
        com.josephison.fiatlux.portal.Portals.close(this);
        super.remove(reason);
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
        farDim = tag.getString("FarDim");
        farBase = BlockPos.of(tag.getLong("FarBase"));
        twin = tag.hasUUID("Twin") ? tag.getUUID("Twin") : null;
        isTwin = tag.getBoolean("IsTwin");
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
        tag.putString("FarDim", farDim);
        tag.putLong("FarBase", farBase.asLong());
        if (twin != null) tag.putUUID("Twin", twin);
        tag.putBoolean("IsTwin", isTwin);
        tag.putString("Affliction", affliction.name());
    }
}
