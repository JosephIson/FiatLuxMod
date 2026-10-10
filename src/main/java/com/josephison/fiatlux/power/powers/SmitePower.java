package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.registry.ModTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** "Then the fire of the LORD fell, and consumed the burnt sacrifice" (1 Kings 18:38). A pillar of fire from heaven. */
public class SmitePower extends AbstractDivinePower {
    public SmitePower() {
        super(50f, 1, 0xFFFFD27A);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ctx.level());
        if (bolt != null) {
            bolt.moveTo(ctx.target());
            bolt.setCause(ctx.caster());
            ctx.level().addFreshEntity(bolt);
        }
        AABB area = new AABB(ctx.target(), ctx.target()).inflate(3.5);
        for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != ctx.caster())) {
            if (ModTags.isCorrupt(e)) {
                e.hurt(ctx.level().damageSources().indirectMagic(ctx.caster(), ctx.caster()), 40f);
                e.igniteForSeconds(8);
            }
        }
        Fx.send(ctx.level(), FxKind.CELESTIAL_PILLAR, ctx.target(), 160f, color(), 30);
        return true;
    }

    @Override
    public int glory() {
        return 0;
    }
}
