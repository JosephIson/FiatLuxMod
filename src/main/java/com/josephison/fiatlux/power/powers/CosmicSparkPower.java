package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.network.FxPayload;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.WorldTicker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "In the beginning God created the heaven and the earth." (Genesis 1:1)
 * The Cosmic Spark: the Big Bang. The first casting forms the universe and its galaxies; later
 * castings kindle further galaxies. Every Creator in the world beholds it.
 */
public class CosmicSparkPower extends AbstractDivinePower {
    public CosmicSparkPower() {
        super(200f, 0, 0xFFFFFFFF);
    }

    /** Until when (server tick) the first light is waiting on the film of the beginning. */
    private static int pendingUntil = -1;

    @Override
    public boolean cast(PowerContext ctx) {
        return beginning(ctx.level().getServer(), ctx.caster().getGameProfile().getName());
    }

    /**
     * The first Big Bang, with its film for all in the Void: darkness on the face of the deep, the word,
     * and then the light (the Big Bang itself waits for that moment of the film). Later ones are at once.
     */
    public static boolean beginning(MinecraftServer server, String actor) {
        if (CosmicExpanseSavedData.get(server).hasBigBang()) return ignite(server, actor);
        if (server.getTickCount() < pendingUntil) return false;   // the word is already being spoken
        ServerLevel v = server.getLevel(com.josephison.fiatlux.world.FiatLuxDimensions.VOID);
        if (v != null && com.josephison.fiatlux.cinema.Films.beginning(v, actor)) {
            pendingUntil = server.getTickCount() + com.josephison.fiatlux.cinema.Films.LET_THERE_BE_LIGHT + 40;
            com.josephison.fiatlux.cinema.Cinema.later(com.josephison.fiatlux.cinema.Films.LET_THERE_BE_LIGHT, () -> {
                pendingUntil = -1;
                if (!CosmicExpanseSavedData.get(server).hasBigBang()) ignite(server, actor);
            });
            return true;
        }
        return ignite(server, actor);
    }

    /** Shared by the power and /fiatlux event bigbang. */
    public static boolean ignite(MinecraftServer server, String actor) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        boolean first = !cosmos.hasBigBang();
        cosmos.bigBang(server.overworld().getGameTime(), first ? FiatLuxConfig.COMMON.bigBangGalaxies.get() : 1, server.overworld().getRandom());
        CosmosData.get(server).record(server, actor, first ? "big_bang" : "galaxy",
                first ? "spoke into the void, and the heavens burst into being: " + cosmos.galaxies.size() + " galaxies were born."
                        : "kindled a new galaxy, " + cosmos.galaxies.getLast().name() + ".");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, new FxPayload(FxKind.BIG_BANG.ordinal(), p.getX(), p.getY() + 1, p.getZ(), 40f, 0xFFFFFFFF, 80));
        }
        WorldTicker.syncAll(server);
        return true;
    }

    @Override
    public int glory() {
        return 25;
    }
}
