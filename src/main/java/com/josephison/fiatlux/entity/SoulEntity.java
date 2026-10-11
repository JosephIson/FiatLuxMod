package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.SoulRealms;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The soul of a mortal who has died, in the realm it was judged to.
 *
 * <p>In Heaven the righteous walk the golden streets in light, "and God shall wipe away all tears from
 * their eyes" (Rev 21:4): they gather to praise, sing, and tell of the lives they lived and the worlds
 * they came from. In the Underworld the souls of the wicked are ash and shadow, labouring and weeping
 * ("there shall be weeping and gnashing of teeth", Matt 8:12), and begging, like the rich man, for a
 * drop of water (Luke 16:24). A soul keeps the name and face it had in life; use it to hear who it
 * was. A Creator may sneak and use a soul in the Underworld to redeem it and raise it into Heaven.
 * Souls cannot die again.
 */
public class SoulEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HEAVEN = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.INT);
    public static final int NONE = 0, PRAISE = 1, SING = 2, WORK = 3, WEEP = 4;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mortal.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mortal.move");
    private static final RawAnimation A_PRAISE = RawAnimation.begin().thenLoop("animation.mortal.pray");
    private static final RawAnimation A_SING = RawAnimation.begin().thenLoop("animation.mortal.cheer");
    private static final RawAnimation A_WORK = RawAnimation.begin().thenLoop("animation.mortal.work");
    private static final RawAnimation A_WEEP = RawAnimation.begin().thenLoop("animation.mortal.bow");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private UUID person;
    private String world = "";
    private String life = "";
    private int actTicks, speechTicks;

    public SoulEntity(EntityType<? extends SoulEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEAVEN, true);
        builder.define(MALE, true);
        builder.define(ACT, NONE);
        builder.define(SPEECH, "");
        builder.define(VARIANT, 0);
    }

    /** Gives this soul the person it was (or, for one of the unnumbered dead, only a world and a fate). */
    public void become(@Nullable Person p, String name, boolean male, String world, boolean heaven) {
        this.person = p == null ? null : p.id;
        this.world = world;
        entityData.set(HEAVEN, heaven);
        entityData.set(MALE, male);
        entityData.set(VARIANT, p != null && p.variant >= 0 ? p.variant : random.nextInt(MortalEntity.VARIANTS));
        setCustomName(Component.literal(name));
        setCustomNameVisible(false);
        if (p != null) {
            life = (p.profession.isEmpty() ? "" : "a " + p.profession + " ") + "of " + p.world + ", who died aged " + p.ageAtDeath;
        } else {
            life = "one of the unnumbered dead of " + world;
        }
    }

    public boolean inHeaven() {
        return entityData.get(HEAVEN);
    }

    public boolean isMale() {
        return entityData.get(MALE);
    }

    public int act() {
        return entityData.get(ACT);
    }

    public String speech() {
        return entityData.get(SPEECH);
    }

    @Nullable
    public UUID person() {
        return person;
    }

    /** The face the soul had in life. */
    public int variant() {
        return entityData.get(VARIANT);
    }

    public void say(String words) {
        entityData.set(SPEECH, words);
        speechTicks = 100;
    }

    private void act(int a, int ticks) {
        entityData.set(ACT, a);
        actTicks = ticks;
        getNavigation().stop();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return act() == NONE && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(inHeaven() ? 4 : 6) == 0) {
                level().addParticle(inHeaven() ? ModParticles.LIGHT_MOTE.get() : (random.nextBoolean() ? ParticleTypes.SMOKE : ParticleTypes.SOUL),
                        getRandomX(0.5), getY() + random.nextDouble() * 1.8, getRandomZ(0.5), 0, 0.02, 0);
            }
            return;
        }
        if (speechTicks > 0 && --speechTicks == 0) entityData.set(SPEECH, "");
        if (act() != NONE && --actTicks <= 0) entityData.set(ACT, NONE);
        if (act() != NONE) getNavigation().stop();
        if (tickCount % 20 != 7 || !(level() instanceof ServerLevel sl)) return;
        if (act() == NONE && random.nextInt(14) == 0) {
            if (inHeaven()) {
                if (random.nextBoolean()) act(PRAISE, 120);
                else act(SING, 80);
                say(SoulRealms.heavenLine(random, world, getName().getString()));
            } else {
                if (random.nextBoolean()) act(WORK, 140);
                else act(WEEP, 100);
                say(SoulRealms.underworldLine(random, world, getName().getString()));
            }
        }
        if (act() == SING && tickCount % 40 == 7) {
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.1, getZ(), 1, 0.2, 0.1, 0.2, 0.5);
        }
        if (act() == WEEP && tickCount % 20 == 7) {
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 1.5, getZ(), 2, 0.15, 0.05, 0.15, 0);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        getLookControl().setLookAt(player, 30f, 30f);
        if (!inHeaven() && player.isShiftKeyDown() && !Divinity.get(sp).pilgrim()) {
            SoulRealms.redeem(sp, this);
            return InteractionResult.CONSUME;
        }
        Person p = person == null ? null : Generations.get(sp.server).person(person);
        String kin = p == null ? "" : SoulRealms.kin(sp.server, p);
        player.sendSystemMessage(Component.literal(getName().getString() + ", " + life + (inHeaven() ? " - at rest in Heaven" : " - held in the Underworld")
                + (p != null && p.redeemed ? " (redeemed)" : "")).withStyle(s -> s.withColor(inHeaven() ? 0xFFF0C0 : 0xFF8A6A)));
        if (!kin.isEmpty()) player.sendSystemMessage(Component.literal(kin).withStyle(s -> s.withColor(0xC8C0B0)));
        say(inHeaven() ? SoulRealms.heavenLine(random, world, getName().getString()) : SoulRealms.underworldLine(random, world, getName().getString()));
        player.sendSystemMessage(Component.literal("\"" + speech() + "\"").withStyle(s -> s.withColor(0xFFFFFF).withItalic(true)));
        return InteractionResult.CONSUME;
    }

    /** "It is appointed unto men once to die" (Heb 9:27): souls are not harmed. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && person != null && level() instanceof ServerLevel sl) {
            Person p = Generations.get(sl.getServer()).person(person);
            if (p != null) {   // may be brought forth again
                p.embodied = false;
                Generations.get(sl.getServer()).setDirty();
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Heaven", inHeaven());
        tag.putBoolean("Male", isMale());
        if (person != null) tag.putUUID("Person", person);
        tag.putString("World", world);
        tag.putString("Life", life);
        tag.putInt("Variant", variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HEAVEN, tag.getBoolean("Heaven"));
        entityData.set(MALE, tag.getBoolean("Male"));
        person = tag.hasUUID("Person") ? tag.getUUID("Person") : null;
        world = tag.getString("World");
        life = tag.getString("Life");
        entityData.set(VARIANT, tag.getInt("Variant"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 5, state -> state.setAndContinue(switch (act()) {
            case PRAISE -> A_PRAISE;
            case SING -> A_SING;
            case WORK -> A_WORK;
            case WEEP -> A_WEEP;
            default -> state.isMoving() ? WALK : IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
