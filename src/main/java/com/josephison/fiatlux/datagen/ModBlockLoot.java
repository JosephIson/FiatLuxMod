package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

public class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.LUX_BLOCK.get());
        dropSelf(ModBlocks.GOLDEN_STREET.get());
        dropSelf(ModBlocks.JASPER.get());
        dropSelf(ModBlocks.CLOUDSTONE.get());
        dropSelf(ModBlocks.PEARL.get());
        dropSelf(ModBlocks.CELESTIAL_CRYSTAL.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ModBlocks.LUX_BLOCK.get(), ModBlocks.GOLDEN_STREET.get(), ModBlocks.JASPER.get(),
                ModBlocks.CLOUDSTONE.get(), ModBlocks.PEARL.get(), ModBlocks.CELESTIAL_CRYSTAL.get());
    }
}
