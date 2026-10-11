package com.josephison.fiatlux.infernal;

import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.UUID;

/**
 * An unclean spirit (Mark 1:23) - one of the fallen of the legions below, or one of the spirits of the
 * dead giants that wander the earth (1 Enoch 15:8-10). A shadow of a fallen angel, half seen, it
 * comes up through a rift and goes about seeking someone whose faith is weak to enter (Matt 12:43);
 * finding none, it walks "through dry places, seeking rest". Cast out, it may come back to the house
 * it left "empty, swept, and garnished", with seven others more wicked (Matt 12:44-45); sometimes it
 * begs to be sent into the swine (Mark 5:12). Its Creator and the holy angels can cast it into the
 * deep with a stroke (Luke 8:31).
 */
public class SpiritEntity extends Mob implements GeoEntity {
    public enum Mode { SEEK, WANDER, RETURN, EXPELLED, SWINE }

    private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(SpiritEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fallen.fly");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String spirit = "an unclean spirit";
    private String lord = "";
    private Affliction affliction = Affliction.DISCORD;
    private Mode mode = Mode.SEEK;
    @Nullable private UUID target, home;
    private int modeTicks;
    private boolean sevenfold;
    @Nullable private Vec3 drift;

    public SpiritEntity(EntityType<? extends SpiritEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FADE, 0);
    }

    /** Ticks it has been fading out (0 while it is about its business). */
    public int fade() {
        return entityData.get(FADE);
    }

    public void become(String spirit, String lord, Affliction affliction) {
        this.spirit = spirit;
        this.lord = lord;
        this.affliction = affliction;
        Divinity.setAlignment(this, new Alignment(0, 100));   // the holy angels know it for what it is
    }

    public String spiritName() {
        return spirit;
    }

    public String lord() {
        return lord;
    }

    public Affliction affliction() {
        return affliction;
    }

    /** Cast out of `host`: it goes out, and may come back to the empty house - or into the swine. */
    public void expelled(MortalEntity host, boolean swine) {
        home = host.getUUID();
        setMode(swine ? Mode.SWINE : Mode.WANDER);
    }

    /** Goes away out of sight (into dry places, or back down through the rift). */
    public void depart() {
        setMode(Mode.EXPELLED);
    }

    private void setMode(Mode m) {
        mode = m;
        modeTicks = 0;
        target = null;
        drift = null;
    }

