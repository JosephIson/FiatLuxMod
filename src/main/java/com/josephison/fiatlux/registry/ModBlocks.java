package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.world.StilledWaterBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FiatLux.MODID);

    /** Solid radiance: a block of created light. */
    public static final DeferredBlock<Block> LUX_BLOCK = BLOCKS.register("lux_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.3f).sound(SoundType.GLASS)
                    .lightLevel(s -> 15).emissiveRendering((s, l, p) -> true).hasPostProcess((s, l, p) -> true)));
    /** "the street of the city was pure gold, as it were transparent glass" (Rev 21:21). */
    public static final DeferredBlock<TransparentBlock> GOLDEN_STREET = BLOCKS.register("golden_street", () -> new TransparentBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3f, 1200f).sound(SoundType.GLASS)
                    .lightLevel(s -> 6).noOcclusion().isViewBlocking((s, l, p) -> false)));
    /** "the building of the wall of it was of jasper" (Rev 21:18). */
    public static final DeferredBlock<Block> JASPER = BLOCKS.registerSimpleBlock("jasper",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(4f, 1200f).requiresCorrectToolForDrops());
    /** The soft white stone of Heaven's floating islands. */
    public static final DeferredBlock<Block> CLOUDSTONE = BLOCKS.registerSimpleBlock("cloudstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.2f).sound(SoundType.WOOL));
    /** Glowing crystal that grows in Heaven. */
    public static final DeferredBlock<AmethystClusterBlock> CELESTIAL_CRYSTAL = BLOCKS.register("celestial_crystal",
            () -> new AmethystClusterBlock(7f, 3f, BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND)
                    .strength(1.5f).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().lightLevel(s -> 11)
                    .pushReaction(PushReaction.DESTROY)));
    /** "the twelve gates were twelve pearls" (Rev 21:21). */
    public static final DeferredBlock<Block> PEARL = BLOCKS.registerSimpleBlock("pearl",
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(3f, 1200f).sound(SoundType.CALCITE).lightLevel(s -> 4));
    /** The walls of the parted sea (Exodus 14:22). */
    public static final DeferredBlock<StilledWaterBlock> STILLED_WATER = BLOCKS.register("stilled_water", () -> new StilledWaterBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(-1f, 3600000f).noLootTable()
                    .noOcclusion().isViewBlocking((s, l, p) -> false).sound(SoundType.WET_GRASS).lightLevel(s -> 2)));

    private ModBlocks() {}
}
