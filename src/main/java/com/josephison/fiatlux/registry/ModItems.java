package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.weapon.DivineScepterItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FiatLux.MODID);

    public static final DeferredItem<DivineScepterItem> DIVINE_SCEPTER = ITEMS.register("divine_scepter",
            () -> new DivineScepterItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> LUX_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.LUX_BLOCK);
    public static final DeferredItem<BlockItem> GOLDEN_STREET = ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_STREET);
    public static final DeferredItem<BlockItem> JASPER = ITEMS.registerSimpleBlockItem(ModBlocks.JASPER);
    public static final DeferredItem<BlockItem> CLOUDSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.CLOUDSTONE);
    public static final DeferredItem<BlockItem> CELESTIAL_CRYSTAL = ITEMS.registerSimpleBlockItem(ModBlocks.CELESTIAL_CRYSTAL);
    public static final DeferredItem<BlockItem> PEARL = ITEMS.registerSimpleBlockItem(ModBlocks.PEARL);
    public static final DeferredItem<BlockItem> SULFUR = ITEMS.registerSimpleBlockItem(ModBlocks.SULFUR);
    public static final DeferredItem<BlockItem> BRIMSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.BRIMSTONE);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.ABYSSAL_STONE);
    public static final DeferredItem<BlockItem> NEON_CRIMSON = ITEMS.registerSimpleBlockItem(ModBlocks.NEON_CRIMSON);
    public static final DeferredItem<BlockItem> NEON_VIOLET = ITEMS.registerSimpleBlockItem(ModBlocks.NEON_VIOLET);
    public static final DeferredItem<BlockItem> SOUL_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.SOUL_ENGINE);
    public static final DeferredItem<BlockItem> REGOLITH = ITEMS.registerSimpleBlockItem(ModBlocks.REGOLITH);
    public static final DeferredItem<BlockItem> CLOUD_BAND_AMBER = ITEMS.registerSimpleBlockItem(ModBlocks.CLOUD_BAND_AMBER);
    public static final DeferredItem<BlockItem> CLOUD_BAND_CREAM = ITEMS.registerSimpleBlockItem(ModBlocks.CLOUD_BAND_CREAM);
    public static final DeferredItem<BlockItem> CLOUD_BAND_RUST = ITEMS.registerSimpleBlockItem(ModBlocks.CLOUD_BAND_RUST);

    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_SPAWN_EGG = ITEMS.register("angel_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ANGEL, 0xF4F2E8, 0xF6C43C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SERAPH_SPAWN_EGG = ITEMS.register("seraph_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SERAPH, 0xFFC45C, 0xFF6E1E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CHERUB_SPAWN_EGG = ITEMS.register("cherub_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CHERUB, 0x5C3428, 0xFF7820, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DOMINION_SPAWN_EGG = ITEMS.register("dominion_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DOMINION, 0xF4F2E8, 0x3050A8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VIRTUE_SPAWN_EGG = ITEMS.register("virtue_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VIRTUE, 0xFFF6D6, 0xFFD878, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> POWER_SPAWN_EGG = ITEMS.register("power_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.POWER, 0xB6BECC, 0xF0C040, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> PRINCIPALITY_SPAWN_EGG = ITEMS.register("principality_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PRINCIPALITY, 0xF4F0E4, 0x9E1E2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ARCHANGEL_SPAWN_EGG = ITEMS.register("archangel_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ARCHANGEL, 0xF4F2E8, 0xF6C43C, new Item.Properties()));
    public static final DeferredItem<com.josephison.fiatlux.item.BookOfGenerationsItem> BOOK_OF_GENERATIONS = ITEMS.register("book_of_generations",
            () -> new com.josephison.fiatlux.item.BookOfGenerationsItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> MORTAL_SPAWN_EGG = ITEMS.register("mortal_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MORTAL, 0xC69C78, 0x8A6A44, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FALLEN_SPAWN_EGG = ITEMS.register("fallen_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FALLEN, 0x2C262E, 0xC0281C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> OPHAN_SPAWN_EGG = ITEMS.register("ophan_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OPHAN, 0xD6E296, 0xF6C43C, new Item.Properties()));

    // --- the garden of Eden
    public static final DeferredItem<com.josephison.fiatlux.eden.EdenFruitItem> FRUIT_OF_LIFE = ITEMS.register("fruit_of_life",
            () -> new com.josephison.fiatlux.eden.EdenFruitItem(new Item.Properties().rarity(Rarity.EPIC)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(1.2f).alwaysEdible().build()), true));
    public static final DeferredItem<com.josephison.fiatlux.eden.EdenFruitItem> FRUIT_OF_KNOWLEDGE = ITEMS.register("fruit_of_knowledge",
            () -> new com.josephison.fiatlux.eden.EdenFruitItem(new Item.Properties().rarity(Rarity.RARE)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build()), false));
    public static final DeferredItem<BlockItem> LIFE_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.LIFE_LEAVES);
    public static final DeferredItem<BlockItem> KNOWLEDGE_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.KNOWLEDGE_LEAVES);
    public static final DeferredItem<DeferredSpawnEggItem> SERPENT_SPAWN_EGG = ITEMS.register("serpent_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SERPENT, 0x34704A, 0xD6BA46, new Item.Properties()));

    private ModItems() {}
}
