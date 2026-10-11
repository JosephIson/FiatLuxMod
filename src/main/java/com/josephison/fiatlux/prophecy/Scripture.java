package com.josephison.fiatlux.prophecy;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Polity;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Write the vision, and make it plain upon tables, that he may run that readeth it" (Hab 2:2). Each
 * people writes its own scriptures out of what happened to it: once it has learned to farm and has a
 * priest or a scribe, the Book of Beginnings - the creation of its world, the garden, the first
 * murder, the generations, the Watchers, the Flood - from the annals of the great acts; the Law when
 * its rulers give it laws; Chronicles of its towns, its kings and its faiths as the years pass; the
 * psalms its faithful sing and the proverbs of its elders; the book of every prophet sent to it; and,
 * when it splits, the testament of the branch. False faiths write their own hymns and sayings. The
 * books lie open on a lectern at the shrine, priests read from them, and they can be held as written
 * books.
 */
public final class Scripture {
    private Scripture() {}

    /** The great acts the scribes keep. */
    private static final Set<String> SACRED = Set.of("fiat_lux", "big_bang", "galaxy", "planet", "life", "eden", "eden_guard", "the_fall",
            "fall_of_man", "exile", "first_murder", "mark_of_cain", "generations", "translated", "watchers", "nephilim", "wickedness",
            "giant_death", "bound", "noah", "ark", "flood", "all_flesh", "covenant", "ararat", "rebellion", "cast_out", "black_hole", "descent",
            "town", "government", "justice", "mortal_era", "exodus", "faith_founded", "schism", "faith_split", "faith_leader", "scandal",
            "rift", "possessed", "exorcism", "anointed", "watch", "answered", "deliverance", "redemption", "prophet", "oracle", "wonder",
            "carmel", "foretold", "spared", "fulfilled", "taken_up", "martyr", "scripture", "underworld_revolution", "cleanse", "averted");
    /** Acts that belong to every world: the making of the heavens, the war in Heaven. */
    private static final Set<String> UNIVERSAL = Set.of("fiat_lux", "big_bang", "galaxy", "rebellion", "cast_out", "black_hole", "watch");
    private static final int MAX_ANNALS = 1500;

    /** The chapters of the Book of Beginnings, each with the acts it tells of. */
    private static final String[][] BEGINNINGS = {
            {"The Creation", "fiat_lux", "big_bang", "galaxy", "planet", "life"},
            {"The War in Heaven", "rebellion", "cast_out", "descent", "black_hole"},
            {"The Garden", "eden", "the_fall", "fall_of_man", "exile", "eden_guard"},
            {"Cain and Abel", "first_murder", "mark_of_cain"},
            {"The Generations", "generations", "translated"},
            {"The Watchers and the Giants", "watchers", "nephilim", "wickedness", "giant_death", "bound"},
            {"The Flood", "noah", "ark", "flood", "all_flesh", "covenant", "ararat"}};
    private static final Set<String> HISTORY = Set.of("town", "government", "justice", "mortal_era", "exodus", "faith_founded", "schism",
            "faith_split", "faith_leader", "scandal", "rift", "possessed", "exorcism", "anointed", "watch", "answered", "deliverance",
            "redemption", "carmel", "spared", "fulfilled", "taken_up", "martyr", "underworld_revolution", "averted");

    // ================================================================== the annals

    /** Called for every act written in the Chronicle: the sacred ones are kept for the scribes. */
    public static void annal(MinecraftServer server, long time, String actor, String kind, String text) {
        if (!SACRED.contains(kind)) return;
        ProphecyData d = ProphecyData.get(server);
        if (seed(server, d)) return;   // the act was just gathered from the Chronicle with the rest
        d.annals.add(new ProphecyData.Annal(time, actor, kind, text));
        while (d.annals.size() > MAX_ANNALS) d.annals.removeFirst();
        d.setDirty();
    }

