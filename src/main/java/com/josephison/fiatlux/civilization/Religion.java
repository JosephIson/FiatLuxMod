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
        FALLEN("fallen", 0xFFB02020, false);

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
        return t;
    }

    public static Religion load(CompoundTag t) {
        return new Religion(t.getUUID("id"), t.getString("name"), Deity.byId(t.getString("deity")), t.getString("deity_name"),
                t.hasUUID("creator") ? t.getUUID("creator") : null, t.getString("founder"), t.getString("world"), t.getLong("founded"));
    }
}