    // ------------------------------------------------------------------ its way

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel sl)) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + random.nextDouble() * 2, getRandomZ(0.6), 0, 0.01, 0);
            }
            return;
        }
        modeTicks++;
        switch (mode) {
            case SEEK, RETURN -> seek(sl);
            case WANDER -> wander(sl);
            case SWINE -> swine(sl);
            case EXPELLED -> {
                entityData.set(FADE, modeTicks);
                fly(position().add(0, 2, 0), 0.08);
                if (modeTicks > 40) discard();
            }
        }
        if (tickCount > 24000) discard();
    }

    private void seek(ServerLevel level) {
        MortalEntity host = target == null ? null : level.getEntity(target) instanceof MortalEntity m && m.isAlive() ? m : null;
        if (host == null || !open(host)) {
            if (mode == Mode.RETURN) {   // the house is no longer empty
                depart();
                return;
            }
            host = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(48), SpiritEntity::open).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            target = host == null ? null : host.getUUID();
        }
        if (host == null) {
            if (modeTicks > 100) setMode(Mode.WANDER);
            else fly(position().add(0, 0.02, 0), 0.05);
            return;
        }
        Vec3 to = host.position().add(0, host.getBbHeight() * 0.6, 0);
        fly(to, 0.26);
        if (distanceToSqr(to) < 1.6) {
            host.possess(spirit, lord, affliction, sevenfold || mode == Mode.RETURN);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, host.getX(), host.getY() + 1.2, host.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, host.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
            discard();
        }
    }

    /**
     * "When the unclean spirit is gone out of a man, he walketh through dry places, seeking rest, and
     * findeth none. Then he saith, I will return into my house from whence I came out; and when he is
     * come, he findeth it empty, swept, and garnished" (Matt 12:43-44).
     */
    private void wander(ServerLevel level) {
        if (drift == null || distanceToSqr(drift) < 2 || modeTicks % 200 == 0) {
            drift = position().add(random.nextInt(41) - 20, random.nextInt(9) - 3, random.nextInt(41) - 20);
        }
        fly(drift, 0.1);
        if (modeTicks % 100 == 0 && home == null) {   // still looking for a house
            if (level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(48), SpiritEntity::open).stream().findAny().isPresent()) {
                setMode(Mode.SEEK);
                return;
            }
        }
        if (modeTicks > 1200) {
            MortalEntity left = home == null ? null : level.getEntity(home) instanceof MortalEntity m && m.isAlive() ? m : null;
            if (left != null && open(left)) {
                sevenfold = true;   // "then goeth he, and taketh with himself seven other spirits more wicked than himself"
                setMode(Mode.RETURN);
                target = left.getUUID();
                InfernalData d = InfernalData.get(level.getServer());
                d.returned++;
                d.setDirty();
            } else {
                depart();
            }
        }
    }

    /**
     * "And all the devils besought him, saying, Send us into the swine ... and the herd ran violently
     * down a steep place into the sea, (they were about two thousand;) and were choked in the sea" (Mark 5:12-13).
     */
    private void swine(ServerLevel level) {
        Pig pig = level.getEntitiesOfClass(Pig.class, getBoundingBox().inflate(40), Entity::isAlive).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (pig == null || modeTicks > 600) {
            depart();
            return;
        }
        fly(pig.position().add(0, 0.5, 0), 0.3);
        if (distanceToSqr(pig) < 2.5) {
            int n = 0;
            for (Pig p : level.getEntitiesOfClass(Pig.class, pig.getBoundingBox().inflate(16), Entity::isAlive)) {
                p.setDeltaMovement((random.nextDouble() - 0.5) * 1.2, 0.6, (random.nextDouble() - 0.5) * 1.2);
                level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), 30, 0.4, 0.4, 0.4, 0.2);
                p.hurt(level.damageSources().drown(), Float.MAX_VALUE);
                n++;
            }
            level.playSound(null, pig.blockPosition(), SoundEvents.PIG_DEATH, SoundSource.NEUTRAL, 2f, 0.6f);
            Infernal.narrate(level, pig.blockPosition(), "And the unclean spirits went out, and entered into the swine: and the herd ran violently down a steep place into the sea ... and were choked in the sea.", "Mark 5:13");
            InfernalData d = InfernalData.get(level.getServer());
            d.swine += n;
            d.setDirty();
            discard();
        }
    }

    private void fly(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        double dist = d.length();
        if (dist < 1e-3) return;
        Vec3 step = d.scale(Math.min(1, speed / dist)).add(0, Mth.sin(tickCount * 0.15f) * 0.02, 0);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        float yaw = (float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /**
     * Whether a mortal is open to a spirit: its faith is weak - an idol or a false god, or a doubting
     * heart, or more sin than virtue - and no guardian angel stands by it (Psalm 91:10-11).
     */
    public static boolean open(MortalEntity m) {
        if (!m.isAlive() || m.isPossessed() || m.isExorcist() || m.isChild() || m.isNephil() || com.josephison.fiatlux.son.TheSon.isSon(m)) return false;
        Religion r = Faiths.of(m);
        Alignment a = Divinity.alignment(m);
        boolean weak = r == null || !r.deity.true_ || m.doubt() > 0.45 || a.sin() > a.virtue() + 2;
        if (!weak) return false;
        return m.level().getEntitiesOfClass(GuardianEntity.class, m.getBoundingBox().inflate(10)).isEmpty();
    }

    // ------------------------------------------------------------------ the holy cast it out

    /** A stroke of its Creator or of a holy angel casts it into the deep (Luke 8:31; Rev 20:3). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl) || mode == Mode.EXPELLED) return false;
        Entity by = source.getEntity();
        boolean holy = by instanceof ServerPlayer p && !Divinity.get(p).pilgrim() || by instanceof HeavenlyBeing b && !b.isRebel();
        if (!holy && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        castIntoTheDeep(sl);
        return true;
    }

    public void castIntoTheDeep(ServerLevel level) {
        Component cry = Component.literal(spirit + ": \"Send us not away into the deep!\"").withStyle(s -> s.withColor(0xC04040).withItalic(true));
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) p.sendSystemMessage(cry);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.2f, 0.5f);
        InfernalData d = InfernalData.get(level.getServer());
        d.castIntoTheDeep++;
        d.setDirty();
        discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return mode != Mode.EXPELLED;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spirit", spirit);
        tag.putString("Lord", lord);
        tag.putString("Affliction", affliction.name());
        tag.putString("Mode", mode.name());
        if (home != null) tag.putUUID("Home", home);
        tag.putBoolean("Sevenfold", sevenfold);
        tag.putInt("ModeTicks", modeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spirit = tag.getString("Spirit");
        lord = tag.getString("Lord");
        try {
            affliction = Affliction.valueOf(tag.getString("Affliction"));
            mode = Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException e) {
            mode = Mode.SEEK;
        }
        home = tag.hasUUID("Home") ? tag.getUUID("Home") : null;
        sevenfold = tag.getBoolean("Sevenfold");
        modeTicks = tag.getInt("ModeTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 5, state -> state.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
