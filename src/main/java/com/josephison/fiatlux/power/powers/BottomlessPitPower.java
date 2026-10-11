package com.josephison.fiatlux.power.powers;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.VoidSpace;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.BlackHole;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * "And he opened the bottomless pit; and there arose a smoke out of the pit, as the smoke of a great
 * furnace" (Revelation 9:2). In the Void, collapses the point the Creator gazes at into a black hole
 * whose pull draws in all that strays near, and whose horizon opens on the Underworld.
 */
public class BottomlessPitPower extends AbstractDivinePower {
    public static final double DISTANCE = 48.0;

    public BottomlessPitPower() {
        super(300f, 3, 0xFF8A3AFF);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        var server = ctx.level().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        if (ctx.level().dimension() != FiatLuxDimensions.VOID || !cosmos.hasBigBang()) {
            ctx.caster().displayClientMessage(Component.translatable("power.fiatlux.bottomless_pit.only_void"), true);
            return false;
        }
        Vec3 at = ctx.caster().getEyePosition().add(ctx.caster().getLookAngle().scale(DISTANCE));
        return open(server, ctx.caster().getGameProfile().getName(), at, 6f) != null;
    }

    /** Opens a black hole at a point in the Void (shared with /fiatlux blackhole create). */
    public static BlackHole open(net.minecraft.server.MinecraftServer server, String actor, Vec3 at, float radius) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        if (VoidSpace.inGate(at) || at.distanceTo(VoidSpace.GATE) < radius * VoidSpace.PULL_REACH * 0.5) return null;   // never at Heaven's Gate
        BlackHole hole = new BlackHole("Abyss " + (cosmos.blackHoles.size() + 1), at.x, at.y, at.z, radius);
        cosmos.blackHoles.add(hole);
        cosmos.setDirty();
        var level = server.getLevel(FiatLuxDimensions.VOID);
        if (level != null) Fx.send(level, FxKind.SKY_FRACTURE, at, radius, 0xFF8A3AFF, 60);
        CosmosData.get(server).record(server, actor, "black_hole", "opened a bottomless pit in the deep: " + hole.name() + ".");
        WorldTicker.syncAll(server);
        return hole;
    }

    @Override
    public int glory() {
        return 0;
    }
}
