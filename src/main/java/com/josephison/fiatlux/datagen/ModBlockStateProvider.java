package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, FiatLux.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(ModBlocks.LUX_BLOCK.get());
        simpleBlock(ModBlocks.JASPER.get());
        simpleBlock(ModBlocks.CLOUDSTONE.get());
        simpleBlock(ModBlocks.PEARL.get());
        for (var b : java.util.List.of(ModBlocks.SULFUR, ModBlocks.BRIMSTONE, ModBlocks.ABYSSAL_STONE, ModBlocks.NEON_CRIMSON,
                ModBlocks.NEON_VIOLET, ModBlocks.SOUL_ENGINE, ModBlocks.REGOLITH, ModBlocks.CLOUD_BAND_AMBER,
                ModBlocks.CLOUD_BAND_CREAM, ModBlocks.CLOUD_BAND_RUST)) {
            simpleBlock(b.get());
        }
        translucent(ModBlocks.GOLDEN_STREET.get(), "golden_street");
        translucent(ModBlocks.STILLED_WATER.get(), "stilled_water");
        translucent(ModBlocks.VOID_GATE.get(), "void_gate");
        ModelFile crystal = models().cross("celestial_crystal", modLoc("block/celestial_crystal")).renderType("cutout");
        directionalBlock(ModBlocks.CELESTIAL_CRYSTAL.get(), crystal);
    }

    private void translucent(Block block, String name) {
        simpleBlock(block, models().cubeAll(name, modLoc("block/" + name)).renderType("translucent"));
    }
}
