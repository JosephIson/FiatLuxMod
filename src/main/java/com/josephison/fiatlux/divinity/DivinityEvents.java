package com.josephison.fiatlux.divinity;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.world.HeavenTravel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-side heartbeat of the divine simulation. */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class DivinityEvents {
    private DivinityEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        EditQueue.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EditQueue.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            Divinity.regenerate(sp);
            Divinity.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        DivineState s = Divinity.get(sp);
        if (!s.visitedHeaven() && FiatLuxConfig.COMMON.startInHeaven.get() && !s.pilgrim()) {
            // "In the beginning" - every Creator first opens their eyes in Heaven
            if (HeavenTravel.toHeaven(sp)) {
                Divinity.set(sp, Divinity.get(sp).withVisitedHeaven(true));
            }
        }
        Divinity.sync(sp);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Divinity.sync(sp);
    }
}
