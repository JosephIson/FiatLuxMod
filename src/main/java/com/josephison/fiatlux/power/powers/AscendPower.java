package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.HeavenTravel;

/** Jacob's ladder (Genesis 28:12): ascend to Heaven, or descend back to the earth. */
public class AscendPower extends AbstractDivinePower {
    public AscendPower() {
        super(0f, 0, 0xFFFFFFFF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        Fx.send(ctx.level(), FxKind.CELESTIAL_PILLAR, ctx.caster().position(), 200f, 0xFFFFF6D8, 30);
        HeavenTravel.toggle(ctx.caster());
        Fx.send(ctx.caster().serverLevel(), FxKind.GLORY_FLASH, ctx.caster().position(), 6f, 0xFFFFF6D8, 30);
        return true;
    }

    @Override
    public int glory() {
        return 0;
    }
}
