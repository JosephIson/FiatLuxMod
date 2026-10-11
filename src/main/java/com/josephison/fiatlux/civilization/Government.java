package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.civilization.Polity.Form;
import com.josephison.fiatlux.civilization.Polity.Law;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * How peoples govern themselves. Once there are enough of them, the families of a world gather
 * under their eldest as a clan; as the ages pass they become a chiefdom (or, if they keep faith, a
 * people ruled by judges), a kingdom - "make us a king to judge us like all the nations" (1 Sam 8:5) -
 * an empire when its towns are many, a republic, and at last a federation. Crowns pass to the eldest
 * child; judges, consuls and chancellors are chosen. Rulers give laws over the years, the proud
 * sometimes rise against them, and everything is set down in the people's annals and the Chronicle.
 */
public final class Government {
    /** Checks made per mortal year (the population ticks every 100 game ticks). */
    private static final double CHECKS_PER_YEAR = 24;

    private Government() {}

    public static void tick(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p, List<Settlement> towns) {
        if (!cosmos.fallOfMan || towns.isEmpty()) return;   // in the garden there was no rule but the Creator's
        MinecraftServer server = level.getServer();
        Polities polities = Polities.get(server);
        Generations gens = Generations.get(server);
        Polity g = polities.of(p.id);
        List<Person> people = living(gens, p.name, level.getGameTime());
        if (g == null) {
            if (people.size() < 6 && towns.size() < 2) return;   // a family needs no government
            g = found(server, level, polities, gens, p, towns, people);
            if (g == null) return;
        }
        RandomSource r = level.random;
        long now = level.getGameTime();
        double yearTicks = FiatLuxConfig.COMMON.mortalYearTicks.get();
        // the ruler
        Person ruler = gens.person(g.ruler);
        if (ruler == null || !ruler.living()) succeed(server, gens, g, p, people, ruler, now);
        else if (!g.form.hereditary && now - g.rulerSince > 4 * yearTicks && g.form != Form.CLAN) elect(server, gens, g, p, people, now);
        g.stateFaith = ruler != null && ruler.living() ? ruler.religion : g.stateFaith;
        // the form of government follows the age
        Religion faith = gens.religion(g.stateFaith);
        Form form = Form.of(p.era, towns.size(), p.faith, faith == null || faith.deity.true_);
        if (form != g.form) reform(server, gens, g, p, form, towns, now);
        // laws, given one at a time over the years
        if (r.nextDouble() < 0.35 / CHECKS_PER_YEAR) legislate(server, g, p, faith, r);
        // the proud rise against the crown
        if (g.form.hereditary && r.nextDouble() < 0.04 / CHECKS_PER_YEAR) Justice.plot(level, g, p);
        polities.setDirty();
    }

    /** The living people of a world, from the Book of Generations. */
    static List<Person> living(Generations gens, String world, long now) {
        List<Person> out = new ArrayList<>();
        for (Person x : gens.people.values()) if (x.living() && world.equals(x.world)) out.add(x);
        return out;
    }

    static double age(Person p, long now) {
        return (now - p.born) / (double) FiatLuxConfig.COMMON.mortalYearTicks.get();
    }

    @Nullable
    private static Polity found(MinecraftServer server, ServerLevel level, Polities polities, Generations gens, Planet p, List<Settlement> towns,
                                List<Person> people) {
        long now = level.getGameTime();
        Person first = people.stream().filter(x -> age(x, now) >= 16).max(Comparator.comparingDouble(x -> age(x, now))).orElse(null);
        if (first == null) return null;
        Religion faith = gens.religion(first.religion);
        Form form = Form.of(p.era, towns.size(), p.faith, faith == null || faith.deity.true_);
        String capital = towns.getFirst().name.isEmpty() ? p.name : towns.getFirst().name;
        Polity g = new Polity(UUID.randomUUID(), p.id, name(form, capital, first.name), form, capital, now);
        polities.byWorld.put(p.id, g);
        crown(gens, g, first, now);
        g.laws.add(Law.KILLING);   // "Whoso sheddeth man's blood, by man shall his blood be shed" (Gen 9:6)
        String what = switch (form) {
            case CLAN -> "gathered the families of " + p.name + " under " + first.name + ", eldest among them, as a clan.";
            case JUDGES -> "raised up " + first.name + " to judge the people of " + p.name + ".";
            default -> "made " + first.name + " " + form.title(first.male) + " of the " + g.name + ".";
        };
        record(server, g, "The people of " + p.name, what);
        return g;
    }

    static String name(Form form, String capital, String eldest) {
        return switch (form) {
            case CLAN -> "Clan of " + eldest;
            case JUDGES -> "People of " + capital;
            default -> form.noun + " of " + capital;
        };
    }

    public static void crown(Generations gens, Polity g, Person who, long now) {
        g.ruler = who.id;
        g.rulerName = who.name;
        g.rulerMale = who.male;
        g.rulerSince = now;
        g.rulers.add(who.name);
        g.reign = (int) g.rulers.stream().filter(n -> n.equals(who.name)).count();
        g.stateFaith = who.religion;
    }