    /** The first time the annals are needed, what the Chronicle still remembers is gathered into them. */
    private static boolean seed(MinecraftServer server, ProphecyData d) {
        if (d.seeded) return false;
        d.seeded = true;
        for (var e : CosmosData.get(server).chronicle()) {
            if (SACRED.contains(e.kind())) d.annals.add(new ProphecyData.Annal(e.gameTime(), e.actor(), e.kind(), e.text()));
        }
        d.setDirty();
        return true;
    }

    /** Whether an act belongs to the story of `world`. */
    private static boolean concerns(MinecraftServer server, ProphecyData.Annal a, Planet world) {
        if (UNIVERSAL.contains(a.kind())) return true;
        if (a.text().contains(world.name) || a.actor().contains(world.name)) return true;
        for (Settlement t : CosmicExpanseSavedData.get(server).townsOf(world.id)) {
            if (!t.name.isEmpty() && (a.text().contains(t.name) || a.actor().contains(t.name))) return true;
        }
        String who = a.actor().split("[ ,]")[0];
        for (Person p : Generations.get(server).people.values()) if (p.name.equals(who)) return p.world.equals(world.name);
        return false;
    }

    /** An act as a verse: "And Noah went forth, and his sons..." */
    static String verse(ProphecyData.Annal a) {
        String text = a.text().trim();
        if (text.isEmpty()) return a.actor();
        String actor = a.actor();
        if (actor.startsWith("The ") || actor.startsWith("the ")) actor = "the" + actor.substring(3);
        return "And " + actor + " " + text;
    }

    // ================================================================== writing the books

    /**
     * Every few seconds for each living world: its peoples' scribes write what is not yet written. A
     * people needs to have learned to farm, and to have a priest, a scholar or an elder to write.
     */
    public static void worldTick(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p) {
        if (!cosmos.fallOfMan || p.era.ordinal() < MortalEra.AGRARIAN.ordinal()) return;
        MinecraftServer server = level.getServer();
        ProphecyData d = ProphecyData.get(server);
        seed(server, d);
        Generations gens = Generations.get(server);
        Map<UUID, Integer> followers = gens.followers();
        RandomSource r = level.random;
        for (Religion faith : new ArrayList<>(gens.religions.values())) {
            if (!faith.world.equals(p.name) || followers.getOrDefault(faith.id, 0) == 0) continue;
            MortalEntity scribe = scribe(level, faith);
            if (scribe == null) continue;
            if (faith.parent == null && d.book(faith.id, Book.Kind.BEGINNINGS) == null) {
                write(level, d, beginnings(server, d, faith, p, scribe), scribe, p);
                continue;
            }
            Polity polity = Polities.get(server).of(p.id);
            if (faith.deity.true_ && polity != null && !polity.laws.isEmpty() && d.book(faith.id, Book.Kind.LAW) == null
                    && (polity.stateFaith == null || polity.stateFaith.equals(faith.id))) {
                write(level, d, law(polity, faith, scribe, level.getGameTime()), scribe, p);
                continue;
            }
            Book chronicle = chronicles(server, d, faith, p, scribe, level.getGameTime());
            if (chronicle != null) {
                write(level, d, chronicle, scribe, p);
                continue;
            }
            // a song or a saying, now and then
            if (r.nextDouble() < 0.04) psalm(level, d, faith, scribe, p);
            else if (r.nextDouble() < 0.03 && scribe.isElder()) proverb(level, d, faith, scribe);
        }
        lecterns(level, cosmos, p, d);
    }

    /** One of the faith who can write: a priest, a scholar, or failing them an elder, at hand. */
    @Nullable
    private static MortalEntity scribe(ServerLevel level, Religion faith) {
        MortalEntity best = null;
        int rank = 0;
        for (MortalEntity m : level.getEntities(com.josephison.fiatlux.registry.ModEntities.MORTAL.get(), m -> faith.id.equals(m.religion()) && !m.isChild())) {
            int k = m.profession() == Profession.SCHOLAR ? 3 : m.profession() == Profession.PRIEST ? 2 : m.isElder() ? 1 : 0;
            if (k > rank) {
                rank = k;
                best = m;
            }
        }
        return best;
    }

