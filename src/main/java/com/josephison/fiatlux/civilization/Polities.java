package com.josephison.fiatlux.civilization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** The governments of every world, by world (SavedData). */
public final class Polities extends SavedData {
    private static final String ID = "fiatlux_polities";
    public final Map<UUID, Polity> byWorld = new LinkedHashMap<>();

    public static Polities get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Polities::new, Polities::load, null), ID);
    }

    @Nullable
    public Polity of(@Nullable UUID world) {
        return world == null ? null : byWorld.get(world);
    }

    @Nullable
    public Polity ruledBy(UUID person) {
        for (Polity p : byWorld.values()) if (person.equals(p.ruler)) return p;
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Polity p : byWorld.values()) list.add(p.save());
        tag.put("polities", list);
        return tag;
    }

    private static Polities load(CompoundTag tag, HolderLookup.Provider registries) {
        Polities d = new Polities();
        for (Tag t : tag.getList("polities", Tag.TAG_COMPOUND)) {
            Polity p = Polity.load((CompoundTag) t);
            d.byWorld.put(p.world, p);
        }
        return d;
    }
}
