package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.realm.RealmTravel;
import net.minecraft.network.chat.Component;

/** Walk upon a created world: each casting carries the Creator to the next world of the cosmos. */
public class VisitWorldPower extends AbstractDivinePower {
    public VisitWorldPower() {
        super(0f, 1, 0xFF7FD0FF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        int slot = RealmTravel.nextSlot(ctx.caster().level().dimension());
        if (slot < 0) {
            ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.no_worlds"), true);
            return false;
        }
        Fx.send(ctx.level(), FxKind.CELESTIAL_PILLAR, ctx.caster().position(), 120f, color(), 20);
        boolean ok = RealmTravel.toPlanet(ctx.caster(), slot);
        if (ok) Fx.send(ctx.caster().serverLevel(), FxKind.PLANET_GENESIS, ctx.caster().position().add(0, 1, 0), 8f, color(), 30);
        return ok;
    }

    @Override
    public int glory() {
        return 0;
    }
}