    private static void write(ServerLevel level, ProphecyData d, Book book, MortalEntity scribe, Planet p) {
        d.books.add(book);
        d.setDirty();
        scribe.say("I have written it down, that it be not forgotten.");
        CosmosData.get(level.getServer()).record(level.getServer(), scribe.getName().getString(), "scripture",
                "of " + p.name + " wrote " + book.title + ", " + book.length() + (book.length() == 1 ? " verse" : " verses") + ".");
    }

    /** "In the beginning..." - the story of the world up to the waters, from the annals. */
    private static Book beginnings(MinecraftServer server, ProphecyData d, Religion faith, Planet p, MortalEntity scribe) {
        String god = faith.deity.true_ ? faith.deityName : faith.deityName;
        Book b = new Book(faith.deity.true_ ? "The Book of Beginnings" : "The Beginnings by " + faith.deityName, Book.Kind.BEGINNINGS,
                scribe.getName().getString(), scribe.level().getGameTime(), faith.id);
        b.chapter("The Creation");
        if (faith.deity.true_) {
            b.verse("In the beginning " + god + " created the heavens and " + p.name + ".");
            b.verse("And " + p.name + " was without form, and void; and darkness was upon the face of the deep.");
        } else {
            b.verse("In the beginning was " + god + ", and " + god + " made " + p.name + " for its children.");
        }
        Map<String, List<ProphecyData.Annal>> byChapter = new LinkedHashMap<>();
        for (ProphecyData.Annal a : d.annals) {
            if (!concerns(server, a, p)) continue;
            for (String[] ch : BEGINNINGS) {
                for (int k = 1; k < ch.length; k++) if (ch[k].equals(a.kind())) byChapter.computeIfAbsent(ch[0], x -> new ArrayList<>()).add(a);
            }
        }
        for (String[] ch : BEGINNINGS) {
            List<ProphecyData.Annal> acts = byChapter.getOrDefault(ch[0], List.of());
            if (acts.isEmpty()) continue;
            if (!ch[0].equals("The Creation")) b.chapter(ch[0]);
            for (ProphecyData.Annal a : acts.subList(0, Math.min(40, acts.size()))) if (!b.verses.contains(verse(a))) b.verse(verse(a));
        }
        if (faith.deity.true_) b.verse("These are the generations of the heavens and of " + p.name + ", when they were created.");
        return b;
    }

    /** The commandments of the people, as its rulers have given them. */
    private static Book law(Polity polity, Religion faith, MortalEntity scribe, long now) {
        Book b = new Book("The Book of the Law", Book.Kind.LAW, scribe.getName().getString(), now, faith.id);
        b.chapter("The Commandments");
        b.verse("I am " + faith.deityName + ", which brought you forth. Thou shalt have no other gods before me.");
        for (Polity.Law l : polity.laws) b.verse(l.text + (l.source.isEmpty() ? "" : " (" + l.source + ")"));
        b.chapter("The Blessing");
        b.verse("Hear, O people of " + faith.world + ": " + faith.deityName + " is one. And thou shalt love " + faith.deityName
                + " with all thine heart, and with all thy soul, and with all thy might.");
        return b;
    }

