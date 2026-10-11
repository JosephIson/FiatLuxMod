package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.world.cosmos.MortalEra;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The government of a people: its form (from a clan under its eldest to a federation of worlds),
 * its ruler and capital, the laws it has given itself, its state faith, and its annals.
 */
public final class Polity {
    /** How a people is governed, by the age it has reached. */
    public enum Form {
        CLAN("Clan", "Elder", "Elder", false),
        CHIEFDOM("Chiefdom", "Chief", "Chieftess", true),
        /** "In those days there was no king in Israel" (Judges 17:6): judges raised up over a faithful people. */
        JUDGES("People", "Judge", "Judge", false),
        KINGDOM("Kingdom", "King", "Queen", true),
        EMPIRE("Empire", "Emperor", "Empress", true),
        REPUBLIC("Republic", "Consul", "Consul", false),
        FEDERATION("Federation", "Chancellor", "Chancellor", false);

        public final String noun, ruler, rulerF;
        public final boolean hereditary;

        Form(String noun, String ruler, String rulerF, boolean hereditary) {
            this.noun = noun;
            this.ruler = ruler;
            this.rulerF = rulerF;
            this.hereditary = hereditary;
        }

        public String title(boolean male) {
            return male ? ruler : rulerF;
        }

        /** Whether its ruler wears a crown. */
        public boolean crowned() {
            return this == KINGDOM || this == EMPIRE || this == CHIEFDOM;
        }

        /** The form a people takes in an age, given its towns and how faithful it is. */
        public static Form of(MortalEra era, int towns, double faith, boolean trueFaith) {
            return switch (era) {
                case HUNTER_GATHERER -> CLAN;
                case AGRARIAN -> trueFaith && faith > 0.75 ? JUDGES : CHIEFDOM;
                case CLASSICAL -> towns >= 4 ? EMPIRE : KINGDOM;
                case MEDIEVAL -> towns >= 6 ? EMPIRE : KINGDOM;
                case INDUSTRIAL -> REPUBLIC;
                case SPACEFARING -> FEDERATION;
            };
        }
    }

    /** The laws a people may give itself, each against a crime. */
    public enum Law {
        STEALING(Crime.THEFT, "Thou shalt not steal.", "Exodus 20:15"),
        KILLING(Crime.MURDER, "Thou shalt not kill.", "Exodus 20:13"),
        VIOLENCE(Crime.VIOLENCE, "Eye for eye, tooth for tooth.", "Exodus 21:24"),
        IDOLS(Crime.IDOLATRY, "Thou shalt not make unto thee any graven image.", "Exodus 20:4"),
        SEDITION(Crime.SEDITION, "Fear thou the LORD and the king: and meddle not with them that are given to change.", "Proverbs 24:21"),
        /** A pagan state's law: "whoso falleth not down and worshippeth shall ... be cast" (Daniel 3:6). */
        STATE_GODS(Crime.HERESY, "All shall fall down and worship the gods of the state.", "Daniel 3:5-6");

        public final Crime crime;
        public final String text, source;

        Law(Crime crime, String text, String source) {
            this.crime = crime;
            this.text = text;
            this.source = source;
        }
    }

    public enum Crime {
        THEFT("theft"), VIOLENCE("violence"), MURDER("murder"), IDOLATRY("idolatry"), HERESY("refusing the gods of the state"), SEDITION("sedition");
        public final String word;

        Crime(String word) {
            this.word = word;
        }
    }

    public final UUID id;
    public final UUID world;
    public String name;
    public Form form;
    public String capital;
    @Nullable public UUID ruler;
    public String rulerName = "";
    public boolean rulerMale = true;
    public int reign;           // the ruler's regnal number (Cain II)
    public long rulerSince;
    public final long founded;
    public final EnumSet<Law> laws = EnumSet.noneOf(Law.class);
    @Nullable public UUID stateFaith;
    public int judged, punished;
    /** Every ruler it has had, in order (for regnal numbers and the chronicles of its kings). */
    public final List<String> rulers = new ArrayList<>();
    /** The latest events of its history, newest last. */
    public final List<String> annals = new ArrayList<>();

    public Polity(UUID id, UUID world, String name, Form form, String capital, long founded) {
        this.id = id;
        this.world = world;
        this.name = name;
        this.form = form;
        this.capital = capital;
        this.founded = founded;
    }

    public String rulerTitle() {
        return form.title(rulerMale) + " " + rulerName + (reign > 1 ? " " + roman(reign) : "");
    }

    public void annal(String line) {
        annals.add(line);
        while (annals.size() > 40) annals.removeFirst();
    }

    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putUUID("world", world);
        t.putString("name", name);
        t.putString("form", form.name());
        t.putString("capital", capital);
        if (ruler != null) t.putUUID("ruler", ruler);
        t.putString("rulerName", rulerName);
        t.putBoolean("rulerMale", rulerMale);
        t.putInt("reign", reign);
        t.putLong("rulerSince", rulerSince);
        t.putLong("founded", founded);
        ListTag laws = new ListTag();
        for (Law l : this.laws) laws.add(StringTag.valueOf(l.name()));
        t.put("laws", laws);
        if (stateFaith != null) t.putUUID("stateFaith", stateFaith);
        t.putInt("judged", judged);
        t.putInt("punished", punished);
        ListTag an = new ListTag();
        for (String a : annals) an.add(StringTag.valueOf(a));
        t.put("annals", an);
        ListTag rl = new ListTag();
        for (String r : rulers) rl.add(StringTag.valueOf(r));
        t.put("rulers", rl);
        return t;
    }

    public static Polity load(CompoundTag t) {
        Form form;
        try {
            form = Form.valueOf(t.getString("form"));
        } catch (IllegalArgumentException e) {
            form = Form.CLAN;
        }
        Polity p = new Polity(t.getUUID("id"), t.getUUID("world"), t.getString("name"), form, t.getString("capital"), t.getLong("founded"));
        if (t.hasUUID("ruler")) p.ruler = t.getUUID("ruler");
        p.rulerName = t.getString("rulerName");
        p.rulerMale = t.getBoolean("rulerMale");
        p.reign = t.getInt("reign");
        p.rulerSince = t.getLong("rulerSince");
        for (Tag l : t.getList("laws", Tag.TAG_STRING)) {
            try {
                p.laws.add(Law.valueOf(l.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (t.hasUUID("stateFaith")) p.stateFaith = t.getUUID("stateFaith");
        p.judged = t.getInt("judged");
        p.punished = t.getInt("punished");
        for (Tag a : t.getList("annals", Tag.TAG_STRING)) p.annals.add(a.getAsString());
        for (Tag a : t.getList("rulers", Tag.TAG_STRING)) p.rulers.add(a.getAsString());
        return p;
    }
}
