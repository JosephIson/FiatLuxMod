package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.creation.ChronicleEntry;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.infernal.InfernalHierarchy;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.UnderworldState;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clientbound: the state of the whole creation for the Cosmos screen (/fiatlux cosmos) - the
 * universe, every world with its living people, towns and faiths, Heaven and the moral climate, the
 * Underworld and the lords of its hierarchy, and the latest of the Chronicle.
 */
public record CosmosReport(CompoundTag data) implements CustomPacketPayload {
    public static final Type<CosmosReport> TYPE = new Type<>(FiatLux.id("cosmos_report"));
    public static final StreamCodec<ByteBuf, CosmosReport> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CosmosReport::new, CosmosReport::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** A number as people read it: 12, 3.4K, 5.6M, 7.80B. */
    public static String num(double v) {
        double a = Math.abs(v);
        if (a < 1_000) return String.valueOf((long) Math.floor(v));
        if (a < 1_000_000) return String.format("%.1fK", v / 1_000);
        if (a < 1_000_000_000) return String.format("%.1fM", v / 1_000_000);
        return String.format("%.2fB", v / 1_000_000_000);
    }

    public static CosmosReport build(MinecraftServer server) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        CosmosData gov = CosmosData.get(server);
        Generations gens = Generations.get(server);
        CompoundTag root = new CompoundTag();

        CompoundTag u = new CompoundTag();
        u.putBoolean("bigBang", c.hasBigBang());
        u.putInt("galaxies", c.galaxies.size());
        u.putDouble("age", c.cosmicAge);
        u.putInt("blackHoles", c.blackHoles.size());
        u.putInt("worlds", c.planets.size());
        u.putInt("living", (int) c.planets.stream().filter(Planet::hasLife).count());
        u.putLong("glory", gov.glory());
        u.putInt("tier", gov.gloryTier());
        u.putInt("yearSeconds", FiatLuxConfig.COMMON.mortalYearTicks.get() / 20);
        u.putDouble("speed", FiatLuxConfig.COMMON.simulationSpeed.get());
        u.putString("eden", com.josephison.fiatlux.eden.EdenEvents.describe(c));
        u.putString("flood", com.josephison.fiatlux.flood.Deluge.describe(server));
        u.putString("spirits", com.josephison.fiatlux.infernal.Infernal.describe(server));
        root.put("universe", u);

        CompoundTag m = new CompoundTag();
        m.putDouble("corruption", c.corruption);
        m.putDouble("pride", c.angelicPride);
        m.putBoolean("rebellion", c.rebellionActive);
        m.putDouble("fallen", c.fallenAngels);
        m.putDouble("heaven", c.heavenSouls);
        root.put("moral", m);

        UnderworldState us = c.underworld;
        CompoundTag uw = new CompoundTag();
        uw.putString("era", us.era.getSerializedName().replace('_', ' '));
        uw.putDouble("knowledge", us.knowledge);
        uw.putDouble("souls", us.souls);
        uw.putDouble("soulPower", us.soulPower);
        uw.putDouble("demons", us.demons);
        uw.putDouble("contracts", us.contracts);
        uw.putInt("lordsRisen", us.overlords);
        uw.putInt("lordsAll", InfernalHierarchy.LORDS);
        uw.putLong("legions", InfernalHierarchy.legions(us.overlords));
        ListTag lords = new ListTag();
        for (InfernalHierarchy.Lord l : InfernalHierarchy.risen(us.overlords)) {
            CompoundTag t = new CompoundTag();
            t.putString("title", l.title());
            t.putString("rank", l.fullRank());
            t.putInt("legions", l.legions());
            t.putString("office", l.office());
            t.putString("sin", l.sin());
            t.putString("source", l.source());
            lords.add(t);
        }
        uw.put("lords", lords);
        root.put("underworld", uw);

