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
