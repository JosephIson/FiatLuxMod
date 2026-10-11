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
    /** Unashamed in the garden: no coat of skins yet, only plain linen (Gen 2:25, 3:21). */
    private static final EntityDataAccessor<Boolean> DATA_INNOCENT = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.BOOLEAN);
    /** The office this mortal holds in its people's government ("King", "Judge", ...), or "". */
    private static final EntityDataAccessor<String> DATA_TITLE = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.STRING);
    /** One of the Nephilim, a Watcher's son: "there were giants in the earth in those days" (Gen 6:4). */
    private static final EntityDataAccessor<Boolean> DATA_NEPHIL = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.BOOLEAN);
    /** An unclean spirit is in this one (Mark 1:23). */
    private static final EntityDataAccessor<Boolean> DATA_POSSESSED = SynchedEntityData.defineId(MortalEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int VARIANTS = 8;
    public static final int NONE = 0, PRAY = 1, WORK = 2, SLEEP = 3, EAT = 4, TALK = 5, BOW = 6, CHEER = 7,
            CRUCIFIED = 8, STOCKS = 9, EXORCISE = 10, IMPRISONED = 11, POSSESSED = 12, TORN = 13;
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
    private static final RawAnimation A_CRUCIFIED = RawAnimation.begin().thenLoop("animation.mortal.crucified");
    private static final RawAnimation A_STOCKS = RawAnimation.begin().thenLoop("animation.mortal.stocks");
    private static final RawAnimation A_EXORCISE = RawAnimation.begin().thenLoop("animation.mortal.exorcise");
    private static final RawAnimation A_POSSESSED = RawAnimation.begin().thenLoop("animation.mortal.possessed");
    private static final RawAnimation A_TORN = RawAnimation.begin().thenLoop("animation.mortal.torn");

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
    // ---- under the law
    private com.josephison.fiatlux.civilization.Justice.Sentence sentence = com.josephison.fiatlux.civilization.Justice.Sentence.NONE;
    private com.josephison.fiatlux.civilization.Polity.Crime sentenceCrime = com.josephison.fiatlux.civilization.Polity.Crime.THEFT;
    private long sentenceUntil;
    @Nullable private BlockPos sentencePlace;
    /** Gone out to found a new town: ticks on the road. */
    private boolean leaving;
    /** "And the LORD set a mark upon Cain, lest any finding him should kill him" (Gen 4:15). */
    private boolean marked;
    private int roadTicks;
    /** Taken to wife by a Watcher (Gen 6:2), and the giants she has borne him. */
    @Nullable private UUID watcherHusband;
    private String watcherHusbandName = "";
    private int giants;
    // ---- unclean spirits, and those who cast them out
    private String spirit = "", spiritLord = "", castOutBy = "", anointedBy = "";
    private com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction affliction = com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction.DISCORD;
    private boolean sevenfold, beingCastOut, exorcist;
    @Nullable private UUID casting;
    private double prayerAndFasting;

    public MortalEntity(EntityType<? extends MortalEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);   // they live in houses with doors
            nav.setCanPassDoors(true);
        }
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
        builder.define(DATA_INNOCENT, false);
        builder.define(DATA_TITLE, "");
        builder.define(DATA_NEPHIL, false);
        builder.define(DATA_POSSESSED, false);
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
        p.variant = variant();
        p.nephil = isNephil();
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
    public boolean looksInnocent() { return entityData.get(DATA_INNOCENT); }
    public String title() { return entityData.get(DATA_TITLE); }
    public com.josephison.fiatlux.civilization.Justice.Sentence sentence() { return sentence; }
    public com.josephison.fiatlux.civilization.Polity.Crime sentenceCrime() { return sentenceCrime; }
    public long sentenceUntil() { return sentenceUntil; }
    @Nullable public BlockPos sentencePlace() { return sentencePlace; }

    public void sentence(com.josephison.fiatlux.civilization.Justice.Sentence s, com.josephison.fiatlux.civilization.Polity.Crime crime, long until, BlockPos place) {
        sentence = s;
        sentenceCrime = crime;
        sentenceUntil = until;
        sentencePlace = place;
    }

    /** Set free (or released by death). */
    public void release() {
        sentence = com.josephison.fiatlux.civilization.Justice.Sentence.NONE;
        sentencePlace = null;
        if (act() == CRUCIFIED || act() == STOCKS || act() == IMPRISONED) endAct();
    }

    @Nullable
    public Planet planet() {
        return world();
    }

    /** Before the Fall of Man: neither ageing, hunger, sin nor death. */
    private boolean innocent(ServerLevel level) {
        return !CosmicExpanseSavedData.get(level.getServer()).fallOfMan;
    }
    public Profession profession() { return profession; }
    public boolean hasTrait(Trait t) { return traitA == t || traitB == t; }
    @Nullable public UUID religion() { return religion; }
    public double doubt() { return doubt; }
    public void setDoubt(double d) { doubt = Math.max(0, Math.min(1, d)); }
    public double ageYears() { return age; }
    public void setHome(BlockPos home) { this.home = home; }
    public boolean isLeaving() { return leaving; }
    public boolean isMarked() { return marked; }
    public boolean isNephil() { return entityData.get(DATA_NEPHIL); }
    @Nullable public UUID watcherHusband() { return watcherHusband; }
    public String fatherName() { return fatherName; }

    /** "The sons of God ... took them wives of all which they chose" (Gen 6:2). */
    public void takenBy(UUID watcher, String name) {
        watcherHusband = watcher;
        watcherHusbandName = name;
        say(spouse != null ? "He is no man... and he has chosen me." : "He shines like the morning. He has chosen me.");
        emote(ParticleTypes.ENCHANT, 12);
    }

    /** Walks somewhere (or, if the way is barred, waits). */
    public void walkTo(BlockPos p, double speed) {
        if (act() == NONE) moveTo(p, speed);
    }

    /** A giant grows to more than twice the height of a man as it comes of age, and is as strong. */
    private void growAsGiant() {
        double grown = Math.min(1, age / ADULT_AGE);
        var scale = getAttribute(Attributes.SCALE);
        double want = 1.0 + 1.3 * grown;
        if (scale != null && Math.abs(scale.getBaseValue() - want) > 0.02) scale.setBaseValue(want);
        var health = getAttribute(Attributes.MAX_HEALTH);
        double hp = 20 + 60 * grown;
        if (health != null && Math.abs(health.getBaseValue() - hp) > 1) {
            health.setBaseValue(hp);
            setHealth(Math.min(getHealth() + 10, getMaxHealth()));
        }
    }
    public void markAsCain() { marked = true; }

    /** The living spouse, if near enough to be loaded. */
    @Nullable
    public MortalEntity spouseEntity() {
        return spouse != null && level() instanceof ServerLevel sl && sl.getEntity(spouse) instanceof MortalEntity m ? m : null;
    }

    /** Goes out to the site of a new town and makes a home there. */
    public void leaveFor(BlockPos site) {
        home = site;
        leaving = true;
        roadTicks = 0;
        say(random.nextBoolean() ? "We go out to find a land of our own." : "Farewell! We shall build a new town beyond the hills.");
        emote(ParticleTypes.HAPPY_VILLAGER, 4);
    }

    /** On the road: walks home, and if the way is long and no one watches, is simply there. */
    private void travelTick(ServerLevel level) {
        if (!leaving || home == null) return;
        roadTicks += 20;
        double d = Math.sqrt(distanceToSqr(Vec3.atBottomCenterOf(home)));
        if (d < 12) {
            leaving = false;
            say("Here we shall live. Let us build.");
            return;
        }
        boolean watched = level.getNearestPlayer(this, 40) != null;
        if (roadTicks > 2400 || (!watched && d > 40 && roadTicks > 200)) {
            BlockPos at = MortalPopulation.safeGround(level, home.getX() + random.nextInt(7) - 3, home.getZ() + random.nextInt(7) - 3, 10);
            if (at != null) teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            leaving = false;
        }
    }
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
        return w == null || getServer() == null ? null : CosmicExpanseSavedData.get(getServer()).townNear(w.id, home != null ? home : blockPosition());
    }

    private void updateStage() {
        entityData.set(DATA_STAGE, age < ADULT_AGE ? 0 : age >= ELDER_AGE ? 2 : 1);
    }

    // ================================================================== speech

    public void say(MortalSpeech.Topic topic) {
        say(MortalSpeech.say(topic, context(), random));
    }

    public void say(String words) {
        if (isPossessed() && affliction == com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction.MUTENESS && !beingCastOut) {
            words = "...";   // "a dumb spirit" (Mark 9:17)
        }
        entityData.set(DATA_SPEECH, words);
        speechTicks = 50 + words.length() * 2;
    }

    private MortalSpeech.Context context() {
        CosmicExpanseSavedData c = getServer() == null ? null : CosmicExpanseSavedData.get(getServer());
        String other = partner != null ? partner.getName().getString() : spouseName.isEmpty() ? "friend" : spouseName;
        return new MortalSpeech.Context(worldName(), era().getSerializedName().replace('_', '-'), profession, traitA, traitB, isChild(), isElder(),
                other, spouseName.isEmpty() ? "my love" : spouseName, lastChildName.isEmpty() ? "one" : lastChildName,
                c != null && c.rebellionActive, c != null && !c.blackHoles.isEmpty(), c == null ? 1 : c.planets.size(),
                c == null ? 0 : (int) c.fallenAngels, c != null && c.fallOfMan, c != null && c.eden == com.josephison.fiatlux.eden.EdenState.EXILED);
    }

    private void emote(ParticleOptions particle, int count) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(particle, getX(), getY() + getBbHeight() + 0.3, getZ(), count, 0.3, 0.2, 0.3, 0.02);
    }

    // ================================================================== goals

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
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
            if (!isDusk() || act() != NONE || prayedDay == day || isPossessed()) return false;
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
            if (isChild() || isNight() || act() != NONE || isPossessed() || random.nextInt(hasTrait(Trait.LAZY) ? 160 : hasTrait(Trait.INDUSTRIOUS) ? 30 : 70) != 0) return false;
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
                        String built = MortalPopulation.renew(level, s, era());
                        if (!built.isEmpty()) {
                            say("Behold, we have built anew!");
                            CosmosData.get(level.getServer()).record(level.getServer(), getName().getString(), "house",
                                    built + " in " + (s.name.isEmpty() ? worldName() : s.name) + ".");
                        } else {
                            MortalPopulation.layPath(level, s);
                        }
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
                if (hasTrait(Trait.PROUD) && opinion.getOrDefault(partner.getUUID(), 0) < -5 && random.nextInt(3) == 0 && !innocent(level)) {
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);   // words become blows
                    partner.hurt(damageSources().mobAttack(this), 3f);
                    com.josephison.fiatlux.civilization.Justice.report(level, this, com.josephison.fiatlux.civilization.Polity.Crime.VIOLENCE, partner);
                }
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
        com.josephison.fiatlux.civilization.Weddings.betroth(level, this, other);
    }

    @Nullable public UUID spouseId() { return spouse; }
    @Nullable public UUID fatherId() { return father; }
    @Nullable public UUID motherId() { return mother; }
    public int childCount() { return children; }

    /**
     * Joins this one and `other` in marriage: each is written into the Book of Generations as the
     * other's spouse, and the Chronicle tells of it. `how` completes the sentence (e.g. "at the shrine of Enoch before Elder Ezra").
     */
    public void wed(ServerLevel level, MortalEntity other, String how) {
        if (spouse != null || other.spouse != null || isMale() == other.isMale()) return;
        spouse = other.getUUID();
        spouseName = other.getName().getString();
        other.spouse = getUUID();
        other.spouseName = getName().getString();
        Generations g = Generations.get(level.getServer());
        Person a = g.person(getUUID()), b = g.person(other.getUUID());
        if (a != null) a.spouse = other.getUUID();
        if (b != null) b.spouse = getUUID();
        g.setDirty();
        emote(ParticleTypes.HEART, 8);
        other.emote(ParticleTypes.HEART, 8);
        MortalEntity husband = isMale() ? this : other, wife = isMale() ? other : this;
        if (husband.isFirstParent() && wife.isFirstParent()) {   // "and they shall be one flesh" (Gen 2:24): the first pair live long
            husband.lifespan = Math.max(husband.lifespan, 120 + random.nextInt(20));
            wife.lifespan = Math.max(wife.lifespan, 115 + random.nextInt(20));
        }
        CosmosData.get(level.getServer()).record(level.getServer(), husband.getName().getString(), "wedding",
                "and " + wife.getName().getString() + " were joined in marriage on " + worldName() + (how.isEmpty() ? "" : ", " + how) + ".");
    }

    /** At a feast: eats its fill among friends. */
    public void feast() {
        hunger = 1;
        social = 1;
        beginAct(EAT, 100);
    }

    /** Adam or Eve: the first of a world's people, made from the dust and not born of anyone. */
    public boolean isFirstParent() {
        String n = getName().getString();
        return father == null && mother == null && ("Adam".equals(n) || "Eve".equals(n));
    }

    /**
     * "And the rib, which the LORD God had taken from man, made he a woman, and brought her unto the man.
     * And Adam said, This is now bone of my bones, and flesh of my flesh" (Gen 2:22-23). The first man and
     * the first woman of a world are husband and wife from the day they are made.
     */
    private void firstMarriage(ServerLevel level) {
        if (spouse != null || !isMale() || !"Adam".equals(getName().getString()) || father != null || mother != null) return;
        MortalEntity eve = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(96),
                m -> m.spouse == null && !m.isMale() && "Eve".equals(m.getName().getString()) && m.father == null && m.mother == null).stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (eve == null) return;
        wed(level, eve, "the first of all marriages, when the woman was brought unto the man");
        say("This is now bone of my bones, and flesh of my flesh: she shall be called Woman.");
        eve.say("And I am thine, and thou art mine.");
        com.josephison.fiatlux.infernal.Infernal.narrate(level, blockPosition(), "Therefore shall a man leave his father and his mother, and shall cleave "
                + "unto his wife: and they shall be one flesh. And they were both naked, the man and his wife, and were not ashamed.", "Genesis 2:24-25");
        Fx.send(level, FxKind.BLESSING, position(), getBbHeight(), 0xFFFFF0C0, 40);
        Fx.send(level, FxKind.BLESSING, eve.position(), eve.getBbHeight(), 0xFFFFF0C0, 40);
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
        if (sentence != com.josephison.fiatlux.civilization.Justice.Sentence.NONE && sentence != com.josephison.fiatlux.civilization.Justice.Sentence.EXILE) {
            com.josephison.fiatlux.civilization.Justice.hold(sl, this);
        }
        conversationTick(sl);
        if (act() != NONE && act() != SLEEP && --actTicks <= 0) finishAct(sl);
        if (act() == SLEEP && !isNight()) {
            endAct();
            energy = 1;
            if (random.nextInt(3) == 0) say(MortalSpeech.Topic.MORNING);
        }
        if (marked && tickCount % 40 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.75, getZ(), 2, 0.12, 0.05, 0.12, 0);
        if (act() == PRAY && tickCount % 20 == 0) {
            sl.sendParticles(com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), getX(), getY() + 1.4, getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        }
        if (tickCount % 20 == 3) lifeTick(sl);
    }

    private void finishAct(ServerLevel level) {
        int a = act();
        int then = NONE;
        if (a == PRAY) finishPrayer(level);
        else if (a == WORK) finishWork(level);
        else if (a == EAT) hunger = 1;
        else if (a == TALK) partner = null;
        else if (a == TORN) release(level);
        else if (a == EXORCISE) then = finishExorcism(level);
        endAct();
        if (then != NONE) beginAct(then, 300);
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
            com.josephison.fiatlux.prophecy.Scripture.onPrayer(level, this, faith);
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
        CosmicExpanseSavedData cosmosNow = CosmicExpanseSavedData.get(level.getServer());
        boolean unclothed = !cosmosNow.fallOfMan || cosmosNow.eden == com.josephison.fiatlux.eden.EdenState.FALLEN
                || cosmosNow.eden == com.josephison.fiatlux.eden.EdenState.JUDGMENT && cosmosNow.edenStep < 8;
        if (unclothed != looksInnocent()) entityData.set(DATA_INNOCENT, unclothed);
        if (tickCount % 40 == 3) firstMarriage(level);
        if (innocent(level)) {   // "And they were both naked, the man and his wife, and were not ashamed" (Gen 2:25)
            hunger = 1;
            energy = Math.max(energy, 0.5);
            updateStage();
            travelTick(level);
            reactToSurroundings(level);
            if ("Adam".equals(getName().getString()) && random.nextInt(25) == 0) nameTheBeasts(level);
            if (speechTicks == 0 && act() == NONE && random.nextInt(40) == 0) say(MortalSpeech.innocentLine(random, getName().getString()));
            return;
        }
        double before = age;
        age += 20.0 / year * (isNephil() && age < ADULT_AGE ? 3 : 1);   // the giants grow up fast
        if ((int) before < ADULT_AGE && (int) age >= ADULT_AGE) {
            // "Abel was a keeper of sheep, but Cain was a tiller of the ground" (Gen 4:2)
            profession = "Abel".equals(getName().getString()) ? Profession.SHEPHERD : "Cain".equals(getName().getString()) ? Profession.FARMER
                    : Profession.pick(era(), random);
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
        if (tickCount % 100 == 3) {
            var polity = com.josephison.fiatlux.civilization.Justice.polityOf(level, this);
            String t = polity != null && getUUID().equals(polity.ruler) ? polity.form.title(isMale())
                    : com.josephison.fiatlux.civilization.Churches.titleOf(level, this);
            if (t.isEmpty()) t = com.josephison.fiatlux.prophecy.Prophets.titleOf(level, this);
            if (!t.equals(title())) entityData.set(DATA_TITLE, t);
        }
        if (isNephil() && tickCount % 100 == 3) growAsGiant();
        if (tickCount % 200 == 7 && com.josephison.fiatlux.civilization.Genealogy.walkWithGod(level, this)) return;
        travelTick(level);
        reactToSurroundings(level);
        nephilTick(level, year);
        possessionTick(level, year);
        exorcistTick(level);
        com.josephison.fiatlux.prophecy.Prophets.tick(level, this);
        faithTick(level, year);
        leaveTick(level, year);
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

    /**
     * "And out of the ground the LORD God formed every beast of the field ... and brought them unto Adam
     * to see what he would call them: and whatsoever Adam called every living creature, that was the
     * name thereof" (Gen 2:19).
     */
    private void nameTheBeasts(ServerLevel level) {
        for (var beast : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, getBoundingBox().inflate(9), a -> !a.hasCustomName())) {
            String kind = beast.getType().getDescription().getString();
            beast.setCustomName(Component.literal(kind));
            getLookControl().setLookAt(beast, 30f, 30f);
            say("You... I shall call you \"" + kind + "\".");
            emote(ParticleTypes.HAPPY_VILLAGER, 3);
            return;
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
            if (isPossessed() && !beingCastOut) {   // "I know thee who thou art, the Holy One" (Mark 1:24)
                say("Let us alone! What have we to do with thee, " + name + "? Art thou come to destroy us? I know thee who thou art!");
                beginAct(POSSESSED, 80);
                return;
            }
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
        if (!isNephil()) {
            MortalEntity giant = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(9), m -> m.isNephil() && !m.isChild()).stream()
                    .findAny().orElse(null);
            if (giant != null) {
                lastWitness = now;
                String[] fear = {"Hush... one of the giants is near.", "Hide the bread before " + giant.getName().getString() + " sees it.",
                        "They are the sons of the shining ones. Who can stand before them?", "Keep the children inside."};
                say(fear[random.nextInt(fear.length)]);
                return;
            }
        }
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
        if (faith != null && random.nextDouble() < 0.3 * 20.0 / year) {   // the state's religion and its law
            var state = com.josephison.fiatlux.civilization.Justice.polityOf(level, this);
            Religion stateFaith = state == null ? null : Generations.get(level.getServer()).religion(state.stateFaith);
            if (stateFaith != null && !stateFaith.id.equals(faith.id)) {
                if (stateFaith.deity.true_ && (faith.deity == Religion.Deity.IDOL || faith.deity == Religion.Deity.FALLEN)) {
                    com.josephison.fiatlux.civilization.Justice.report(level, this, com.josephison.fiatlux.civilization.Polity.Crime.IDOLATRY, null);
                } else if (!stateFaith.deity.true_ && faith.deity.true_) {   // persecution of the faithful
                    com.josephison.fiatlux.civilization.Justice.report(level, this, com.josephison.fiatlux.civilization.Polity.Crime.HERESY, null);
                }
            }
        }
        if (faith == null || s == null || !faith.deity.true_) return;
        if (level.getGameTime() - s.lastSign > Faiths.SILENCE) {
            double rate = hasTrait(Trait.PROUD) || hasTrait(Trait.CURIOUS) ? 0.004 : hasTrait(Trait.PIOUS) ? 0.0005 : 0.001;
            setDoubt(doubt + rate);
        }
    }

    /**
     * Those who doubt, or whose faith's leader has shamed it, may leave it - for a faith that is near,
     * for one of their own founding, or for none (see {@link com.josephison.fiatlux.civilization.Churches#apostasy}).
     * Its leader does not leave the flock it leads.
     */
    private void leaveTick(ServerLevel level, int year) {
        Religion faith = Faiths.of(this);
        if (faith == null || isChild() || getUUID().equals(faith.leader)) return;
        MortalEntity leader = faith.leader != null && level.getEntity(faith.leader) instanceof MortalEntity m ? m : null;
        boolean shamed = leader != null && com.josephison.fiatlux.civilization.Churches.corrupt(leader) && distanceToSqr(leader) < 96 * 96;
        double pull = (doubt > 0.6 ? (doubt - 0.6) * 2.5 : 0) + (shamed ? 0.3 : 0) + (hasTrait(Trait.PROUD) || hasTrait(Trait.CURIOUS) ? 0.1 : 0)
                - (hasTrait(Trait.PIOUS) ? 0.2 : 0);
        if (pull <= 0 || random.nextDouble() > pull * FiatLuxConfig.COMMON.apostasyRate.get() * 20.0 / year) return;
        Religion now = com.josephison.fiatlux.civilization.Churches.apostasy(level, this, faith);
        if (now != null) emote(now.deity.true_ ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, 10);
    }

    /** Married women of age may bear children; answered prayers for a child are kept. */
    private void familyTick(ServerLevel level, int year) {
        if (watcherHusband != null && !isMale() && age >= 16 && age <= 45 && giants < 2 && random.nextDouble() < 20.0 / (year * 0.8)
                && !com.josephison.fiatlux.flood.FloodData.get(level.getServer()).active()) {
            bearGiant(level);
            return;
        }
        // "And Adam lived an hundred and thirty years, and begat a son ... and he begat sons and daughters" (Gen 5:3-4)
        boolean firstPair = isFirstParent();
        if (isMale() || spouse == null || age < 18 || age > (firstPair ? 90 : 45)) return;
        Entity husband = level.getEntity(spouse);
        if (!(husband instanceof MortalEntity dad) || distanceToSqr(dad) > 48 * 48) return;
        Settlement s = settlement();
        int max = FiatLuxConfig.COMMON.maxMortalsPerSettlement.get();
        if (level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(80)).size() >= max && !(firstPair && children < 3)) return;
        boolean blessed = s != null && s.blessedWithChild;
        // "Be fruitful, and multiply, and replenish the earth" (Gen 1:28): the first pair bear often, until they have many
        double chance = 20.0 / (year * 2.5) * (firstPair && children < 8 ? 5 : 1);
        if (!blessed && random.nextDouble() > chance) return;
        if (blessed) s.blessedWithChild = false;
        // "Adam knew Eve his wife; and she conceived, and bare Cain ... And she again bare his brother Abel" (Gen 4:1-2),
        // "and she bare a son, and called his name Seth" (Gen 4:25)
        String name = null;
        Boolean male = null;
        if ("Adam".equals(dad.getName().getString()) && "Eve".equals(getName().getString()) && children < 3) {
            name = new String[]{"Cain", "Abel", "Seth"}[children];
            male = true;
        }
        // the generations of Genesis 4, 5 and 11: the eldest son of each line bears the next name in it
        com.josephison.fiatlux.civilization.Genealogy.Heir heir = name == null ? com.josephison.fiatlux.civilization.Genealogy.nextSon(level.getServer(), dad) : null;
        if (heir != null) {
            name = heir.name();
            male = true;
        }
        MortalEntity child = MortalPopulation.bringForth(level, blockPosition(), home, era(), name, male, 0, dad, this, religion);
        if (child == null) return;
        com.josephison.fiatlux.civilization.Genealogy.enter(level.getServer(), child, dad, heir);
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

    // ================================================================== unclean spirits

    public boolean isPossessed() { return entityData.get(DATA_POSSESSED); }
    public boolean isExorcist() { return exorcist; }
    public String spiritName() { return spirit; }
    public String spiritLord() { return spiritLord; }
    public boolean beingCastOut() { return beingCastOut; }

    /**
     * An unclean spirit enters this one. `lord` is the lord of the Ars Goetia whose legions it is of ("" if
     * none); `sevenfold` if it has come back with seven others worse than itself (Matt 12:45).
     */
    public void possess(String name, String lord, com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction does, boolean sevenfold) {
        if (!(level() instanceof ServerLevel sl)) return;
        spirit = name;
        spiritLord = lord;
        affliction = does;
        this.sevenfold = sevenfold;
        beingCastOut = false;
        entityData.set(DATA_POSSESSED, true);
        endAct();
        beginAct(POSSESSED, 100);
        say(sevenfold ? "We have come home... and we have brought seven more." : name.equals("Legion") ? "My name is Legion: for we are many."
                : "This house is ours now.");
        Person p = person();
        if (p != null) {
            p.possessed = name + (lord.isEmpty() ? "" : " (of " + lord + ")");
            Generations.get(sl.getServer()).setDirty();
        }
        var d = com.josephison.fiatlux.infernal.InfernalData.get(sl.getServer());
        d.possessions++;
        d.setDirty();
        CosmosData.get(sl.getServer()).record(sl.getServer(), getName().getString(), "possessed",
                (sevenfold ? "was entered again by the spirit that had been cast out, and seven others more wicked: the last state was worse than the first"
                        : "was entered by an unclean spirit, " + name + (lord.isEmpty() ? "" : ", of the legions of " + lord)) + ", on " + worldName() + ".");
    }

    /** "And Jesus rebuked him, saying, Hold thy peace, and come out of him" (Mark 1:25): the spirit tears its host, and goes. */
    public void exorcise(String by) {
        if (!isPossessed() || beingCastOut) return;
        beingCastOut = true;
        castOutBy = by;
        endAct();
        beginAct(TORN, 60);
        say(spirit.equals("Legion") ? "I adjure thee, torment me not! Send us into the swine!" : "Torment us not! Where shall we go?");
    }

    /** "And when the unclean spirit had torn him, and cried with a loud voice, he came out of him" (Mark 1:26). */
    private void release(ServerLevel level) {
        if (!isPossessed()) return;
        boolean swine = (spirit.equals("Legion") || random.nextInt(4) == 0)
                && !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, getBoundingBox().inflate(40)).isEmpty();
        var s = com.josephison.fiatlux.registry.ModEntities.SPIRIT.get().create(level);
        if (s != null) {
            s.moveTo(getX(), getY() + 1.2, getZ(), getYRot(), 0);
            s.become(spirit, spiritLord, affliction);
            s.expelled(this, swine);
            level.addFreshEntity(s);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.3, 0.8, 0.3, 0.04);
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 2f, 0.6f);
        String was = spirit;
        entityData.set(DATA_POSSESSED, false);
        spirit = "";
        spiritLord = "";
        sevenfold = false;
        beingCastOut = false;
        heal(getMaxHealth());
        setDoubt(doubt - 0.3);
        say("I... I am myself again. Thank you.");   // "sitting, and clothed, and in his right mind" (Mark 5:15)
        Person p = person();
        if (p != null) {
            p.possessed = "";
            Generations.get(level.getServer()).setDirty();
        }
        var d = com.josephison.fiatlux.infernal.InfernalData.get(level.getServer());
        d.exorcisms++;
        d.setDirty();
        com.josephison.fiatlux.infernal.Infernal.narrate(level, blockPosition(), "And when the unclean spirit had torn him, and cried with a loud "
                + "voice, he came out of him. And they were all amazed.", "Mark 1:26-27");
        CosmosData.get(level.getServer()).record(level.getServer(), castOutBy.isEmpty() ? "The faithful" : castOutBy, "exorcism",
                "cast the unclean spirit " + was + " out of " + getName().getString() + " on " + worldName() + ".");
    }

    /** "In my name shall they cast out devils" (Mark 16:17): a guardian angel anoints this one (Mark 6:13). */
    public void anoint(com.josephison.fiatlux.infernal.GuardianEntity angel) {
        if (!(level() instanceof ServerLevel sl) || exorcist) return;
        exorcist = true;
        anointedBy = angel.getName().getString();
        Religion faith = Faiths.of(this);
        say("Me? I will go in the name of " + (faith != null ? faith.deityName : "the Creator") + ".");
        beginAct(BOW, 60);
        Fx.send(sl, FxKind.CELESTIAL_PILLAR, position(), 12f, 0xFFFFF0C0, 30);
        Fx.send(sl, FxKind.BLESSING, position(), getBbHeight(), 0xFFFFE8A0, 30);
        Person p = person();
        if (p != null) {
            p.exorcist = true;
            Generations.get(sl.getServer()).setDirty();
        }
        var d = com.josephison.fiatlux.infernal.InfernalData.get(sl.getServer());
        d.anointed++;
        d.setDirty();
        CosmosData.get(sl.getServer()).record(sl.getServer(), getName().getString(), "anointed", "was anointed by the " + anointedBy
                + " to cast out unclean spirits in the name of " + (faith != null ? faith.deityName : "the Creator") + ", on " + worldName() + ".");
    }

    /** What the spirit in this one does to it and to those around it, and the fits it throws it into. */
    private void possessionTick(ServerLevel level, int year) {
        if (!isPossessed() || beingCastOut) return;
        double perYear = 20.0 / year * (sevenfold ? 2 : 1);
        if (tickCount % 60 == 3) level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() + 0.2, getZ(), 4, 0.15, 0.1, 0.15, 0.01);
        if (act() == NONE && random.nextInt(sevenfold ? 12 : 25) == 0) {
            beginAct(POSSESSED, 60 + random.nextInt(60));
            String[] cries = {"We are many, and we are hungry.", "Do not look at us!", "This one is ours. Ours!", "Leave us alone!",
                    "Ha! Ha! Your prayers are dust.", "Burn... let it all burn..."};
            say(cries[random.nextInt(cries.length)]);
        }
        if (tickCount % 200 == 3) addSin(1);
        if (random.nextDouble() < 6 * perYear) afflict(level);
        // "Jesus I know, and Paul I know; but who are ye?" (Acts 19:13-16)
        if (random.nextDouble() < 2 * perYear) {
            MortalEntity priest = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(8),
                    m -> m != this && m.profession == Profession.PRIEST && !m.exorcist && !m.isChild() && m.act() == NONE).stream().findAny().orElse(null);
            if (priest != null) {
                Religion f = Faiths.of(priest);
                priest.say("I adjure you by " + (f != null ? f.deityName : "the gods") + ": come out!");
                say("Your god I know, and the anointed I know; but who are ye?");
                swing(InteractionHand.MAIN_HAND);
                priest.hurt(damageSources().mobAttack(this), 4f);
                priest.moveTo(priest.home != null ? priest.home : priest.blockPosition(), 1.2);
            }
        }
    }

    private void afflict(ServerLevel level) {
        Settlement s = settlement();
        MortalEntity near = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(10), m -> m != this && m.isAlive() && !m.isPossessed())
                .stream().findAny().orElse(null);
        switch (affliction) {
            case THEFT -> {
                if (s != null && s.food > 0) s.food = Math.max(0, s.food - 3);
                say("Ours. All of it is ours.");
            }
            case DISCORD -> {
                if (near != null) {
                    near.say(MortalSpeech.Topic.QUARREL);
                    near.addSin(1);
                    near.emote(ParticleTypes.ANGRY_VILLAGER, 3);
                }
                say("Did you hear what they say of you?");
            }
            case VIOLENCE -> {   // "the man in whom the evil spirit was leaped on them" (Acts 19:16)
                if (near != null) {
                    swing(InteractionHand.MAIN_HAND);
                    near.hurt(damageSources().mobAttack(this), 3f);
                    near.say("He is not himself! Bind him!");
                }
            }
            case SICKNESS -> {   // "a spirit of infirmity ... bowed together, and could in no wise lift up herself" (Luke 13:11)
                if (getHealth() > 8) hurt(damageSources().generic(), 2f);
                say("I cannot lift myself up...");
            }
            case SLOTH -> {
                energy = 0;
                say("Leave us lie. There is no use in anything.");
            }
            case FIRE -> {   // "ofttimes it hath cast him into the fire" (Mark 9:22)
                if (s != null) moveTo(s.centre, 1.0);
                if (getHealth() > 8) igniteForSeconds(2);
                say("Into the fire! Into the fire!");
            }
            case DECEIT -> {   // "a spirit of divination ... which brought her masters much gain by soothsaying" (Acts 16:16)
                say(new String[]{"I see your fortune: gold, and then a grave.", "The stars say your god has forgotten you.",
                        "Pay me, and I will tell you who you will marry."}[random.nextInt(3)]);
                if (near != null) near.setDoubt(near.doubt + 0.15);
            }
            case MUTENESS -> emote(ParticleTypes.SMOKE, 6);
            case DROWNING -> {   // "and into the waters, to destroy him" (Mark 9:22)
                if (s != null && s.field != null) moveTo(s.field, 1.0);
                entityData.set(DATA_SPEECH, "The water... it calls us.");
                speechTicks = 80;
            }
            case DESPAIR -> {
                say("No one is listening. No one ever was.");
                if (near != null) near.setDoubt(near.doubt + 0.1);
            }
        }
    }

    /** One anointed by an angel goes to the possessed near them and casts the spirit out in their Creator's name. */
    private void exorcistTick(ServerLevel level) {
        if (!exorcist || isChild() || isPossessed() || act() != NONE) return;
        if (tickCount % 80 == 3) {
            level.sendParticles(com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), getX(), getY() + 1.6, getZ(), 2, 0.3, 0.3, 0.3, 0.01);
        }
        MortalEntity t = casting != null && level.getEntity(casting) instanceof MortalEntity m && m.isPossessed() && !m.beingCastOut ? m : null;
        if (t == null) {
            t = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(40), m -> m.isPossessed() && !m.beingCastOut).stream()
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            casting = t == null ? null : t.getUUID();
        }
        if (t == null) return;
        if (distanceToSqr(t) > 9) {
            moveTo(t.blockPosition(), 0.85);
            return;
        }
        getLookControl().setLookAt(t, 30f, 30f);
        t.getLookControl().setLookAt(this, 30f, 30f);
        Religion faith = Faiths.of(this);
        beginAct(EXORCISE, 120);
        t.endAct();
        t.beginAct(POSSESSED, 120);
        say("In the name of " + (faith != null ? faith.deityName : "the Creator") + ", I command thee to come out of " + (t.isMale() ? "him!" : "her!"));
        t.say(t.affliction == com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction.MUTENESS ? "..." : "What have we to do with thee? Torment us not!");
    }

    /**
     * The spirit goes out, or it does not: the lords of the Ars Goetia and those come back sevenfold hold
     * on hard - "This kind can come forth by nothing, but by prayer and fasting" (Mark 9:29) - and the
     * anointed one goes to pray before trying again. Returns the act to take up next.
     */
    private int finishExorcism(ServerLevel level) {
        MortalEntity t = casting != null && level.getEntity(casting) instanceof MortalEntity m && m.isPossessed() && !m.beingCastOut ? m : null;
        if (t == null || distanceToSqr(t) > 25) return NONE;
        String lord = t.spiritLord;
        double hold = lord.isEmpty() ? 0 : lord.contains("Prince of Hell") || lord.contains("the Devil") || lord.contains("Bottomless") ? 0.45
                : lord.startsWith("King") || lord.startsWith("Prince") ? 0.3 : lord.startsWith("Duke") || lord.startsWith("Marquis") ? 0.2 : 0.1;
        if (t.sevenfold) hold += 0.2;
        if (t.spirit.equals("Legion")) hold += 0.15;
        Alignment a = Divinity.alignment(this);
        double chance = 0.6 + prayerAndFasting + Math.min(0.2, (a.virtue() - a.sin()) / 100.0) - hold;
        if (random.nextDouble() < chance) {
            prayerAndFasting = 0;
            t.exorcise(getName().getString());
            addVirtue(2);
            return NONE;
        }
        prayerAndFasting += 0.25;
        say("This kind goeth not out but by prayer and fasting.");
        return PRAY;
    }

    /**
     * "When the sons of God came in unto the daughters of men, and they bare children to them, the same
     * became mighty men which were of old, men of renown" (Gen 6:4).
     */
    public void bearGiant(ServerLevel level) {
        MortalEntity child = com.josephison.fiatlux.registry.ModEntities.MORTAL.get().create(level);
        if (child == null) return;
        String name = com.josephison.fiatlux.flood.Nephilim.giantName(watcherHusbandName, giants, random);
        child.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360f, 0);
        child.setEra(era());
        child.setOrigin(0, null, this, religion);
        child.father = watcherHusband;
        child.fatherName = watcherHusbandName;
        child.entityData.set(DATA_NEPHIL, true);
        child.becomeLivingSoul(name, true);
        child.setHome(home != null ? home : blockPosition());
        level.addFreshEntity(child);
        giants++;
        children++;
        lastChildName = name;
        say("My son... how he grows! He is not like other children.");
        level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.3);
        var flood = com.josephison.fiatlux.flood.FloodData.get(level.getServer());
        flood.giantsBorn++;
        flood.setDirty();
        CosmosData.get(level.getServer()).record(level.getServer(), getName().getString(), "nephilim",
                "bare to " + watcherHusbandName + " the Watcher a son, " + name + ", on " + worldName() + ". There were giants in the earth in those days.");
    }

    /**
     * The giants' violence: "they consumed all the acquisitions of men. And when men could no longer
     * sustain them, the giants turned against them" (1 Enoch 7:3-4); "the earth was filled with
     * violence" (Gen 6:11). They boast, devour the town's store, and strike whoever is near.
     */
    private void nephilTick(ServerLevel level, int year) {
        if (!isNephil() || isChild() || act() != NONE) return;
        double perYear = 20.0 / year;
        Settlement s = settlement();
        if (s != null && s.food > 0 && random.nextDouble() < 4 * perYear) {
            s.food = Math.max(0, s.food - 6);
            hunger = 1;
            addSin(1);
            say(random.nextBoolean() ? "More bread, little ones. It is never enough." : "Your store is ours. Bring more.");
            CosmicExpanseSavedData.get(level.getServer()).setDirty();
        }
        if (random.nextDouble() < 3 * perYear) {
            MortalEntity victim = level.getEntitiesOfClass(MortalEntity.class, getBoundingBox().inflate(10),
                    m -> m != this && !m.isNephil() && m.isAlive() && m.act() != SLEEP).stream().findAny().orElse(null);
            if (victim != null) {
                getLookControl().setLookAt(victim, 30f, 30f);
                swing(InteractionHand.MAIN_HAND);
                victim.hurt(damageSources().mobAttack(this), 5f);
                victim.say(victim.isChild() ? "Mother! The giant!" : "Mercy! We have nothing left!");
                victim.emote(ParticleTypes.DAMAGE_INDICATOR, 3);
                addSin(2);
            }
        }
        if (speechTicks == 0 && random.nextInt(30) == 0) {
            String[] boasts = {"We are the mighty men, men of renown.", "Our fathers came down from the heights of Hermon.",
                    "Who is like us in all the earth?", "The little ones tremble when we pass.", "What god do we need? We are as gods."};
            say(boasts[random.nextInt(boasts.length)]);
        }
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
            com.josephison.fiatlux.civilization.Justice.report(level, this, com.josephison.fiatlux.civilization.Polity.Crime.THEFT, null);
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
        if (level() instanceof ServerLevel sl && !isRemoved() && !dead && source.getEntity() instanceof MortalEntity killer && killer != this) {
            com.josephison.fiatlux.civilization.Justice.report(sl, killer, com.josephison.fiatlux.civilization.Polity.Crime.MURDER, this);
        }
        if (level() instanceof ServerLevel sl && !isRemoved() && !dead && isNephil()) {
            // "the spirits of the giants ... shall be evil spirits on the earth, and on the earth shall be their dwelling" (1 Enoch 15:8-10)
            var flood = com.josephison.fiatlux.flood.FloodData.get(sl.getServer());
            flood.giantSpirits++;
            flood.setDirty();
            Person p = person();
            if (p != null) {
                p.died = sl.getGameTime();
                p.soul = "spirit";
                p.ageAtDeath = (int) age;
                Generations.get(sl.getServer()).setDirty();
            }
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 30, 0.6, 1.2, 0.6, 0.03);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 1.0, 0.6, 0.02);
            CosmosData.get(sl.getServer()).record(sl.getServer(), getName().getString() + " the giant, son of " + fatherName, "giant_death",
                    "died on " + worldName() + ". His spirit went not down to the Underworld, but wanders the earth, an evil spirit.");
            super.die(source);
            return;
        }
        if (level() instanceof ServerLevel sl && !isRemoved() && !dead) com.josephison.fiatlux.prophecy.Prophets.onDeath(sl, this, source);
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
                p.variant = variant();
                p.profession = profession.id;
                Generations.get(sl.getServer()).setDirty();
                com.josephison.fiatlux.civilization.SoulRealms.onDeath(sl.getServer(), p, saved);   // the soul arrives in its realm
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
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp && !Divinity.get(sp).pilgrim() && isPossessed()) {
            exorcise(sp.getGameProfile().getName());   // "Hold thy peace, and come out of him" (Mark 1:25)
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp && !Divinity.get(sp).pilgrim()
                && sentence != com.josephison.fiatlux.civilization.Justice.Sentence.NONE && sentence != com.josephison.fiatlux.civilization.Justice.Sentence.EXILE) {
            com.josephison.fiatlux.civilization.Justice.deliver(sp, this);   // "the angel of the Lord by night opened the prison doors" (Acts 5:19)
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp && !Divinity.get(sp).pilgrim()) {
            // the Creator lays a hand in blessing
            if (com.josephison.fiatlux.eden.CainAndAbel.onBlessed((ServerLevel) level(), this)) {
                emote(ParticleTypes.HEART, 6);
                return InteractionResult.CONSUME;
            }
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
        if (!title().isEmpty()) {
            var polity = com.josephison.fiatlux.civilization.Justice.polityOf((ServerLevel) level(), this);
            boolean rules = polity != null && getUUID().equals(polity.ruler);
            stage = title() + (rules ? " of the " + polity.name : faith != null ? " of the " + faith.name : "");
        }
        if (isPossessed()) {
            player.sendSystemMessage(Component.literal("An unclean spirit is in " + (isMale() ? "him" : "her") + ": " + spirit
                    + (spiritLord.isEmpty() ? "" : ", of the legions of " + spiritLord) + " - " + affliction.does + (sevenfold ? " (returned sevenfold)" : "")
                    + ". Sneak-use to cast it out.").withStyle(st -> st.withColor(0xC04040)));
        } else if (exorcist) {
            player.sendSystemMessage(Component.literal("Anointed by the " + anointedBy + " to cast out unclean spirits.").withStyle(st -> st.withColor(0xF5D77A)));
        }
        var prophet = com.josephison.fiatlux.prophecy.Prophets.of(player.getServer(), getUUID());
        if (prophet != null) {
            player.sendSystemMessage(Component.literal((prophet.false_ ? "A false prophet of " : "A prophet of ") + prophet.calledBy + ": "
                    + prophet.oracles.size() + " oracles spoken, " + prophet.wonders + " wonders worked"
                    + (prophet.disciple != null ? ", followed by " + prophet.discipleName : "") + ".").withStyle(st -> st.withColor(prophet.false_ ? 0xC08070 : 0xF5D77A)));
        }
        if (sentence != com.josephison.fiatlux.civilization.Justice.Sentence.NONE) {
            player.sendSystemMessage(com.josephison.fiatlux.civilization.Justice.describe(this).copy().withStyle(st -> st.withColor(0xE08060)));
        }
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
        tag.putBoolean("Leaving", leaving);
        tag.putBoolean("Marked", marked);
        tag.putBoolean("Nephil", isNephil());
        if (watcherHusband != null) tag.putUUID("WatcherHusband", watcherHusband);
        tag.putString("WatcherHusbandName", watcherHusbandName);
        tag.putInt("Giants", giants);
        tag.putBoolean("Possessed", isPossessed());
        tag.putString("Spirit", spirit);
        tag.putString("SpiritLord", spiritLord);
        tag.putString("Affliction", affliction.name());
        tag.putBoolean("Sevenfold", sevenfold);
        tag.putBoolean("Exorcist", exorcist);
        tag.putString("AnointedBy", anointedBy);
        tag.putDouble("PrayerAndFasting", prayerAndFasting);
        tag.putString("Sentence", sentence.name());
        tag.putString("SentenceCrime", sentenceCrime.name());
        tag.putLong("SentenceUntil", sentenceUntil);
        if (sentencePlace != null) tag.putLong("SentencePlace", sentencePlace.asLong());
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
        leaving = tag.getBoolean("Leaving");
        marked = tag.getBoolean("Marked");
        entityData.set(DATA_NEPHIL, tag.getBoolean("Nephil"));
        watcherHusband = tag.hasUUID("WatcherHusband") ? tag.getUUID("WatcherHusband") : null;
        watcherHusbandName = tag.getString("WatcherHusbandName");
        giants = tag.getInt("Giants");
        entityData.set(DATA_POSSESSED, tag.getBoolean("Possessed"));
        spirit = tag.getString("Spirit");
        spiritLord = tag.getString("SpiritLord");
        try {
            affliction = com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction.valueOf(tag.getString("Affliction"));
        } catch (IllegalArgumentException ignored) {
        }
        sevenfold = tag.getBoolean("Sevenfold");
        exorcist = tag.getBoolean("Exorcist");
        anointedBy = tag.getString("AnointedBy");
        prayerAndFasting = tag.getDouble("PrayerAndFasting");
        try {
            sentence = com.josephison.fiatlux.civilization.Justice.Sentence.valueOf(tag.getString("Sentence"));
            sentenceCrime = com.josephison.fiatlux.civilization.Polity.Crime.valueOf(tag.getString("SentenceCrime"));
        } catch (IllegalArgumentException ignored) {
            sentence = com.josephison.fiatlux.civilization.Justice.Sentence.NONE;
        }
        sentenceUntil = tag.getLong("SentenceUntil");
        sentencePlace = tag.contains("SentencePlace") ? BlockPos.of(tag.getLong("SentencePlace")) : null;
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
            case CRUCIFIED -> A_CRUCIFIED;
            case STOCKS -> A_STOCKS;
            case EXORCISE -> A_EXORCISE;
            case POSSESSED -> A_POSSESSED;
            case TORN -> A_TORN;
            default -> state.isMoving() ? WALK : IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
