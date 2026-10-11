package com.josephison.fiatlux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every tunable in Fiat Lux. COMMON holds simulation and gameplay math, CLIENT holds visuals,
 * SERVER holds per-world multiplayer rules. All values are live-reloadable and appear in the
 * in-game configuration screen (Mods > Fiat Lux > Config).
 */
public final class FiatLuxConfig {
    private FiatLuxConfig() {}

    public static final class Common {
        public final ModConfigSpec.EnumValue<PlayMode> defaultPlayMode;
        public final ModConfigSpec.DoubleValue maxEssence;
        public final ModConfigSpec.DoubleValue essenceRegenPerSecond;
        public final ModConfigSpec.DoubleValue unboundCostMultiplier;
        public final ModConfigSpec.DoubleValue ascensionCostMultiplier;
        public final ModConfigSpec.IntValue gloryPerTier;
        public final ModConfigSpec.IntValue editBlocksPerTick;
        public final ModConfigSpec.IntValue maxEditVolume;
        public final ModConfigSpec.IntValue undoHistory;
        public final ModConfigSpec.IntValue chronicleLength;
        public final ModConfigSpec.BooleanValue startInHeaven;
        public final ModConfigSpec.IntValue heavenStreetSpacing;
        public final ModConfigSpec.IntValue heavenIslandLevel;
        // simulation engine
        public final ModConfigSpec.BooleanValue simulationEnabled;
        public final ModConfigSpec.DoubleValue simulationSpeed;
        public final ModConfigSpec.IntValue cosmicStepInterval;
        public final ModConfigSpec.IntValue planetsPerStep;
        public final ModConfigSpec.IntValue cosmosSyncInterval;
        // cosmos
        public final ModConfigSpec.IntValue maxPlanets;
        public final ModConfigSpec.DoubleValue orbitSpeedMultiplier;
        public final ModConfigSpec.IntValue bigBangGalaxies;
        // civilisation & faith
        public final ModConfigSpec.DoubleValue populationGrowthRate;
        public final ModConfigSpec.IntValue mortalYearTicks;
        public final ModConfigSpec.IntValue maxMortalsPerSettlement;
        public final ModConfigSpec.IntValue maxTownsPerWorld;
        public final ModConfigSpec.IntValue edenTemptationTicks;
        public final ModConfigSpec.DoubleValue gloryPerFaith;
        public final ModConfigSpec.DoubleValue sinRate;
        public final ModConfigSpec.DoubleValue mortalEraProgressRate;
        // angelic rebellion
        public final ModConfigSpec.BooleanValue rebellionEnabled;
        public final ModConfigSpec.DoubleValue rebellionCorruptionThreshold;
        public final ModConfigSpec.DoubleValue rebellionChancePerCheck;
        public final ModConfigSpec.DoubleValue prideGrowthRate;
        public final ModConfigSpec.DoubleValue primordialPrideRate;
        public final ModConfigSpec.DoubleValue corruptionDecay;
        public final ModConfigSpec.IntValue maxEmbodiedRebels;
        // underworld
        public final ModConfigSpec.DoubleValue underworldTechRate;
        public final ModConfigSpec.DoubleValue soulPowerPerSoul;
        public final ModConfigSpec.DoubleValue feudalThreshold;
        public final ModConfigSpec.DoubleValue industrialThreshold;
        public final ModConfigSpec.DoubleValue modernThreshold;
        public final ModConfigSpec.IntValue maxInfernalPopulation;
        public final ModConfigSpec.BooleanValue divineCleansesEnabled;
        public final ModConfigSpec.IntValue cleanseIntervalDays;
        public final ModConfigSpec.DoubleValue cleanseStrength;
        public final ModConfigSpec.DoubleValue hostAggroRange;
        public final ModConfigSpec.DoubleValue swordFireWaveRange;
        public final ModConfigSpec.BooleanValue swordSlaysCorruptInstantly;

