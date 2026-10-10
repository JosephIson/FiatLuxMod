package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** "Let the waters under the heaven be gathered together unto one place, and let the dry land appear" (Genesis 1:9). */
public class RaiseLandPower extends AbstractDivinePower {
    private static final int RADIUS = 9;
    private static final int HEIGHT = 7;

    public RaiseLandPower() {
        super(60f, 1, 0xFF8FC46A);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        ServerLevel level = ctx.level();
        BlockPos c = ctx.targetBlock();
        List<EditQueue.Change> changes = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double d2 = (dx * dx + dz * dz) / (double) (RADIUS * RADIUS);
                if (d2 > 1) continue;
                int h = (int) Math.round(HEIGHT * Math.pow(1 - d2, 1.4));
                if (h <= 0) continue;
                int x = c.getX() + dx, z = c.getZ() + dz;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                BlockState top = level.getBlockState(new BlockPos(x, ground, z));
                BlockState body = top.is(Blocks.SAND) || top.is(Blocks.GRAVEL) ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
                BlockState surface = top.isAir() ? Blocks.GRASS_BLOCK.defaultBlockState() : top.is(Blocks.WATER) ? Blocks.GRASS_BLOCK.defaultBlockState() : top;
                for (int y = 1; y <= h; y++) {
                    BlockState place = y == h ? (surface.is(Blocks.DIRT) ? Blocks.GRASS_BLOCK.defaultBlockState() : surface) : body;
                    changes.add(new EditQueue.Change(new BlockPos(x, ground + y, z),
                            s -> s.isAir() || s.canBeReplaced() || s.is(Blocks.WATER) ? place : null));
                }
                if (!top.is(Blocks.GRASS_BLOCK) && !top.isAir()) {
                    changes.add(new EditQueue.Change(new BlockPos(x, ground, z), s -> s.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState() : null));
                }
            }
        }
        EditQueue.submit(ctx.caster().getUUID(), level, changes.iterator(), true, null);
        Fx.send(level, FxKind.SHOCKWAVE, Vec3.atBottomCenterOf(c.above()), RADIUS, color(), 30);
        return true;
    }

    @Override
    public int glory() {
        return 3;
    }
}