    /** A new book of Chronicles once enough has happened since the last. */
    @Nullable
    private static Book chronicles(MinecraftServer server, ProphecyData d, Religion faith, Planet p, MortalEntity scribe, long now) {
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        long since = faith.founded;
        int count = 0;
        for (Book b : d.booksOf(faith.id)) {
            if (b.kind == Book.Kind.CHRONICLES || b.kind == Book.Kind.BEGINNINGS) since = Math.max(since, b.written);
            if (b.kind == Book.Kind.CHRONICLES) count++;
        }
        if (now - since < 4 * year) return null;
        List<ProphecyData.Annal> acts = new ArrayList<>();
        for (ProphecyData.Annal a : d.annals) if (a.time() > since && HISTORY.contains(a.kind()) && concerns(server, a, p)) acts.add(a);
        if (acts.size() < 5) return null;
        String[] ordinals = {"First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth"};
        String title = count < ordinals.length ? "The " + ordinals[count] + " Book of Chronicles" : "Chronicles " + (count + 1);
        Book b = new Book(title, Book.Kind.CHRONICLES, scribe.getName().getString(), now, faith.id);
        String chapter = "";
        for (ProphecyData.Annal a : acts.subList(0, Math.min(60, acts.size()))) {
            String heading = switch (a.kind()) {
                case "town", "exodus" -> "Of the Towns";
                case "government", "justice", "underworld_revolution" -> "Of the Rulers and the Law";
                case "faith_founded", "schism", "faith_split", "faith_leader", "scandal" -> "Of the Faiths";
                case "rift", "possessed", "exorcism", "anointed", "watch", "deliverance", "redemption" -> "Of Spirits and Angels";
                case "carmel", "spared", "fulfilled", "taken_up", "martyr", "averted" -> "Of the Prophets";
                default -> "Of the Ages";
            };
            if (!heading.equals(chapter)) {
                b.chapter(heading);
                chapter = heading;
            }
            if (!b.verses.contains(verse(a))) b.verse(verse(a));
        }
        return b;
    }

    // ================================================================== songs and sayings

    private static final String[] PRAISE = {
            "The heavens declare the glory of %s; and the firmament sheweth his handywork.",
            "O %s, our Lord, how excellent is thy name in all the earth!",
            "Make a joyful noise unto %s, all ye lands. Serve %s with gladness: come before his presence with singing.",
            "%s is my shepherd; I shall not want. He maketh me to lie down in green pastures.",
            "Praise ye %s. Praise %s in the firmament of his power.",
            "I will lift up mine eyes unto the hills, from whence cometh my help. My help cometh from %s, which made heaven and earth.",
            "Bless %s, O my soul: and all that is within me, bless his holy name.",
            "Out of the depths have I cried unto thee, O %s. %s, hear my voice."};
    private static final String[] CLOSING = {
            "Let every thing that hath breath praise %s.", "Surely goodness and mercy shall follow me all the days of my life.",
            "For his mercy endureth for ever.", "Wait on %s: be of good courage, and he shall strengthen thine heart.",
            "Weeping may endure for a night, but joy cometh in the morning.", "Be still, and know that %s is God."};
    private static final String[] PROVERBS = {
            "A soft answer turneth away wrath: but grievous words stir up anger.", "Pride goeth before destruction, and an haughty spirit before a fall.",
            "Train up a child in the way he should go: and when he is old, he will not depart from it.",
            "A merry heart doeth good like a medicine: but a broken spirit drieth the bones.", "Iron sharpeneth iron; so a man sharpeneth the countenance of his friend.",
            "Go to the ant, thou sluggard; consider her ways, and be wise.", "Better is a dinner of herbs where love is, than a stalled ox and hatred therewith.",
            "He that walketh with wise men shall be wise: but a companion of fools shall be destroyed.", "A good name is rather to be chosen than great riches.",
            "Where there is no vision, the people perish.", "The fear of %s is the beginning of knowledge.",
            "Trust in %s with all thine heart; and lean not unto thine own understanding.", "As a dog returneth to his vomit, so a fool returneth to his folly.",
            "Hope deferred maketh the heart sick: but when the desire cometh, it is a tree of life.", "Even a child is known by his doings.",
            "The hoary head is a crown of glory, if it be found in the way of righteousness.", "Open rebuke is better than secret love.",
            "A friend loveth at all times, and a brother is born for adversity.", "Boast not thyself of to morrow; for thou knowest not what a day may bring forth.",
            "Whoso diggeth a pit shall fall therein.", "The %s that rises early and tends the field shall have bread enough.",
            "Who can find a virtuous woman? for her price is far above rubies."};
    private static final String[] HYMN = {
            "Great is %s, who sees all and gives all!", "We bring our offerings to %s, and %s is pleased.", "Hail, %s! Thy children call upon thee.",
            "None stands before %s; let the earth tremble.", "Glory to %s, in the morning and in the night."};

