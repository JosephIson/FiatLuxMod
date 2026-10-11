package com.josephison.fiatlux.prophecy;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Churches;
import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Justice;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Polity;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.SoulRealms;
import com.josephison.fiatlux.civilization.Trait;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Then said I, Here am I; send me" (Isaiah 6:8). When a people goes astray - after idols, into sin,
 * or doubting because heaven is silent - the word of its Creator comes to one of the faithful, and
 * they go from town to town saying what they are given to say: against the idols, against the
 * shepherds who feed themselves, for the hungry and for justice, comfort for the doubting, and,
 * now and then, of one who is to come (Isaiah 7:14, Micah 5:2). They work wonders - the sick are
 * healed, the bread does not run out, unclean spirits go out - and take a disciple to follow them.
 * They foretell judgment on a wicked town; if it repents it is spared (Jonah 3:10), and if the
 * Creator strikes it the word is fulfilled. Where a town serves another god they challenge its
 * priest, as Elijah did on Carmel: "the God that answereth by fire, let him be God" (1 Kings 18:24),
 * and the Creator answers - or does not. They are persecuted by rulers of other faiths and by
 * corrupt priests of their own, and some are slain; an old prophet with a disciple may be taken up
 * in a chariot of fire (2 Kings 2:11), the mantle falling to the disciple. When a prophet is gone,
 * their words and deeds are written down as their book. False faiths have their false prophets.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Prophets {
    private Prophets() {}

    // ================================================================== who they are

    @Nullable
    public static Prophet of(MinecraftServer server, UUID id) {
        Prophet p = ProphecyData.get(server).prophet(id);
        return p != null && p.living() ? p : null;
    }

    /** Whether this one is a living prophet or follows one: they leave their trade (Amos 7:15; 1 Kings 19:21). */
    public static boolean called(@Nullable MinecraftServer server, UUID id) {
        if (server == null) return false;
        ProphecyData d = ProphecyData.get(server);
        if (d.prophets.isEmpty()) return false;
        Prophet p = d.prophet(id);
        return p != null && p.living() || d.masterOf(id) != null;
    }

    /** The title a prophet (or a prophet's disciple) carries, or "". */
    public static String titleOf(ServerLevel level, MortalEntity m) {
        ProphecyData d = ProphecyData.get(level.getServer());
        Prophet p = d.prophet(m.getUUID());
        if (p != null && p.living()) {
            var son = com.josephison.fiatlux.son.SonData.get(level.getServer());
            if (son.church != null && son.church.equals(p.faith)) return "Apostle";
            return p.false_ ? "False Prophet" : m.isMale() ? "Prophet" : "Prophetess";
        }
        Prophet master = d.masterOf(m.getUUID());
        return master != null ? "Disciple of " + master.name : "";
    }

    // ================================================================== the calling

    /** Every few seconds for each living world: prophets are raised where a people goes astray; judgments come due. */
    public static void worldTick(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p) {
        if (!cosmos.fallOfMan) return;
        MinecraftServer server = level.getServer();
        ProphecyData d = ProphecyData.get(server);
        long now = level.getGameTime();
        dueJudgments(level, d, p, now);
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        if (now - d.lastCalled.getOrDefault(p.name, -10 * year) < 3 * year) return;
        Generations gens = Generations.get(server);
        Map<UUID, Integer> followers = gens.followers();
        int all = 0, astrayCount = 0;
        for (Religion r : gens.religions.values()) {
            if (!r.world.equals(p.name)) continue;
            int n = followers.getOrDefault(r.id, 0);
            all += n;
            if (!r.deity.true_) astrayCount += n;
        }
        if (all < 4) return;
        double perCheck = FiatLuxConfig.COMMON.prophetChance.get() * 100.0 / year;   // MortalPopulation ticks every 5 seconds
        RandomSource r = level.random;
        for (Religion faith : new ArrayList<>(gens.religions.values())) {
            if (!faith.world.equals(p.name) || followers.getOrDefault(faith.id, 0) < 2) continue;
            boolean has = d.prophets.values().stream().anyMatch(x -> x.living() && x.faith.equals(faith.id));
            if (has) continue;
            if (faith.deity.true_) {
                boolean astray = p.sin > 0.3 || astrayCount > all * 0.3 || averageDoubt(level, faith) > 0.35 || corruptLeader(level, faith);
                if (astray && r.nextDouble() < perCheck) call(level, faith, null, faith.deityName);
            } else if ((faith.deity == Religion.Deity.IDOL || faith.deity == Religion.Deity.FALLEN || faith.deity == Religion.Deity.SUN
                    || faith.deity == Religion.Deity.EMPEROR) && followers.getOrDefault(faith.id, 0) >= 6 && r.nextDouble() < perCheck * 0.3) {
                call(level, faith, null, faith.deityName);
            }
        }
    }

    private static double averageDoubt(ServerLevel level, Religion faith) {
        double sum = 0;
        int n = 0;
        for (MortalEntity m : level.getEntities(com.josephison.fiatlux.registry.ModEntities.MORTAL.get(), m -> faith.id.equals(m.religion()))) {
            sum += m.doubt();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static boolean corruptLeader(ServerLevel level, Religion faith) {
        return faith.leader != null && level.getEntity(faith.leader) instanceof MortalEntity l && Churches.corrupt(l);
    }

    /**
     * The word comes to one of the faith: `chosen` if given, else the most devout of them at hand who
     * holds no office - a herdsman or a farmer as soon as a priest (Amos 7:14). Returns the prophet.
     */
    @Nullable
    public static Prophet call(ServerLevel level, Religion faith, @Nullable MortalEntity chosen, String calledBy) {
        MinecraftServer server = level.getServer();
        ProphecyData d = ProphecyData.get(server);
        MortalEntity m = chosen;
        if (m == null) {
            m = level.getEntities(com.josephison.fiatlux.registry.ModEntities.MORTAL.get(), x -> faith.id.equals(x.religion()) && eligible(level, d, x))
                    .stream().max(Comparator.comparingDouble(x -> devotion(x) + level.random.nextDouble() * 3)).orElse(null);
        }
        if (m == null || d.prophet(m.getUUID()) != null && d.prophet(m.getUUID()).living()) return null;
        Planet world = m.planet();
        String worldName = world != null ? world.name : m.worldName();
        Prophet p = new Prophet(m.getUUID(), m.getName().getString(), m.isMale(), worldName, faith.id, calledBy, level.getGameTime(), !faith.deity.true_);
        d.prophets.put(p.id, p);
        d.lastCalled.put(worldName, level.getGameTime());
        Person person = Generations.get(server).person(m.getUUID());
        d.setDirty();
        Settlement town = m.settlement();
        String place = town != null && !town.name.isEmpty() ? town.name : worldName;
        m.beginAct(MortalEntity.BOW, 80);
        if (p.false_) {
            m.say(faith.deityName + " has spoken to me in the night! Hear me, all of you!");
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1.6, m.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            CosmosData.get(server).record(server, p.name + " of " + place, "prophet",
                    "rose up as a prophet of " + faith.deityName + ", saying, " + faith.deityName + " hath spoken to me.");
        } else {
            m.say(level.random.nextBoolean() ? "Here am I; send me." : "Ah, " + calledBy + "! behold, I cannot speak: for I am a child.");
            Fx.send(level, FxKind.CELESTIAL_PILLAR, m.position(), 18f, 0xFFFFF0C0, 40);
            level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), m.getX(), m.getY() + 1.2, m.getZ(), 40, 0.3, 0.8, 0.3, 0.08);
            narrate(level, m.blockPosition(), "The word of " + calledBy + " came unto " + p.name + ", saying, Before I formed thee I knew thee; "
                    + "and I ordained thee a prophet unto the nations.", "Jeremiah 1:4-5");
            CosmosData.get(server).record(server, p.name + " of " + place, "prophet",
                    "was called: the word of " + calledBy + " came unto " + p.him() + ", and " + p.he() + " went out to the towns of " + worldName + ".");
            tellCreator(level, faith, "[Prophet] The word of " + faith.deityName + " has come to " + p.name + " of " + place
                    + ". " + (p.male ? "He" : "She") + " will go to the towns of " + worldName + " and speak in your name.");
        }
        p.deeds.add((p.false_ ? "And " + p.name + " rose up in " + place + ", saying, " + faith.deityName + " hath spoken to me."
                : "And " + p.name + " was in " + place + " when the word came, and " + p.he() + " arose and went."));
        if (person != null) Generations.get(server).setDirty();
        return p;
    }

    private static boolean eligible(ServerLevel level, ProphecyData d, MortalEntity m) {
        if (m.isChild() || m.isPossessed() || m.sentence() != Justice.Sentence.NONE || m.isNephil() || m.isLeaving()) return false;
        Religion faith = Faiths.of(m);
        if (faith != null && m.getUUID().equals(faith.leader)) return false;   // the priests are not the ones sent
        Polity g = Justice.polityOf(level, m);
        if (g != null && m.getUUID().equals(g.ruler)) return false;
        Prophet p = d.prophet(m.getUUID());
        return (p == null || !p.living()) && d.masterOf(m.getUUID()) == null;
    }

    private static double devotion(MortalEntity m) {
        Alignment a = Divinity.alignment(m);
        return (m.hasTrait(Trait.PIOUS) ? 4 : 0) + (m.profession() == Profession.SHEPHERD || m.profession() == Profession.FARMER ? 1.5 : 0)
                + (m.profession() == Profession.PRIEST ? 1 : 0) + Math.min(4, (a.virtue() - a.sin()) / 5.0) - m.doubt() * 3;
    }

    /** "Whom shall I send, and who will go for us?" (Isaiah 6:8), spoken by a Creator among the faithful. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String said = event.getRawText().toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        if (!said.contains("whom shall i send") && !said.contains("who will go for us") && !said.contains("who will go for me")) return;
        ServerPlayer player = event.getPlayer();
        player.server.execute(() -> {
            if (Divinity.get(player).pilgrim()) return;
            ServerLevel level = player.serverLevel();
            ProphecyData d = ProphecyData.get(player.server);
            MortalEntity m = level.getEntitiesOfClass(MortalEntity.class, player.getBoundingBox().inflate(24), x -> eligible(level, d, x)
                            && Faiths.of(x) != null && Faiths.of(x).deity.true_ && player.getUUID().equals(Faiths.of(x).creator)).stream()
                    .max(Comparator.comparingDouble(Prophets::devotion)).orElse(null);
            if (m == null) {
                player.sendSystemMessage(Component.literal("No one of your faithful near you answers.").withStyle(s -> s.withColor(0x9A94A8).withItalic(true)));
                return;
            }
            Prophet p = call(level, Faiths.of(m), m, player.getGameProfile().getName());
            if (p != null) m.say("Here am I; send me.");
        });
    }

    // ================================================================== the prophet's life

    /** Once a second for every mortal: prophets go about their calling, and disciples follow them. */
    public static void tick(ServerLevel level, MortalEntity m) {
        ProphecyData d = ProphecyData.get(level.getServer());
        Prophet p = d.prophet(m.getUUID());
        if (p == null || !p.living()) {
            Prophet master = d.prophets.isEmpty() ? null : d.masterOf(m.getUUID());
            if (master != null) follow(level, m, master);
            return;
        }
        if (m.tickCount % 60 == 3) {
            level.sendParticles(p.false_ ? ParticleTypes.SMOKE : com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(),
                    m.getX(), m.getY() + 1.8, m.getZ(), 2, 0.25, 0.2, 0.25, 0.01);
        }
        if (m.act() == MortalEntity.WORK) m.beginAct(MortalEntity.NONE, 1);
        if (m.act() != MortalEntity.NONE || m.sentence() != Justice.Sentence.NONE || m.isPossessed()) return;
        if (busy(m.getUUID())) return;
        Religion faith = Generations.get(level.getServer()).religion(p.faith);
        if (faith == null) return;
        long now = level.getGameTime();
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        // "And Elijah went up by a whirlwind into heaven" (2 Kings 2:11)
        if (!p.false_ && m.ageYears() > 55 && p.disciple != null && level.random.nextDouble() < 0.3 * 20.0 / year
                && level.getEntity(p.disciple) instanceof MortalEntity disciple && disciple.distanceToSqr(m) < 24 * 24) {
            takenUp(level, m, p, disciple);
            return;
        }
        Settlement town = journey(level, m, p, now);
        if (town == null) return;   // still on the road
        if (!p.false_ && now - p.lastWonder > 1500 && wonder(level, m, p, faith, town)) {
            p.lastWonder = now;
            d.setDirty();
            return;
        }
        if (now - p.lastWord < 500) return;
        p.lastWord = now;
        if (!p.false_ && p.preached >= 1 && level.random.nextDouble() < 0.35 && challenge(level, m, p, faith, town)) return;
        preach(level, m, p, faith, town);
        if (p.disciple == null && p.visits + p.preached >= 3) takeDisciple(level, m, p, faith);
        d.setDirty();
    }

    /**
     * Goes to the town the prophet is sent to, choosing the next when the word has been spoken in this
     * one. Returns the town once there (null while on the road).
     */
    @Nullable
    private static Settlement journey(ServerLevel level, MortalEntity m, Prophet p, long now) {
        Planet world = m.planet();
        if (world == null) return null;
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        List<Settlement> towns = cosmos.townsOf(world.id);
        if (towns.isEmpty()) return null;
        Settlement current = p.visiting == null ? null : towns.stream().filter(t -> t.centre.equals(p.visiting)).findFirst().orElse(null);
        if (current == null || p.preached >= 4 && now - p.arrived > 2400) {
            // the next town: one that has gone astray if there is one
            Settlement next = towns.stream().filter(t -> t != null && (current == null || t != current) && level.isLoaded(t.centre))
                    .max(Comparator.comparingDouble(t -> astrayIn(level, t, p) + level.random.nextDouble())).orElse(current);
            if (next == null) return null;
            p.visiting = next.centre;
            p.preached = 0;
            p.arrived = 0;
            p.road = 0;
            if (next != current && current != null) m.say("I must go to " + (next.name.isEmpty() ? "the next town" : next.name) + ".");
            return null;
        }
        double dist = Math.sqrt(m.distanceToSqr(Vec3.atBottomCenterOf(current.centre)));
        if (dist > 14) {
            p.road += 20;
            boolean watched = level.getNearestPlayer(m, 40) != null;
            if (!watched && p.road > 400 || p.road > 3000) {
                BlockPos g = MortalPopulation.safeGround(level, current.centre.getX() + level.random.nextInt(9) - 4, current.centre.getZ() + 6, 8);
                if (g != null) m.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
            } else {
                m.walkTo(current.centre.offset(level.random.nextInt(7) - 3, 0, 5), 0.75);
            }
            return null;
        }
        if (p.arrived == 0) {
            p.arrived = now;
            p.visits++;
            m.setHome(current.centre);
        }
        return current;
    }

    /** How far a town has gone astray, as the prophet sees it. */
    private static double astrayIn(ServerLevel level, Settlement t, Prophet p) {
        double score = 0;
        for (MortalEntity x : level.getEntitiesOfClass(MortalEntity.class, new AABB(t.centre).inflate(40))) {
            if (!p.faith.equals(x.religion())) score += 1;
            Alignment a = Divinity.alignment(x);
            if (a.sin() > a.virtue() + 2) score += 0.5;
        }
        return score;
    }

    // ------------------------------------------------------------------ the word

    private static final String[] AGAINST_IDOLS = {
            "Turn ye from your idols; %s hath eyes, but sees not; ears, but hears not.",
            "They that make them are like unto them; so is every one that trusteth in them.",
            "Why will ye die, O house of %s? Cast away your idols, and make you a new heart and a new spirit.",
            "Can a man make gods unto himself, and they are no gods?"};
    private static final String[] AGAINST_SIN = {
            "Wash you, make you clean; cease to do evil; learn to do well.",
            "Your iniquities have separated between you and your God, and your sins have hid his face from you.",
            "Rend your heart, and not your garments, and turn unto %s your God.",
            "Though your sins be as scarlet, they shall be as white as snow."};
    private static final String[] AGAINST_SHEPHERDS = {
            "Woe be to the shepherds that feed themselves! should not the shepherds feed the flocks?",
            "The priests thereof teach for hire, and the prophets thereof divine for money.",
            "Woe unto them that call evil good, and good evil."};
    private static final String[] FOR_JUSTICE = {
            "Let judgment run down as waters, and righteousness as a mighty stream.",
            "Seek judgment, relieve the oppressed, judge the fatherless, plead for the widow.",
            "Woe unto them that decree unrighteous decrees, to turn aside the needy from judgment."};
    private static final String[] FOR_THE_HUNGRY = {
            "Is not this the fast that I have chosen? to deal thy bread to the hungry.",
            "He that hath two coats, let him impart to him that hath none; and he that hath meat, let him do likewise."};
    private static final String[] COMFORT = {
            "Comfort ye, comfort ye my people. Speak ye comfortably, and cry unto her that her warfare is accomplished.",
            "Fear thou not; for I am with thee: be not dismayed; for I am thy God.",
            "They that wait upon %s shall renew their strength; they shall mount up with wings as eagles.",
            "I know the thoughts that I think toward you, thoughts of peace, and not of evil, to give you an expected end."};
    private static final String[] WORD = {
            "He hath shewed thee, O man, what is good: to do justly, and to love mercy, and to walk humbly with thy God.",
            "Seek ye %s while he may be found, call ye upon him while he is near.",
            "The grass withereth, the flower fadeth: but the word of %s shall stand for ever.",
            "Let the wicked forsake his way, and the unrighteous man his thoughts: and let him return unto %s."};
    private static final String[] FALSE_WORD = {
            "Peace, peace! There is no evil coming. %s is pleased with you.", "Bring gold to %s, and you shall prosper all your days.",
            "The old god is silent. Who answers you now? %s answers!", "%s showed me a dream: your enemies shall fall before you.",
            "Why do you fear? %s keeps this town; the prophets of the old god lie."};

    /** The prophet speaks the word given for the town as it is, and the people hear it - or do not. */
    public static void preach(ServerLevel level, MortalEntity m, Prophet p, Religion faith, @Nullable Settlement town) {
        RandomSource r = level.random;
        String g = faith.deityName;
        BlockPos at = town != null ? town.centre : m.blockPosition();
        List<MortalEntity> hearers = level.getEntitiesOfClass(MortalEntity.class, new AABB(at).inflate(28), x -> x != m && !x.isChild());
        String words;
        if (p.false_) {
            words = fresh(FALSE_WORD, p, r, g);
            for (MortalEntity h : hearers) {
                Religion hf = Faiths.of(h);
                if (hf != null && hf.id.equals(faith.id)) continue;
                if (h.doubt() > 0.35 && r.nextDouble() < 0.15 + (h.hasTrait(Trait.GREEDY) ? 0.15 : 0)) {
                    h.setReligion(faith.id);
                    h.say("Then I will serve " + g + ".");
                }
            }
        } else {
            String place = town != null && !town.name.isEmpty() ? town.name : p.world;
            List<MortalEntity> idolaters = hearers.stream().filter(h -> h.religion() != null && !faith.id.equals(h.religion())).toList();
            List<MortalEntity> sinners = hearers.stream().filter(h -> {
                Alignment a = Divinity.alignment(h);
                return a.sin() > a.virtue() + 2;
            }).toList();
            boolean hungry = town != null && town.food < 6;
            boolean oppressed = hearers.stream().anyMatch(h -> h.sentence() != Justice.Sentence.NONE);
            boolean shepherdCorrupt = corruptLeader(level, faith);
            double doubt = hearers.stream().filter(h -> faith.id.equals(h.religion())).mapToDouble(MortalEntity::doubt).average().orElse(0);
            ProphecyData d = ProphecyData.get(level.getServer());
            ProphecyData.Foretelling pending = town == null ? null : d.foretellings.stream()
                    .filter(f -> f.outcome.isEmpty() && f.centre.equals(town.centre)).findFirst().orElse(null);
            // "Yet forty days, and Nineveh shall be overthrown" (Jonah 3:4)
            if (town != null && pending == null && p.preached >= 2 && (idolaters.size() + sinners.size()) * 2 >= Math.max(4, hearers.size())
                    && r.nextDouble() < 0.4) {
                foretell(level, m, p, faith, town);
                p.preached++;
                return;
            }
            String kind;
            if (!idolaters.isEmpty() && r.nextDouble() < 0.6) {
                Religion other = Faiths.of(idolaters.get(r.nextInt(idolaters.size())));
                words = fresh(AGAINST_IDOLS, p, r, other != null ? other.deityName : "the idol");
                kind = "idols";
            } else if (shepherdCorrupt && r.nextDouble() < 0.6) {
                words = fresh(AGAINST_SHEPHERDS, p, r, g);
                kind = "shepherds";
            } else if (!sinners.isEmpty() && r.nextDouble() < 0.6) {
                words = fresh(AGAINST_SIN, p, r, g);
                kind = "sin";
            } else if (oppressed && r.nextDouble() < 0.6) {
                words = fresh(FOR_JUSTICE, p, r, g);
                kind = "justice";
            } else if (hungry) {
                words = fresh(FOR_THE_HUNGRY, p, r, g);
                kind = "hungry";
            } else if (doubt > 0.3) {
                words = fresh(COMFORT, p, r, g);
                kind = "comfort";
            } else if (r.nextInt(5) == 0 && CosmicExpanseSavedData.get(level.getServer()).fallOfMan) {
                words = ofTheSon(level, d, p, town);
                kind = "son";
            } else {
                words = fresh(WORD, p, r, g);
                kind = "word";
            }
            words = "Thus saith " + g + ": " + words;
            int turned = 0;
            for (MortalEntity h : hearers) {
                switch (kind) {
                    case "idols" -> {
                        if (idolaters.contains(h) && r.nextDouble() < (h.hasTrait(Trait.PROUD) ? 0.08 : 0.2)) {
                            h.setReligion(faith.id);
                            h.setDoubt(0.1);
                            h.say("I have served a thing of nothing. I will turn to " + g + ".");
                            turned++;
                        }
                    }
                    case "sin" -> {
                        if (sinners.contains(h) && r.nextDouble() < 0.35) {
                            h.addVirtue(2);
                            h.beginAct(MortalEntity.PRAY, 60);
                            turned++;
                        }
                    }
                    case "hungry" -> {
                        if (town != null && r.nextDouble() < 0.3) town.food += 1;
                    }
                    case "comfort", "word", "son" -> {
                        if (faith.id.equals(h.religion())) h.setDoubt(h.doubt() - 0.15);
                    }
                    case "shepherds" -> {
                        if (faith.id.equals(h.religion())) h.setDoubt(h.doubt() + 0.05);
                    }
                    default -> {
                    }
                }
            }
            if (pending != null) pending.repented += turned;
            if (turned > 0) p.deeds.add("And in " + place + " " + turned + (turned == 1 ? " turned" : " turned") + " at the word of " + p.name + ".");
            if (kind.equals("shepherds") && faith.leader != null && level.getEntity(faith.leader) instanceof MortalEntity leader && leader.distanceToSqr(m) < 48 * 48) {
                leader.say("Who is this troubler of the people? Seize " + p.him() + "!");
                if (r.nextDouble() < 0.3) Justice.report(level, m, Polity.Crime.SEDITION, null);
            }
            // a ruler who serves another god will not hear it
            Polity g2 = Justice.polityOf(level, m);
            if (g2 != null && g2.stateFaith != null && !g2.stateFaith.equals(faith.id) && r.nextDouble() < 0.2) {
                Justice.report(level, m, Polity.Crime.HERESY, null);
            }
        }
        m.say(words);
        m.beginAct(MortalEntity.TALK, 80);
        for (MortalEntity h : hearers) if (h.act() == MortalEntity.NONE && h.distanceToSqr(m) < 20 * 20) h.getLookControl().setLookAt(m, 30f, 30f);
        if (p.oracles.size() < 60 && !p.oracles.contains(words)) p.oracles.add(words);
        p.preached++;
        speak(level, m, p, words);
    }

    /** A word from `words` the prophet has not yet spoken, if there is one (with the god's name put in). */
    private static String fresh(String[] words, Prophet p, RandomSource r, String name) {
        List<String> unsaid = new ArrayList<>();
        for (String w : words) {
            String text = w.replace("%s", name);
            if (p.oracles.stream().noneMatch(o -> o.endsWith(text))) unsaid.add(text);
        }
        if (unsaid.isEmpty()) return words[r.nextInt(words.length)].replace("%s", name);
        return unsaid.get(r.nextInt(unsaid.size()));
    }

    /** A word of one who is to come, sometimes naming the town he shall come from. */
    private static String ofTheSon(ServerLevel level, ProphecyData d, Prophet p, @Nullable Settlement town) {
        String[] words = {
                "Behold, a virgin shall conceive, and bear a son, and shall call his name Immanuel.",
                "For unto us a child is born, unto us a son is given: and the government shall be upon his shoulder.",
                "He is despised and rejected of men; a man of sorrows, and acquainted with grief. With his stripes we are healed.",
                "Rejoice greatly; behold, thy King cometh unto thee: lowly, and riding upon an ass.",
                (town != null && !town.name.isEmpty() ? "But thou, " + town.name : "But thou, little town")
                        + ", though thou be little among the thousands, yet out of thee shall he come forth that is to be ruler."};
        String w = words[level.random.nextInt(words.length)];
        String line = p.name + ": " + w;
        if (!d.ofTheSon.contains(line)) {
            d.ofTheSon.add(line);
            d.setDirty();
        }
        return w;
    }

    /** What a prophet says is heard by any Creator near, in full. */
    private static void speak(ServerLevel level, MortalEntity m, Prophet p, String words) {
        Component line = Component.literal(p.name + (p.false_ ? " (false prophet): " : ", " + (p.male ? "prophet" : "prophetess") + ": "))
                .withStyle(s -> s.withColor(p.false_ ? 0xC08070 : 0xE8C890))
                .append(Component.literal(words).withStyle(s -> s.withColor(p.false_ ? 0xD8B0A0 : 0xF5E6B8).withItalic(true)));
        for (ServerPlayer pl : level.players()) if (pl.distanceToSqr(m) < 48 * 48) pl.sendSystemMessage(line);
    }

    // ------------------------------------------------------------------ wonders

    /**
     * Where there is need, the prophet works a wonder in the name of their God: the sick are healed
     * ("Arise, and walk", Acts 3:6), the bread does not run out (2 Kings 4:42-44), an unclean spirit
     * goes out. Returns whether one was worked.
     */
    private static boolean wonder(ServerLevel level, MortalEntity m, Prophet p, Religion faith, Settlement town) {
        String g = faith.deityName;
        String place = town.name.isEmpty() ? p.world : town.name;
        MortalEntity sick = level.getEntitiesOfClass(MortalEntity.class, m.getBoundingBox().inflate(16), x -> x != m && x.getHealth() < x.getMaxHealth() * 0.7)
                .stream().findFirst().orElse(null);
        MortalEntity possessed = level.getEntitiesOfClass(MortalEntity.class, m.getBoundingBox().inflate(16), x -> x.isPossessed() && !x.beingCastOut())
                .stream().findFirst().orElse(null);
        String deed;
        Vec3 where;
        if (possessed != null) {
            m.say("In the name of " + g + ", come out of " + (possessed.isMale() ? "him!" : "her!"));
            possessed.exorcise(p.name);
            deed = "And " + p.name + " cast out an unclean spirit from " + possessed.getName().getString() + " in " + place + ".";
            where = possessed.position();
        } else if (sick != null) {
            m.say("In the name of " + g + ", arise, and be whole.");
            sick.heal(sick.getMaxHealth());
            sick.say("I am healed! Blessed be " + g + "!");
            deed = "And " + p.name + " healed " + sick.getName().getString() + " in " + place + ", and " + (sick.isMale() ? "he" : "she") + " was made whole.";
            where = sick.position();
        } else if (town.food < 6) {
            m.say("Thus saith " + g + ": They shall eat, and shall leave thereof.");
            town.food += 18;
            CosmicExpanseSavedData.get(level.getServer()).setDirty();
            deed = "And there was famine in " + place + "; and " + p.name + " blessed the little bread they had, and they did eat, and left thereof.";
            where = Vec3.atCenterOf(town.centre);
        } else {
            return false;
        }
        m.beginAct(MortalEntity.PRAY, 40);
        Fx.send(level, FxKind.BLESSING, where, 2.4f, 0xFFFFF0C0, 30);
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), where.x, where.y + 1, where.z, 25, 0.4, 0.6, 0.4, 0.05);
        for (MortalEntity w : level.getEntitiesOfClass(MortalEntity.class, new AABB(BlockPos.containing(where)).inflate(20), x -> x != m)) {
            w.witness(false);
            if (!faith.id.equals(w.religion()) && level.random.nextDouble() < (w.hasTrait(Trait.PROUD) ? 0.15 : 0.45)) w.setReligion(faith.id);
            if (faith.id.equals(w.religion())) w.setDoubt(w.doubt() - 0.3);
        }
        p.deeds.add(deed);
        p.wonders++;
        ProphecyData d = ProphecyData.get(level.getServer());
        d.wonders++;
        d.setDirty();
        CosmosData.get(level.getServer()).record(level.getServer(), p.name, "wonder", deed.substring(("And " + p.name + " ").length()));
        return true;
    }

    // ------------------------------------------------------------------ the disciple

    /** "And Elijah passed by him, and cast his mantle upon him ... Then he arose, and went after Elijah" (1 Kings 19:19-21). */
    private static void takeDisciple(ServerLevel level, MortalEntity m, Prophet p, Religion faith) {
        ProphecyData d = ProphecyData.get(level.getServer());
        MortalEntity young = level.getEntitiesOfClass(MortalEntity.class, m.getBoundingBox().inflate(24),
                        x -> x != m && faith.id.equals(x.religion()) && eligible(level, d, x) && x.ageYears() < 35 && x.spouseId() == null)
                .stream().max(Comparator.comparingDouble(Prophets::devotion)).orElse(null);
        if (young == null) return;
        p.disciple = young.getUUID();
        p.discipleName = young.getName().getString();
        young.say("Let me kiss my father and my mother, and then I will follow thee.");
        m.say("Follow me, " + p.discipleName + ".");
        p.deeds.add("And " + p.name + " cast " + p.his() + " mantle upon " + p.discipleName + ", who arose and went after " + p.him() + ", and ministered unto " + p.him() + ".");
        d.setDirty();
    }

    private static void follow(ServerLevel level, MortalEntity disciple, Prophet master) {
        if (!(level.getEntity(master.id) instanceof MortalEntity m) || disciple.act() != MortalEntity.NONE) return;
        double dist = disciple.distanceTo(m);
        if (dist > 48 && level.getNearestPlayer(disciple, 40) == null) {
            disciple.teleportTo(m.getX() + 1.5, m.getY(), m.getZ() + 1.5);
        } else if (dist > 6) {
            disciple.walkTo(m.blockPosition(), 0.8);
        }
        if (master.visiting != null) disciple.setHome(master.visiting);
    }

    // ------------------------------------------------------------------ judgment foretold

    private static void foretell(ServerLevel level, MortalEntity m, Prophet p, Religion faith, Settlement town) {
        ProphecyData d = ProphecyData.get(level.getServer());
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        String name = town.name.isEmpty() ? "this town" : town.name;
        ProphecyData.Foretelling f = new ProphecyData.Foretelling(p.id, p.name, p.world, name, faith.id, faith.name, town.centre,
                level.getGameTime() + 3 * year);
        d.foretellings.add(f);
        d.setDirty();
        String words = "Thus saith " + faith.deityName + ": Yet forty days, and " + name + " shall be overthrown - unless ye repent.";
        m.say(words);
        m.beginAct(MortalEntity.TALK, 80);
        p.oracles.add(words);
        speak(level, m, p, words);
        CosmosData.get(level.getServer()).record(level.getServer(), p.name, "foretold", "foretold that " + name + " on " + p.world
                + " would be overthrown in forty days, for the wickedness of it.");
        long minutes = Math.max(1, 3 * year / 1200);
        tellCreator(level, faith, "[Prophecy] " + p.name + " has foretold in your name that " + name + " will be overthrown within about "
                + minutes + " minutes, unless it repents. Strike it with a wrathful power to fulfil the word, or spare it.");
    }

    /** Judgments that have come due: spared if the town repented, else the word fails. */
    private static void dueJudgments(ServerLevel level, ProphecyData d, Planet p, long now) {
        for (ProphecyData.Foretelling f : d.foretellings) {
            if (!f.outcome.isEmpty() || !f.world.equals(p.name) || now < f.deadline) continue;
            MinecraftServer server = level.getServer();
            if (f.repented >= 3) {
                f.outcome = "spared";
                narrate(level, f.centre, "And God saw their works, that they turned from their evil way; and God repented of the evil, "
                        + "that he had said that he would do unto them; and he did it not.", "Jonah 3:10");
                CosmosData.get(server).record(server, f.town, "spared", "repented at the preaching of " + f.prophetName + ", and was spared.");
                for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(f.centre).inflate(48), m -> f.faith.equals(m.religion()))) {
                    m.setDoubt(m.doubt() - 0.2);
                }
            } else {
                f.outcome = "failed";
                narrate(level, f.centre, "When a prophet speaketh in the name of the LORD, if the thing follow not, nor come to pass, that is the "
                        + "thing which the LORD hath not spoken.", "Deuteronomy 18:22");
                CosmosData.get(server).record(server, f.prophetName, "foretold", "foretold the overthrow of " + f.town + ", and it came not to pass; "
                        + "the people mocked.");
                for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(f.centre).inflate(64), m -> f.faith.equals(m.religion()))) {
                    m.setDoubt(m.doubt() + 0.1);
                }
            }
            Prophet pr = d.prophet(f.prophet);
            if (pr != null) pr.deeds.add("And " + f.town + (f.outcome.equals("spared") ? " repented, and was spared." : " was not overthrown, and the people mocked."));
            d.setDirty();
        }
    }

    // ------------------------------------------------------------------ fire from heaven

    private static final class Contest {
        final ResourceKey<Level> dim;
        final UUID prophet, priest, faith, falseFaith;
        final BlockPos altar;
        final String town, god, idol;
        int stage;
        int ticks;
        boolean waiting;
        @Nullable UUID creator;

        Contest(ResourceKey<Level> dim, UUID prophet, UUID priest, UUID faith, UUID falseFaith, BlockPos altar, String town, String god, String idol) {
            this.dim = dim;
            this.prophet = prophet;
            this.priest = priest;
            this.faith = faith;
            this.falseFaith = falseFaith;
            this.altar = altar;
            this.town = town;
            this.god = god;
            this.idol = idol;
        }
    }

    private static final List<Contest> CONTESTS = new ArrayList<>();
    private static final Map<BlockPos, Long> LAST_CONTEST = new HashMap<>();

    private static boolean busy(UUID prophet) {
        for (Contest c : CONTESTS) if (c.prophet.equals(prophet)) return true;
        return false;
    }

    /**
     * Where a town serves another god and that god's priest is at hand, the prophet challenges them
     * (1 Kings 18). Returns whether a contest began.
     */
    private static boolean challenge(ServerLevel level, MortalEntity m, Prophet p, Religion faith, Settlement town) {
        long now = level.getGameTime();
        if (now - LAST_CONTEST.getOrDefault(town.centre, -100000L) < 20 * FiatLuxConfig.COMMON.mortalYearTicks.get()) return false;
        Map<UUID, Integer> votes = new HashMap<>();
        for (MortalEntity x : level.getEntitiesOfClass(MortalEntity.class, new AABB(town.centre).inflate(48), x -> x.religion() != null)) {
            votes.merge(x.religion(), 1, Integer::sum);
        }
        UUID top = votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        Religion other = Generations.get(level.getServer()).religion(top);
        if (other == null || other.deity.true_ || other.deity == Religion.Deity.REASON) return false;
        MortalEntity priest = level.getEntitiesOfClass(MortalEntity.class, new AABB(town.centre).inflate(64),
                        x -> other.id.equals(x.religion()) && !x.isChild() && (x.getUUID().equals(other.leader) || x.profession() == Profession.PRIEST || x.isElder()))
                .stream().findFirst().orElse(null);
        if (priest == null) return false;
        return challenge(level, m, p, faith, priest, other, town.shrine, town.name.isEmpty() ? p.world : town.name);
    }

    /** Begins the contest at `altar` between the prophet and the priest of another god. */
    public static boolean challenge(ServerLevel level, MortalEntity m, Prophet p, Religion faith, MortalEntity priest, Religion other, BlockPos altar, String town) {
        if (busy(p.id)) return false;
        LAST_CONTEST.put(altar, level.getGameTime());
        CONTESTS.add(new Contest(level.dimension(), p.id, priest.getUUID(), faith.id, other.id, altar, town, faith.deityName, other.deityName));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CONTESTS.isEmpty() || event.getServer().getTickCount() % 20 != 9) return;
        for (Contest c : new ArrayList<>(CONTESTS)) step(event.getServer(), c);
    }

    private static void step(MinecraftServer server, Contest c) {
        ServerLevel level = server.getLevel(c.dim);
        MortalEntity m = level != null && level.getEntity(c.prophet) instanceof MortalEntity x && x.isAlive() ? x : null;
        MortalEntity priest = level != null && level.getEntity(c.priest) instanceof MortalEntity x && x.isAlive() ? x : null;
        if (m == null) {
            CONTESTS.remove(c);
            return;
        }
        c.ticks += 20;
        BlockPos front = c.altar.offset(0, 0, 3);
        // the people gather
        if (c.ticks <= 200) {
            for (MortalEntity x : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.altar).inflate(40), x -> x != m && x != priest && x.act() == MortalEntity.NONE)) {
                x.walkTo(front.offset(level.random.nextInt(9) - 4, 0, 3 + level.random.nextInt(4)), 0.7);
            }
            if (m.distanceToSqr(Vec3.atBottomCenterOf(front)) > 9) m.walkTo(front.east(2), 0.75);
            if (priest != null && priest.distanceToSqr(Vec3.atBottomCenterOf(front)) > 9) priest.walkTo(front.west(2), 0.75);
        }
        Prophet p = ProphecyData.get(server).prophet(c.prophet);
        switch (c.stage) {
            case 0 -> {
                if (c.ticks < 100) return;
                say(level, m, p, "How long halt ye between two opinions? if " + c.god + " be God, follow him: but if " + c.idol + ", then follow him.");
                c.stage++;
            }
            case 1 -> {
                if (c.ticks < 180) return;
                say(level, m, p, "Call ye on the name of your gods, and I will call on the name of " + c.god + ": and the God that answereth by fire, let him be God.");
                if (priest != null) {
                    priest.beginAct(MortalEntity.PRAY, 300);
                    priest.say("O " + c.idol + ", hear us!");
                }
                c.stage++;
            }
            case 2 -> {
                if (c.ticks < 340) return;
                say(level, m, p, "Cry aloud: for he is a god; either he is talking, or he is pursuing, or he is in a journey, or peradventure he sleepeth.");
                if (priest != null) priest.say("O " + c.idol + ", answer us!");
                c.stage++;
            }
            case 3 -> {
                if (c.ticks < 480) return;
                if (priest != null) {
                    priest.beginAct(MortalEntity.NONE, 1);
                    priest.say("...");
                }
                narrate(level, c.altar, "But there was no voice, nor any that answered, nor any that regarded.", "1 Kings 18:29");
                say(level, m, p, "Hear me, O " + c.god + ", hear me, that this people may know that thou art God.");
                m.beginAct(MortalEntity.PRAY, 400);
                Religion faith = Generations.get(server).religion(c.faith);
                ServerPlayer creator = faith != null && faith.creator != null ? server.getPlayerList().getPlayer(faith.creator) : null;
                c.waiting = true;
                c.creator = creator != null && creator.level() == level ? creator.getUUID() : null;
                if (c.creator != null) {
                    creator.sendSystemMessage(Component.literal("[Carmel] " + (p != null ? p.name : "Your prophet") + " calls on you to answer by fire at the altar of "
                            + c.town + "! Work any wonder there within 60 seconds.").withStyle(s -> s.withColor(0xFFB060)));
                }
                c.ticks = 0;
                c.stage++;
            }
            case 4 -> {
                boolean alone = c.creator == null && FiatLuxConfig.COMMON.prophetsActAlone.get();
                if (alone && c.ticks >= 160) fire(level, c, m, priest, null);
                else if (!alone && c.ticks >= 1200) unanswered(level, c, m, p);
            }
            default -> CONTESTS.remove(c);
        }
    }

    private static void say(ServerLevel level, MortalEntity m, @Nullable Prophet p, String words) {
        m.say(words);
        m.beginAct(MortalEntity.TALK, 70);
        if (p != null) {
            speak(level, m, p, words);
            if (p.oracles.size() < 60 && !p.oracles.contains(words)) p.oracles.add(words);
        }
    }

    /** "Then the fire of the LORD fell, and consumed the burnt sacrifice ... And when all the people saw it, they fell on their faces" (1 Kings 18:38-39). */
    private static void fire(ServerLevel level, Contest c, MortalEntity m, @Nullable MortalEntity priest, @Nullable ServerPlayer by) {
        MinecraftServer server = level.getServer();
        CONTESTS.remove(c);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(c.altar.above()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Vec3 at = Vec3.atCenterOf(c.altar.above());
        Fx.send(level, FxKind.CELESTIAL_PILLAR, at, 30f, 0xFFFFB050, 50);
        Fx.send(level, FxKind.FIRE_WAVE, at, 6f, 0xFFFF8030, 40);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 120, 0.8, 1.2, 0.8, 0.06);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 20, 0.5, 0.5, 0.5, 0.1);
        narrate(level, c.altar, "Then the fire of " + c.god + " fell, and consumed the burnt sacrifice, and the wood, and the stones. And when all the "
                + "people saw it, they fell on their faces: and they said, " + c.god + ", he is the God.", "1 Kings 18:38-39");
        int turned = 0;
        for (MortalEntity x : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.altar).inflate(40), x -> x != m)) {
            x.beginAct(MortalEntity.BOW, 80);
            if (!c.faith.equals(x.religion()) && level.random.nextDouble() < (x.hasTrait(Trait.PROUD) ? 0.45 : 0.85)) {
                x.setReligion(c.faith);
                turned++;
            }
            if (c.faith.equals(x.religion())) {
                x.setDoubt(0);
                x.say(c.god + ", he is the God; " + c.god + ", he is the God!");
            }
        }
        if (priest != null && !c.faith.equals(priest.religion())) priest.say("No... it cannot be. " + c.idol + " has forsaken me.");
        level.setBlock(c.altar.above(), Religion.Deity.CREATOR.shrineCrown().defaultBlockState(), 3);
        if (level.getBlockState(c.altar.above(2)).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) level.removeBlock(c.altar.above(2), false);
        ProphecyData d = ProphecyData.get(server);
        d.contestsWon++;
        Prophet p = d.prophet(c.prophet);
        if (p != null) p.deeds.add("And " + p.name + " called upon " + c.god + " at the altar of " + c.town + " before the priest of " + c.idol
                + ", and the fire fell from heaven; and " + turned + " of the people turned that day.");
        d.setDirty();
        CosmosData.get(server).record(server, p != null ? p.name : "A prophet", "carmel", "called down fire from heaven upon the altar of " + c.town
                + " before the priest of " + c.idol + (by != null ? "; " + by.getGameProfile().getName() + " answered by fire" : "") + ", and "
                + turned + " of the people turned to " + c.god + ".");
        if (by != null) CosmosData.get(server).addGlory(50);
    }

    /** The Creator was near, and did not answer: the people jeer, and the prophet flees (1 Kings 19:3). */
    private static void unanswered(ServerLevel level, Contest c, MortalEntity m, @Nullable Prophet p) {
        CONTESTS.remove(c);
        m.beginAct(MortalEntity.NONE, 1);
        m.say("I, even I only, am left; and they seek my life, to take it away.");
        for (MortalEntity x : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.altar).inflate(40), x -> x != m)) {
            if (c.faith.equals(x.religion())) x.setDoubt(x.doubt() + 0.2);
            else if (level.random.nextInt(3) == 0) x.say("Where is thy God now?");
        }
        ProphecyData d = ProphecyData.get(level.getServer());
        d.contestsLost++;
        if (p != null) {
            p.deeds.add("And " + p.name + " called upon " + c.god + " at the altar of " + c.town + ", and there was no answer; and " + p.he() + " fled.");
            p.visiting = null;
        }
        d.setDirty();
        CosmosData.get(level.getServer()).record(level.getServer(), p != null ? p.name : "A prophet", "carmel", "called upon " + c.god + " at the altar of "
                + c.town + ", and there was no answer; the people of " + c.idol + " mocked, and the prophet fled.");
    }

    /**
     * A Creator worked a wonder: a prophet's call for fire near it is answered, and a wrathful wonder
     * on a town under judgment fulfils the word spoken against it.
     */
    public static void onMiracle(ServerLevel level, Vec3 at, ServerPlayer caster, boolean wrathful) {
        for (Contest c : new ArrayList<>(CONTESTS)) {
            if (!c.waiting || c.dim != level.dimension() || Vec3.atCenterOf(c.altar).distanceTo(at) > 32) continue;
            if (level.getEntity(c.prophet) instanceof MortalEntity m) {
                fire(level, c, m, level.getEntity(c.priest) instanceof MortalEntity pr ? pr : null, caster);
            }
        }
        if (!wrathful) return;
        ProphecyData d = ProphecyData.get(level.getServer());
        int slot = RealmPool.slotOf(level.dimension());
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        String world = slot >= 0 && slot < cosmos.planets.size() ? cosmos.planets.get(slot).name : "";
        for (ProphecyData.Foretelling f : d.foretellings) {
            if (!f.outcome.isEmpty() || !f.world.equals(world) || Vec3.atCenterOf(f.centre).distanceTo(at) > 72) continue;
            f.outcome = "fulfilled";
            narrate(level, f.centre, "Then shall ye know that I am " + caster.getGameProfile().getName() + ", when I have laid the land most desolate "
                    + "because of all their abominations which they have committed.", "Ezekiel 33:29");
            CosmosData.get(level.getServer()).record(level.getServer(), f.town, "fulfilled", "was overthrown, as " + f.prophetName
                    + " had foretold in the name of " + caster.getGameProfile().getName() + ".");
            for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(f.centre).inflate(96))) {
                if (f.faith.equals(m.religion())) m.setDoubt(0);
                else if (level.random.nextDouble() < 0.4) m.setReligion(f.faith);
            }
            Prophet pr = d.prophet(f.prophet);
            if (pr != null) pr.deeds.add("And the word of " + pr.name + " against " + f.town + " came to pass.");
            CosmosData.get(level.getServer()).addGlory(40);
            d.setDirty();
        }
    }

    // ------------------------------------------------------------------ the end of a prophet

    /** "And it came to pass, as they still went on, and talked, that, behold, there appeared a chariot of fire" (2 Kings 2:11). */
    private static void takenUp(ServerLevel level, MortalEntity m, Prophet p, MortalEntity disciple) {
        MinecraftServer server = level.getServer();
        disciple.say("My father, my father, the chariot of Israel, and the horsemen thereof!");
        Fx.send(level, FxKind.FIRE_WAVE, m.position(), 5f, 0xFFFF9030, 50);
        Fx.send(level, FxKind.CELESTIAL_PILLAR, m.position(), 50f, 0xFFFFC060, 70);
        level.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + 2, m.getZ(), 200, 1.2, 4, 1.2, 0.1);
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), m.getX(), m.getY() + 1, m.getZ(), 80, 0.4, 2, 0.4, 0.2);
        narrate(level, m.blockPosition(), "And it came to pass, as they still went on, and talked, that, behold, there appeared a chariot of fire, "
                + "and horses of fire, and parted them both asunder; and " + p.name + " went up by a whirlwind into heaven.", "2 Kings 2:11");
        p.fate = "taken up";
        Generations g = Generations.get(server);
        Person person = g.person(m.getUUID());
        if (person != null) {
            person.died = level.getGameTime();
            person.soul = "heaven";
            person.translated = true;
            person.ageAtDeath = (int) m.ageYears();
            g.setDirty();
            CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
            c.heavenSouls += 1;
            c.setDirty();
            SoulRealms.onDeath(server, person, true);
        }
        CosmosData.get(server).record(server, p.name, "taken_up", "went up by a whirlwind into heaven in a chariot of fire, and "
                + p.discipleName + " took up " + p.his() + " mantle.");
        Scripture.prophetBook(level, p, p.discipleName);
        Religion faith = g.religion(p.faith);
        ProphecyData.get(server).setDirty();
        m.discard();
        if (faith != null) mantle(level, p, disciple, faith);
    }

    /** "He took up also the mantle of Elijah that fell from him ... The spirit of Elijah doth rest on Elisha" (2 Kings 2:13-15). */
    private static void mantle(ServerLevel level, Prophet p, MortalEntity disciple, Religion faith) {
        Prophet next = call(level, faith, disciple, p.calledBy);
        if (next == null) return;
        next.deeds.add("And " + next.name + " took up the mantle of " + p.name + " that fell from " + p.him() + "; and the spirit of " + p.name
                + " did rest on " + next.name + ".");
        next.lastWonder = 0;
        disciple.say("Where is " + p.calledBy + ", the God of " + p.name + "?");
    }

    /** A prophet has died: the book is written, and the disciple takes up the mantle. */
    public static void onDeath(ServerLevel level, MortalEntity m, DamageSource source) {
        ProphecyData d = ProphecyData.get(level.getServer());
        Prophet p = d.prophet(m.getUUID());
        if (p == null || !p.living()) return;
        boolean slain = m.sentence() != Justice.Sentence.NONE || source.getEntity() instanceof MortalEntity;
        p.fate = slain ? "martyred" : "died";
        MinecraftServer server = level.getServer();
        CosmosData.get(server).record(server, p.name, slain ? "martyr" : "prophet", slain
                ? "the prophet was slain for the word of " + p.calledBy + "; precious in the sight of " + p.calledBy + " is the death of " + p.his() + " saints."
                : "the prophet died, full of years, and was gathered unto " + p.his() + " people.");
        MortalEntity disciple = p.disciple != null && level.getEntity(p.disciple) instanceof MortalEntity x && x.isAlive() ? x : null;
        Scripture.prophetBook(level, p, disciple != null ? p.discipleName : "the scribes of " + p.world);
        d.setDirty();
        Religion faith = Generations.get(server).religion(p.faith);
        if (disciple != null && faith != null && !p.false_) mantle(level, p, disciple, faith);
    }

    // ================================================================== reports

    public static List<String> describe(MinecraftServer server) {
        ProphecyData d = ProphecyData.get(server);
        List<String> out = new ArrayList<>();
        if (d.prophets.isEmpty()) out.add("No prophet has yet been called. When a people goes astray, one will be; or say \"Whom shall I send?\" among your faithful.");
        for (Prophet p : d.prophets.values()) {
            Religion faith = Generations.get(server).religion(p.faith);
            String state = p.living() ? "living" + (p.disciple != null ? ", followed by " + p.discipleName : "") : p.fate;
            out.add((p.false_ ? "False prophet " : "") + p.name + " of " + p.world + " (" + (faith != null ? faith.name : "?") + ") - " + state + "; "
                    + p.oracles.size() + " oracles, " + p.wonders + " wonders");
        }
        for (ProphecyData.Foretelling f : d.foretellings) {
            out.add("  " + f.prophetName + " foretold the overthrow of " + f.town + ": " + (f.outcome.isEmpty() ? "awaiting (" + f.repented + " have repented)" : f.outcome));
        }
        if (d.contestsWon + d.contestsLost > 0) out.add("Fire from heaven: answered " + d.contestsWon + ", unanswered " + d.contestsLost + ".");
        if (!d.ofTheSon.isEmpty()) out.add("Spoken of one who is to come: " + d.ofTheSon.size() + (d.ofTheSon.size() == 1 ? " word" : " words") + ".");
        out.add("Books written: " + d.books.size() + ".");
        return out;
    }

    private static void tellCreator(ServerLevel level, Religion faith, String text) {
        if (faith.creator == null) return;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(faith.creator);
        if (p != null) p.sendSystemMessage(Component.literal(text).withStyle(s -> s.withColor(0xE8C890)));
    }

    static void narrate(ServerLevel level, BlockPos near, String text, String ref) {
        com.josephison.fiatlux.infernal.Infernal.narrate(level, near, text, ref);
    }
}
