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
        extra().forEach(b -> dropSelf(b));
        dropWhenSilkTouch(ModBlocks.LIFE_LEAVES.get());
        dropWhenSilkTouch(ModBlocks.KNOWLEDGE_LEAVES.get());
        add(ModBlocks.LIFE_FRUIT.get(), createSingleItemTable(com.josephison.fiatlux.registry.ModItems.FRUIT_OF_LIFE.get()));
        add(ModBlocks.KNOWLEDGE_FRUIT.get(), createSingleItemTable(com.josephison.fiatlux.registry.ModItems.FRUIT_OF_KNOWLEDGE.get()));
    }

    private static List<Block> extra() {
        return List.of(ModBlocks.SULFUR.get(), ModBlocks.BRIMSTONE.get(), ModBlocks.ABYSSAL_STONE.get(), ModBlocks.NEON_CRIMSON.get(),
                ModBlocks.NEON_VIOLET.get(), ModBlocks.SOUL_ENGINE.get(), ModBlocks.REGOLITH.get(), ModBlocks.CLOUD_BAND_AMBER.get(),
                ModBlocks.CLOUD_BAND_CREAM.get(), ModBlocks.CLOUD_BAND_RUST.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> all = new java.util.ArrayList<>(List.of(ModBlocks.LUX_BLOCK.get(), ModBlocks.GOLDEN_STREET.get(), ModBlocks.JASPER.get(),
                ModBlocks.CLOUDSTONE.get(), ModBlocks.PEARL.get(), ModBlocks.CELESTIAL_CRYSTAL.get()));
        all.addAll(extra());
        all.addAll(List.of(ModBlocks.LIFE_LEAVES.get(), ModBlocks.KNOWLEDGE_LEAVES.get(), ModBlocks.LIFE_FRUIT.get(), ModBlocks.KNOWLEDGE_FRUIT.get()));
        return all;
    }
}
