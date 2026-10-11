package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

/**
 * Planetary Genesis: forms a new world in orbit (Earth-like, volcanic, barren moon, gas giant or
 * crystalline, in turn). Requires the Big Bang. Each world orbits by Kepler's law and is visible
 * from Heaven's sky; habitable worlds can receive the breath of life.
 */
public class PlanetaryGenesisPower extends AbstractDivinePower {
    private static final String[] NAMES = {"Eden", "Ararat", "Shinar", "Goshen", "Canaan", "Moriah", "Sinai", "Carmel",
            "Hermon", "Tabor", "Gilead", "Sharon", "Lebanon", "Bashan", "Zoar", "Elim"};

    public PlanetaryGenesisPower() {
        super(120f, 1, 0xFF6FC2FF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        MinecraftServer server = ctx.level().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        if (!cosmos.hasBigBang()) {
            ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.no_universe"), true);
            return false;
        }
        PlanetType type = PlanetType.values()[cosmos.planets.size() % PlanetType.values().length];
        Planet p = create(server, ctx.caster().getGameProfile().getName(), type, null);
        if (p == null) return false;
        Fx.send(ctx.level(), FxKind.PLANET_GENESIS, ctx.caster().position().add(0, 1, 0), 10f, p.type.accentColor, 40);
        if (ctx.level().dimension() == com.josephison.fiatlux.world.FiatLuxDimensions.VOID) {
            com.josephison.fiatlux.cinema.Films.genesis(ctx.level(), ctx.caster(), p);
        }
        ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.planet_formed", p.name,
                Component.translatable("planet_type.fiatlux." + p.type.getSerializedName())), true);
        return true;
    }

    /** Shared by the power and /fiatlux planet create. Returns null if the cosmos is full. */
    public static Planet create(MinecraftServer server, String actor, PlanetType type, String name) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        if (cosmos.planets.size() >= FiatLuxConfig.COMMON.maxPlanets.get()) return null;
        RandomSource r = server.overworld().getRandom();
        if (name == null) {
            int n = cosmos.planets.size();
            name = NAMES[n % NAMES.length] + (n >= NAMES.length ? " " + (n / NAMES.length + 1) : "");
        }
        Planet p = cosmos.createPlanet(name, type, r);
        CosmosData.get(server).record(server, actor, "planet", "formed " + p.name + ", a "
                + type.getSerializedName().replace('_', ' ') + " world, and set it in its orbit.");
        WorldTicker.syncAll(server);
        return p;
    }

    @Override
    public int glory() {
        return 10;
    }
}
