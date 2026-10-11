package com.josephison.fiatlux.world;

import com.josephison.fiatlux.realm.RealmTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The veil within a gate of pearl (Revelation 21:21). Beyond it lies the Void, the formless deep
 * where the universe begins: whoever passes through is carried out of Heaven into it.
 */
public class VoidGateBlock extends Block {
    private static final VoxelShape VEIL = Block.box(0, 0, 6, 16, 16, 10);

    public VoidGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VEIL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** One seamless curtain: no faces drawn between neighbouring veil blocks (as with glass). */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ServerPlayer player && !player.isOnPortalCooldown()) {
            player.setPortalCooldown();
            RealmTravel.toVoid(player);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
        }
    }
}
