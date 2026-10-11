package com.josephison.fiatlux.infernal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The war for the souls of the living, kept for the whole world: whether the Creator has sent the
 * angels out to watch over the worlds (and who), and the tally of rifts opened, of the possessed, of
 * those set free, and of the spirits cast into the deep.
 */
public final class InfernalData extends SavedData {
    private static final String ID = "fiatlux_infernal";

    /** "He shall give his angels charge over thee" (Psalm 91:11): the guardians have been sent. */
    public boolean watch;
    public String watchedBy = "";
    public int rifts, sealed, possessions, exorcisms, anointed, swine, castIntoTheDeep, returned;

    public static InfernalData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(InfernalData::new, InfernalData::load, null), ID);
    }

    private static InfernalData load(CompoundTag t, HolderLookup.Provider registries) {
        InfernalData d = new InfernalData();
        d.watch = t.getBoolean("watch");
        d.watchedBy = t.getString("watched_by");
        d.rifts = t.getInt("rifts");
        d.sealed = t.getInt("sealed");
        d.possessions = t.getInt("possessions");
        d.exorcisms = t.getInt("exorcisms");
        d.anointed = t.getInt("anointed");
        d.swine = t.getInt("swine");
        d.castIntoTheDeep = t.getInt("cast_into_the_deep");
        d.returned = t.getInt("returned");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider registries) {
        t.putBoolean("watch", watch);
        t.putString("watched_by", watchedBy);
        t.putInt("rifts", rifts);
        t.putInt("sealed", sealed);
        t.putInt("possessions", possessions);
        t.putInt("exorcisms", exorcisms);
        t.putInt("anointed", anointed);
        t.putInt("swine", swine);
        t.putInt("cast_into_the_deep", castIntoTheDeep);
        t.putInt("returned", returned);
        return t;
    }
}
