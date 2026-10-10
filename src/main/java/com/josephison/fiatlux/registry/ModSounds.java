package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events. sounds.json layers vanilla sounds as placeholders until bespoke audio is recorded. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FiatLux.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FIAT_LUX = register("power.fiat_lux");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMITE = register("power.smite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWORD_WAVE = register("item.flaming_sword.wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLY = register("entity.seraph.holy");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINGS = register("entity.cherub.wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL = register("entity.ophan.wheel");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_ARRIVE = register("entity.host.arrive");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(FiatLux.id(name)));
    }

    private ModSounds() {}
}
