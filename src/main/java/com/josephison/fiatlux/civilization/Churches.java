package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The life of faiths as bodies of people: each has a leader - an elder, a high priest, a patriarch,
 * an archbishop, a hierophant - who preaches, keeps the faithful, and is succeeded when they die; a
 * corrupt leader brings scandal ("the priests thereof teach for hire", Micah 3:11). Faiths split into
 * branches over doctrine and over their leaders - "every one of you saith, I am of Paul; and I of
 * Apollos" (1 Cor 1:12) - and the followers of the place where the dissent began go with it. Single
 * mortals leave their faith: some for another that is near, some to found a faith of their own, some
 * to believe in nothing. A faith whose last follower dies is no more.
 */
public final class Churches {
    private static final double CHECKS_PER_YEAR = 24;
    private static final String[] DISPUTES = {
            "over how the Creator is to be worshipped", "over whether the dead are judged at once or at the last day",
            "over the corruption of its leader", "over the keeping of the holy days", "over who may enter the sanctuary",
            "over the words of the prayers", "over whether the old law still binds", "over the right to choose its own leaders",
            "over images in the sanctuary", "over whether the faith may be preached to strangers", "over the reckoning of the calendar",
            "over the place where sacrifice may be offered"};

    private Churches() {}

    /** Called for each living world with someone in it, every few seconds. */
    public static void tick(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p) {
        if (!cosmos.fallOfMan) return;
        MinecraftServer server = level.getServer();
        Generations gens = Generations.get(server);
        long now = level.getGameTime();
        Map<UUID, List<Person>> followers = new HashMap<>();
        for (Person x : gens.people.values()) {
            if (x.living() && p.name.equals(x.world) && x.religion != null) followers.computeIfAbsent(x.religion, k -> new ArrayList<>()).add(x);
        }
        RandomSource r = level.random;
        for (Religion faith : new ArrayList<>(gens.religions.values())) {
            if (!p.name.equals(faith.world)) continue;
            List<Person> flock = followers.getOrDefault(faith.id, List.of());
            if (flock.isEmpty()) {
                if (faith.extinct < 0 && faith.leaders > 0) {
                    faith.extinct = now;
                    gens.setDirty();
                    CosmosData.get(server).record(server, faith.name, "faith_ended", "lost the last of its followers on " + p.name + "; it is remembered no more.");
                }
                continue;
            }
            faith.extinct = -1;
            Person leader = gens.person(faith.leader);
            if (leader == null || !leader.living() || !faith.id.equals(leader.religion)) succeed(level, gens, faith, flock, p, leader);
            // scandal: a corrupt leader makes the faithful doubt
            MortalEntity l = faith.leader != null && level.getEntity(faith.leader) instanceof MortalEntity m ? m : null;
            if (l != null && corrupt(l) && r.nextDouble() < 0.5 / CHECKS_PER_YEAR) scandal(level, faith, l, p);
            // a branch splits off
            double split = FiatLuxConfig.COMMON.schismChance.get() * (flock.size() >= 8 ? 1 : 0)
                    * (1 + cosmos.townsOf(p.id).size() * 0.3) * (l != null && corrupt(l) ? 3 : 1);
            if (r.nextDouble() < split / CHECKS_PER_YEAR) branch(level, faith, p, r);
        }
    }

    // ------------------------------------------------------------------ leaders

    /** Whether one who leads lives worse than the flock: more sin than virtue. */
    public static boolean corrupt(MortalEntity m) {
        Alignment a = Divinity.alignment(m);
        return a.sin() > a.virtue() + 4;
    }

    /**
     * The leader is dead or gone: the faithful choose another - a priest if there is one, the most
     * devout and the eldest among them first. The new leader is the one at hand (loaded) if possible.
     */
    private static void succeed(ServerLevel level, Generations gens, Religion faith, List<Person> flock, Planet p, @Nullable Person was) {
        long now = level.getGameTime();
        int year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        Person next = flock.stream().filter(x -> (now - x.born) / (double) year >= 18)
                .max(Comparator.comparingDouble(x -> (Profession.PRIEST.id.equals(x.profession) ? 100 : 0) + (level.getEntity(x.id) != null ? 50 : 0)
                        + (now - x.born) / (double) year)).orElse(null);
        if (next == null) return;
        faith.leader = next.id;
        faith.leaderName = next.name;
        faith.leaderMale = next.male;
        faith.leaders++;
        gens.setDirty();
        String title = faith.leaderTitle(p.era);
        boolean died = was != null && !was.living();
        String what = (died ? "died, and " + next.name + " became " : "became ") + title + " of " + the(faith.name)
                + (faith.leaders > 1 ? ", the " + ordinal(faith.leaders) + " to lead it" : "") + ".";
        CosmosData.get(level.getServer()).record(level.getServer(), died ? was.name : next.name, "faith_leader", what);
        if (level.getEntity(next.id) instanceof MortalEntity m) {
            m.say("I am not worthy... but I will keep the faithful of " + faith.deityName + ".");
            Fx.send(level, FxKind.BLESSING, m.position(), m.getBbHeight(), faith.deity.color, 30);
        }
    }

