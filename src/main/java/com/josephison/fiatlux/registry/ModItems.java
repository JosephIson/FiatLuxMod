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
    public static final DeferredItem<DeferredSpawnEggItem> OPHAN_SPAWN_EGG = ITEMS.register("ophan_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OPHAN, 0xD6E296, 0xF6C43C, new Item.Properties()));

    private ModItems() {}
}
