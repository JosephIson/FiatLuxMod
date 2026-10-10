package com.josephison.fiatlux.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Where and by whom a power is cast, resolved server-side from the caster's gaze. */
public record PowerContext(ServerPlayer caster, ServerLevel level, HitResult hit, Vec3 target, BlockPos targetBlock,
                           @Nullable Entity targetEntity) {
    public boolean hitSomething() {
        return hit.getType() != HitResult.Type.MISS;
    }
}
