package com.josephison.fiatlux.world;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Divinity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Moving between Heaven and the created worlds. */
public final class HeavenTravel {
    private HeavenTravel() {}

    public static boolean inHeaven(ServerPlayer player) {
        return player.level().dimension() == FiatLuxDimensions.HEAVEN;
    }

    public static void toggle(ServerPlayer player) {
        if (inHeaven(player)) {
            toEarth(player);
        } else {
            toHeaven(player);
        }
    }

    public static boolean toHeaven(ServerPlayer player) {
        ServerLevel heaven = player.server.getLevel(FiatLuxDimensions.HEAVEN);
        if (heaven == null) return false;
        if (Divinity.get(player).pilgrim() && !FiatLuxConfig.SERVER.pilgrimsCanEnterHeaven.get()) {
            player.displayClientMessage(Component.translatable("message.fiatlux.heaven_closed"), true);
            return false;
        }
        int street = heaven.getChunkSource().getGenerator() instanceof HeavenChunkGenerator g ? g.streetLevel() : 120;
        // origin is the crossing of two golden streets
        player.teleportTo(heaven, 0.5, street + 1, 0.5, player.getYRot(), 0f);
        player.fallDistance = 0;
        return true;
    }

    public static void toEarth(ServerPlayer player) {
        ServerLevel target = player.server.getLevel(player.getRespawnDimension());
        BlockPos pos = player.getRespawnPosition();
        if (target == null || pos == null || target.dimension() == FiatLuxDimensions.HEAVEN) {
            target = player.server.overworld();
            pos = target.getSharedSpawnPos();
            pos = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        }
        player.teleportTo(target, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, player.getYRot(), 0f);
        player.fallDistance = 0;
    }
}
