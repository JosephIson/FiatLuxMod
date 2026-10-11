package com.josephison.fiatlux.son;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "But when the fulness of the time was come, God sent forth his Son, made of a woman" (Gal 4:4).
 * The life of the Son on the world chosen for him: who he is, where he was born and to whom, where in
 * his life the story stands, his disciples, what he said and did, and where he was laid.
 */
public final class SonData extends SavedData {
    private static final String ID = "fiatlux_son";

    public enum Stage {
        NONE, ANNUNCIATION, NATIVITY, CHILDHOOD, BAPTISM, TEMPTATION, MINISTRY, ENTRY, SUPPER, GETHSEMANE, TRIAL, CROSS, TOMB, RISEN,
        ASCENSION, PENTECOST, DONE
    }

    public Stage stage = Stage.NONE;
    /** When the present stage began (game time), and how far into it the story is (its own steps). */
    public long since;
    public int step;
    // ---- who
    public String name = "";
    public int variant;
    /** The Creator whose Son he is. */
    @Nullable public UUID creator;
    public String creatorName = "";
    public String world = "";
    public String dim = "";
    @Nullable public BlockPos town;
    public String townName = "";
    @Nullable public UUID son, mother, father;
    public String motherName = "", fatherName = "";
    public long born = -1;
    @Nullable public UUID faith, church;
    // ---- his life
    public final List<UUID> disciples = new ArrayList<>();
    public final List<String> discipleNames = new ArrayList<>();
    @Nullable public UUID betrayer, baptist;
    public String betrayerName = "", baptistName = "";
    /** What he said (parables, sermons) and what he did, as they will be written in the Gospels. */
    public final List<String> sayings = new ArrayList<>();
    public final List<String> deeds = new ArrayList<>();
    public int miracles, parables, raised, fed;
    @Nullable public BlockPos cross, tomb, capital;
    /** Whether the tomb was hewn into a hillside (rather than raised on flat ground). */
    public boolean tombHewn;
    public String capitalName = "";
    public long lastSign;
    /** His going about: the town he is in, when he came, when he last taught and last worked a sign. */
    @Nullable public BlockPos visiting;
    public long arrived, lastWord, lastWonder;
    public boolean transfigured, walkedOnWater, wine;

