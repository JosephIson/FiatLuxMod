package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * "the LORD caused the sea to go back by a strong east wind all that night, and made the sea dry
 * land, and the waters were divided ... and the waters were a wall unto them on their right hand,
 * and on their left." (Exodus 14:21-22)
 * Opens a dry corridor through water along the caster's gaze, walled by stilled water.
 */
public class PartWatersPower extends AbstractDivinePower {
    private static final int LENGTH = 64;
    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 48;

    public PartWatersPower() {
        super(120f, 2, 0xFF5FB4FF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        ServerLevel level = ctx.level();
        Vec3 look = ctx.caster().getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1e-4) return false;
        dir = dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 start = ctx.caster().position();
        int top = (int) Math.floor(start.y) + 2;
        BlockState wall = ModBlocks.STILLED_WATER.get().defaultBlockState();
        List<EditQueue.Change> changes = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int s = 0; s < LENGTH; s++) {
            for (int w = -HALF_WIDTH - 1; w <= HALF_WIDTH + 1; w++) {
                Vec3 p = start.add(dir.scale(s)).add(side.scale(w));
                int x = (int) Math.floor(p.x), z = (int) Math.floor(p.z);
                if (!seen.add(BlockPos.asLong(x, 0, z))) continue;
                boolean edge = Math.abs(w) == HALF_WIDTH + 1;
                for (int y = top; y > top - DEPTH && y > level.getMinBuildHeight(); y--) {
                    BlockPos bp = new BlockPos(x, y, z);
                    changes.add(new EditQueue.Change(bp, st -> {
                        boolean wet = st.getFluidState().is(FluidTags.WATER) || st.is(Blocks.KELP) || st.is(Blocks.KELP_PLANT)
                                || st.is(Blocks.SEAGRASS) || st.is(Blocks.TALL_SEAGRASS);
                        if (!wet) return null;
                        return edge ? wall : Blocks.AIR.defaultBlockState();
                    }));
                }
            }
        }
        EditQueue.submit(ctx.caster().getUUID(), level, changes.iterator(), true, null);
        Fx.send(level, FxKind.SHOCKWAVE, start, 12f, color(), 40);
        CosmosData.get(level.getServer()).record(level.getServer(), ctx.caster().getGameProfile().getName(), "exodus",
                "divided the waters, and they went into the midst of the sea upon the dry ground.");
        return true;
    }

    @Override
    public double reach() {
        return 8.0;
    }
}
