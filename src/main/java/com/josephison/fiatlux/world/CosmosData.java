package com.josephison.fiatlux.world;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.creation.ChronicleEntry;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * World-wide state of the creation, stored once on the overworld: the active play mode, the
 * world's accumulated Glory, and the Chronicle - the timeline of every great act.
 */
public class CosmosData extends SavedData {
    private static final String NAME = FiatLux.MODID + "_cosmos";
    private static final Codec<List<ChronicleEntry>> CHRONICLE_CODEC = ChronicleEntry.CODEC.listOf();

    private PlayMode playMode;
    private long glory;
    private final List<ChronicleEntry> chronicle = new ArrayList<>();

    public CosmosData() {
        this.playMode = FiatLuxConfig.COMMON.defaultPlayMode.get();
    }

    public static CosmosData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CosmosData::new, CosmosData::load), NAME);
    }

    public static CosmosData load(CompoundTag tag, HolderLookup.Provider registries) {
        CosmosData d = new CosmosData();
        PlayMode.CODEC.parse(NbtOps.INSTANCE, tag.get("play_mode")).result().ifPresent(m -> d.playMode = m);
        d.glory = tag.getLong("glory");
        if (tag.contains("chronicle", Tag.TAG_LIST)) {
            CHRONICLE_CODEC.parse(NbtOps.INSTANCE, tag.get("chronicle")).result().ifPresent(d.chronicle::addAll);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        PlayMode.CODEC.encodeStart(NbtOps.INSTANCE, playMode).result().ifPresent(t -> tag.put("play_mode", t));
        tag.putLong("glory", glory);
        CHRONICLE_CODEC.encodeStart(NbtOps.INSTANCE, chronicle).result().ifPresent(t -> tag.put("chronicle", t));
        return tag;
    }

    public PlayMode playMode() {
        return playMode;
    }

    public void setPlayMode(PlayMode mode) {
        this.playMode = mode;
        setDirty();
    }

    public long glory() {
        return glory;
    }

    /** Ascension tier reached by the world's Glory (tier 0 is always available). */
    public int gloryTier() {
        return (int) Math.min(10, glory / Math.max(1, FiatLuxConfig.COMMON.gloryPerTier.get()));
    }

    public void addGlory(long amount) {
        glory = Math.max(0, glory + amount);
        setDirty();
    }

    public List<ChronicleEntry> chronicle() {
        return Collections.unmodifiableList(chronicle);
    }

    /** Records an act in the Chronicle, trimming the oldest entries beyond the configured length. */
    public void record(MinecraftServer server, String actor, String kind, String text) {
        chronicle.add(new ChronicleEntry(server.overworld().getGameTime(), actor, kind, text));
        com.josephison.fiatlux.prophecy.Scripture.annal(server, server.overworld().getGameTime(), actor, kind, text);
        int max = FiatLuxConfig.COMMON.chronicleLength.get();
        while (chronicle.size() > max) {
            chronicle.removeFirst();
        }
        setDirty();
        if (FiatLuxConfig.SERVER.announceChronicle.get()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("chat.fiatlux.chronicle", actor, text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true)),
                    false);
        }
    }
}
