package com.josephison.fiatlux.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** "the waters were a wall unto them on their right hand, and on their left" (Exodus 14:22). */
public class StilledWaterBlock extends HalfTransparentBlock {
    public static final MapCodec<StilledWaterBlock> CODEC = simpleCodec(StilledWaterBlock::new);

    public StilledWaterBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction d = Direction.getRandom(random);
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5 + d.getStepX() * 0.52,
                    pos.getY() + random.nextDouble(), pos.getZ() + 0.5 + d.getStepZ() * 0.52, 0, 0, 0);
        }
    }
}
