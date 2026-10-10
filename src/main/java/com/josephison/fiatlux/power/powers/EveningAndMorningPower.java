package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.server.level.ServerLevel;

/** "And the evening and the morning were the first day." (Genesis 1:5) Advances to the next dusk or dawn. */
public class EveningAndMorningPower extends AbstractDivinePower {
    public EveningAndMorningPower() {
        super(10f, 0, 0xFFFFC27A);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        ServerLevel overworld = ctx.level().getServer().overworld();
        long time = overworld.getDayTime();
        long dayStart = time - Math.floorMod(time, 24000L);
        boolean isDay = Math.floorMod(time, 24000L) < 12000L;
        long next = isDay ? dayStart + 12500L : dayStart + 24000L;
        for (ServerLevel level : ctx.level().getServer().getAllLevels()) {
            level.setDayTime(next);
        }
        Fx.send(ctx.level(), FxKind.GLORY_FLASH, ctx.caster().position(), 4f, isDay ? 0xFFFF9A5A : 0xFFFFF1C0, 20);
        if (!isDay) {
            long day = next / 24000L;
            CosmosData.get(ctx.level().getServer()).record(ctx.level().getServer(), ctx.caster().getGameProfile().getName(),
                    "day", "And the evening and the morning were day " + day + ".");
        }
        return true;
    }
}
