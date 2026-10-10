package com.josephison.fiatlux.power;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.power.powers.AscendPower;
import com.josephison.fiatlux.power.powers.BlessPower;
import com.josephison.fiatlux.power.powers.BreathOfLifePower;
import com.josephison.fiatlux.power.powers.CosmicSparkPower;
import com.josephison.fiatlux.power.powers.PlanetaryGenesisPower;
import com.josephison.fiatlux.power.powers.DelugePower;
import com.josephison.fiatlux.power.powers.EveningAndMorningPower;
import com.josephison.fiatlux.power.powers.FiatLuxPower;
import com.josephison.fiatlux.power.powers.PartWatersPower;
import com.josephison.fiatlux.power.powers.RaiseLandPower;
import com.josephison.fiatlux.power.powers.SmitePower;
import com.josephison.fiatlux.power.powers.SummonHostPower;
import com.josephison.fiatlux.registry.ModEntities;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The fiatlux:divine_power registry and the built-in powers, in power-wheel order. */
public final class PowerRegistry {
    public static final ResourceKey<Registry<AbstractDivinePower>> KEY = ResourceKey.createRegistryKey(FiatLux.id("divine_power"));
    public static final DeferredRegister<AbstractDivinePower> POWERS = DeferredRegister.create(KEY, FiatLux.MODID);
    public static final Registry<AbstractDivinePower> REGISTRY = POWERS.makeRegistry(b -> b.sync(true));

    // Genesis 1:1 - "In the beginning God created the heaven and the earth."
    public static final DeferredHolder<AbstractDivinePower, CosmicSparkPower> COSMIC_SPARK = POWERS.register("cosmic_spark", CosmicSparkPower::new);
    // Genesis 1:3 - "And God said, Let there be light: and there was light."
    public static final DeferredHolder<AbstractDivinePower, FiatLuxPower> FIAT_LUX = POWERS.register("fiat_lux", FiatLuxPower::new);
    // Genesis 1:5 - "And the evening and the morning were the first day."
    public static final DeferredHolder<AbstractDivinePower, EveningAndMorningPower> EVENING_AND_MORNING =
            POWERS.register("evening_and_morning", EveningAndMorningPower::new);
    // Numbers 6:24 - "The LORD bless thee, and keep thee."
    public static final DeferredHolder<AbstractDivinePower, BlessPower> BLESS = POWERS.register("bless", BlessPower::new);
    // Genesis 1:9 - "let the dry land appear"
    public static final DeferredHolder<AbstractDivinePower, RaiseLandPower> RAISE_LAND = POWERS.register("raise_land", RaiseLandPower::new);
    // 2 Kings 1:12 - "the fire of God came down from heaven"
    public static final DeferredHolder<AbstractDivinePower, SmitePower> SMITE = POWERS.register("smite", SmitePower::new);
    // Exodus 14:21 - "the LORD caused the sea to go back ... and the waters were divided"
    public static final DeferredHolder<AbstractDivinePower, PartWatersPower> PART_WATERS = POWERS.register("part_waters", PartWatersPower::new);
    // Genesis 7:11 - "the fountains of the great deep broken up, and the windows of heaven were opened"
    public static final DeferredHolder<AbstractDivinePower, DelugePower> DELUGE = POWERS.register("deluge", DelugePower::new);
    // Genesis 1:14-16 - lights in the firmament; worlds set in their courses
    public static final DeferredHolder<AbstractDivinePower, PlanetaryGenesisPower> PLANETARY_GENESIS = POWERS.register("planetary_genesis", PlanetaryGenesisPower::new);
    // Genesis 2:7 - "breathed into his nostrils the breath of life"
    public static final DeferredHolder<AbstractDivinePower, BreathOfLifePower> BREATH_OF_LIFE = POWERS.register("breath_of_life", BreathOfLifePower::new);
    // Hebrews 1:14 - "ministering spirits, sent forth to minister"
    public static final DeferredHolder<AbstractDivinePower, SummonHostPower> SEND_ANGEL = POWERS.register("send_angel",
            () -> new SummonHostPower(40f, 0, 0xFFF4EBC8, ModEntities.ANGEL::get));
    // Isaiah 6:2
    public static final DeferredHolder<AbstractDivinePower, SummonHostPower> SUMMON_SERAPH = POWERS.register("summon_seraph",
            () -> new SummonHostPower(160f, 2, 0xFFFFA040, ModEntities.SERAPH::get));
    // Genesis 3:24 / Ezekiel 10
    public static final DeferredHolder<AbstractDivinePower, SummonHostPower> SUMMON_CHERUB = POWERS.register("summon_cherub",
            () -> new SummonHostPower(220f, 3, 0xFFE0A050, ModEntities.CHERUB::get));
    // Ezekiel 1:15
    public static final DeferredHolder<AbstractDivinePower, SummonHostPower> SUMMON_OPHAN = POWERS.register("summon_ophan",
            () -> new SummonHostPower(260f, 4, 0xFFD6E296, ModEntities.OPHAN::get));
    // Genesis 28:12 - "a ladder set up on the earth, and the top of it reached to heaven"
    public static final DeferredHolder<AbstractDivinePower, AscendPower> ASCEND = POWERS.register("ascend", AscendPower::new);

    private PowerRegistry() {}
}
