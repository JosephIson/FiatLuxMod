package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.weapon.WeaponTraits;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components. Raw item NBT is never used in Fiat Lux. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, FiatLux.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponTraits>> WEAPON_TRAITS =
            COMPONENTS.registerComponentType("weapon_traits", b -> b
                    .persistent(WeaponTraits.CODEC).networkSynchronized(WeaponTraits.STREAM_CODEC));

    /** The divine power bound to a scepter. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BOUND_POWER =
            COMPONENTS.registerComponentType("bound_power", b -> b
                    .persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ModDataComponents() {}
}
