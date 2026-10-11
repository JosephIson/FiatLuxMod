package com.josephison.fiatlux.network;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.power.PowerRegistry;
import com.josephison.fiatlux.power.PowerCasting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payload registration. Clientbound handlers live in client.ClientPayloads and only load on the client. */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(CastPowerPayload.TYPE, CastPowerPayload.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) PowerCasting.cast(sp, p.power());
        });
        r.playToServer(SelectPowerPayload.TYPE, SelectPowerPayload.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp && PowerRegistry.REGISTRY.containsKey(p.power())) {
                Divinity.set(sp, Divinity.get(sp).withSelectedPower(p.power()));
                Divinity.sync(sp);
            }
        });
        r.playToServer(LineagePayloads.Request.TYPE, LineagePayloads.Request.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, LineagePayloads.view(sp.server, p.person().orElse(null)));
            }
        });
        r.playToClient(LineagePayloads.View.TYPE, LineagePayloads.View.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onLineage(p));
        r.playToClient(CosmosReport.TYPE, CosmosReport.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onCosmosReport(p));
        r.playToClient(SyncDivinityPayload.TYPE, SyncDivinityPayload.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onSync(p));
        r.playToClient(SyncCosmosPayload.TYPE, SyncCosmosPayload.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onCosmos(p));
        r.playToClient(FxPayload.TYPE, FxPayload.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onFx(p));
    }
}
