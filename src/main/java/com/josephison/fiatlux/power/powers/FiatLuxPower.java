package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * "And God said, Let there be light: and there was light." (Genesis 1:3)
 * Fills every dark pocket within the radius with unseen light, with a burst of radiance.
 */
public class FiatLuxPower extends AbstractDivinePower {
    private static final int RADIUS = 14;
    private static final int SPACING = 4;

    public FiatLuxPower() {
        super(20f, 0, 0xFFFFF4C8);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        // in the formless Void, before anything is, "Let there be light" is the beginning of all things
        if (ctx.level().dimension() == com.josephison.fiatlux.world.FiatLuxDimensions.VOID
                && !com.josephison.fiatlux.world.CosmicExpanseSavedData.get(ctx.level().getServer()).hasBigBang()) {
            return CosmicSparkPower.beginning(ctx.level().getServer(), ctx.caster().getGameProfile().getName());
        }
        BlockPos c = ctx.targetBlock();
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        List<EditQueue.Change> changes = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x += SPACING) {
            for (int y = -RADIUS; y <= RADIUS; y += SPACING) {
                for (int z = -RADIUS; z <= RADIUS; z += SPACING) {
                    if (x * x + y * y + z * z > RADIUS * RADIUS) continue;
                    BlockPos p = c.offset(x, y, z);
                    if (ctx.level().getBrightness(LightLayer.BLOCK, p) >= 10 && ctx.level().canSeeSky(p)) continue;
                    changes.add(new EditQueue.Change(p, s -> s.isAir() ? light : null));
                }
            }
        }
        EditQueue.submit(ctx.caster().getUUID(), ctx.level(), changes.iterator(), true, null);
        Fx.send(ctx.level(), FxKind.FIAT_LUX, ctx.target(), RADIUS, color(), 40);
        CosmosData cosmos = CosmosData.get(ctx.level().getServer());
        if (cosmos.chronicle().stream().noneMatch(e -> e.kind().equals("fiat_lux"))) {
            cosmos.record(ctx.level().getServer(), ctx.caster().getGameProfile().getName(), "fiat_lux",
                    "said, Let there be light: and there was light.");
        }
        return true;
    }

    @Override
    public int glory() {
        return 2;
    }
}