        Common(ModConfigSpec.Builder b) {
            b.comment("Divinity: Divine Essence, play modes and Glory").push("divinity");
            defaultPlayMode = b.comment("Play mode for new worlds. Change any world with /fiatlux mode.")
                    .defineEnum("defaultPlayMode", PlayMode.UNBOUND);
            maxEssence = b.comment("Maximum Divine Essence a Creator can hold.")
                    .defineInRange("maxEssence", 1000.0, 1.0, 1_000_000.0);
            essenceRegenPerSecond = b.defineInRange("essenceRegenPerSecond", 25.0, 0.0, 100_000.0);
            unboundCostMultiplier = b.comment("Essence cost multiplier in Unbound mode (0 = infinite power).")
                    .defineInRange("unboundCostMultiplier", 0.0, 0.0, 100.0);
            ascensionCostMultiplier = b.defineInRange("ascensionCostMultiplier", 1.0, 0.0, 100.0);
            gloryPerTier = b.comment("World Glory needed per Ascension tier.")
                    .defineInRange("gloryPerTier", 250, 1, 1_000_000);
            b.pop();

            b.comment("Creation engine: queued, tick-budgeted world edits").push("creation");
            editBlocksPerTick = b.comment("Blocks placed per server tick across all queued edits (protects TPS).")
                    .defineInRange("editBlocksPerTick", 4096, 64, 1_000_000);
            maxEditVolume = b.defineInRange("maxEditVolume", 2_000_000, 1, 100_000_000);
            undoHistory = b.defineInRange("undoHistory", 16, 0, 256);
            chronicleLength = b.comment("Entries kept in the world's Chronicle timeline.")
                    .defineInRange("chronicleLength", 512, 16, 100_000);
            b.pop();

            b.comment("Heaven, the radiant hub realm").push("heaven");
            startInHeaven = b.comment("Bring Creators to Heaven the first time they join a world.")
                    .define("startInHeaven", true);
            heavenStreetSpacing = b.comment("Distance between the golden streets (Rev 21:21).")
                    .defineInRange("heavenStreetSpacing", 48, 16, 512);
            heavenIslandLevel = b.defineInRange("heavenIslandLevel", 120, 32, 300);
            b.pop();

            b.comment("Self-sustaining simulation engine (WorldTicker). Work is spread over ticks under budgets.").push("simulation");
            simulationEnabled = b.define("simulationEnabled", true);
            simulationSpeed = b.comment("Multiplier on every simulated rate: 0 pauses creation, 10 is ten times faster.")
                    .defineInRange("simulationSpeed", 1.0, 0.0, 1000.0);
            cosmicStepInterval = b.comment("Server ticks between simulation steps (20 = once per second).")
                    .defineInRange("cosmicStepInterval", 20, 1, 12000);
            planetsPerStep = b.comment("Planets simulated per step (round-robin) - bounds the per-tick cost.")
                    .defineInRange("planetsPerStep", 8, 1, 4096);
            cosmosSyncInterval = b.comment("Ticks between cosmos sky syncs to clients.")
                    .defineInRange("cosmosSyncInterval", 100, 20, 12000);
            b.pop();

            b.comment("The Cosmic Expanse: the Big Bang, galaxies and orbiting worlds").push("cosmos");
            maxPlanets = b.defineInRange("maxPlanets", 64, 1, 4096);
            orbitSpeedMultiplier = b.defineInRange("orbitSpeedMultiplier", 1.0, 0.0, 1000.0);
            bigBangGalaxies = b.comment("Galaxies formed by the Big Bang.").defineInRange("bigBangGalaxies", 7, 1, 64);
            b.pop();

            b.comment("Mortal civilisation and faith. Peoples grow by births and deaths each mortal year, at the rates of their age,",
                    "up to what their age and world can feed; the living mortals you meet are counted among them.").push("civilization");
            mortalYearTicks = b.comment("Ticks in one mortal year (2400 = two minutes: a child grows up in about half an hour, a long life lasts some hours).")
                    .defineInRange("ticksPerMortalYear", 2400, 200, 1_000_000);
            populationGrowthRate = b.comment("Multiplies every age's natural growth (births minus deaths).")
                    .defineInRange("populationGrowthMultiplier", 1.0, 0.0, 100.0);
            gloryPerFaith = b.comment("Glory a world offers per mortal year, per square root of its believers.").defineInRange("gloryPerBelieverYear", 2.0, 0.0, 1000.0);
            sinRate = b.comment("Multiplies how much of a people turns to sin.").defineInRange("sinMultiplier", 1.0, 0.0, 10.0);
            mortalEraProgressRate = b.comment("Multiplies the years a people needs in each age before the next.").defineInRange("eraYearsMultiplier", 1.0, 0.01, 100.0);
            maxMortalsPerSettlement = b.comment("Most mortals that live in one town (children included); beyond it, young families leave to found another.")
                    .defineInRange("maxMortalsPerSettlement", 28, 2, 200);
            maxTownsPerWorld = b.comment("Most towns a people founds on one world.").defineInRange("maxTownsPerWorld", 10, 1, 64);
            edenTemptationTicks = b.comment("How long Adam and Eve live in innocence before the serpent comes (once Satan has fallen).")
                    .defineInRange("edenTemptationTicks", 6000, 0, 10_000_000);
            b.pop();

            b.comment("Angelic free will: pride, loyalty and rebellion").push("rebellion");
            rebellionEnabled = b.define("rebellionEnabled", true);
            rebellionCorruptionThreshold = b.comment("World corruption (0..1) above which rebellion may break out.")
                    .defineInRange("rebellionCorruptionThreshold", 0.6, 0.0, 1.0);
            rebellionChancePerCheck = b.defineInRange("rebellionChancePerCheck", 0.01, 0.0, 1.0);
            prideGrowthRate = b.defineInRange("prideGrowthRate", 0.001, 0.0, 1.0);
            primordialPrideRate = b.comment("Before the first Fall, the pride of the morning star grows on its own (Ezek 28:17), per simulated second.")
                    .defineInRange("primordialPrideRate", 0.0004, 0.0, 1.0);
            corruptionDecay = b.comment("Fraction of world corruption that fades each step.").defineInRange("corruptionDecay", 0.0005, 0.0, 1.0);
            maxEmbodiedRebels = b.comment("Most rebel angels that gather in the world when war breaks out in heaven (0 = the war stays abstract).")
                    .defineInRange("maxEmbodiedRebels", 9, 0, 64);
            b.pop();

            b.comment("The Underworld: infernal population, soul power and technological eras").push("underworld");
            underworldTechRate = b.comment("Multiplies the knowledge the Underworld gathers each mortal year (from the fallen and the soul power below).")
                    .defineInRange("knowledgeMultiplier", 1.0, 0.0, 100.0);
            soulPowerPerSoul = b.comment("Soul power each sinner soul held below yields.").defineInRange("soulPowerPerSinnerSoul", 1.0, 0.0, 1000.0);
            feudalThreshold = b.comment("Knowledge to leave the Tribal/Pits era.").defineInRange("feudalKnowledge", 2_000.0, 1.0, 1e12);
            industrialThreshold = b.defineInRange("industrialKnowledge", 60_000.0, 1.0, 1e12);
            modernThreshold = b.defineInRange("modernKnowledge", 1_500_000.0, 1.0, 1e12);
            maxInfernalPopulation = b.defineInRange("maxInfernalPopulation", 100000, 0, Integer.MAX_VALUE);
            divineCleansesEnabled = b.comment("Exorcist Angels periodically descend to keep infernal empires in check.").define("divineCleansesEnabled", true);
            cleanseIntervalDays = b.defineInRange("cleanseIntervalDays", 7, 1, 100000);
            cleanseStrength = b.comment("Fraction of infernal population and soul power purged by a cleanse.").defineInRange("cleanseStrength", 0.25, 0.0, 1.0);
            b.pop();

            b.comment("The Heavenly Host and divine weaponry").push("host");
            hostAggroRange = b.defineInRange("hostAggroRange", 24.0, 4.0, 128.0);
            swordFireWaveRange = b.defineInRange("swordFireWaveRange", 8.0, 1.0, 64.0);
            swordSlaysCorruptInstantly = b.comment("The Flaming Sword slays corrupt and demonic beings in one stroke.")
                    .define("swordSlaysCorruptInstantly", true);
            b.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.DoubleValue particleDensity;
        public final ModConfigSpec.BooleanValue screenFlashes;
        public final ModConfigSpec.DoubleValue screenFlashStrength;
        public final ModConfigSpec.BooleanValue heavenSkybox;
        public final ModConfigSpec.IntValue godRayCount;
        public final ModConfigSpec.BooleanValue heavenVignette;
        public final ModConfigSpec.BooleanValue showHud;
        public final ModConfigSpec.IntValue hudX;
        public final ModConfigSpec.IntValue hudY;
        public final ModConfigSpec.BooleanValue hostAuras;

        Client(ModConfigSpec.Builder b) {
            b.comment("Visual effects").push("fx");
            particleDensity = b.comment("Multiplier on every Fiat Lux particle burst.")
                    .defineInRange("particleDensity", 1.0, 0.0, 4.0);
            screenFlashes = b.define("screenFlashes", true);
            screenFlashStrength = b.defineInRange("screenFlashStrength", 0.85, 0.0, 1.0);
            hostAuras = b.comment("Radiance particles around the Heavenly Host (Luke 2:9).").define("hostAuras", true);
            b.pop();
            b.comment("Heaven sky").push("sky");
            heavenSkybox = b.define("heavenSkybox", true);
            godRayCount = b.defineInRange("godRayCount", 24, 0, 128);
            heavenVignette = b.define("heavenVignette", true);
            b.pop();
            b.comment("HUD").push("hud");
            showHud = b.define("showHud", true);
            hudX = b.defineInRange("hudX", 6, 0, 8192);
            hudY = b.defineInRange("hudY", 6, 0, 8192);
            b.pop();
        }
    }

    public static final class Server {
        public final ModConfigSpec.IntValue godPermissionLevel;
        public final ModConfigSpec.BooleanValue pilgrimsCanEnterHeaven;
        public final ModConfigSpec.BooleanValue announceChronicle;

        Server(ModConfigSpec.Builder b) {
            b.comment("Multiplayer rules").push("multiplayer");
            godPermissionLevel = b.comment("Permission level needed for /fiatlux administration.")
                    .defineInRange("godPermissionLevel", 2, 0, 4);
            pilgrimsCanEnterHeaven = b.define("pilgrimsCanEnterHeaven", false);
            announceChronicle = b.comment("Broadcast new Chronicle entries to chat.").define("announceChronicle", true);
            b.pop();
        }
    }

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        var c = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = c.getLeft();
        COMMON_SPEC = c.getRight();
        var cl = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = cl.getLeft();
        CLIENT_SPEC = cl.getRight();
        var s = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = s.getLeft();
        SERVER_SPEC = s.getRight();
    }
}
