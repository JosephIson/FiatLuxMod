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

    /** Lands the player on solid ground near (x, z) in the given level. Returns false if there is no ground. */
    public static boolean arrive(ServerPlayer player, ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);                                   // generate the landing site
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (y <= level.getMinBuildHeight() + 1) return false;
        player.teleportTo(level, x + 0.5, y + 0.1, z + 0.5, player.getYRot(), 0f);
        player.fallDistance = 0;
        return true;
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