    /** One of the faithful sings a new psalm, and it is added to the Psalms (or, for other faiths, their hymns). */
    public static void psalm(ServerLevel level, ProphecyData d, Religion faith, MortalEntity singer, Planet p) {
        Book psalms = d.book(faith.id, Book.Kind.PSALMS);
        if (psalms == null) {
            psalms = new Book(faith.deity.true_ ? "The Psalms" : "Hymns to " + faith.deityName, Book.Kind.PSALMS, singer.getName().getString(),
                    level.getGameTime(), faith.id);
            d.books.add(psalms);
        }
        int n = (int) psalms.verses.stream().filter(v -> v.startsWith(Book.CHAPTER)).count();
        if (n >= 150) return;
        RandomSource r = level.random;
        String g = faith.deityName;
        psalms.chapter((faith.deity.true_ ? "A Psalm of " : "A Hymn of ") + singer.getName().getString());
        if (faith.deity.true_) {
            psalms.verse(String.format(PRAISE[r.nextInt(PRAISE.length)], g, g));
            String deed = deedOf(level.getServer(), d, p, g);
            if (deed != null) psalms.verse(deed);
            psalms.verse(String.format(CLOSING[r.nextInt(CLOSING.length)], g));
        } else {
            psalms.verse(String.format(HYMN[r.nextInt(HYMN.length)], g, g));
            psalms.verse(String.format(HYMN[r.nextInt(HYMN.length)], g, g));
        }
        d.setDirty();
        singer.say(psalms.verses.get(psalms.verses.size() - 2));
    }

    /** A line remembering something the god did on this world, if the annals tell of it. */
    @Nullable
    private static String deedOf(MinecraftServer server, ProphecyData d, Planet p, String g) {
        List<String> lines = new ArrayList<>();
        for (ProphecyData.Annal a : d.annals) {
            if (!concerns(server, a, p)) continue;
            switch (a.kind()) {
                case "covenant" -> lines.add("Thou didst set thy bow in the cloud, O " + g + ", and the waters returned no more to destroy all flesh.");
                case "exile" -> lines.add("Thou didst send us forth from the garden, yet thou didst clothe us with coats of skins.");
                case "flood", "ark" -> lines.add("When the waters came in unto my soul, thou didst bear us up upon the face of the deep.");
                case "bound" -> lines.add("The mighty ones of old thou didst bind in the dark places of the earth.");
                case "answered" -> lines.add("I cried unto " + g + " with my voice, and he heard me out of his holy hill.");
                case "exorcism" -> lines.add("Thou hast loosed the bound, and cast out the unclean spirit from among us.");
                case "carmel" -> lines.add("Fire fell from heaven upon the altar, and all the people said, " + g + ", he is the God.");
                case "life" -> lines.add("Thou didst breathe into our nostrils the breath of life, and we became living souls.");
                default -> {
                }
            }
        }
        return lines.isEmpty() ? null : lines.get(new java.util.Random(lines.size() * 31L + p.name.hashCode() + d.annals.size()).nextInt(lines.size()));
    }

    /** An elder's saying, added to the Proverbs (or the sayings of a false faith). */
    public static void proverb(ServerLevel level, ProphecyData d, Religion faith, MortalEntity elder) {
        Book book = d.book(faith.id, Book.Kind.PROVERBS);
        if (book == null) {
            book = new Book(faith.deity.true_ ? "The Proverbs" : "The Sayings of " + faith.founder, Book.Kind.PROVERBS, elder.getName().getString(),
                    level.getGameTime(), faith.id);
            book.chapter("The sayings of the wise of " + faith.world);
            d.books.add(book);
        }
        if (book.length() >= 100) return;
        List<String> unused = new ArrayList<>();
        for (String s : PROVERBS) {
            String line = String.format(s, s.contains("The %s that") ? elder.profession().id : faith.deityName);
            if (!book.verses.contains(line)) unused.add(line);
        }
        if (unused.isEmpty()) return;
        String line = unused.get(level.random.nextInt(unused.size()));
        book.verse(line);
        d.setDirty();
        elder.say(line);
    }

