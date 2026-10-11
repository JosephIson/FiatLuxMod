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
        add(ModBlocks.SULFUR.get(), "Sulfur");
        add(ModBlocks.BRIMSTONE.get(), "Brimstone");
        add(ModBlocks.ABYSSAL_STONE.get(), "Abyssal Stone");
        add(ModBlocks.NEON_CRIMSON.get(), "Crimson Neon");
        add(ModBlocks.NEON_VIOLET.get(), "Violet Neon");
        add(ModBlocks.SOUL_ENGINE.get(), "Soul Engine");
        add(ModBlocks.REGOLITH.get(), "Regolith");
        add(ModBlocks.CLOUD_BAND_AMBER.get(), "Amber Cloud Band");
        add(ModBlocks.CLOUD_BAND_CREAM.get(), "Cream Cloud Band");
        add(ModBlocks.CLOUD_BAND_RUST.get(), "Rust Cloud Band");
        add("tooltip.fiatlux.kindled", "Holy fire: %s / 5");
        add("tooltip.fiatlux.smitten", "Corrupt beings smitten: %s");
        add("tooltip.fiatlux.flaming_sword.use", "Use: wave of holy fire. Sneak-use on ground: station it as a guardian.");
        add("tooltip.fiatlux.scepter.use", "Use: speak the selected power. Sneak-use: next power. [R] power wheel, [G] cast.");

        // --- the Heavenly Host
        add(ModEntities.ANGEL.get(), "Angel");
        add(ModEntities.SERAPH.get(), "Seraph");
        add(ModEntities.CHERUB.get(), "Cherub");
        add(ModEntities.OPHAN.get(), "Ophan");
        add(ModEntities.DOMINION.get(), "Dominion");
        add(ModItems.DOMINION_SPAWN_EGG.get(), "Dominion Spawn Egg");
        add(ModEntities.VIRTUE.get(), "Virtue");
        add(ModItems.VIRTUE_SPAWN_EGG.get(), "Virtue Spawn Egg");
        add(ModEntities.POWER.get(), "Power");
        add(ModItems.POWER_SPAWN_EGG.get(), "Power Spawn Egg");
        add(ModEntities.PRINCIPALITY.get(), "Principality");
        add(ModItems.PRINCIPALITY_SPAWN_EGG.get(), "Principality Spawn Egg");
        add(ModEntities.ARCHANGEL.get(), "Archangel");
        add(ModItems.ARCHANGEL_SPAWN_EGG.get(), "Archangel Spawn Egg");
        add("choir.fiatlux.seraphim", "Seraphim");
        add("choir.fiatlux.seraphim.office", "First sphere: the burning ones who cry Holy, Holy, Holy before the throne.");
        add("choir.fiatlux.cherubim", "Cherubim");
        add("choir.fiatlux.cherubim.office", "First sphere: the living creatures who guard the way of the tree of life and bear the throne.");
        add("choir.fiatlux.thrones", "Thrones (Ophanim)");
        add("choir.fiatlux.thrones.office", "First sphere: the wheels within wheels, full of eyes, on which the glory moves.");
        add("choir.fiatlux.dominions", "Dominions");
        add("choir.fiatlux.dominions.office", "Second sphere: they govern the offices of the lower choirs; their sceptre strengthens those beneath them.");
        add("choir.fiatlux.virtues", "Virtues");
        add("choir.fiatlux.virtues.office", "Second sphere: spirits through whom signs and miracles are done; they heal and lift afflictions.");
        add("choir.fiatlux.powers", "Powers");
        add("choir.fiatlux.powers.office", "Second sphere: the warrior choir that holds the fallen in chains.");
        add("choir.fiatlux.principalities", "Principalities");
        add("choir.fiatlux.principalities.office", "Third sphere: princes set over peoples and nations; they guard the mortals in their keeping.");
        add("choir.fiatlux.archangels", "Archangels");
        add("choir.fiatlux.archangels.office", "Third sphere: captains of the Host; the voice of the archangel and the trump of God.");
        add("choir.fiatlux.angels", "Angels");
        add("choir.fiatlux.angels.office", "Third sphere: messengers, ministering spirits sent forth to minister.");
        add(ModEntities.FALLEN.get(), "Fallen Angel");
        add(ModItems.FALLEN_SPAWN_EGG.get(), "Fallen Angel Spawn Egg");
        add(ModBlocks.VOID_GATE.get(), "Veil of the Gate of Pearl");
        add("message.fiatlux.void_formless", "The earth was without form, and void; and darkness was upon the face of the deep. (Genesis 1:2) - Speak Let There Be Light, or the Cosmic Spark.");
        add("message.fiatlux.void_expanse", "The Cosmic Expanse: fly to a world to descend upon it; return through Heaven's Gate behind you.");
        add("message.fiatlux.left_world", "You rise out of the skies of %s into the Expanse.");
        add("entity.fiatlux.lucifer", "Lucifer, Son of the Morning");
        add("entity.fiatlux.rebel_captain", "Captain of the Rebel Host");
        add("entity.fiatlux.michael", "Michael the Archangel");
        add("command.fiatlux.host.header", "The celestial hierarchy - nine choirs in three spheres:");
        add("command.fiatlux.host.line", "%s. %s (sphere %s) - %s [%s] · here: %s loyal, %s in rebellion");
        add("chat.fiatlux.war_in_heaven", "There was war in heaven (Rev 12:7): %s rebel angels gather in %s near %s %s %s. Michael and his angels muster against them. Defeat the rebels, or speak Cast Out.");
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
        power("summon_dominion", "Summon a Dominion", "Colossians 1:16. A governor of the lower choirs: its sceptre strengthens the Host beneath it and burdens the corrupt.");
        power("summon_virtue", "Summon a Virtue", "Ephesians 1:21. A worker of signs and miracles: it heals the wounded and lifts afflictions.");
        power("summon_power", "Summon a Power", "Ephesians 6:12; Jude 1:6. An armoured warrior who strikes with the sword and binds the corrupt in chains; doubly fearsome to the fallen.");
        power("send_principality", "Send a Principality", "Daniel 10:21. A prince over a people: it guards mortals from every hostile creature and shields them.");
        power("send_archangel", "Send an Archangel", "1 Thessalonians 4:16. A captain of the Host whose trumpet strikes down and hurls back every enemy around it.");
        power("cast_out", "Cast Out", "Revelation 12:9: strikes down the rebel angels in your gaze and around it. Lightning breaks their radiance; they fall through a fracture in the sky into the Underworld.");
        add("power.fiatlux.cast_out.none_near", "No rebel angels are near your gaze. The rebels gather in Heaven: /fiatlux heaven");
        add("power.fiatlux.cast_out.no_war", "There is no war in heaven. (Begin one with /fiatlux event rebellion.)");
        power("visit_world", "Walk upon a World", "Carries you to the next created world, to stand upon its ground under its own sky and gravity.");
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
        add("message.fiatlux.world_unformed", "That world is without form, and void.");
        add("message.fiatlux.no_worlds", "No world has yet been formed. Speak Planetary Genesis.");
        add("message.fiatlux.arrived", "You stand upon %s, a %s world.");
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
