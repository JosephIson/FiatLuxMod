package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Sends one of the Heavenly Host to the place the Creator gazes upon. */
public class SummonHostPower extends AbstractDivinePower {
    private final Supplier<? extends EntityType<? extends HeavenlyBeing>> type;

    public SummonHostPower(float cost, int tier, int color, Supplier<? extends EntityType<? extends HeavenlyBeing>> type) {
        super(cost, tier, color);
        this.type = type;
    }

    @Override
    public boolean cast(PowerContext ctx) {
        Vec3 at = ctx.target().add(0, 1.0, 0);
        HeavenlyBeing being = type.get().create(ctx.level());
        if (being == null) return false;
        being.moveTo(at.x, at.y, at.z, ctx.caster().getYRot() + 180f, 0f);
        being.finalizeSpawn(ctx.level(), ctx.level().getCurrentDifficultyAt(being.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        being.setCreator(ctx.caster().getUUID());
        ctx.level().addFreshEntity(being);
        Fx.send(ctx.level(), FxKind.HOST_ARRIVAL, at, being.getBbWidth() * 2f, color(), 40);
        return true;
    }

    @Override
    public int glory() {
        return 5;
    }
}
