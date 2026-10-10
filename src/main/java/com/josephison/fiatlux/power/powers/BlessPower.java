package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.DivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** "The LORD bless thee, and keep thee: The LORD make his face shine upon thee" (Numbers 6:24-25). */
public class BlessPower extends DivinePower {
    public BlessPower() {
        super(15f, 0, 0xFFFFE27A);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        LivingEntity target = ctx.targetEntity() instanceof LivingEntity l ? l : ctx.caster();
        target.addEffect(new MobEffectInstance(ModEffects.BLESSED, 20 * 60, 0));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
        Divinity.setAlignment(target, Divinity.alignment(target).addVirtue(1));
        Fx.send(ctx.level(), FxKind.BLESSING, target.position(), target.getBbHeight(), color(), 40);
        return true;
    }
}
