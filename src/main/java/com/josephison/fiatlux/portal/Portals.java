package com.josephison.fiatlux.portal;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Portals that can be seen through, after the manner of the Immersive Portals mod: an entity (a rift
 * from the Underworld, the door of a TARDIS) carries an opening, and those who look into it see the
 * real blocks of the other place, in perspective, as if through a window. The server captures the far
 * side and sends it to everyone who can see the opening; the client draws it inside the opening only
 * (see client.portal.PortalViews). Crossing is the carrier's business ({@link #cross}).
 *
 * <p>Geometry: an opening stands upright in the plane through its carrier's position, across the
 * carrier's facing. Its local frame is x along the opening, y up, z its facing:
 * {@code facing = (-sin yaw, 0, cos yaw)}, {@code across = (cos yaw, 0, sin yaw)}.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Portals {
    /** The far side of each open portal, kept to send to those who come within sight of it later. */
    private static final Map<Entity, PortalPayload> OPEN = new WeakHashMap<>();
    private static final Map<UUID, Long> CROSSED = new java.util.HashMap<>();

    private Portals() {}

    public static Vec3 facing(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    public static Vec3 across(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(Math.cos(r), 0, Math.sin(r));
    }

    /**
     * Opens the view through `carrier` onto `far` at `anchor` (the far point its opening's base looks
     * out from, facing `farYaw`): the box around it is captured and sent to everyone near.
     */
    public static void open(Entity carrier, ServerLevel far, BlockPos anchor, float farYaw, String shape, int fog, int r, int down, int up) {
        RegionSnapshot snap = RegionSnapshot.capture(far, anchor, r, down, up);
        PortalPayload p = new PortalPayload(carrier.getId(), shape, anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, farYaw, fog, snap);
        OPEN.put(carrier, p);
        PacketDistributor.sendToPlayersTrackingEntity(carrier, p);
    }

    public static boolean isOpen(Entity carrier) {
        return OPEN.containsKey(carrier);
    }

    public static void close(Entity carrier) {
        OPEN.remove(carrier);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        PortalPayload p = OPEN.get(event.getTarget());
        if (p != null && event.getEntity() instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, p);
    }

    /**
     * Carries a player who steps through the opening of `carrier` (half-width `half`, height `height`)
     * to the same place relative to the far opening (at `farBase`, facing `farYaw`) in `far`, coming out
     * on its far side. Returns whether anyone crossed.
     */
    public static boolean cross(Entity carrier, double half, double height, ServerLevel far, Vec3 farBase, float farYaw) {
        if (!(carrier.level() instanceof ServerLevel here)) return false;
        long now = here.getServer().overworld().getGameTime();
        boolean any = false;
        Vec3 f = facing(carrier.getYRot()), a = across(carrier.getYRot());
        Vec3 ff = facing(farYaw), fa = across(farYaw);
        for (ServerPlayer p : here.getEntitiesOfClass(ServerPlayer.class, carrier.getBoundingBox().inflate(half + 1, 1, half + 1))) {
            if (now - CROSSED.getOrDefault(p.getUUID(), -100L) < 40) continue;
            Vec3 d = p.position().subtract(carrier.position());
            double lx = d.dot(a), ly = d.y, lz = d.dot(f);
            Vec3 v = p.getDeltaMovement();
            double toward = v.dot(f);
            if (Math.abs(lx) > half || ly < -0.5 || ly > height || Math.abs(lz) > 0.45) continue;
            if (Math.abs(toward) < 0.02 && Math.abs(lz) > 0.2) continue;
            double side = toward != 0 ? -Math.signum(toward) : Math.signum(lz);   // the side they come from
            Vec3 at = farBase.add(fa.scale(lx)).add(0, ly, 0).add(ff.scale(-side * 0.9));
            float yaw = p.getYRot() + (farYaw - carrier.getYRot());
            CROSSED.put(p.getUUID(), now);
            p.teleportTo(far, at.x, at.y, at.z, yaw, p.getXRot());
            p.resetFallDistance();
            any = true;
        }
        return any;
    }
}
