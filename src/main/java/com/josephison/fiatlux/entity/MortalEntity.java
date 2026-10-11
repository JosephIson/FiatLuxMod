package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.MortalNames;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.MortalSpeech;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.Trait;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mortal - "and man became a living soul" (Genesis 2:7) - with a life of its own.
 *
 * Each has a name, a family, two traits of temperament, a trade suited to its people's age, and
 * needs: hunger, rest and company. Its day follows them: it works at its trade, eats at the hearth
 * from the settlement's store, seeks out others to talk (in speech bubbles, about their lives and the
 * news of the cosmos), gathers at the shrine at dusk to pray, and sleeps in its bed at night. Mortals
 * court and marry, have children who grow up, grow old and die; each death is judged by the deeds of
 * a life. They hold a faith - first of all the Creator who breathed life into their world, called on
 * by that player's name - lay petitions before that Creator, doubt when heaven is silent, may found
 * faiths of their own, and turn back when they see a miracle. Every life is written in the Book of
 * Generations.
 */
public class MortalEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MALE = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ERA = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACT = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.INT);
    /** 0 child, 1 adult, 2 elder. */
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SPEECH = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.STRING);
    public static final int VARIANTS = 8;
    public static final int NONE = 0, PRAY = 1, WORK = 2, SLEEP = 3, EAT = 4, TALK = 5, BOW = 6, CHEER = 7;
    public static final int ADULT_AGE = 16, ELDER_AGE = 62;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mortal.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mortal.move");
    private static final RawAnimation A_PRAY = RawAnimation.begin().thenLoop("animation.mortal.pray");
    private static final RawAnimation A_WORK = RawAnimation.begin().thenLoop("animation.mortal.work");
    private static final RawAnimation A_SLEEP = RawAnimation.begin().thenLoop("animation.mortal.sleep");
    private static final RawAnimation A_EAT = RawAnimation.begin().thenLoop("animation.mortal.eat");
    private static final RawAnimation A_TALK = RawAnimation.begin().thenLoop("animation.mortal.talk");
    private static final RawAnimation A_BOW = RawAnimation.begin().thenLoop("animation.mortal.bow");
    private static final RawAnimation A_CHEER = RawAnimation.begin().thenLoop("animation.mortal.cheer");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    // ---- life (server)
    private boolean named;
    @Nullable private BlockPos home;
    private int actTicks;
    private double age = 24;
    private int lifespan = 78;
    private Trait traitA = Trait.KIND, traitB = Trait.JOYFUL;
    private Profession profession = Profession.GATHERER;
    @Nullable private UUID spouse, father, mother, religion;
    private String spouseName = "", fatherName = "", motherName = "", lastChildName = "";
    private int children;
    private double hunger = 1, energy = 1, social = 0.6, doubt;
    private int speechTicks;
    private final Map<UUID, Long> greeted = new HashMap<>();
    private final Map<UUID, Integer> opinion = new HashMap<>();
    @Nullable private MortalEntity partner;
    private int talkTicks;
    private long prayedDay = -1;
    private long lastWitness;

    public MortalEntity(EntityType<? extends MortalEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_MALE, true);
        builder.define(DATA_ERA, 0);
        builder.define(DATA_ACT, 0);
        builder.define(DATA_STAGE, 1);
        builder.define(DATA_SPEECH, "");
    }

    // ================================================================== identity

    /**
     * Gives this mortal its life: a name, sex, look, temperament, age and trade, parents and faith,
     * and writes it into the Book of Generations. Pass null for anything to be chosen at random.
     */
    public void becomeLivingSoul(@Nullable String name, @Nullable Boolean male) {
        boolean m = male != null ? male : random.nextBoolean();
        entityData.set(DATA_MALE, m);
        entityData.set(DATA_VARIANT, random.nextInt(VARIANTS));
        setCustomName(Component.literal(name != null ? name : MortalNames.pick(random, m)));
        Trait[] t = Trait.values();
        traitA = t[random.nextInt(t.length)];
        do traitB = t[random.nextInt(t.length)]; while (traitB == traitA);
        lifespan = 66 + random.nextInt(30);
        profession = Profession.pick(era(), random);
        named = true;
        updateStage();
        writeIntoBook();
    }

    /** Writes this mortal into the Book of Generations (also for mortals that lived before the book was kept). */
    private void writeIntoBook() {
        if (!(level() instanceof ServerLevel sl)) return;
        int year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        Person p = new Person(getUUID(), getName().getString(), isMale(), worldName(), sl.getGameTime() - (long) (age * year), father, mother);
        p.profession = profession.id;
        p.religion = religion;
        p.spouse = spouse;
        Generations.get(sl.getServer()).add(p);
    }

    /** Sets the circumstances of a birth (before becomeLivingSoul). */
    public void setOrigin(double ageYears, @Nullable MortalEntity dad, @Nullable MortalEntity mum, @Nullable UUID faith) {
        this.age = ageYears;
        if (dad != null) { father = dad.getUUID(); fatherName = dad.getName().getString(); }
        if (mum != null) { mother = mum.getUUID(); motherName = mum.getName().getString(); }
        this.religion = faith;
    }

    public boolean isMale() { return entityData.get(DATA_MALE); }
    public int variant() { return entityData.get(DATA_VARIANT); }
    public int act() { return entityData.get(DATA_ACT); }
    public boolean isPraying() { return act() == PRAY; }
    public boolean isChild() { return entityData.get(DATA_STAGE) == 0; }
    public boolean isElder() { return entityData.get(DATA_STAGE) == 2; }
    public String speech() { return entityData.get(DATA_SPEECH); }
    public Profession profession() { return profession; }
    public boolean hasTrait(Trait t) { return traitA == t || traitB == t; }
    @Nullable public UUID religion() { return religion; }
    public double doubt() { return doubt; }
    public void setDoubt(double d) { doubt = Math.max(0, Math.min(1, d)); }
    public double ageYears() { return age; }
    public void setHome(BlockPos home) { this.home = home; }
    @Nullable public BlockPos home() { return home; }

    public MortalEra era() {
        MortalEra[] v = MortalEra.values();
        return v[Math.max(0, Math.min(v.length - 1, entityData.get(DATA_ERA)))];
    }

    public void setEra(MortalEra era) {
        entityData.set(DATA_ERA, era.ordinal());
        profession = profession.adapt(era, random);
    }

    public void setReligion(@Nullable UUID faith) {
        religion = faith;
        Person p = person();
        if (p != null) {
            p.religion = faith;
            Generations.get(getServer()).setDirty();
        }
    }

    @Nullable
    private Person person() {
        return getServer() == null ? null : Generations.get(getServer()).person(getUUID());
    }

    @Nullable
    private Planet world() {
        if (!(level() instanceof ServerLevel sl)) return null;
        int slot = RealmPool.slotOf(sl.dimension());
        var planets = CosmicExpanseSavedData.get(sl.getServer()).planets;
        return slot >= 0 && slot < planets.size() ? planets.get(slot) : null;
    }

    public String worldName() {
        Planet w = world();
        return w != null ? w.name : "the earth";
    }

    @Nullable
    public Settlement settlement() {
        Planet w = world();
        return w == null || getServer() == null ? null : CosmicExpanseSavedData.get(getServer()).settlements.get(w.id);
    }

    private void updateStage() {
        entityData.set(DATA_STAGE, age < ADULT_AGE ? 0 : age >= ELDER_AGE ? 2 : 1);
    }

    // ================================================================== speech

    public void say(MortalSpeech.Topic topic) {
        say(MortalSpeech.say(topic, context(), random));
    }

    public void say(String words) {
        entityData.set(DATA_SPEECH, words);
        speechTicks = 50 + words.length() * 2;
    }

    private MortalSpeech.Context context() {
        CosmicExpanseSavedData c = getServer() == null ? null : CosmicExpanseSavedData.get(getServer());
        String other = partner != null ? partner.getName().getString() : spouseName.isEmpty() ? "friend" : spouseName;
        return new MortalSpeech.Context(worldName(), era().getSerializedName().replace('_', '-'), profession, traitA, traitB, isChild(), isElder(),
                other, spouseName.isEmpty() ? "my love" : spouseName, lastChildName.isEmpty() ? "one" : lastChildName,
                c != null && c.rebellionActive, c != null && !c.blackHoles.isEmpty(), c == null ? 1 : c.planets.size(),
                c == null ? 0 : (int) c.fallenAngels);
    }

    private void emote(ParticleOptions particle, int count) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(particle, getX(), getY() + getBbHeight() + 0.3, getZ(), count, 0.3, 0.2, 0.3, 0.02);
    }

    // ================================================================== goals

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10f, 0.75, 1.1));
        goalSelector.addGoal(2, new ActGoal());
        goalSelector.addGoal(3, new SleepGoal());
        goalSelector.addGoal(4, new EatGoal());
        goalSelector.addGoal(5, new EveningPrayerGoal());
        goalSelector.addGoal(6, new WorkGoal());
        goalSelector.addGoal(7, new SocialGoal());
        goalSelector.addGoal(8, new KeepHomeGoal());
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));
    }

    public void beginAct(int act, int ticks) {
        entityData.set(DATA_ACT, act);
        actTicks = ticks;
        getNavigation().stop();
    }

    private void endAct() {
        entityData.set(DATA_ACT, NONE);
        actTicks = 0;
    }

    private boolean isNight() {
        long t = level().getDayTime() % 24000L;
        return t >= 13500 && t < 23200;
    }

    private boolean isDusk() {
        long t = level().getDayTime() % 24000L;
        return t >= 11600 && t < 13200;
    }

    private boolean moveTo(BlockPos p, double speed) {
        return getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, speed);
    }

    private boolean near(BlockPos p, double r) {
        return distanceToSqr(Vec3.atBottomCenterOf(p)) < r * r;
    }

    /** Holds still while praying, working, eating, sleeping, talking. */
    private class ActGoal extends Goal {
        ActGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return act() != NONE; }
        @Override public void start() { getNavigation().stop(); }
        @Override public void tick() {
            if (act() == TALK && partner != null) getLookControl().setLookAt(partner, 30f, 30f);
        }
    }

    /** At night: go home to bed and sleep until morning. */
    private class SleepGoal extends Goal {
        private BlockPos bed;
        SleepGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (!isNight() || act() != NONE) return false;
            Settlement s = settlement();
            bed = s != null && !s.beds.isEmpty() ? s.beds.get(Math.floorMod(getUUID().hashCode(), s.beds.size())) : home;
            return bed != null;
        }
        @Override public void start() { moveTo(bed, 0.7); }
        @Override public boolean canContinueToUse() { return isNight() && act() == NONE && !getNavigation().isDone(); }
        @Override public void stop() {
            if (isNight() && near(bed, 2.5)) {
                if (random.nextInt(3) == 0) say(MortalSpeech.Topic.NIGHT);
                beginAct(SLEEP, 24000);   // until the morning comes
            }
        }
    }

    /** When hungry: eat at the hearth from the store; with no store, forage. */
    private class EatGoal extends Goal {
        private BlockPos hearth;
        EatGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (hunger > 0.35 || act() != NONE) return false;
            Settlement s = settlement();
            hearth = s != null ? s.centre : home;
            return hearth != null;
        }
        @Override public void start() { moveTo(hearth, 0.7); }
        @Override public boolean canContinueToUse() { return act() == NONE && !getNavigation().isDone() && !near(hearth, 3); }
        @Override public void stop() {
            if (!near(hearth, 4)) return;
            Settlement s = settlement();
            if (s == null || s.food > 0) {
                if (s != null) s.food--;
                beginAct(EAT, 60);
                if (random.nextInt(2) == 0) say(MortalSpeech.Topic.EAT);
            } else {
                say(MortalSpeech.Topic.HUNGRY);
                beginAct(PRAY, 80);   // "Give us this day our daily bread"
            }
        }
    }

    /** At dusk the people gather at the shrine to pray together; the priest calls them. */
    private class EveningPrayerGoal extends Goal {
        private BlockPos shrine;
        EveningPrayerGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            long day = level().getDayTime() / 24000L;
            if (!isDusk() || act() != NONE || prayedDay == day) return false;
            Settlement s = settlement();
            shrine = s != null ? s.shrine : null;
            return shrine != null;
        }
        @Override public void start() {
            moveTo(shrine.offset(random.nextInt(5) - 2, 0, 3 + random.nextInt(3)), 0.7);
            if (profession == Profession.PRIEST) say(MortalSpeech.Topic.WORK);
        }
        @Override public boolean canContinueToUse() { return act() == NONE && !getNavigation().isDone(); }
        @Override public void stop() {
            if (near(shrine, 7)) {
                prayedDay = level().getDayTime() / 24000L;
                beginAct(PRAY, 220);
                if (random.nextInt(3) == 0) say(MortalSpeech.Topic.PRAYER);
            }
        }
    }

    /** By day, adults go to their work and do it; what they do changes the settlement. */
    private class WorkGoal extends Goal {
        private BlockPos site;
        WorkGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (isChild() || isNight() || act() != NONE || random.nextInt(hasTrait(Trait.LAZY) ? 160 : hasTrait(Trait.INDUSTRIOUS) ? 30 : 70) != 0) return false;
            site = workSite();
            return site != null;
        }
        @Override public void start() { moveTo(site, 0.65); }
        @Override public boolean canContinueToUse() { return act() == NONE && !getNavigation().isDone() && !near(site, 2.5); }
        @Override public void stop() {
            if (!near(site, 4)) return;
            beginAct(WORK, 120 + random.nextInt(80));
            if (random.nextInt(3) == 0) say(MortalSpeech.Topic.WORK);
        }
    }

    @Nullable
    private BlockPos workSite() {
        Settlement s = settlement();
        BlockPos base = s != null ? s.centre : home;
        if (base == null) return null;
        return switch (profession) {
            case FARMER -> s != null && s.field != null ? s.field.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2) : base;
            case HUNTER, SHEPHERD -> level().getEntitiesOfClass(Animal.class, new AABB(base).inflate(32)).stream().findAny()
                    .map(Entity::blockPosition).orElse(base.offset(random.nextInt(31) - 15, 0, random.nextInt(31) - 15));
            case GATHERER -> base.offset(random.nextInt(41) - 20, 0, random.nextInt(41) - 20);
            case BUILDER -> s != null ? (needsHouse(s) ? s.houseSpot(s.houses.size()) : s.houses.isEmpty() ? base
                    : s.houses.get(s.works % s.houses.size())) : base;
            case PRIEST, SCHOLAR -> s != null ? s.shrine.south(2) : base;
            default -> base.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
        };
    }

    private boolean needsHouse(Settlement s) {
        return level().getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(80)).size() > s.capacity();
    }

    /** What a finished spell of work gives the settlement. */
    private void finishWork(ServerLevel level) {
        Settlement s = settlement();
        switch (profession) {
            case FARMER -> {
                int got = s != null && s.field != null ? MortalPopulation.harvest(level, s.field) : 1;
                if (s != null) s.food += got;
            }
            case GATHERER -> { if (s != null) s.food += 2; else hunger = 1; }
            case HUNTER -> { if (s != null) s.food += 3; }
            case SHEPHERD -> { if (s != null) s.food += 1; }
            case BUILDER -> {
                if (s != null) {
                    if (needsHouse(s)) {
                        MortalPopulation.raiseHouse(level, s, era());
                        say("A new house for a growing people!");
                        CosmosData.get(level.getServer()).record(level.getServer(), getName().getString(), "house",
                                "raised a new house in the settlement of " + worldName() + ".");
                    } else {
                        MortalPopulation.layPath(level, s);
                    }
                }
            }
            case PRIEST -> addVirtue(1);
            default -> {
            }
        }
        if (s != null) CosmicExpanseSavedData.get(level.getServer()).setDirty();
    }

    /** When lonely: find a neighbour, walk over, and talk; friendships grow, and sometimes love. */
    private class SocialGoal extends Goal {
        private MortalEntity other;
        SocialGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (social > 0.45 || isNight() || act() != NONE) return false;
            other = level().getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(16),
                    m -> m != MortalEntity.this && m.act() == NONE && !m.isNight()).stream().findAny().orElse(null);
            return other != null;
        }
        @Override public void start() { getNavigation().moveTo(other, 0.65); }
        @Override public boolean canContinueToUse() { return act() == NONE && other.isAlive() && other.act() == NONE && distanceToSqr(other) > 4.5; }
        @Override public void stop() {
            if (other.isAlive() && other.act() == NONE && distanceToSqr(other) <= 6.5) startConversation(other);
        }
    }

    private void startConversation(MortalEntity other) {
        partner = other;
        other.partner = this;
        beginAct(TALK, 110);
        other.beginAct(TALK, 110);
        talkTicks = 0;
        other.talkTicks = -1000;   // the listener waits for the opener
        say(isChild() ? MortalSpeech.Topic.CHILD : MortalSpeech.Topic.CHAT);
    }

    private void conversationTick(ServerLevel level) {
        if (partner == null || act() != TALK) return;
        talkTicks++;
        if (talkTicks == 45 && partner.isAlive()) {
            boolean quarrel = (hasTrait(Trait.PROUD) || hasTrait(Trait.GREEDY)) && (partner.hasTrait(Trait.PROUD) || partner.hasTrait(Trait.GREEDY))
                    && random.nextInt(3) == 0;
            if (quarrel) {
                partner.say(MortalSpeech.Topic.QUARREL);
                emote(ParticleTypes.ANGRY_VILLAGER, 4);
                partner.emote(ParticleTypes.ANGRY_VILLAGER, 4);
                addSin(1);
                partner.addSin(1);
                opinion.merge(partner.getUUID(), -2, Integer::sum);
                partner.opinion.merge(getUUID(), -2, Integer::sum);
            } else {
                partner.say(MortalSpeech.Topic.REPLY);
                opinion.merge(partner.getUUID(), 1, Integer::sum);
                partner.opinion.merge(getUUID(), 1, Integer::sum);
                if (Faiths.maybeConvert(this, partner, random)) partner.emote(ParticleTypes.ENCHANT, 12);
                else if (Faiths.maybeConvert(partner, this, random)) emote(ParticleTypes.ENCHANT, 12);
                tryMarry(level, partner);
            }
            social = 1;
            partner.social = 1;
        }
    }

    /** "Therefore shall a man leave his father and his mother, and shall cleave unto his wife" (Gen 2:24). */
    private void tryMarry(ServerLevel level, MortalEntity other) {
        if (spouse != null || other.spouse != null || isChild() || other.isChild() || isMale() == other.isMale()) return;
        if (opinion.getOrDefault(other.getUUID(), 0) < 3 || other.opinion.getOrDefault(getUUID(), 0) < 3) return;
        if (other.getUUID().equals(father) || other.getUUID().equals(mother) || (father != null && father.equals(other.father))) return;
        spouse = other.getUUID();
        spouseName = other.getName().getString();
        other.spouse = getUUID();
        other.spouseName = getName().getString();
        Generations g = Generations.get(level.getServer());
        Person a = g.person(getUUID()), b = g.person(other.getUUID());
        if (a != null) a.spouse = other.getUUID();
        if (b != null) b.spouse = getUUID();
        g.setDirty();
        say(MortalSpeech.Topic.WEDDING);
        other.say(MortalSpeech.Topic.WEDDING);
        emote(ParticleTypes.HEART, 8);
        other.emote(ParticleTypes.HEART, 8);
        for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(16), m -> m != this && m != other)) {
            m.beginAct(CHEER, 60);
        }
        CosmosData.get(level.getServer()).record(level.getServer(), getName().getString(), "wedding",
                "and " + other.getName().getString() + " were joined in marriage on " + worldName() + ".");
    }

    /** Mortals keep near their hearth: within a stroll by day, close in by night. */
    private class KeepHomeGoal extends Goal {
        KeepHomeGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        private double range() { return isNight() ? 7.0 : isChild() ? 14.0 : 24.0; }
        @Override public boolean canUse() { return home != null && act() == NONE && distanceToSqr(Vec3.atBottomCenterOf(home)) > range() * range(); }
        @Override public void start() { moveTo(home, 0.7); }
        @Override public boolean canContinueToUse() { return home != null && !getNavigation().isDone() && !near(home, 3); }
    }

    // ================================================================== life

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!named) becomeLivingSoul(null, null);
        if (speechTicks > 0 && --speechTicks == 0) entityData.set(DATA_SPEECH, "");
        conversationTick(sl);
        if (act() != NONE && act() != SLEEP && --actTicks <= 0) finishAct(sl);
        if (act() == SLEEP && !isNight()) {
            endAct();
            energy = 1;
            if (random.nextInt(3) == 0) say(MortalSpeech.Topic.MORNING);
        }
        if (act() == PRAY && tickCount % 20 == 0) {
            sl.sendParticles(com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), getX(), getY() + 1.4, getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        }
        if (tickCount % 20 == 3) lifeTick(sl);
    }

    private void finishAct(ServerLevel level) {
        int a = act();
        if (a == PRAY) finishPrayer(level);
        else if (a == WORK) finishWork(level);
        else if (a == EAT) hunger = 1;
        else if (a == TALK) partner = null;
        endAct();
    }

    private void finishPrayer(ServerLevel level) {
        Religion faith = Faiths.of(this);
        Planet w = world();
        if (faith != null && faith.deity.true_) {
            addVirtue(1);
            if (w != null) w.faith += 1;
            Fx.send(level, FxKind.BLESSING, position(), 2.2f, 0xFFFFF0C0, 16);   // the prayer rises
            Settlement s = settlement();
            if (s != null && random.nextInt(4) == 0) maybePetition(level, s, faith);
        } else if (faith != null && (faith.deity == Religion.Deity.IDOL || faith.deity == Religion.Deity.FALLEN)) {
            addSin(1);
            CosmicExpanseSavedData.get(level.getServer()).corruption += 0.0005;
            emote(ParticleTypes.SMOKE, 6);
        }
        CosmicExpanseSavedData.get(level.getServer()).setDirty();
    }

    private void maybePetition(ServerLevel level, Settlement s, Religion faith) {
        String d = faith.deityName;
        boolean hurt = !level.getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(40), m -> m.getHealth() < m.getMaxHealth() * 0.6).isEmpty();
        boolean danger = !level.getEntitiesOfClass(Monster.class, new AABB(s.centre).inflate(28)).isEmpty();
        if (s.food < 6) Faiths.petition(level, this, s, Faiths.Petition.BREAD, "O " + d + ", give us bread; our store is empty.");
        else if (hurt) Faiths.petition(level, this, s, Faiths.Petition.HEALING, "Heal the sick among us, " + d + ".");
        else if (danger) Faiths.petition(level, this, s, Faiths.Petition.PROTECTION, "Protect us from the things that walk in the dark, " + d + ".");
        else if (spouse != null && children == 0 && age > 24 && !isMale()) Faiths.petition(level, this, s, Faiths.Petition.CHILD, "Grant us a child, " + d + ".");
        else if (doubt > 0.5) Faiths.petition(level, this, s, Faiths.Petition.SIGN, "Show us a sign, " + d + ", that You hear us.");
    }

    /** Once a second: ageing, needs, faith, temperament, family. */
    private void lifeTick(ServerLevel level) {
        int year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        if (tickCount % 200 == 3 && person() == null) writeIntoBook();
        double before = age;
        age += 20.0 / year;
        if ((int) before < ADULT_AGE && (int) age >= ADULT_AGE) {
            profession = Profession.pick(era(), random);
            say("I am grown now. I shall be a " + profession.id + ".");
            Person p = person();
            if (p != null) { p.profession = profession.id; Generations.get(level.getServer()).setDirty(); }
        }
        updateStage();
        hunger = Math.max(0, hunger - 20.0 / 7000.0);
        social = Math.max(0, social - 20.0 / 4000.0);
        energy = Math.max(0, energy - 20.0 / 14000.0);
        if (tickCount % 100 == 3) {
            Planet w = world();
            if (w != null && w.era.ordinal() != entityData.get(DATA_ERA)) setEra(w.era);
        }
        if (age > lifespan) {
            hurt(damageSources().generic(), Float.MAX_VALUE);   // full of years (Gen 25:8)
            return;
        }
        reactToSurroundings(level);
        faithTick(level, year);
        familyTick(level, year);
        viceTick(level, year);
        if (speechTicks == 0 && act() == NONE && random.nextInt(40) == 0) {
            say(hunger < 0.3 ? MortalSpeech.Topic.HUNGRY : energy < 0.2 ? MortalSpeech.Topic.TIRED : MortalSpeech.Topic.IDLE);
        }
        if (random.nextInt(4) == 0) {   // moods show
            double mood = (hunger + social + energy) / 3 - doubt * 0.3;
            if (mood > 0.75 && random.nextInt(4) == 0) emote(ParticleTypes.HAPPY_VILLAGER, 2);
            else if (mood < 0.3) emote(ParticleTypes.DRIPPING_WATER, 2);
        }
    }

    /** The Creator, angels and the fallen are noticed, and met as each temperament would. */
    private void reactToSurroundings(ServerLevel level) {
        if (act() == SLEEP) return;
        long now = level.getGameTime();
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7), pl -> !pl.isSpectator())) {
            if (!(p instanceof ServerPlayer sp) || Divinity.get(sp).pilgrim()) continue;
            if (now - greeted.getOrDefault(p.getUUID(), -99999L) < 2400) continue;
            greeted.put(p.getUUID(), now);
            getLookControl().setLookAt(p, 30f, 30f);
            Religion faith = Faiths.of(this);
            boolean theirGod = faith != null && p.getUUID().equals(faith.creator);
            String name = p.getGameProfile().getName();
            if (theirGod) {
                String[] lines = hasTrait(Trait.PROUD) && doubt > 0.4
                        ? new String[]{"So, " + name + ", You come at last. Where were You?", "Our prayers went unanswered, " + name + "."}
                        : new String[]{name + "! You walk among us!", "Holy is " + name + ", maker of " + worldName() + "!",
                        "Blessed be " + name + ", who breathed life into us!", "My Lord " + name + "... I am not worthy."};
                say(lines[random.nextInt(lines.length)]);
                beginAct(hasTrait(Trait.PIOUS) ? PRAY : BOW, 50);
            } else if (faith != null && !faith.deity.true_) {
                say(hasTrait(Trait.FEARFUL) ? "Stay back, shining one! We serve " + faith.deityName + "!" : "You are not " + faith.deityName + ". What are you?");
            } else {
                say(MortalSpeech.Topic.GREET_CREATOR);
                beginAct(BOW, 40);
            }
            return;
        }
        if (now - lastWitness < 2400) return;
        for (HeavenlyBeing b : level.getEntitiesOfClass(HeavenlyBeing.class, getBoundingBox().inflate(12))) {
            lastWitness = now;
            if (b.isRebel()) {
                say(MortalSpeech.Topic.FALLEN);
            } else {
                say(MortalSpeech.Topic.ANGEL);
                beginAct(BOW, 60);
                setDoubt(doubt - 0.3);
            }
            return;
        }
        if (!level.getEntitiesOfClass(FallenEntity.class, getBoundingBox().inflate(14)).isEmpty()) {
            lastWitness = now;
            say(MortalSpeech.Topic.FALLEN);
        }
    }

    /** A wonder was worked near this mortal. */
    public void witness(boolean wrathful) {
        if (act() == SLEEP) endAct();
        if (wrathful) {
            say(MortalSpeech.Topic.TERROR);
            emote(ParticleTypes.SPLASH, 6);
        } else {
            say(MortalSpeech.Topic.MIRACLE);
            beginAct(CHEER, 50);
            addVirtue(1);
        }
    }

    /** Doubt grows when heaven is silent; the proud and the curious doubt first, and some found faiths of their own. */
    private void faithTick(ServerLevel level, int year) {
        Settlement s = settlement();
        Religion faith = Faiths.of(this);
        if (religion == null && s != null) {   // newcomers take up the faith they find
            var lifeGiver = CosmicExpanseSavedData.get(level.getServer()).lifeGiverOf(world());
            if (lifeGiver != null) setReligion(Generations.get(level.getServer()).faithOf(lifeGiver.getFirst(), lifeGiver.getSecond(), worldName(), level.getGameTime()).id);
            return;
        }
        if (faith == null || s == null || !faith.deity.true_) return;
        if (level.getGameTime() - s.lastSign > Faiths.SILENCE) {
            double rate = hasTrait(Trait.PROUD) || hasTrait(Trait.CURIOUS) ? 0.004 : hasTrait(Trait.PIOUS) ? 0.0005 : 0.001;
            setDoubt(doubt + rate);
        }
        if (doubt > 0.85 && (profession == Profession.PRIEST || hasTrait(Trait.PROUD)) && random.nextInt(600) == 0
                && level.getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(80)).size() >= 4) {
            Religion born = Faiths.schism(level, this, worldName());
            say("No more waiting on silent heavens. We will worship " + born.deityName + "!");
            emote(ParticleTypes.SMOKE, 10);
        }
    }

    /** Married women of age may bear children; answered prayers for a child are kept. */
    private void familyTick(ServerLevel level, int year) {
        if (isMale() || spouse == null || age < 18 || age > 45) return;
        Entity husband = level.getEntity(spouse);
        if (!(husband instanceof MortalEntity dad) || distanceToSqr(dad) > 48 * 48) return;
        Settlement s = settlement();
        int max = FiatLuxConfig.COMMON.maxMortalsPerSettlement.get();
        if (level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(80)).size() >= max) return;
        boolean blessed = s != null && s.blessedWithChild;
        double chance = 20.0 / (year * 2.5);
        if (!blessed && random.nextDouble() > chance) return;
        if (blessed) s.blessedWithChild = false;
        MortalEntity child = MortalPopulation.bringForth(level, blockPosition(), home, era(), null, null, 0, dad, this, religion);
        if (child == null) return;
        children++;
        dad.children++;
        lastChildName = dad.lastChildName = child.getName().getString();
        say(MortalSpeech.Topic.BIRTH);
        dad.say(MortalSpeech.Topic.BIRTH);
        emote(ParticleTypes.HEART, 6);
        CosmosData.get(level.getServer()).record(level.getServer(), getName().getString(), "birth",
                "and " + dad.getName().getString() + " welcomed a " + (child.isMale() ? "son" : "daughter") + ", " + child.getName().getString()
                        + ", on " + worldName() + ".");
    }

    /** Small sins of temperament: quarrels, and taking from the common store. */
    private void viceTick(ServerLevel level, int year) {
        double vice = traitA.vice + traitB.vice;
        if (vice <= 0 || isChild() || random.nextDouble() > vice * 20.0 / 24000.0) return;
        Settlement s = settlement();
        if (hasTrait(Trait.GREEDY) && s != null && s.food > 2) {
            s.food -= 2;
            hunger = 1;
            addSin(2);
        } else {
            say(MortalSpeech.Topic.QUARREL);
            emote(ParticleTypes.ANGRY_VILLAGER, 3);
            addSin(1);
        }
    }

    public void addVirtue(int n) {
        Divinity.setAlignment(this, Divinity.alignment(this).addVirtue(n));
    }

    public void addSin(int n) {
        Divinity.setAlignment(this, Divinity.alignment(this).addSin(n));
    }

    // ================================================================== death and judgment

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl && !isRemoved() && !dead) {
            Alignment a = Divinity.alignment(this);
            boolean saved = a.sin() <= a.virtue() + 3;
            CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(sl.getServer());
            if (saved) {
                cosmos.heavenSouls += 1;
                sl.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), getX(), getY() + 1, getZ(), 40, 0.3, 1.2, 0.3, 0.1);
                Fx.send(sl, FxKind.CELESTIAL_PILLAR, position(), 30f, 0xFFFFF0C0, 30);
            } else {
                cosmos.underworld.souls += 1;
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 30, 0.3, 0.3, 0.3, 0.02);
            }
            cosmos.setDirty();
            Person p = person();
            int years = (int) age;
            if (p != null) {
                p.died = sl.getGameTime();
                p.soul = saved ? "heaven" : "underworld";
                p.ageAtDeath = years;
                Generations.get(sl.getServer()).setDirty();
            }
            String lineage = fatherName.isEmpty() && motherName.isEmpty() ? "" : ", " + (isMale() ? "son" : "daughter") + " of "
                    + (fatherName.isEmpty() ? motherName : motherName.isEmpty() ? fatherName : fatherName + " and " + motherName);
            CosmosData.get(sl.getServer()).record(sl.getServer(), getName().getString() + lineage, "death",
                    "died on " + worldName() + " aged " + years + (saved ? "; their soul was taken up to Heaven." : "; their soul went down to the Underworld."));
            if (spouse != null && sl.getEntity(spouse) instanceof MortalEntity widow) {
                widow.partner = this;
                widow.say(MortalSpeech.Topic.MOURNING);
                widow.emote(ParticleTypes.DRIPPING_WATER, 8);
                widow.spouse = null;
                widow.spouseName = "";
            }
        }
        super.die(source);
    }

    // ================================================================== meeting a mortal

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        getLookControl().setLookAt(player, 30f, 30f);
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp && !Divinity.get(sp).pilgrim()) {
            // the Creator lays a hand in blessing
            addVirtue(3);
            heal(getMaxHealth());
            setDoubt(0);
            Religion faith = Generations.get(sp.server).faithOf(sp.getUUID(), sp.getGameProfile().getName(), worldName(), level().getGameTime());
            setReligion(faith.id);
            say(MortalSpeech.Topic.BLESSED);
            emote(ParticleTypes.HEART, 6);
            Fx.send((ServerLevel) level(), FxKind.BLESSING, position(), getBbHeight(), 0xFFFFF0C0, 30);
            return InteractionResult.CONSUME;
        }
        Generations g = Generations.get(player.getServer());
        Religion faith = g.religion(religion);
        String lineage = fatherName.isEmpty() && motherName.isEmpty() ? "" : (isMale() ? ", son of " : ", daughter of ")
                + (fatherName.isEmpty() ? motherName : motherName.isEmpty() ? fatherName : fatherName + " and " + motherName);
        String stage = isChild() ? "child" : isElder() ? "elder" : profession.id;
        player.sendSystemMessage(Component.literal(getName().getString() + lineage + " - " + (int) age + ", " + stage
                + " (" + traitA.id + ", " + traitB.id + ")").withStyle(s -> s.withColor(0xF5D77A)));
        String family = (spouse != null ? (isMale() ? "Husband of " : "Wife of ") + spouseName : isChild() ? "" : "Unmarried")
                + (children > 0 ? " - " + children + (children == 1 ? " child" : " children") : "");
        String belief = faith == null ? "Seeking a faith" : faith.name + (faith.deity.true_ ? (doubt > 0.5 ? " (doubting)" : " (devout)") : "");
        String needs = (hunger < 0.35 ? "hungry" : "fed") + ", " + (energy < 0.25 ? "weary" : "rested") + ", " + (social < 0.3 ? "lonely" : "content");
        player.sendSystemMessage(Component.literal(family + (family.isEmpty() ? "" : " · ") + belief + " · " + needs).withStyle(s -> s.withColor(0xC8C0B0)));
        say(isChild() ? MortalSpeech.Topic.CHILD : MortalSpeech.Topic.IDLE);
        player.sendSystemMessage(Component.literal("\"" + speech() + "\"").withStyle(s -> s.withColor(0xFFFFFF).withItalic(true)));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ================================================================== persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putBoolean("Male", isMale());
        tag.putInt("Era", entityData.get(DATA_ERA));
        tag.putBoolean("Named", named);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putDouble("Age", age);
        tag.putInt("Lifespan", lifespan);
        tag.putString("TraitA", traitA.id);
        tag.putString("TraitB", traitB.id);
        tag.putString("Profession", profession.id);
        if (spouse != null) tag.putUUID("Spouse", spouse);
        if (father != null) tag.putUUID("Father", father);
        if (mother != null) tag.putUUID("Mother", mother);
        if (religion != null) tag.putUUID("Religion", religion);
        tag.putString("SpouseName", spouseName);
        tag.putString("FatherName", fatherName);
        tag.putString("MotherName", motherName);
        tag.putString("LastChild", lastChildName);
        tag.putInt("Children", children);
        tag.putDouble("Hunger", hunger);
        tag.putDouble("Energy", energy);
        tag.putDouble("Social", social);
        tag.putDouble("Doubt", doubt);
        tag.putLong("PrayedDay", prayedDay);
        CompoundTag op = new CompoundTag();
        opinion.forEach((k, v) -> op.putInt(k.toString(), v));
        tag.put("Opinion", op);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        entityData.set(DATA_MALE, !tag.contains("Male") || tag.getBoolean("Male"));
        entityData.set(DATA_ERA, tag.getInt("Era"));
        named = tag.getBoolean("Named");
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        if (tag.contains("Age")) age = tag.getDouble("Age");
        if (tag.contains("Lifespan")) lifespan = tag.getInt("Lifespan");
        if (tag.contains("TraitA")) traitA = Trait.byId(tag.getString("TraitA"));
        if (tag.contains("TraitB")) traitB = Trait.byId(tag.getString("TraitB"));
        if (tag.contains("Profession")) profession = Profession.byId(tag.getString("Profession"));
        spouse = tag.hasUUID("Spouse") ? tag.getUUID("Spouse") : null;
        father = tag.hasUUID("Father") ? tag.getUUID("Father") : null;
        mother = tag.hasUUID("Mother") ? tag.getUUID("Mother") : null;
        religion = tag.hasUUID("Religion") ? tag.getUUID("Religion") : null;
        spouseName = tag.getString("SpouseName");
        fatherName = tag.getString("FatherName");
        motherName = tag.getString("MotherName");
        lastChildName = tag.getString("LastChild");
        children = tag.getInt("Children");
        if (tag.contains("Hunger")) hunger = tag.getDouble("Hunger");
        if (tag.contains("Energy")) energy = tag.getDouble("Energy");
        if (tag.contains("Social")) social = tag.getDouble("Social");
        doubt = tag.getDouble("Doubt");
        prayedDay = tag.contains("PrayedDay") ? tag.getLong("PrayedDay") : -1;
        CompoundTag op = tag.getCompound("Opinion");
        for (String k : op.getAllKeys()) {
            try {
                opinion.put(UUID.fromString(k), op.getInt(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        updateStage();
    }

    // ================================================================== GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 5, state -> state.setAndContinue(switch (act()) {
            case PRAY -> A_PRAY;
            case WORK -> A_WORK;
            case SLEEP -> A_SLEEP;
            case EAT -> A_EAT;
            case TALK -> A_TALK;
            case BOW -> A_BOW;
            case CHEER -> A_CHEER;
            default -> state.isMoving() ? WALK : IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