    /** "The priests thereof teach for hire" (Micah 3:11): the corruption of the leader becomes known. */
    private static void scandal(ServerLevel level, Religion faith, MortalEntity leader, Planet p) {
        String[] what = {"was found to have taken the offerings for himself", "was seen at the feasts of " + (faith.deity.true_ ? "the idols" : "a rival god"),
                "sold the blessings of the sanctuary for silver", "was shown to have lied to the faithful"};
        String deed = what[level.random.nextInt(what.length)].replace("himself", leader.isMale() ? "himself" : "herself");
        for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, leader.getBoundingBox().inflate(96), m -> faith.id.equals(m.religion()))) {
            m.setDoubt(m.doubt() + 0.25);
        }
        leader.say("Lies! All lies! Who are you to judge me?");
        CosmosData.get(level.getServer()).record(level.getServer(), leader.getName().getString(), "scandal",
                faith.leaderTitle(p.era) + " of " + the(faith.name) + ", " + deed + "; the faithful of " + p.name + " were shaken.");
    }

    /** The title a mortal holds in its faith ("" if it leads none). */
    public static String titleOf(ServerLevel level, MortalEntity m) {
        Religion faith = Faiths.of(m);
        if (faith == null || !m.getUUID().equals(faith.leader)) return "";
        Planet p = m.planet();
        return faith.leaderTitle(p != null ? p.era : MortalEra.HUNTER_GATHERER);
    }

    // ------------------------------------------------------------------ branches

    /**
     * A dissenter rises among the faithful - proud, or curious, or a priest who would not be ruled -
     * and a branch splits away over doctrine, keeping the same god. Those of the same faith in the
     * dissenter's town go with it more often than not; others here and there.
     */
    @Nullable
    public static Religion branch(ServerLevel level, Religion parent, Planet p, RandomSource r) {
        List<? extends MortalEntity> faithful = level.getEntities(com.josephison.fiatlux.registry.ModEntities.MORTAL.get(),
                m -> m.isAlive() && parent.id.equals(m.religion()) && !m.isChild());
        MortalEntity dissenter = faithful.stream().filter(m -> !m.getUUID().equals(parent.leader))
                .max(Comparator.comparingDouble(m -> (m.hasTrait(Trait.PROUD) ? 3 : 0) + (m.hasTrait(Trait.CURIOUS) ? 2 : 0)
                        + (m.profession() == Profession.PRIEST || m.profession() == Profession.SCHOLAR ? 2 : 0) + r.nextDouble() * 2)).orElse(null);
        if (dissenter == null) return null;
        String dispute = DISPUTES[r.nextInt(DISPUTES.length)];
        Settlement town = dissenter.settlement();
        String place = town != null && !town.name.isEmpty() ? town.name : p.name;
        String founder = dissenter.getName().getString();
        String base = rootName(parent);
        String[] names = {"Reformed " + base, base + " of " + place, "Orthodox " + base, "The True " + base, "Free " + base,
                "The " + founder + "ites", "Brethren of " + parent.deityName, "Old " + base};
        String name = names[r.nextInt(names.length)];
        Generations gens = Generations.get(level.getServer());
        for (Religion other : gens.religions.values()) if (other.name.equals(name)) name = name + " of " + place;
        Religion b = new Religion(UUID.randomUUID(), name, parent.deity, parent.deityName, parent.creator, founder, p.name, level.getGameTime());
        b.parent = parent.id;
        b.doctrine = dispute;
        b.leader = dissenter.getUUID();
        b.leaderName = founder;
        b.leaderMale = dissenter.isMale();
        b.leaders = 1;
        gens.add(b);
        dissenter.setReligion(b.id);
        dissenter.say("We will not be ruled by " + (parent.leaderName.isEmpty() ? "them" : parent.leaderName) + " any longer. Who is with me?");
        int went = 1;
        for (MortalEntity m : faithful) {
            if (m == dissenter || m.getUUID().equals(parent.leader)) continue;
            boolean neighbour = town != null && m.settlement() == town;
            double chance = (neighbour ? 0.45 : 0.12) + m.doubt() * 0.3 + (m.hasTrait(Trait.PROUD) ? 0.1 : 0) - (m.hasTrait(Trait.PIOUS) ? 0.1 : 0);
            if (r.nextDouble() < chance) {
                m.setReligion(b.id);
                went++;
            }
        }
        String title = parent.leaderTitle(p.era);
        CosmosData.get(level.getServer()).record(level.getServer(), founder, "faith_split",
                "broke with " + the(parent.name) + " " + dispute + (parent.leaderName.isEmpty() ? "" : ", against " + title + " " + parent.leaderName)
                        + ", and " + went + (went == 1 ? " follower" : " followers") + " went with " + (dissenter.isMale() ? "him" : "her")
                        + " into the " + name + ".");
        tellCreator(level, parent, Component.translatable("message.fiatlux.faith.split", founder, parent.name, name, dispute));
        return b;
    }

    /** The name a branch is named from: its parent's, without the marks of earlier splits ("Reformed", "... of Enoch"). */
    private static String rootName(Religion r) {
        String n = r.name;
        for (String pre : new String[]{"Reformed ", "Orthodox ", "The True ", "Free ", "Old "}) if (n.startsWith(pre)) n = n.substring(pre.length());
        int first = n.indexOf(" of "), last = n.lastIndexOf(" of ");
        return first >= 0 && last != first ? n.substring(0, last) : n;
    }

    // ------------------------------------------------------------------ those who leave

    /**
     * A mortal leaves its faith, from doubt, pride, or disgust at its leader. It may turn to the faith
     * most of its neighbours hold, found a faith of its own, or hold to no god at all and gather others
     * who reason as it does. Returns the faith it now holds, or null if it stayed.
     */
    @Nullable
    public static Religion apostasy(ServerLevel level, MortalEntity m, Religion from) {
        RandomSource r = level.random;
        Generations gens = Generations.get(level.getServer());
        String world = m.worldName();
        // another faith that is near
        Map<UUID, Integer> near = new HashMap<>();
        for (MortalEntity n : level.getEntitiesOfClass(MortalEntity.class, m.getBoundingBox().inflate(48), n -> n != m && n.religion() != null
                && !n.religion().equals(from.id))) near.merge(n.religion(), 1, Integer::sum);
        UUID best = near.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        double roll = r.nextDouble();
        Religion to;
        String how;
        if (best != null && roll < 0.45) {
            to = gens.religion(best);
            if (to == null) return null;
            m.setReligion(to.id);
            how = "left " + the(from.name) + " for " + the(to.name);
        } else if (roll < 0.75 && (m.hasTrait(Trait.PROUD) || m.hasTrait(Trait.CURIOUS) || m.profession() == Profession.PRIEST
                || m.profession() == Profession.SCHOLAR || m.isElder())) {
            to = found(level, m, world, from);
            return to;
        } else {
            to = reason(level, m, world);
            m.setReligion(to.id);
            how = "left " + the(from.name) + " and would hold to no god at all, joining " + the(to.name);
        }
        m.setDoubt(0.1);
        m.say(to.deity == Religion.Deity.REASON ? "There is no one up there. We have only ourselves." : "I have found a better way: " + to.name + ".");
        CosmosData.get(level.getServer()).record(level.getServer(), m.getName().getString(), "apostasy", how + ", on " + world + ".");
        return to;
    }

    /** "The fool hath said in his heart, There is no God" (Psalm 14:1): those of a world who hold to none. */
    public static Religion reason(ServerLevel level, MortalEntity m, String world) {
        Generations gens = Generations.get(level.getServer());
        for (Religion r : gens.religions.values()) if (r.deity == Religion.Deity.REASON && r.world.equals(world)) return r;
        String[] names = {"School of Reason", "Followers of the Open Eye", "Society of the Unbound", "Lovers of Wisdom"};
        Religion r = new Religion(UUID.randomUUID(), names[level.random.nextInt(names.length)], Religion.Deity.REASON, "Reason", null,
                m.getName().getString(), world, level.getGameTime());
        r.leader = m.getUUID();
        r.leaderName = m.getName().getString();
        r.leaderMale = m.isMale();
        r.leaders = 1;
        gens.add(r);
        CosmosData.get(level.getServer()).record(level.getServer(), m.getName().getString(), "faith_founded",
                "of " + world + " said there is no god, and gathered those who reason into " + the(r.name) + ".");
        return r;
    }

    /**
     * A new faith of the leaver's own: what it worships follows its temperament and its age - the sun,
     * the stars, the ancestors, idols and the fallen ({@link Faiths#schism}), or the earth and its
     * seasons, or, under an empire, the emperor himself.
     */
    public static Religion found(ServerLevel level, MortalEntity m, String world, Religion from) {
        RandomSource r = level.random;
        var polity = Justice.polityOf(level, m);
        boolean empire = polity != null && polity.form == Polity.Form.EMPIRE;
        Religion made;
        if (empire && r.nextInt(3) == 0) {
            String ruler = polity.rulerName.isEmpty() ? "the Emperor" : polity.rulerName;
            made = founded(level, m, world, Religion.Deity.EMPEROR, "the Divine " + ruler, "Cult of the Divine " + ruler);
        } else if ((m.profession() == Profession.FARMER || m.profession() == Profession.SHEPHERD || m.profession() == Profession.GATHERER) && r.nextInt(3) == 0) {
            String[] names = {"the Great Mother", "the Green One", "Asherah", "the Lady of the Grove"};
            String deity = names[r.nextInt(names.length)];
            made = founded(level, m, world, Religion.Deity.NATURE, deity, "Grove of " + deity);
        } else {
            made = Faiths.schism(level, m, world);
            m.say("No more waiting on silent heavens. We will worship " + made.deityName + "!");
            return made;
        }
        m.say("I will follow " + made.deityName + ", and whoever would may come with me.");
        CosmosData.get(level.getServer()).record(level.getServer(), m.getName().getString(), "schism",
                "of " + world + " left " + the(from.name) + " and founded " + the(made.name) + ", worshipping " + made.deityName + ".");
        tellCreator(level, from, Component.translatable("message.fiatlux.faith.schism", m.getName(), world, made.name, made.deityName));
        return made;
    }

    private static Religion founded(ServerLevel level, MortalEntity m, String world, Religion.Deity deity, String deityName, String name) {
        Religion r = new Religion(UUID.randomUUID(), name, deity, deityName, null, m.getName().getString(), world, level.getGameTime());
        r.leader = m.getUUID();
        r.leaderName = m.getName().getString();
        r.leaderMale = m.isMale();
        r.leaders = 1;
        Generations.get(level.getServer()).add(r);
        m.setReligion(r.id);
        return r;
    }

    private static void tellCreator(ServerLevel level, Religion faith, Component message) {
        if (faith.creator == null) return;
        ServerPlayer creator = level.getServer().getPlayerList().getPlayer(faith.creator);
        if (creator != null) creator.sendSystemMessage(message.copy().withStyle(s -> s.withColor(0xE0A060)));
    }

    // ------------------------------------------------------------------ reports

    /** Every faith of a world that has followers, with its branches under it, for the command and the report. */
    public static List<String> describe(MinecraftServer server, Planet p) {
        Generations gens = Generations.get(server);
        Map<UUID, Integer> count = gens.followers();
        List<String> out = new ArrayList<>();
        List<Religion> roots = gens.religions.values().stream().filter(r -> p.name.equals(r.world) && (r.parent == null || gens.religion(r.parent) == null))
                .sorted(Comparator.comparingLong(r -> r.founded)).toList();
        for (Religion root : roots) tree(gens, count, root, p, 0, out);
        return out;
    }

    private static void tree(Generations gens, Map<UUID, Integer> count, Religion r, Planet p, int depth, List<String> out) {
        int n = count.getOrDefault(r.id, 0);
        String lead = r.leaderName.isEmpty() || n == 0 ? "" : ", led by " + r.leaderTitle(p.era) + " " + r.leaderName;
        String state = n == 0 ? (r.extinct >= 0 ? " (no more)" : " (no followers)") : " - " + n + (n == 1 ? " follower" : " followers");
        out.add("  ".repeat(depth) + (depth > 0 ? "└ " : "") + r.name + " (" + r.deityName + ")" + state + lead
                + (r.doctrine.isEmpty() ? "" : "; split " + r.doctrine));
        for (Religion child : gens.religions.values()) if (r.id.equals(child.parent)) tree(gens, count, child, p, depth + 1, out);
    }

    /** "the Way of Dev", "the Children of Shemesh": a faith's name as it stands in a sentence. */
    public static String the(String name) {
        return name.startsWith("The ") ? "the " + name.substring(4) : "the " + name;
    }

    private static String ordinal(int n) {
        int m = n % 100;
        String suffix = m >= 11 && m <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }

    /** For tests: whether a faith is a branch of another. */
    public static boolean branchOf(Religion child, Religion parent) {
        return parent.id.equals(child.parent);
    }

    /** All the faithful of a faith near a place (used when a leader preaches). */
    static List<MortalEntity> flockNear(ServerLevel level, Religion faith, AABB area) {
        return level.getEntitiesOfClass(MortalEntity.class, area, m -> faith.id.equals(m.religion()));
    }
}
