package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.weapon.DivineWeaponRegistry;
import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.registry.ModEffects;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput out) {
        super(out, FiatLux.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.fiatlux", "Fiat Lux");

        // --- items & blocks
        add(DivineWeaponRegistry.FLAMING_SWORD.get(), "Flaming Sword of the Cherubim");
        add("item.fiatlux.flaming_sword_of_cherubim.verse", "\"...a flaming sword which turned every way\" - Genesis 3:24");
        add(ModItems.DIVINE_SCEPTER.get(), "Scepter of the Creator");
        add("item.fiatlux.divine_scepter.verse", "\"And God said, Let there be light\" - Genesis 1:3");
        add(ModItems.ANGEL_SPAWN_EGG.get(), "Angel Spawn Egg");
        add(ModItems.SERAPH_SPAWN_EGG.get(), "Seraph Spawn Egg");
        add(ModItems.CHERUB_SPAWN_EGG.get(), "Cherub Spawn Egg");
        add(ModItems.OPHAN_SPAWN_EGG.get(), "Ophan Spawn Egg");
        add(ModBlocks.LUX_BLOCK.get(), "Block of Lux");
        add(ModBlocks.GOLDEN_STREET.get(), "Golden Street");
        add(ModBlocks.JASPER.get(), "Jasper");
        add(ModBlocks.CLOUDSTONE.get(), "Cloudstone");
        add(ModBlocks.CELESTIAL_CRYSTAL.get(), "Celestial Crystal");
        add(ModBlocks.PEARL.get(), "Pearl");
        add(ModBlocks.STILLED_WATER.get(), "Stilled Water");
        add("tooltip.fiatlux.kindled", "Holy fire: %s / 5");
        add("tooltip.fiatlux.smitten", "Corrupt beings smitten: %s");
        add("tooltip.fiatlux.flaming_sword.use", "Use: wave of holy fire. Sneak-use on ground: station it as a guardian.");
        add("tooltip.fiatlux.scepter.use", "Use: speak the selected power. Sneak-use: next power. [R] power wheel, [G] cast.");

        // --- the Heavenly Host
        add(ModEntities.ANGEL.get(), "Angel");
        add(ModEntities.SERAPH.get(), "Seraph");
        add(ModEntities.CHERUB.get(), "Cherub");
        add(ModEntities.OPHAN.get(), "Ophan");
        add(ModEntities.STATIONED_SWORD.get(), "Flaming Sword (Guardian)");

        // --- effects
        add(ModEffects.BLESSED.get(), "Blessed");
        add(ModEffects.SANCTIFIED.get(), "Sanctified");
        add(ModEffects.CURSED.get(), "Cursed");
        add(ModEffects.CORRUPTED.get(), "Corrupted");
        add(ModEffects.SPIRIT_FILLED.get(), "Spirit-Filled");

        // --- powers
        power("cosmic_spark", "Cosmic Spark", "\"In the beginning God created the heaven and the earth.\" The Big Bang: galaxies are born. (Gen 1:1)");
        power("planetary_genesis", "Planetary Genesis", "Forms a new world and sets it in its orbit around the throne. (Gen 1:14-16)");
        power("breath_of_life", "Breath of Life", "\"...breathed into his nostrils the breath of life; and man became a living soul.\" (Gen 2:7)");
        power("fiat_lux", "Fiat Lux", "\"Let there be light: and there was light.\" Floods the darkness with unseen light. (Gen 1:3)");
        power("evening_and_morning", "Evening and Morning", "Brings the next dusk or dawn. \"And the evening and the morning were the first day.\" (Gen 1:5)");
        power("bless", "Bless", "\"The LORD bless thee, and keep thee.\" Blesses the creature you behold, or yourself. (Num 6:24)");
        power("raise_land", "Let the Dry Land Appear", "Raises a hill of earth from the ground or the sea. (Gen 1:9)");
        power("smite", "Fire from Heaven", "Calls down fire and lightning; the corrupt are consumed. (1 Kgs 18:38)");
        power("part_waters", "Divide the Waters", "Opens a dry path through the sea, walled by stilled water. (Ex 14:21-22)");
        power("deluge", "Windows of Heaven", "The fountains of the deep break up and the waters prevail. (Gen 7:11)");
        power("send_angel", "Send an Angel", "Sends a ministering spirit clothed in linen. (Heb 1:14; Dan 10:5)");
        power("summon_seraph", "Summon a Seraph", "A burning one with six wings, crying Holy, holy, holy. (Isa 6:2-3)");
        power("summon_cherub", "Summon a Cherub", "A living creature of four faces and four wings. (Ezek 1:5-11)");
        power("summon_ophan", "Summon an Ophan", "A wheel within a wheel, its rims full of eyes. (Ezek 1:15-18)");
        power("ascend", "Jacob's Ladder", "Ascend to Heaven, or descend again to the earth. (Gen 28:12)");

        // --- messages
        add("message.fiatlux.pilgrim", "You walk as a mortal pilgrim; divine power is not yours to wield.");
        add("message.fiatlux.locked", "%s is not yet revealed (Glory tier %s).");
        add("message.fiatlux.exhausted", "Your Divine Essence must be renewed.");
        add("message.fiatlux.heaven_closed", "The gates of Heaven are closed to pilgrims in this world.");
        add("chat.fiatlux.chronicle", "The Chronicle: %s %s");
        add("message.fiatlux.no_universe", "The void is without form: speak the Cosmic Spark first.");
        add("message.fiatlux.planet_formed", "%s is formed: a %s world, set in its orbit.");
        add("message.fiatlux.no_lifeless_world", "There is no habitable world still waiting for life.");
        add("planet_type.fiatlux.earthlike", "Earth-like");
        add("planet_type.fiatlux.volcanic", "volcanic");
        add("planet_type.fiatlux.barren_moon", "barren moon");
        add("planet_type.fiatlux.gas_giant", "gas giant");
        add("planet_type.fiatlux.crystalline", "crystalline");
        add("command.fiatlux.cosmos.header", "--- The Cosmic Expanse ---");
        add("command.fiatlux.life", "Life is breathed upon that world.");
        add("command.fiatlux.life.fail", "That world cannot receive life (unknown, uninhabitable, or already alive).");
        add("command.fiatlux.planet.bad_type", "Unknown world type: %s");
        add("command.fiatlux.planet.full", "The cosmos holds as many worlds as this server allows (maxPlanets).");
        add("command.fiatlux.cast", "Cast %s: %s");
        add("command.fiatlux.undo", "Your last act is being undone.");
        add("command.fiatlux.undo.none", "There is nothing to undo.");
        add("command.fiatlux.mode.get", "This world is in %s mode.");
        add("command.fiatlux.mode.set", "This world is now in %s mode.");
        add("command.fiatlux.glory", "World Glory is %s (tier %s).");
        add("command.fiatlux.pilgrim.on", "%s now walks as a mortal pilgrim.");
        add("command.fiatlux.pilgrim.off", "%s is restored to divinity.");
        add("command.fiatlux.chronicle.empty", "The Chronicle is yet unwritten.");
        add("command.fiatlux.chronicle.header", "--- The Chronicle ---");

        // --- HUD, screens, keys
        add("hud.fiatlux.pilgrim", "Mortal Pilgrim");
        add("hud.fiatlux.essence", "Divine Essence %s / %s");
        add("hud.fiatlux.glory", "Glory %s · Tier %s");
        add("hud.fiatlux.mode.unbound", "Unbound");
        add("hud.fiatlux.mode.ascension", "Ascension");
        add("hud.fiatlux.mode.mortal_pilgrim", "Mortal Pilgrim");
        add("hud.fiatlux.power", "Power: ");
        add("screen.fiatlux.power_wheel", "Powers of Creation");
        add("screen.fiatlux.power_cost", "Essence %s · Tier %s");
        add("key.categories.fiatlux", "Fiat Lux");
        add("key.fiatlux.power_wheel", "Power Wheel");
        add("key.fiatlux.cast", "Speak Selected Power");

        // --- subtitles
        add("subtitles.fiatlux.power.fiat_lux", "Light is spoken into being");
        add("subtitles.fiatlux.power.smite", "Fire falls from heaven");
        add("subtitles.fiatlux.item.flaming_sword.wave", "Holy fire roars");
        add("subtitles.fiatlux.entity.seraph.holy", "Seraph cries Holy");
        add("subtitles.fiatlux.entity.cherub.wings", "Wings like great waters");
        add("subtitles.fiatlux.entity.ophan.wheel", "Wheels turn");
        add("subtitles.fiatlux.entity.host.arrive", "The Host descends");

        // --- configuration screen
        add("fiatlux.configuration.title", "Fiat Lux Configuration");
        String[][] sections = {{"divinity", "Divinity"}, {"creation", "Creation Engine"}, {"heaven", "Heaven"}, {"host", "Heavenly Host & Weapons"},
                {"fx", "Visual Effects"}, {"simulation", "Simulation Engine"}, {"cosmos", "The Cosmic Expanse"},
                {"civilization", "Mortal Civilisation & Faith"}, {"rebellion", "Angelic Rebellion"}, {"underworld", "The Underworld"}, {"sky", "Heaven Sky"}, {"hud", "HUD"}, {"multiplayer", "Multiplayer"}};
        for (String[] s : sections) add("fiatlux.configuration." + s[0], s[1]);
        String[][] values = {
                {"defaultPlayMode", "Default Play Mode"}, {"maxEssence", "Maximum Divine Essence"}, {"essenceRegenPerSecond", "Essence Regeneration / s"},
                {"unboundCostMultiplier", "Unbound Cost Multiplier"}, {"ascensionCostMultiplier", "Ascension Cost Multiplier"}, {"gloryPerTier", "Glory per Tier"},
                {"editBlocksPerTick", "Edit Blocks per Tick"}, {"maxEditVolume", "Maximum Edit Volume"}, {"undoHistory", "Undo History"},
                {"chronicleLength", "Chronicle Length"}, {"startInHeaven", "Begin in Heaven"}, {"heavenStreetSpacing", "Golden Street Spacing"},
                {"heavenIslandLevel", "Heaven Island Level"}, {"hostAggroRange", "Host Watch Range"}, {"swordFireWaveRange", "Sword Fire-Wave Range"},
                {"swordSlaysCorruptInstantly", "Sword Slays the Corrupt Instantly"}, {"particleDensity", "Particle Density"},
                {"screenFlashes", "Screen Flashes"}, {"screenFlashStrength", "Screen Flash Strength"}, {"hostAuras", "Host Radiance Auras"},
                {"heavenSkybox", "Custom Heaven Sky"}, {"godRayCount", "God Rays"}, {"heavenVignette", "Heaven Vignette"},
                {"showHud", "Show HUD"}, {"hudX", "HUD X"}, {"hudY", "HUD Y"}, {"godPermissionLevel", "God Permission Level"},
                {"pilgrimsCanEnterHeaven", "Pilgrims May Enter Heaven"}, {"announceChronicle", "Announce the Chronicle"},
                {"simulationEnabled", "Simulation Enabled"}, {"simulationSpeed", "Simulation Speed"}, {"cosmicStepInterval", "Ticks per Simulation Step"},
                {"planetsPerStep", "Planets per Step"}, {"cosmosSyncInterval", "Sky Sync Interval"}, {"maxPlanets", "Maximum Worlds"},
                {"orbitSpeedMultiplier", "Orbit Speed"}, {"bigBangGalaxies", "Galaxies Born of the Big Bang"},
                {"populationGrowthRate", "Population Growth"}, {"faithPerBeliever", "Faith per Believer"}, {"gloryPerFaith", "Glory per Faith"},
                {"sinRate", "Sin Rate"}, {"mortalEraProgressRate", "Mortal Era Progress"}, {"rebellionEnabled", "Rebellion Enabled"},
                {"rebellionCorruptionThreshold", "Rebellion Corruption Threshold"}, {"rebellionChancePerCheck", "Rebellion Chance"},
                {"prideGrowthRate", "Angelic Pride Growth"}, {"corruptionDecay", "Corruption Decay"}, {"underworldTechRate", "Underworld Tech Rate"},
                {"soulPowerPerSoul", "Soul Power per Soul"}, {"feudalThreshold", "Feudal Era Threshold"}, {"industrialThreshold", "Industrial Era Threshold"},
                {"modernThreshold", "Modern/Cybernetic Era Threshold"}, {"maxInfernalPopulation", "Max Infernal Population"},
                {"divineCleansesEnabled", "Divine Cleanses"}, {"cleanseIntervalDays", "Cleanse Interval (days)"}, {"cleanseStrength", "Cleanse Strength"}};
        for (String[] v : values) add("fiatlux.configuration." + v[0], v[1]);
    }

    private void power(String id, String name, String desc) {
        add("power.fiatlux." + id, name);
        add("power.fiatlux." + id + ".desc", desc);
    }
}
