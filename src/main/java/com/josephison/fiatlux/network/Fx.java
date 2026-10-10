package com.josephison.fiatlux.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side helper to trigger client visual effects for everyone nearby. */
public final class Fx {
    private Fx() {}

    public static void send(ServerLevel level, FxKind kind, Vec3 pos, float radius, int color, int duration) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, Math.max(96.0, radius * 4.0),
                new FxPayload(kind.ordinal(), pos.x, pos.y, pos.z, radius, color, duration));
    }
}
