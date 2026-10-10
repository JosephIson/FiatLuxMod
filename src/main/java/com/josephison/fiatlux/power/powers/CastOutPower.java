package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.RebellionWar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "And the great dragon was cast out, that old serpent, called the Devil, and Satan, which
 * deceiveth the whole world: he was cast out into the earth, and his angels were cast out with
 * him" (Revelation 12:9). The Creator casts out the rebel angel in its gaze and every rebel near
 * it: each is struck by lightning, its radiance shatters, its wings tear and mutate, and it
 * plunges through a fracture in the sky into the Underworld.
 */
public class CastOutPower extends AbstractDivinePower {
    public static final double RADIUS = 32.0;

    public CastOutPower() {
        super(90f, 1, 0xFFFF5A3A);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        List<HeavenlyBeing> rebels = new ArrayList<>();
        if (ctx.targetEntity() instanceof HeavenlyBeing aimed && aimed.isRebel() && !aimed.isBeingCastOut()) rebels.add(aimed);
        AABB area = new AABB(ctx.target(), ctx.target()).inflate(RADIUS).minmax(ctx.caster().getBoundingBox().inflate(RADIUS));
        ctx.level().getEntitiesOfClass(HeavenlyBeing.class, area, b -> b.isRebel() && !b.isBeingCastOut() && !rebels.contains(b))
                .stream().sorted(Comparator.comparingDouble(b -> b.distanceToSqr(ctx.target()))).forEach(rebels::add);
        if (rebels.isEmpty()) {
            CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(ctx.level().getServer());
            ctx.caster().displayClientMessage(Component.translatable(cosmos.rebellionActive
                    ? "power.fiatlux.cast_out.none_near" : "power.fiatlux.cast_out.no_war"), true);
            return false;
        }
        int n = 0;
        for (HeavenlyBeing rebel : rebels) {
            if (rebel.castOut(n * 8)) n++;
        }
        Fx.send(ctx.level(), FxKind.GLORY_FLASH, ctx.caster().getEyePosition(), 6f, 0xFFFFF0C0, 20);
        CosmosData.get(ctx.level().getServer()).record(ctx.level().getServer(), ctx.caster().getGameProfile().getName(), "cast_out",
                n == 1 ? "cast " + rebels.getFirst().getName().getString() + " out of heaven."
                        : "cast " + n + " rebel angels out of heaven, and they fell like lightning.");
        return n > 0;
    }

    @Override
    public double reach() {
        return 128.0;
    }

    @Override
    public int glory() {
        return 3;
    }
}