    /** A ruler is dead: the crown passes to the eldest living child, or the people choose. */
    private static void succeed(MinecraftServer server, Generations gens, Polity g, Planet p, List<Person> people, @Nullable Person dead, long now) {
        Person heir = null;
        if (g.form.hereditary && dead != null) {
            heir = gens.childrenOf(dead.id).stream().filter(c -> c.living() && age(c, now) >= 14).findFirst().orElse(null);
            if (heir == null && dead.spouse != null) {
                Person widow = gens.person(dead.spouse);
                if (widow != null && widow.living()) heir = widow;
            }
        }
        if (heir == null) heir = choose(gens, g, people, now);
        if (heir == null) return;
        String was = g.rulerTitle();
        crown(gens, g, heir, now);
        record(server, g, g.name, (dead == null ? "" : was + " died; ") + heir.name + " " + (g.form.hereditary ? "was crowned " : "was chosen ")
                + g.form.title(heir.male) + (g.reign > 1 ? " " + Polity.roman(g.reign) : "") + ".");
    }

    /** Judges, consuls and chancellors serve their term and the people choose again. */
    private static void elect(MinecraftServer server, Generations gens, Polity g, Planet p, List<Person> people, long now) {
        Person next = choose(gens, g, people, now);
        if (next == null) return;
        boolean again = next.id.equals(g.ruler);
        crown(gens, g, next, now);
        if (!again) record(server, g, g.name, "chose " + next.name + " as " + g.form.title(next.male) + ".");
        else g.rulerSince = now;
    }

    /** Someone fit to rule: the eldest for a clan, the most faithful for judges, otherwise any grown man or woman of standing. */
    @Nullable
    private static Person choose(Generations gens, Polity g, List<Person> people, long now) {
        List<Person> adults = people.stream().filter(x -> age(x, now) >= 20).toList();
        if (adults.isEmpty()) adults = people.stream().filter(x -> age(x, now) >= 14).toList();
        if (adults.isEmpty()) return null;
        if (g.form == Form.CLAN) return adults.stream().max(Comparator.comparingDouble(x -> age(x, now))).orElse(null);
        if (g.form == Form.JUDGES) {
            List<Person> faithful = adults.stream().filter(x -> {
                Religion r = gens.religion(x.religion);
                return r != null && r.deity.true_;
            }).toList();
            if (!faithful.isEmpty()) adults = faithful;
        }
        return adults.get((int) Math.floorMod(now * 31 + adults.size(), adults.size()));
    }

    /** The age changes the form of government. */
    private static void reform(MinecraftServer server, Generations gens, Polity g, Planet p, Form form, List<Settlement> towns, long now) {
        String before = g.name;
        Form old = g.form;
        g.form = form;
        g.capital = towns.getFirst().name.isEmpty() ? p.name : towns.getFirst().name;
        g.name = name(form, g.capital, g.rulerName);
        String what;
        if (old == Form.JUDGES && form == Form.KINGDOM) {
            what = "The people said, Make us a king to judge us like all the nations; and " + g.rulerName + " was anointed "
                    + form.title(g.rulerMale) + ". So the " + before + " became the " + g.name + ".";
        } else if (form == Form.REPUBLIC || form == Form.FEDERATION) {
            what = "The " + before + " became the " + g.name + "; its people now choose who governs them.";
        } else {
            what = "The " + before + " became the " + g.name + ", and " + g.rulerName + " its " + form.title(g.rulerMale) + ".";
        }
        record(server, g, g.name, what);
    }

    /** A ruler gives a law. A people that keeps faith with its Creator is given the commandments; others, the decrees of their gods. */
    private static void legislate(MinecraftServer server, Polity g, Planet p, @Nullable Religion faith, RandomSource r) {
        boolean trueFaith = faith == null || faith.deity.true_;
        List<Law> pool = new ArrayList<>(List.of(Law.STEALING, Law.KILLING, Law.VIOLENCE, Law.SEDITION));
        pool.add(trueFaith ? Law.IDOLS : Law.STATE_GODS);
        // a change of state faith overturns the old religious law
        if (trueFaith) g.laws.remove(Law.STATE_GODS);
        else g.laws.remove(Law.IDOLS);
        pool.removeAll(g.laws);
        if (pool.isEmpty()) return;
        Law law = pool.get(r.nextInt(pool.size()));
        g.laws.add(law);
        String words = law == Law.STATE_GODS && faith != null ? "All shall fall down and worship " + faith.deityName + "." : law.text;
        record(server, g, g.rulerTitle(), "of the " + g.name + " decreed: \"" + words + "\"");
    }

    public static void record(MinecraftServer server, Polity g, String actor, String text) {
        g.annal(actor + " " + text);
        CosmosData.get(server).record(server, actor, "government", text);
        Polities.get(server).setDirty();
    }
}
