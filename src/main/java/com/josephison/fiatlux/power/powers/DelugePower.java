package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.DivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * "the same day were all the fountains of the great deep broken up, and the windows of heaven were
 * opened." (Genesis 7:11) Floods the land around the gaze up to a rising waterline, column by
 * column through the creation queue, under a sky of storm.
 */
public class DelugePower extends DivinePower {
    private static final int RADIUS = 28;
    private static final int RISE = 6;

    public DelugePower() {
        super(300f, 3, 0xFF4F8CFF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        ServerLevel level = ctx.level();
        BlockPos c = ctx.targetBlock();
        int waterline = c.getY() + RISE;
        BlockState water = Blocks.WATER.defaultBlockState();
        // lazily walk columns from the centre outward so the flood visibly spreads
        Iterator<EditQueue.Change> it = new Iterator<>() {
            int ring = 0, i = 0, y = Integer.MIN_VALUE, x, z;
            int[][] cols = columnsOfRing(0);

            @Override
            public boolean hasNext() {
                while (true) {
                    if (y != Integer.MIN_VALUE && y <= waterline) return true;
                    if (i >= cols.length) {
                        if (++ring > RADIUS) return false;
                        cols = columnsOfRing(ring);
                        i = 0;
                        continue;
                    }
                    x = c.getX() + cols[i][0];
                    z = c.getZ() + cols[i][1];
                    i++;
                    y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                }
            }

            @Override
            public EditQueue.Change next() {
                if (!hasNext()) throw new NoSuchElementException();
                BlockPos p = new BlockPos(x, y++, z);
                return new EditQueue.Change(p, s -> s.isAir() || (s.canBeReplaced() && !s.liquid()) ? water : null);
            }
        };
        EditQueue.submit(ctx.caster().getUUID(), level, it, true, null);
        level.setWeatherParameters(0, 20 * 60 * 5, true, true);
        Fx.send(level, FxKind.DELUGE, ctx.target(), RADIUS, color(), 200);
        CosmosData.get(level.getServer()).record(level.getServer(), ctx.caster().getGameProfile().getName(), "flood",
                "opened the windows of heaven, and the waters prevailed upon the earth.");
        return true;
    }

    private static int[][] columnsOfRing(int r) {
        if (r == 0) return new int[][]{{0, 0}};
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= r * r && d2 > (r - 1) * (r - 1)) out.add(new int[]{dx, dz});
            }
        }
        return out.toArray(new int[0][]);
    }

    @Override
    public int glory() {
        return 0;
    }
}
