package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Run with `./gradlew runData`; output goes to src/generated/resources. */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeClient(), new ModBlockStateProvider(out, files));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(out, files));
        gen.addProvider(event.includeClient(), new ModLanguageProvider(out));
        gen.addProvider(event.includeClient(), new ModSoundProvider(out, files));
        gen.addProvider(event.includeClient(), new ModParticleProvider(out, files));

        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        BlockTagsProvider blockTags = new ModTagProviders.Blocks(out, lookup, files);
        gen.addProvider(event.includeServer(), blockTags);
        gen.addProvider(event.includeServer(), new ModTagProviders.Entities(out, lookup, files));
    }
}
