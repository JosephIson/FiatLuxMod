package com.josephison.fiatlux.realm;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Travel between Heaven, the created worlds and the Underworld, and the physical laws of each world. */
public final class RealmTravel {
    private static final net.minecraft.resources.ResourceLocation GRAVITY_ID = FiatLux.id("world_gravity");
    private static final net.minecraft.resources.ResourceLocation FALL_ID = FiatLux.id("world_safe_fall");

    private RealmTravel() {}

    /**
     * Lands the player on safe, solid ground near (x, z): searches outward in a spiral for a column
     * whose surface is a solid block with no fluid on it (not the lava of a crater lake or the sea).
     * Falls back to the plain surface at (x, z). Returns false if there is no ground at all.
     */
    public static boolean arrive(ServerPlayer player, ServerLevel level, int x, int z) {
        for (int r = 0; r <= 96; r += 6) {
            int steps = r == 0 ? 1 : 16;
            for (int k = 0; k < steps; k++) {
                double a = Math.PI * 2 * k / steps;
                int px = x + (int) Math.round(Math.cos(a) * r), pz = z + (int) Math.round(Math.sin(a) * r);
                level.getChunk(px >> 4, pz >> 4);                     // generate the landing site
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
                if (y <= level.getMinBuildHeight() + 1) continue;
                net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(px, y - 1, pz);
                var state = level.getBlockState(ground);
                if (state.getFluidState().isEmpty() && state.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                        && !state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) {
                    land(player, level, px, y, pz);
                    return true;
                }
            }
        }
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (y <= level.getMinBuildHeight() + 1) return false;
        land(player, level, x, y, z);
        return true;
    }

    private static void land(ServerPlayer player, ServerLevel level, int x, int y, int z) {
        player.teleportTo(level, x + 0.5, y + 0.1, z + 0.5, player.getYRot(), 0f);
        player.fallDistance = 0;
    }

    public static boolean toPlanet(ServerPlayer player, int slot) {
        RealmPool.Settings s = RealmPool.get(slot);
        ServerLevel level = player.server.getLevel(RealmPool.dimension(slot));
        if (s == null || level == null) {
            player.displayClientMessage(Component.translatable("message.fiatlux.world_unformed"), true);
            return false;
        }
        boolean ok = arrive(player, level, 0, 0);
        if (ok) player.displayClientMessage(Component.translatable("message.fiatlux.arrived",
                s.name(), Component.translatable("planet_type.fiatlux." + s.type().getSerializedName())), true);
        return ok;
    }

    // ------------------------------------------------------------------ the Void

    private static final String VOID_FLIGHT = "fiatlux_void_flight";

    /** Out of Heaven into the Void: before Heaven's Gate, facing the greater light. */
    public static boolean toVoid(ServerPlayer player) {
        ServerLevel the_void = player.server.getLevel(FiatLuxDimensions.VOID);
        if (the_void == null) return false;
        net.minecraft.world.phys.Vec3 at = com.josephison.fiatlux.world.VoidSpace.arrival();
        the_void.getChunk(net.minecraft.core.BlockPos.containing(at));
        player.teleportTo(the_void, at.x, at.y, at.z, 0f, 0f);
        player.fallDistance = 0;
        grantVoidFlight(player);
        boolean formed = com.josephison.fiatlux.world.CosmicExpanseSavedData.get(player.server).hasBigBang();
        player.displayClientMessage(Component.translatable(formed ? "message.fiatlux.void_expanse" : "message.fiatlux.void_formless"), true);
        return true;
    }

    /** Back through Heaven's Gate: onto the street before the southern gate of pearl, facing the crossing. */
    public static boolean voidToHeaven(ServerPlayer player) {
        ServerLevel heaven = player.server.getLevel(FiatLuxDimensions.HEAVEN);
        if (heaven == null) return false;
        int street = heaven.getChunkSource().getGenerator() instanceof com.josephison.fiatlux.world.HeavenChunkGenerator g ? g.streetLevel() : 120;
        player.setPortalCooldown();
        player.teleportTo(heaven, 0.5, street + 1, com.josephison.fiatlux.world.HeavenChunkGenerator.GATE_DISTANCE - 3.5, 180f, 0f);
        player.fallDistance = 0;
        return true;
    }

    /** Rising out of a created world's atmosphere: back into the Void beside its body. */
    public static boolean worldToVoid(ServerPlayer player, int slot) {
        ServerLevel the_void = player.server.getLevel(FiatLuxDimensions.VOID);
        var planets = com.josephison.fiatlux.world.CosmicExpanseSavedData.get(player.server).planets;
        if (the_void == null || slot < 0 || slot >= planets.size()) return false;
        var planet = planets.get(slot);
        net.minecraft.world.phys.Vec3 at = com.josephison.fiatlux.world.VoidSpace.leavingWorld(planet);
        float yaw = com.josephison.fiatlux.world.VoidSpace.yawToward(at, com.josephison.fiatlux.world.VoidSpace.planetPos(planet));
        player.teleportTo(the_void, at.x, at.y, at.z, yaw, 0f);
        player.fallDistance = 0;
        grantVoidFlight(player);
        player.displayClientMessage(Component.translatable("message.fiatlux.left_world", planet.name), true);
        return true;
    }