        // the worlds, with their living people, towns and faiths
        Map<String, Integer> census = gens.census();
        Map<UUID, Integer> followers = gens.followers();
        Map<String, Map<UUID, Integer>> faithsByWorld = new HashMap<>();
        for (Person p : gens.people.values()) {
            if (p.living() && p.religion != null) faithsByWorld.computeIfAbsent(p.world, k -> new HashMap<>()).merge(p.religion, 1, Integer::sum);
        }
        ListTag worlds = new ListTag();
        for (int i = 0; i < c.planets.size(); i++) {
            Planet p = c.planets.get(i);
            CompoundTag w = new CompoundTag();
            w.putString("name", p.name);
            w.putString("type", p.type.getSerializedName().replace('_', ' '));
            w.putInt("color", p.type.baseColor);
            w.putBoolean("life", p.hasLife());
            w.putString("era", p.era.getSerializedName().replace('_', ' '));
            w.putDouble("population", p.population);
            w.putInt("living", census.getOrDefault(p.name, 0));
            w.putDouble("faith", p.faith);
            w.putDouble("sin", p.sin);
            w.putDouble("orbit", p.orbitRadius);
            var giver = c.lifeGiverOf(p);
            w.putString("giver", giver == null ? "" : giver.getSecond());
            ServerLevel level = i < RealmPool.SIZE ? server.getLevel(RealmPool.dimension(i)) : null;
            ListTag towns = new ListTag();
            for (Settlement s : c.townsOf(p.id)) {
                CompoundTag t = new CompoundTag();
                t.putString("name", s.name.isEmpty() ? p.name : s.name);
                int people = -1;
                if (level != null && level.isLoaded(s.centre)) {
                    people = level.getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(80)).size();
                }
                t.putInt("people", people);
                t.putInt("houses", s.houses.size());
                t.putString("founders", s.founders);
                t.putInt("food", s.food);
                t.putInt("petitions", s.petitions.size());
                towns.add(t);
            }
            w.put("towns", towns);
            ListTag faiths = new ListTag();
            faithsByWorld.getOrDefault(p.name, Map.of()).entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue()).limit(6).forEach(e -> {
                        Religion r = gens.religion(e.getKey());
                        if (r == null) return;
                        CompoundTag f = new CompoundTag();
                        f.putString("name", r.name);
                        f.putString("deity", r.deityName);
                        f.putInt("followers", e.getValue());
                        f.putInt("color", r.deity.color);
                        f.putBoolean("true", r.deity.true_);
                        f.putString("leader", r.leaderName.isEmpty() ? "" : r.leaderTitle(p.era) + " " + r.leaderName);
                        Religion parent = gens.religion(r.parent);
                        f.putString("parent", parent == null ? "" : parent.name);
                        faiths.add(f);
                    });
            w.put("faiths", faiths);
            com.josephison.fiatlux.civilization.Polity g = com.josephison.fiatlux.civilization.Polities.get(server).of(p.id);
            if (g != null) {
                CompoundTag rule = new CompoundTag();
                rule.putString("name", g.name);
                rule.putString("ruler", g.rulerTitle());
                rule.putString("capital", g.capital);
                rule.putInt("judged", g.judged);
                ListTag laws = new ListTag();
                for (var law : g.laws) laws.add(StringTag.valueOf(law.text + " (" + law.source + ")"));
                rule.put("laws", laws);
                ListTag an = new ListTag();
                for (String a : g.annals.subList(Math.max(0, g.annals.size() - 5), g.annals.size())) an.add(StringTag.valueOf(a));
                rule.put("annals", an);
                w.put("government", rule);
            }
            worlds.add(w);
        }
        int earth = census.getOrDefault("the earth", 0);
        root.putInt("earthLiving", earth);
        root.put("worlds", worlds);
        root.putInt("totalLiving", census.values().stream().mapToInt(Integer::intValue).sum());
        root.putInt("totalWritten", gens.people.size());
        root.putInt("faithCount", gens.religions.size());

        ListTag chron = new ListTag();
        List<ChronicleEntry> all = gov.chronicle();
        for (ChronicleEntry e : all.subList(Math.max(0, all.size() - 14), all.size())) {
            chron.add(StringTag.valueOf("Day " + (e.gameTime() / 24000L + 1) + " - " + e.actor() + " " + e.text()));
        }
        root.put("chronicle", chron);
        return new CosmosReport(root);
    }
}
