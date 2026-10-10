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

    @Override
    public boolean cast(PowerContext ctx) {
        return ignite(ctx.level().getServer(), ctx.caster().getGameProfile().getName());
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