    /** Weightless flight in the Void, granted on arrival and taken back on leaving. */
    private static void grantVoidFlight(ServerPlayer player) {
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.getPersistentData().putBoolean(VOID_FLIGHT, true);
        }
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    /** Called on every change of dimension: take back the Void's flight once the player has left it. */
    public static void onChangedDimension(ServerPlayer player) {
        if (player.level().dimension() == FiatLuxDimensions.VOID) {
            grantVoidFlight(player);
        } else if (player.getPersistentData().getBoolean(VOID_FLIGHT)) {
            player.getPersistentData().remove(VOID_FLIGHT);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        applyGravity(player);
    }

    /**
     * Per-tick passage between realms by flight: in the Void, flying into Heaven's Gate returns to
     * Heaven and flying into a world's body descends onto it; on a world, rising far above its
     * highest ground carries one back out into the Void; in Heaven, falling below the islands drops
     * one into the Void beneath.
     */
    public static void tickPassage(ServerPlayer player) {
        if (player.isSpectator() || player.isOnPortalCooldown()) return;
        var dim = player.level().dimension();
        net.minecraft.world.phys.Vec3 c = player.getBoundingBox().getCenter();
        if (dim == FiatLuxDimensions.VOID) {
            if (com.josephison.fiatlux.world.VoidSpace.inGate(c)) {
                voidToHeaven(player);
                return;
            }
            var cosmos = com.josephison.fiatlux.world.CosmicExpanseSavedData.get(player.server);
            int i = com.josephison.fiatlux.world.VoidSpace.planetAt(cosmos.planets, c, 1.5);
            if (i >= 0 && i < RealmPool.SIZE && RealmPool.get(i) != null) {
                player.setPortalCooldown();
                if (toPlanet(player, i)) {
                    com.josephison.fiatlux.network.Fx.send(player.serverLevel(), com.josephison.fiatlux.network.FxKind.GLORY_FLASH,
                            player.position().add(0, 1, 0), 8f, cosmos.planets.get(i).atmosphere | 0xFF000000, 30);
                }
                return;
            }
            if (c.y < player.level().getMinBuildHeight() - 24) {   // sunk beneath the deep: drawn back up to the gate
                net.minecraft.world.phys.Vec3 at = com.josephison.fiatlux.world.VoidSpace.arrival();
                player.teleportTo(player.serverLevel(), at.x, at.y, at.z, 0f, 0f);
            }
            return;
        }
        int slot = RealmPool.slotOf(dim);
        if (slot >= 0 && c.y > player.level().getMaxBuildHeight() + 16) {
            player.setPortalCooldown();
            worldToVoid(player, slot);
            return;
        }
        if (dim == FiatLuxDimensions.HEAVEN && c.y < player.level().getMinBuildHeight() + 8) {
            player.setPortalCooldown();
            toVoid(player);   // fallen from Heaven's islands into the deep beneath them
        }
    }

    public static boolean toUnderworld(ServerPlayer player) {
        ServerLevel level = player.server.getLevel(FiatLuxDimensions.UNDERWORLD);
        return level != null && arrive(player, level, 8, 8);
    }

    /** The next created world after the one the player stands on (or the first), or -1. */
    public static int nextSlot(ResourceKey<Level> current) {
        int from = RealmPool.slotOf(current);
        for (int i = 1; i <= RealmPool.SIZE; i++) {
            int s = Math.floorMod(from + i, RealmPool.SIZE);
            if (RealmPool.get(s) != null) return s;
        }
        return -1;
    }

    /** Applies the world's gravity (and a matching safe fall distance) to a living being, or clears it. */
    public static void applyGravity(LivingEntity entity) {
        if (entity.level().dimension() == FiatLuxDimensions.VOID) {   // weightless in the deep
            AttributeInstance g0 = entity.getAttribute(Attributes.GRAVITY);
            AttributeInstance f0 = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
            if (g0 != null) g0.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (f0 != null) f0.addOrUpdateTransientModifier(new AttributeModifier(FALL_ID, 1000.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            return;
        }
        int slot = RealmPool.slotOf(entity.level().dimension());
        RealmPool.Settings s = RealmPool.get(slot);
        AttributeInstance g = entity.getAttribute(Attributes.GRAVITY);
        AttributeInstance fall = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (g == null) return;
        if (s == null || s.type() == null) {
            g.removeModifier(GRAVITY_ID);
            if (fall != null) fall.removeModifier(FALL_ID);
            return;
        }
        double gravity = Math.max(0.1, s.gravity());
        g.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY_ID, gravity - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (fall != null) {
            fall.addOrUpdateTransientModifier(new AttributeModifier(FALL_ID, 1.0 / gravity - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