    /** When a branch splits from a faith, its founder writes down why. */
    public static void testament(ServerLevel level, Religion branch, Religion parent) {
        ProphecyData d = ProphecyData.get(level.getServer());
        Book b = new Book("The Testament of " + branch.founder, Book.Kind.TESTAMENT, branch.founder, level.getGameTime(), branch.id);
        b.chapter("Why we went out");
        b.verse("I, " + branch.founder + ", went out from " + parent.name + " " + branch.doctrine + ".");
        b.verse("Prove all things; hold fast that which is good.");
        b.verse("We keep the faith of " + branch.deityName + " as it was first given, and the books of our fathers with it.");
        d.books.add(b);
        d.setDirty();
    }

    /** The prophet is gone: a disciple (or a scribe) writes down their words and deeds. */
    public static Book prophetBook(ServerLevel level, Prophet p, String scribe) {
        ProphecyData d = ProphecyData.get(level.getServer());
        Book b = new Book((p.false_ ? "The Visions of " : "The Book of ") + p.name, Book.Kind.PROPHET, scribe, level.getGameTime(), p.faith);
        b.chapter("The Calling");
        b.verse("The words of " + p.name + ", to whom the word of " + p.calledBy + " came in the days of " + p.world + ".");
        if (!p.oracles.isEmpty()) {
            b.chapter("The Oracles");
            p.oracles.forEach(b::verse);
        }
        if (!p.deeds.isEmpty()) {
            b.chapter("The Acts of " + p.name);
            p.deeds.forEach(b::verse);
        }
        b.chapter("The End of " + p.name);
        b.verse(switch (p.fate) {
            case "taken up" -> "And it came to pass, as they still went on, and talked, that, behold, there appeared a chariot of fire, and "
                    + p.name + " went up by a whirlwind into heaven.";
            case "martyred" -> "And they slew " + p.name + " that spake in the name of " + p.calledBy + ". Precious in the sight of "
                    + p.calledBy + " is the death of his saints.";
            default -> "And " + p.name + " died, and was gathered unto " + p.his() + " people.";
        });
        d.books.add(b);
        d.setDirty();
        return b;
    }

    // ================================================================== reading them

    /** All the books a faith holds holy: its own, and those of the faiths it came from, written before it parted. */
    public static List<Book> canon(MinecraftServer server, Religion faith) {
        ProphecyData d = ProphecyData.get(server);
        Generations gens = Generations.get(server);
        List<Book> out = new ArrayList<>();
        long until = Long.MAX_VALUE;
        Religion r = faith;
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        while (r != null && seen.add(r.id)) {
            for (Book b : d.booksOf(r.id)) if (b.written <= until) out.add(b);
            until = Math.min(until, r.founded);
            r = gens.religion(r.parent);
        }
        out.sort((a, b) -> Integer.compare(order(a.kind), order(b.kind)) != 0 ? Integer.compare(order(a.kind), order(b.kind)) : Long.compare(a.written, b.written));
        return out;
    }

    private static int order(Book.Kind k) {
        return switch (k) {
            case BEGINNINGS -> 0;
            case LAW -> 1;
            case CHRONICLES -> 2;
            case PSALMS -> 3;
            case PROVERBS -> 4;
            case PROPHET -> 5;
            case TESTAMENT -> 6;
            case GOSPEL -> 7;
        };
    }

    /** A verse a priest might read aloud, with its reference ("Psalms 3:2"), or null if they have no books. */
    @Nullable
    public static String quote(MinecraftServer server, Religion faith, RandomSource r) {
        List<Book> canon = canon(server, faith);
        if (canon.isEmpty()) return null;
        Book b = canon.get(r.nextInt(canon.size()));
        List<Integer> verses = new ArrayList<>();
        for (int i = 0; i < b.verses.size(); i++) if (!b.verses.get(i).startsWith(Book.CHAPTER) && b.verses.get(i).length() < 150) verses.add(i);
        if (verses.isEmpty()) return null;
        int i = verses.get(r.nextInt(verses.size()));
        String name = b.title.startsWith("The Book of ") ? b.title.substring(12) : b.title.startsWith("The ") ? b.title.substring(4) : b.title;
        return b.verses.get(i) + " (" + name + " " + b.reference(i) + ")";
    }