    public static SonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SonData::new, SonData::load, null), ID);
    }

    public boolean active() {
        return stage != Stage.NONE && stage != Stage.DONE;
    }

    public void to(Stage s, long now) {
        stage = s;
        since = now;
        step = 0;
        setDirty();
    }

    public void deed(String d) {
        if (deeds.size() < 120) deeds.add(d);
        setDirty();
    }

    public void saying(String s) {
        if (!sayings.contains(s) && sayings.size() < 120) sayings.add(s);
        setDirty();
    }

    private static SonData load(CompoundTag t, HolderLookup.Provider registries) {
        SonData d = new SonData();
        try {
            d.stage = Stage.valueOf(t.getString("stage"));
        } catch (IllegalArgumentException e) {
            d.stage = Stage.NONE;
        }
        d.since = t.getLong("since");
        d.step = t.getInt("step");
        d.name = t.getString("name");
        d.variant = t.getInt("variant");
        if (t.hasUUID("creator")) d.creator = t.getUUID("creator");
        d.creatorName = t.getString("creator_name");
        d.world = t.getString("world");
        d.dim = t.getString("dim");
        if (t.contains("town")) d.town = BlockPos.of(t.getLong("town"));
        d.townName = t.getString("town_name");
        if (t.hasUUID("son")) d.son = t.getUUID("son");
        if (t.hasUUID("mother")) d.mother = t.getUUID("mother");
        if (t.hasUUID("father")) d.father = t.getUUID("father");
        d.motherName = t.getString("mother_name");
        d.fatherName = t.getString("father_name");
        d.born = t.contains("born") ? t.getLong("born") : -1;
        if (t.hasUUID("faith")) d.faith = t.getUUID("faith");
        if (t.hasUUID("church")) d.church = t.getUUID("church");
        for (Tag x : t.getList("disciples", Tag.TAG_STRING)) {
            String[] kv = x.getAsString().split("\\|", 2);
            d.disciples.add(UUID.fromString(kv[0]));
            d.discipleNames.add(kv.length > 1 ? kv[1] : "");
        }
        if (t.hasUUID("betrayer")) d.betrayer = t.getUUID("betrayer");
        d.betrayerName = t.getString("betrayer_name");
        if (t.hasUUID("baptist")) d.baptist = t.getUUID("baptist");
        d.baptistName = t.getString("baptist_name");
        for (Tag x : t.getList("sayings", Tag.TAG_STRING)) d.sayings.add(x.getAsString());
        for (Tag x : t.getList("deeds", Tag.TAG_STRING)) d.deeds.add(x.getAsString());
        d.miracles = t.getInt("miracles");
        d.parables = t.getInt("parables");
        d.raised = t.getInt("raised");
        d.fed = t.getInt("fed");
        if (t.contains("cross")) d.cross = BlockPos.of(t.getLong("cross"));
        if (t.contains("tomb")) d.tomb = BlockPos.of(t.getLong("tomb"));
        d.tombHewn = t.getBoolean("tomb_hewn");
        if (t.contains("capital")) d.capital = BlockPos.of(t.getLong("capital"));
        d.capitalName = t.getString("capital_name");
        d.lastSign = t.getLong("last_sign");
        if (t.contains("visiting")) d.visiting = BlockPos.of(t.getLong("visiting"));
        d.arrived = t.getLong("arrived");
        d.lastWord = t.getLong("last_word");
        d.lastWonder = t.getLong("last_wonder");
        d.transfigured = t.getBoolean("transfigured");
        d.walkedOnWater = t.getBoolean("walked_on_water");
        d.wine = t.getBoolean("wine");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider registries) {
        t.putString("stage", stage.name());
        t.putLong("since", since);
        t.putInt("step", step);
        t.putString("name", name);
        t.putInt("variant", variant);
        if (creator != null) t.putUUID("creator", creator);
        t.putString("creator_name", creatorName);
        t.putString("world", world);
        t.putString("dim", dim);
        if (town != null) t.putLong("town", town.asLong());
        t.putString("town_name", townName);
        if (son != null) t.putUUID("son", son);
        if (mother != null) t.putUUID("mother", mother);
        if (father != null) t.putUUID("father", father);
        t.putString("mother_name", motherName);
        t.putString("father_name", fatherName);
        t.putLong("born", born);
        if (faith != null) t.putUUID("faith", faith);
        if (church != null) t.putUUID("church", church);
        ListTag ds = new ListTag();
        for (int i = 0; i < disciples.size(); i++) ds.add(StringTag.valueOf(disciples.get(i) + "|" + discipleNames.get(i)));
        t.put("disciples", ds);
        if (betrayer != null) t.putUUID("betrayer", betrayer);
        t.putString("betrayer_name", betrayerName);
        if (baptist != null) t.putUUID("baptist", baptist);
        t.putString("baptist_name", baptistName);
        ListTag ss = new ListTag();
        sayings.forEach(x -> ss.add(StringTag.valueOf(x)));
        t.put("sayings", ss);
        ListTag dd = new ListTag();
        deeds.forEach(x -> dd.add(StringTag.valueOf(x)));
        t.put("deeds", dd);
        t.putInt("miracles", miracles);
        t.putInt("parables", parables);
        t.putInt("raised", raised);
        t.putInt("fed", fed);
        if (cross != null) t.putLong("cross", cross.asLong());
        if (tomb != null) t.putLong("tomb", tomb.asLong());
        t.putBoolean("tomb_hewn", tombHewn);
        if (capital != null) t.putLong("capital", capital.asLong());
        t.putString("capital_name", capitalName);
        t.putLong("last_sign", lastSign);
        if (visiting != null) t.putLong("visiting", visiting.asLong());
        t.putLong("arrived", arrived);
        t.putLong("last_word", lastWord);
        t.putLong("last_wonder", lastWonder);
        t.putBoolean("transfigured", transfigured);
        t.putBoolean("walked_on_water", walkedOnWater);
        t.putBoolean("wine", wine);
        return t;
    }
}
