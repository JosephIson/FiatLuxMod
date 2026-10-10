package com.josephison.fiatlux.power;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.divinity.DivineState;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative casting pipeline: validate -> resolve gaze -> pay -> act -> glorify -> sync. */
public final class PowerCasting {
    private PowerCasting() {}

    public enum Result { CAST, UNKNOWN, PILGRIM, LOCKED, EXHAUSTED, FAILED }

    public static Result cast(ServerPlayer player, ResourceLocation id) {
        AbstractDivinePower power = PowerRegistry.REGISTRY.get(id);
        if (power == null) {
            return Result.UNKNOWN;
        }
        DivineState state = Divinity.get(player);
        CosmosData cosmos = CosmosData.get(player.server);
        if (state.pilgrim() || cosmos.playMode() == PlayMode.MORTAL_PILGRIM && !player.hasPermissions(FiatLuxConfig.SERVER.godPermissionLevel.get())) {
            player.displayClientMessage(Component.translatable("message.fiatlux.pilgrim"), true);
            return Result.PILGRIM;
        }
        if (cosmos.playMode() == PlayMode.ASCENSION && power.tier() > cosmos.gloryTier()) {
            player.displayClientMessage(Component.translatable("message.fiatlux.locked", power.name(), power.tier()), true);
            return Result.LOCKED;
        }
        float cost = power.cost() * costMultiplier(cosmos.playMode());
        if (state.essence() < cost) {
            player.displayClientMessage(Component.translatable("message.fiatlux.exhausted"), true);
            return Result.EXHAUSTED;
        }
        PowerContext ctx = resolve(player, power.reach());
        if (!power.cast(ctx)) {
            return Result.FAILED;
        }
        Divinity.set(player, Divinity.get(player).withEssence(state.essence() - cost));
        cosmos.addGlory(power.glory());
        Divinity.sync(player);
        return Result.CAST;
    }

    public static float costMultiplier(PlayMode mode) {
        return switch (mode) {
            case UNBOUND -> FiatLuxConfig.COMMON.unboundCostMultiplier.get().floatValue();
            case ASCENSION, MORTAL_PILGRIM -> FiatLuxConfig.COMMON.ascensionCostMultiplier.get().floatValue();
        };
    }

    /** Ray-traces the caster's gaze for blocks and entities up to {@code reach}. */
    public static PowerContext resolve(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        Vec3 end = eye.add(look.scale(reach));
        HitResult blockHit = player.pick(reach, 1f, false);
        double blockDist = blockHit.getType() == HitResult.Type.MISS ? reach * reach : blockHit.getLocation().distanceToSqr(eye);
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> !e.isSpectator() && e.isPickable() && e != player, blockDist);
        HitResult hit = entityHit != null ? entityHit : blockHit;
        Entity target = entityHit != null ? entityHit.getEntity() : null;
        Vec3 pos = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        BlockPos block = hit instanceof BlockHitResult bhr && hit.getType() != HitResult.Type.MISS
                ? bhr.getBlockPos() : BlockPos.containing(pos);
        return new PowerContext(player, player.serverLevel(), hit, pos, block, target);
    }
}