    // ------------------------------------------------------------------ as written books

    private static final int LINE_WIDTH = 112, LINES = 14, MAX_PAGES = 100;

    /** One book of scripture as a written book that can be held and read. */
    public static ItemStack item(Book b, String faithName) {
        Pages pages = new Pages();
        pages.title(b.title, "written by " + b.scribe, faithName);
        int chapter = 0, verse = 0;
        boolean opened = false;
        for (String v : b.verses) {
            if (v.startsWith(Book.CHAPTER)) {
                if (opened) chapter++;
                else chapter = 1;
                opened = true;
                verse = 0;
                String heading = v.substring(Book.CHAPTER.length());
                pages.heading((heading.isEmpty() ? "Chapter " + chapter : heading));
            } else {
                if (!opened) {
                    chapter = 1;
                    opened = true;
                }
                verse++;
                pages.verse(verse, v);
            }
        }
        return stack(b.title, faithName, pages.done());
    }

    /** All a faith's books bound together in one volume, as far as a book can hold. */
    public static ItemStack volume(List<Book> canon, Religion faith) {
        Pages pages = new Pages();
        pages.title(faith.deity.true_ ? "The Holy Writ" : "The Writings", "of " + faith.name, canon.size() + (canon.size() == 1 ? " book" : " books"));
        for (Book b : canon) {
            pages.newPage();
            pages.heading(b.title);
            int verse = 0;
            for (String v : b.verses) {
                if (v.startsWith(Book.CHAPTER)) {
                    String heading = v.substring(Book.CHAPTER.length());
                    if (!heading.isEmpty()) pages.subheading(heading);
                    verse = 0;
                } else {
                    pages.verse(++verse, v);
                }
            }
        }
        return stack(faith.deity.true_ ? "The Holy Writ" : "The Writings of " + faith.deityName, faith.name, pages.done());
    }

