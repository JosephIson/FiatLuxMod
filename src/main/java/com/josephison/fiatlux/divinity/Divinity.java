package com.josephison.fiatlux.divinity;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.SyncDivinityPayload;
import com.josephison.fiatlux.registry.ModAttachments;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Accessors for divine state and alignment attachments. */
public final class Divinity {
    private Divinity() {}

    public static DivineState get(ServerPlayer player) {
        return player.getData(ModAttachments.DIVINE_STATE);
    }

    public static void set(ServerPlayer player, DivineState state) {
        player.setData(ModAttachments.DIVINE_STATE, state);
    }

    public static Alignment alignment(LivingEntity entity) {
        return entity.getData(ModAttachments.ALIGNMENT);
    }

    public static void setAlignment(LivingEntity entity, Alignment a) {
        entity.setData(ModAttachments.ALIGNMENT, a);
    }

    public static float maxEssence() {
        return FiatLuxConfig.COMMON.maxEssence.get().floatValue();
    }

    /** Sends the HUD state to the player. */
    public static void sync(ServerPlayer player) {
        CosmosData cosmos = CosmosData.get(player.server);
        PacketDistributor.sendToPlayer(player, new SyncDivinityPayload(get(player), cosmos.playMode(), cosmos.glory(),
                cosmos.gloryTier(), maxEssence()));
    }

    /** Regenerates essence; called once per second per player. */
    public static void regenerate(ServerPlayer player) {
        DivineState s = get(player);
        float max = maxEssence();
        if (s.essence() < max && !s.pilgrim()) {
            float next = Math.min(max, s.essence() + FiatLuxConfig.COMMON.essenceRegenPerSecond.get().floatValue());
            set(player, s.withEssence(next));
        }
    }
}
