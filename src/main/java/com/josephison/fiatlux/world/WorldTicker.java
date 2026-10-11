package com.josephison.fiatlux.world;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.network.FxPayload;
import com.josephison.fiatlux.network.SyncCosmosPayload;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.UnderworldEra;
import com.josephison.fiatlux.world.cosmos.UnderworldState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The self-sustaining heartbeat of creation (design doc section 0). Every {@code cosmicStepInterval}
 * ticks it advances, scaled by {@code simulationSpeed}:
 * <ol>
 *   <li>planetary orbits (all planets - O(n) arithmetic),</li>
 *   <li>mortal civilisations (round-robin, at most {@code planetsPerStep} per step): logistic growth,
 *       prayer -> faith -> world Glory, sin -> corruption, death -> souls to Heaven or the Underworld,
 *       and advancement through the ages,</li>
 *   <li>the moral climate: corruption, angelic pride and loyalty, the Angelic Rebellion and the Fall,</li>
 *   <li>the Underworld: soul power, infernal population and hierarchy, soul contracts, knowledge and
 *       technological revolutions, and periodic divine cleanses,</li>
 *   <li>the cosmic clock,</li>
 * </ol>
 * and periodically syncs the cosmos to clients for the sky. Every milestone is written to the Chronicle.
 * Nothing is scripted: events arise when the simulated conditions are met (or a Creator forces them).
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class WorldTicker {
    private static long ticks;

    private WorldTicker() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ticks++;
        var c = FiatLuxConfig.COMMON;
        if (c.simulationEnabled.get() && c.simulationSpeed.get() > 0 && ticks % c.cosmicStepInterval.get() == 0) {
            // f = simulated seconds elapsed this step: rates in the config are "per second at speed 1"
            double f = c.simulationSpeed.get() * c.cosmicStepInterval.get() / 20.0;
            step(server, f);
        }
        if (ticks % c.cosmosSyncInterval.get() == 0) {
            syncAll(server);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, SyncCosmosPayload.of(CosmicExpanseSavedData.get(sp.server), sp.server));
        }
    }

    public static void syncAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(SyncCosmosPayload.of(CosmicExpanseSavedData.get(server), server));
    }

    /** One simulation step covering {@code f} simulated seconds. Public for tests and /fiatlux sim step. */
    public static void step(MinecraftServer server, double f) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        CosmosData governance = CosmosData.get(server);
        RandomSource random = server.overworld().getRandom();
        var c = FiatLuxConfig.COMMON;
        double days = f / 1200.0; // 1200 simulated seconds = one Minecraft day

        // 1. orbits
        double orbitMul = c.orbitSpeedMultiplier.get();
        for (Planet p : cosmos.planets) {
            p.phase = (p.phase + days / p.orbitPeriodDays * orbitMul) % 1.0;
        }
        if (cosmos.hasBigBang()) cosmos.cosmicAge += days;

        // 2. civilisations (bounded round-robin)
        double sinFlow = 0, faithFlow = 0;
        java.util.Map<String, Integer> census = com.josephison.fiatlux.civilization.Generations.get(server).census();
        int n = cosmos.planets.size();
        int budget = Math.min(n, c.planetsPerStep.get());
        for (int k = 0; k < budget; k++) {
            cosmos.planetCursor = (cosmos.planetCursor + 1) % Math.max(1, n);
            Planet p = cosmos.planets.get(cosmos.planetCursor);
            if (!p.hasLife()) continue;
            // a planet is visited once per ceil(n/budget) steps, so scale its share of time accordingly
            double pf = f * Math.max(1.0, (double) n / budget);
            double[] flows = stepCivilisation(server, governance, cosmos, p, pf, census.getOrDefault(p.name, 0));
            sinFlow += flows[0];
            faithFlow += flows[1];
        }

        // 3. the moral climate of creation
        UnderworldState u = cosmos.underworld;
        double infernalPressure = u.demons / (u.demons + 100_000.0);
        double target = (sinFlow + infernalPressure) / (sinFlow + faithFlow + infernalPressure + 1e-9);
        if (sinFlow + faithFlow > 0 || infernalPressure > 0) {
            cosmos.corruption += (target - cosmos.corruption) * Math.min(1.0, 0.002 * f);
        }
        cosmos.corruption = clamp01(cosmos.corruption * (1.0 - c.corruptionDecay.get() * f));
        // before the first Fall the morning star's pride grows of itself: "Thine heart was lifted up because
        // of thy beauty" (Ezek 28:17); after it, pride feeds on the corruption of creation
        double primordial = cosmos.fallenAngels <= 0 && cosmos.hasBigBang() ? c.primordialPrideRate.get() * f : 0;
        cosmos.angelicPride = clamp01(cosmos.angelicPride + primordial + c.prideGrowthRate.get() * cosmos.corruption * f
                - (primordial > 0 ? 0 : 0.0002 * (1 - cosmos.corruption) * f));
        stepRebellion(server, governance, cosmos, random, days);

        // 4. the Underworld
        stepUnderworld(server, governance, cosmos, f, days);

        com.josephison.fiatlux.realm.UnderworldSettlements.currentEra = cosmos.underworld.era;
        com.josephison.fiatlux.realm.UnderworldSettlements.dirty = false;
        cosmos.setDirty();
    }

    @SubscribeEvent
    public static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        // load the cosmos now so worldgen threads see each Underworld region's founding era and the planet slots
        CosmicExpanseSavedData.get(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        com.josephison.fiatlux.realm.UnderworldSettlements.CELL_ERAS.clear();
        com.josephison.fiatlux.realm.UnderworldSettlements.currentEra = com.josephison.fiatlux.world.cosmos.UnderworldEra.TRIBAL_PITS;
    }

    /** Mortal years in f simulated seconds. */
    public static double years(double f) {
        return f / (FiatLuxConfig.COMMON.mortalYearTicks.get() / 20.0);
    }

    /**
     * One world's people over f simulated seconds: births and deaths at the rates of their age up to
     * what that age can feed on this world, never fewer than the living mortals actually walking it
     * (the census); how many of them pray and how many have turned to sin; souls of the unseen dead;
     * and the passage of the ages once enough years have gone by and the people are numerous enough.
     */
    private static double[] stepCivilisation(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos, Planet p, double f, int census) {
        var c = FiatLuxConfig.COMMON;
        double hab = p.type.habitability * p.type.size;
        if (hab <= 0) return new double[]{0, 0};
        double years = years(f);
        double cap = Math.max(2, p.era.capacity * hab);
        double before = p.population;
        double r = (p.era.growth - p.era.mortality) * c.populationGrowthRate.get();
        p.population = Math.max(2, p.population + r * p.population * (1 - p.population / cap) * years);
        p.population = Math.max(p.population, census);
        // faith and sin are shares of the people, drifting toward what their age and the climate of creation make them
        double faithTarget = clamp01(p.era.piety * (1 - cosmos.corruption * 0.6));
        double sinTarget = clamp01((0.08 + (1 - p.era.piety) * 0.35 + cosmos.corruption * 0.5) * c.sinRate.get());
        double k = Math.min(1, years * 0.15);
        p.faith = clamp01(p.faith + (faithTarget - p.faith) * k);
        p.sin = clamp01(p.sin + (sinTarget - p.sin) * k);
        // prayer offers Glory
        cosmos.pendingGlory += Math.sqrt(p.population * p.faith) * c.gloryPerFaith.get() * years;
        if (cosmos.pendingGlory >= 1) {
            long whole = (long) cosmos.pendingGlory;
            gov.addGlory(whole);
            cosmos.pendingGlory -= whole;
        }
        // the unseen dead (the living mortals one meets are judged one by one as they die)
        double unseen = Math.max(0, before - census);
        double deaths = unseen * p.era.mortality * years;
        double righteous = p.faith / (p.faith + p.sin + 1e-9);
        cosmos.heavenSouls += deaths * righteous;
        cosmos.underworld.souls += deaths * (1 - righteous);
        // the ages of mortals
        p.eraProgress += years / c.mortalEraProgressRate.get();
        MortalEra next = p.era.next();
        if (p.era != MortalEra.SPACEFARING && p.eraProgress >= p.era.minYears && p.population >= next.minPopulation) {
            p.eraProgress = 0;
            p.era = next;
            gov.record(server, p.name, "mortal_era", "entered the " + human(p.era.getSerializedName()) + " age.");
        }
        return new double[]{p.population * p.sin, p.population * p.faith};
    }

    private static void stepRebellion(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos, RandomSource random, double days) {
        var c = FiatLuxConfig.COMMON;
        if (cosmos.rebellionActive) {
            cosmos.rebellionProgress += days;
            if (cosmos.rebellionProgress >= 1.0) {
                theFall(server, gov, cosmos);
            }
            return;
        }
        boolean firstPride = cosmos.fallenAngels <= 0 && cosmos.angelicPride >= 0.6;   // Lucifer's own pride needs no corrupt world
        if (c.rebellionEnabled.get() && (cosmos.corruption >= c.rebellionCorruptionThreshold.get() || firstPride)
                && random.nextDouble() < c.rebellionChancePerCheck.get() * cosmos.angelicPride) {
            startRebellion(server, gov, cosmos);
        }
    }

    /** "And there was war in heaven: Michael and his angels fought against the dragon" (Revelation 12:7). */
    public static void startRebellion(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos) {
        cosmos.rebellionActive = true;
        cosmos.rebellionProgress = 0;
        gov.record(server, "Heaven", "rebellion", "There was war in heaven: the proud among the Host rose against the throne.");
        globalFx(server, FxKind.GLORY_FLASH, 0xFFFF4A2A);
        RebellionWar.begin(server, cosmos);
        cosmos.setDirty();
    }

    /** "And the great dragon was cast out ... and his angels were cast out with him" (Revelation 12:9). */
    public static void theFall(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos) {
        double fallen = 100 + cosmos.angelicPride * 900;
        cosmos.fallenAngels += fallen;
        cosmos.underworld.demons += fallen;
        cosmos.angelicPride *= 0.2;
        cosmos.corruption *= 0.6;
        cosmos.rebellionActive = false;
        cosmos.rebellionProgress = 0;
        gov.record(server, "Heaven", "the_fall", "The rebels were cast out: " + (long) fallen
                + " fallen ones plunged through the sky-fracture into the Underworld.");
        globalFx(server, FxKind.CELESTIAL_PILLAR, 0xFFFF6A2A);
        RebellionWar.castOutAll(server, 6);   // any rebels still standing are struck down with them
        cosmos.setDirty();
    }

    private static void stepUnderworld(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos, double f, double days) {
        var c = FiatLuxConfig.COMMON;
        UnderworldState u = cosmos.underworld;
        double years = years(f);
        // the demons are the fallen angels themselves ("the angels which kept not their first estate", Jude 1:6):
        // they do not multiply; only a Fall adds to them, and only the chains of a cleanse take them away
        u.demons = Math.min(c.maxInfernalPopulation.get(), Math.max(0, cosmos.fallenAngels));
        // soul power is drawn from the sinner souls held below
        u.soulPower = u.souls * c.soulPowerPerSoul.get();
        // the lords of the hierarchy rise one by one as the fallen gather and the deep grows rich in souls
        u.overlords = u.demons <= 0 ? 0 : Math.min(com.josephison.fiatlux.infernal.InfernalHierarchy.LORDS,
                1 + (int) (Math.log10(1 + u.demons) * 6 + Math.log10(1 + u.soulPower) * 6) + u.era.ordinal() * 8);
        u.territories = u.overlords * 3;
        // soul contracts: bargains struck with the living
        double signed = Math.sqrt(u.souls) * 0.05 * years * (1 + u.era.ordinal());
        u.contracts += signed;
        // knowledge and technological revolutions
        u.knowledge += (u.demons * 0.05 + Math.sqrt(u.soulPower) * 2) * c.underworldTechRate.get() * years * (1 + u.overlords * 0.02);
        UnderworldEra before = u.era;
        if (u.era == UnderworldEra.TRIBAL_PITS && u.knowledge >= c.feudalThreshold.get()) u.era = UnderworldEra.FEUDAL_CASTLE;
        if (u.era == UnderworldEra.FEUDAL_CASTLE && u.knowledge >= c.industrialThreshold.get()) u.era = UnderworldEra.INDUSTRIAL;
        if (u.era == UnderworldEra.INDUSTRIAL && u.knowledge >= c.modernThreshold.get()) u.era = UnderworldEra.MODERN_CYBERNETIC;
        if (u.era != before) {
            gov.record(server, "The Underworld", "underworld_revolution", switch (u.era) {
                case FEUDAL_CASTLE -> "raised dark fortresses and walled bastions: the Feudal Era begins below.";
                case INDUSTRIAL -> "lit the first soul-engines and brimstone foundries: the Industrial Revolution of the deep.";
                case MODERN_CYBERNETIC -> "woke as neon megacities on electrical grids and digital soul-contract networks.";
                default -> "stirred.";
            });
            globalFx(server, FxKind.SHOCKWAVE, 0xFFB03A1A);
        }
        // periodic divine cleanses
        long day = server.overworld().getDayTime() / 24000L;
        if (c.divineCleansesEnabled.get() && u.demons > 500 && day - u.lastCleanseDay >= c.cleanseIntervalDays.get()) {
            divineCleanse(server, gov, cosmos, day);
        }
    }

    /** Exorcist Angels descend to keep infernal empires from overrunning creation. */
    public static void divineCleanse(MinecraftServer server, CosmosData gov, CosmicExpanseSavedData cosmos, long day) {
        UnderworldState u = cosmos.underworld;
        double s = FiatLuxConfig.COMMON.cleanseStrength.get();
        long purged = (long) (cosmos.fallenAngels * s);
        cosmos.fallenAngels *= (1 - s);   // "delivered them into chains of darkness" (2 Peter 2:4)
        u.demons = cosmos.fallenAngels;
        u.lastCleanseDay = day;
        gov.record(server, "The Host", "cleanse", "Exorcist Angels descended into the Underworld and bound " + purged + " of the fallen in chains of darkness.");
        globalFx(server, FxKind.HOST_ARRIVAL, 0xFFFFF4D6);
        cosmos.setDirty();
    }

    private static void globalFx(MinecraftServer server, FxKind kind, int color) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, new FxPayload(kind.ordinal(), p.getX(), p.getY() + 1, p.getZ(), 6f, color, 30));
        }
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static String human(String id) {
        return id.replace('_', ' ');
    }
}