    private static ItemStack stack(String title, String author, List<Component> pages) {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> ps = new ArrayList<>();
        for (Component c : pages) ps.add(Filterable.passThrough(c));
        String t = title.length() > 32 ? title.substring(0, 31) + "." : title;
        s.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(t), author, 0, ps, true));
        return s;
    }

    /** Lays text out on book pages of 14 lines, about 112 pixels wide. */
    private static final class Pages {
        final List<Component> pages = new ArrayList<>();
        MutableComponent page = Component.empty();
        int lines;

        void newPage() {
            if (lines > 0) {
                pages.add(page);
                page = Component.empty();
                lines = 0;
            }
        }

        void line(Component c, int count) {
            if (pages.size() >= MAX_PAGES) return;
            if (lines + count > LINES) newPage();
            if (pages.size() >= MAX_PAGES) return;
            if (lines > 0) page.append("\n");
            page.append(c);
            lines += count;
        }

        void title(String title, String by, String of) {
            line(Component.empty(), 2);
            for (String l : wrap(title, true)) line(Component.literal(centre(l, true)).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED), 1);
            line(Component.empty(), 1);
            for (String l : wrap(by, false)) line(Component.literal(centre(l, false)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY), 1);
            for (String l : wrap(of, false)) line(Component.literal(centre(l, false)).withStyle(ChatFormatting.DARK_GRAY), 1);
            newPage();
        }

        void heading(String h) {
            if (lines > LINES - 3) newPage();
            for (String l : wrap(h, true)) line(Component.literal(l).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED), 1);
        }

        void subheading(String h) {
            if (lines > LINES - 3) newPage();
            for (String l : wrap(h, false)) line(Component.literal(l).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GREEN), 1);
        }

        void verse(int n, String text) {
            List<String> ls = wrap(n + " " + text, false);
            for (int i = 0; i < ls.size(); i++) {
                String l = ls.get(i);
                if (i == 0) {
                    String num = String.valueOf(n);
                    line(Component.literal(num).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                            .append(Component.literal(l.substring(num.length())).withStyle(Style.EMPTY.withColor(ChatFormatting.BLACK))), 1);
                } else {
                    line(Component.literal(l).withStyle(ChatFormatting.BLACK), 1);
                }
            }
        }

        List<Component> done() {
            if (lines > 0 && pages.size() < MAX_PAGES) pages.add(page);
            if (pages.isEmpty()) pages.add(Component.empty());
            return pages;
        }
    }

    private static int width(char c, boolean bold) {
        int w = switch (c) {
            case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', '[', ']', ' ', '"', '(', ')' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
        return w + (bold && c != ' ' ? 1 : 0);
    }

    static List<String> wrap(String text, boolean bold) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int w = 0;
        for (String word : text.split(" ")) {
            int ww = 0;
            for (char c : word.toCharArray()) ww += width(c, bold);
            int space = line.isEmpty() ? 0 : width(' ', bold);
            if (w + space + ww > LINE_WIDTH && !line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
                w = 0;
                space = 0;
            }
            if (space > 0) line.append(' ');
            line.append(word);
            w += space + ww;
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private static String centre(String l, boolean bold) {
        int w = 0;
        for (char c : l.toCharArray()) w += width(c, bold);
        int pad = Math.max(0, (LINE_WIDTH - w) / 2 / width(' ', false));
        return " ".repeat(pad) + l;
    }

    // ------------------------------------------------------------------ lecterns

    /** Where the writings lie open at the shrine (beside the altar, toward the people). */
    public static BlockPos lecternAt(Settlement s) {
        return s.shrine.offset(1, 0, 1);
    }

    private static final Map<BlockPos, Integer> LAID = new HashMap<>();

    /** At each town's shrine, the writings of the faith most of its people hold lie open on a lectern. */
    private static void lecterns(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p, ProphecyData d) {
        Generations gens = Generations.get(level.getServer());
        for (Settlement s : cosmos.townsOf(p.id)) {
            if (!level.isLoaded(s.shrine)) continue;
            Map<UUID, Integer> votes = new HashMap<>();
            for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(48), m -> m.religion() != null)) {
                votes.merge(m.religion(), 1, Integer::sum);
            }
            Religion faith = votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> gens.religion(e.getKey())).orElse(null);
            if (faith == null) continue;
            List<Book> canon = canon(level.getServer(), faith);
            if (canon.isEmpty()) continue;
            int version = faith.id.hashCode() * 31 + canon.stream().mapToInt(b -> b.verses.size()).sum();
            BlockPos at = lecternAt(s);
            BlockState state = level.getBlockState(at);
            if (LAID.getOrDefault(at, 0) == version && state.is(Blocks.LECTERN)) continue;
            if (!state.is(Blocks.LECTERN)) {
                if (!state.canBeReplaced() && !state.isAir()) continue;
                level.setBlock(at, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH), 3);
                state = level.getBlockState(at);
            }
            if (level.getBlockEntity(at) instanceof LecternBlockEntity lectern) {
                if (state.getValue(LecternBlock.HAS_BOOK)) {
                    lectern.clearContent();
                    LecternBlock.resetBookState(null, level, at, state, false);
                    state = level.getBlockState(at);
                }
                LecternBlock.tryPlaceBook(null, level, at, state, volume(canon, faith));
            }
            LAID.put(at, version);
        }
    }

    /** A priest at prayer may read aloud from the writings; one of the faithful may sing a new psalm. */
    public static void onPrayer(ServerLevel level, MortalEntity m, Religion faith) {
        RandomSource r = level.random;
        if (m.profession() == Profession.PRIEST && r.nextInt(3) == 0) {
            String q = quote(level.getServer(), faith, r);
            if (q != null) m.say(q);
        } else if (m.hasTrait(com.josephison.fiatlux.civilization.Trait.PIOUS) && r.nextInt(14) == 0) {
            Planet p = m.planet();
            if (p != null && p.era.ordinal() >= MortalEra.AGRARIAN.ordinal()) psalm(level, ProphecyData.get(level.getServer()), faith, m, p);
        }
    }
}
