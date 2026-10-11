package com.josephison.fiatlux.realm;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.VoidSpace;
import com.josephison.fiatlux.world.cosmos.BlackHole;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The pull of the bottomless pits (Revelation 9:1-2). Every tick, everything within reach of a black
 * hole in the Void is drawn toward it; whatever crosses the event horizon is gone - players fall
 * through into the Underworld, everything else is swallowed. Players in creative or spectator
 * mode are left free to study it.
 */
public final class AbyssPhysics {
    private AbyssPhysics() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(FiatLuxDimensions.VOID);
        if (level == null || level.players().isEmpty()) return;
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        if (cosmos.blackHoles.isEmpty()) return;
        for (BlackHole hole : cosmos.blackHoles) {
            Vec3 c = hole.center();
            double reach = hole.radius() * VoidSpace.PULL_REACH;
            for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(reach), Entity::isAlive)) {
                // the Creator in creative may study the pit unharmed; mortals and survivors are drawn in
                if (e instanceof ServerPlayer p && (p.isSpectator() || p.isCreative() || p.isOnPortalCooldown())) continue;
                Vec3 at = e.getBoundingBox().getCenter();
                if (VoidSpace.beyondHorizon(at, c, hole.radius())) {
                    swallow(level, e, hole);
                    continue;
                }
                Vec3 pull = VoidSpace.pull(at, c, hole.radius());
                if (pull == Vec3.ZERO) continue;
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurtMarked = true;   // players' motion is theirs to simulate: send them the pull
                e.fallDistance = 0;
            }
        }
    }

    private static void swallow(ServerLevel level, Entity e, BlackHole hole) {
        Fx.send(level, FxKind.FALLEN_IMPACT, hole.center(), hole.radius() * 1.5f, 0xFFFF7A2A, 30);
        level.playSound(null, e.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.AMBIENT, 1.2f, 0.4f);
        if (e instanceof ServerPlayer p) {
            p.setPortalCooldown();
            if (RealmTravel.toUnderworld(p)) {
                p.displayClientMessage(Component.translatable("message.fiatlux.fell_into_abyss", hole.name()), false);
                Fx.send(p.serverLevel(), FxKind.FALLEN_IMPACT, p.position(), 6f, 0xFFFF3A1A, 30);
            }
        } else {
            e.discard();   // swallowed: nothing comes back out of the pit
        }
    }
}
