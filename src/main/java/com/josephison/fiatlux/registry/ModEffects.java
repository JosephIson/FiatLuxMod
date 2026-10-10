package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.effect.DivineEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FiatLux.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BLESSED = EFFECTS.register("blessed", () ->
            new DivineEffect(DivineEffect.Kind.BLESSED, MobEffectCategory.BENEFICIAL, 0xFFE27A, ParticleTypes.END_ROD)
                    .addAttributeModifier(Attributes.LUCK, FiatLux.id("effect.blessed.luck"), 2.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MAX_HEALTH, FiatLux.id("effect.blessed.health"), 4.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> SANCTIFIED = EFFECTS.register("sanctified", () ->
            new DivineEffect(DivineEffect.Kind.SANCTIFIED, MobEffectCategory.BENEFICIAL, 0xFFFFFF, ParticleTypes.WAX_OFF)
                    .addAttributeModifier(Attributes.ARMOR, FiatLux.id("effect.sanctified.armor"), 4.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> CURSED = EFFECTS.register("cursed", () ->
            new DivineEffect(DivineEffect.Kind.CURSED, MobEffectCategory.HARMFUL, 0x5A2A4A, ParticleTypes.SMOKE)
                    .addAttributeModifier(Attributes.LUCK, FiatLux.id("effect.cursed.luck"), -3.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, FiatLux.id("effect.cursed.speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> CORRUPTED = EFFECTS.register("corrupted", () ->
            new DivineEffect(DivineEffect.Kind.CORRUPTED, MobEffectCategory.HARMFUL, 0x7A1414, ParticleTypes.ASH)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, FiatLux.id("effect.corrupted.damage"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> SPIRIT_FILLED = EFFECTS.register("spirit_filled", () ->
            new DivineEffect(DivineEffect.Kind.SPIRIT_FILLED, MobEffectCategory.BENEFICIAL, 0xFFB347, ParticleTypes.FLAME)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, FiatLux.id("effect.spirit_filled.speed"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, FiatLux.id("effect.spirit_filled.damage"), 3.0, AttributeModifier.Operation.ADD_VALUE));

    private ModEffects() {}
}
