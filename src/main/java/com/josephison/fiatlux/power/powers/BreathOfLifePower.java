package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.network.chat.Component;

/**
 * "And the LORD God formed man of the dust of the ground, and breathed into his nostrils the breath
 * of life; and man became a living soul." (Genesis 2:7) Seeds mortal life on the most recently
 * formed habitable world that has none. From then on it lives without the Creator's hand.
 */
public class BreathOfLifePower extends AbstractDivinePower {
    public BreathOfLifePower() {
        super(150f, 2, 0xFF9CE38C);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        var server = ctx.level().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        Planet target = null;
        for (int i = cosmos.planets.size() - 1; i >= 0 && target == null; i--) {
            Planet p = cosmos.planets.get(i);
            if (p.capacity() > 0 && !p.hasLife()) target = p;
        }
        if (target == null) {
            ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.no_lifeless_world"), true);
            return false;
        }
        cosmos.breatheLife(target);
        CosmosData.get(server).record(server, ctx.caster().getGameProfile().getName(), "life",
                "breathed the breath of life upon " + target.name + ", and mortals became living souls.");
        Fx.send(ctx.level(), FxKind.BLESSING, ctx.caster().position(), 3f, color(), 40);
        WorldTicker.syncAll(server);
        return true;
    }

    @Override
    public int glory() {
        return 15;
    }
}
