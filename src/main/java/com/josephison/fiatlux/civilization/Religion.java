package com.josephison.fiatlux.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * A faith held by mortals. The first people of a world worship the Creator who breathed life into
 * them - by that player's own name. Others arise when doubters among them, unanswered for too long,
 * turn to what they can see: the sun, the stars, their ancestors, an idol of gold (Exodus 32), or the
 * fallen ones of the deep. Faiths spread by word of mouth, split, and win or lose followers by the
 * signs they are shown.
 */
public final class Religion {
    public enum Deity {
        CREATOR("creator", 0xFFF5D77A, true),
        SUN("sun", 0xFFFFA530, false),
        STARS("stars", 0xFF9FB8FF, false),
        ANCESTORS("ancestors", 0xFFB89A7A, false),
        IDOL("idol", 0xFFE8C040, false),
        FALLEN("fallen", 0xFFB02020, false),
        /** No god at all: those who left their faith and follow reason (Psalm 14:1). */
        REASON("reason", 0xFFA0A8B0, false),
        /** The earth and its seasons, the great mother of all living things. */
        NATURE("nature", 0xFF7FB060, false),
        /** The divine ruler of an empire (Daniel 3; Revelation 13:15). */
        EMPEROR("emperor", 0xFFB060C0, false);

        public final String id;
        public final int color;
        public final boolean true_;

        Deity(String id, int color, boolean trueFaith) {
            this.id = id;
            this.color = color;
            this.true_ = trueFaith;
        }

        public static Deity byId(String id) {
            for (Deity d : values()) if (d.id.equals(id)) return d;
            return SUN;
        }

        /** The block that crowns a shrine given to this worship. */
        public Block shrineCrown() {
            return switch (this) {
                case CREATOR -> com.josephison.fiatlux.registry.ModBlocks.LUX_BLOCK.get();
                case SUN -> Blocks.SHROOMLIGHT;
                case STARS -> Blocks.SEA_LANTERN;
                case ANCESTORS -> Blocks.SKELETON_SKULL;
                case IDOL -> Blocks.GOLD_BLOCK;
                case FALLEN -> Blocks.CRYING_OBSIDIAN;
                case REASON -> Blocks.LODESTONE;
                case NATURE -> Blocks.MOSS_BLOCK;
                case EMPEROR -> Blocks.AMETHYST_BLOCK;
            };
        }
    }

    public final UUID id;
    public final String name;
    public final Deity deity;
    /** Whom they call upon: the Creator player's name, or the name they give the sun, the idol... */
    public final String deityName;
    /** For the true faith: the Creator player they worship. */
    public final UUID creator;
    public final String founder;
    public final String world;
    public final long founded;
    /** The faith this one split from, if it is a branch of another (a denomination), and over what. */
    @org.jetbrains.annotations.Nullable public UUID parent;
    public String doctrine = "";
    /** Who leads it now, and how many have led it. */
    @org.jetbrains.annotations.Nullable public UUID leader;
    public String leaderName = "";
    public boolean leaderMale = true;
    public int leaders;
    /** When it lost its last follower (-1 while it lives). */
    public long extinct = -1;

    public Religion(UUID id, String name, Deity deity, String deityName, UUID creator, String founder, String world, long founded) {
        this.id = id;
        this.name = name;
        this.deity = deity;
        this.deityName = deityName;
        this.creator = creator;
        this.founder = founder;
        this.world = world;
        this.founded = founded;
    }

    /** The title of the one who leads it, for the age of its people (e.g. "High Priest", "Patriarch", "Archbishop"). */
    public String leaderTitle(com.josephison.fiatlux.world.cosmos.MortalEra era) {
        int age = era.ordinal();
        return switch (deity) {
            case CREATOR -> age == 0 ? (leaderMale ? "Elder" : "Elder") : age <= 2 ? (leaderMale ? "High Priest" : "High Priestess")
                    : age == 3 ? (leaderMale ? "Patriarch" : "Matriarch") : age == 4 ? "Archbishop" : "Primate";
            case SUN -> leaderMale ? "Sun-Priest" : "Sun-Priestess";
            case STARS -> "Star-Seer";
            case ANCESTORS -> "Keeper of the Fathers";
            case IDOL -> leaderMale ? "Priest of " + deityName : "Priestess of " + deityName;
            case FALLEN -> "Hierophant";
            case REASON -> "Master";
            case NATURE -> leaderMale ? "Druid" : "Druidess";
            case EMPEROR -> "Pontifex";
        };
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putString("deity", deity.id);
        t.putString("deity_name", deityName);
        if (creator != null) t.putUUID("creator", creator);
        t.putString("founder", founder);
        t.putString("world", world);
        t.putLong("founded", founded);
        if (parent != null) t.putUUID("parent", parent);
        t.putString("doctrine", doctrine);
        if (leader != null) t.putUUID("leader", leader);
        t.putString("leader_name", leaderName);
        t.putBoolean("leader_male", leaderMale);
        t.putInt("leaders", leaders);
        t.putLong("extinct", extinct);
        return t;
    }

    public static Religion load(CompoundTag t) {
        Religion r = new Religion(t.getUUID("id"), t.getString("name"), Deity.byId(t.getString("deity")), t.getString("deity_name"),
                t.hasUUID("creator") ? t.getUUID("creator") : null, t.getString("founder"), t.getString("world"), t.getLong("founded"));
        r.parent = t.hasUUID("parent") ? t.getUUID("parent") : null;
        r.doctrine = t.getString("doctrine");
        r.leader = t.hasUUID("leader") ? t.getUUID("leader") : null;
        r.leaderName = t.getString("leader_name");
        r.leaderMale = !t.contains("leader_male") || t.getBoolean("leader_male");
        r.leaders = t.getInt("leaders");
        r.extinct = t.contains("extinct") ? t.getLong("extinct") : -1;
        return r;
    }
}
