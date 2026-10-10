package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class ModTagProviders {
    private ModTagProviders() {}

    public static class Blocks extends BlockTagsProvider {
        public Blocks(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(out, lookup, FiatLux.MODID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.JASPER.get(), ModBlocks.PEARL.get(), ModBlocks.GOLDEN_STREET.get(),
                    ModBlocks.CELESTIAL_CRYSTAL.get(), ModBlocks.LUX_BLOCK.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SULFUR.get(), ModBlocks.BRIMSTONE.get(), ModBlocks.ABYSSAL_STONE.get(),
                    ModBlocks.NEON_CRIMSON.get(), ModBlocks.NEON_VIOLET.get(), ModBlocks.SOUL_ENGINE.get());
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.CLOUDSTONE.get(), ModBlocks.REGOLITH.get(), ModBlocks.CLOUD_BAND_AMBER.get(),
                    ModBlocks.CLOUD_BAND_CREAM.get(), ModBlocks.CLOUD_BAND_RUST.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.JASPER.get(), ModBlocks.GOLDEN_STREET.get());
        }
    }

    public static class Entities extends EntityTypeTagsProvider {
        public Entities(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(out, lookup, FiatLux.MODID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.HEAVENLY_HOST).add(ModEntities.ANGEL.get(), ModEntities.SERAPH.get(), ModEntities.CHERUB.get(), ModEntities.OPHAN.get());
            tag(ModTags.CORRUPT).addTag(EntityTypeTags.UNDEAD).add(EntityType.BLAZE, EntityType.GHAST, EntityType.VEX,
                    EntityType.EVOKER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH, EntityType.PIGLIN_BRUTE,
                    EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.MAGMA_CUBE);
        }
    }
}
