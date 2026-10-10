package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FiatLux.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIAT_LUX = TABS.register("fiat_lux", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux"))
            .icon(() -> new ItemStack(ModItems.DIVINE_SCEPTER.get()))
            .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    private ModCreativeTabs() {}
}
