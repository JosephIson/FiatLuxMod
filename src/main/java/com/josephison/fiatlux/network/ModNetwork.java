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
        r.playToClient(com.josephison.fiatlux.portal.PortalPayload.TYPE, com.josephison.fiatlux.portal.PortalPayload.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.portal.PortalViews.receive(p));
        r.playToClient(FloodPayload.TYPE, FloodPayload.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.fx.FloodSea.update(p));
        r.playToClient(com.josephison.fiatlux.cinema.CinemaPayloads.Play.TYPE, com.josephison.fiatlux.cinema.CinemaPayloads.Play.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.cinema.FilmPlayer.play(p.film()));
        r.playToClient(com.josephison.fiatlux.cinema.CinemaPayloads.Live.TYPE, com.josephison.fiatlux.cinema.CinemaPayloads.Live.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.cinema.FilmPlayer.live(p.cue()));
        r.playToServer(com.josephison.fiatlux.cinema.CinemaPayloads.Watch.TYPE, com.josephison.fiatlux.cinema.CinemaPayloads.Watch.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) com.josephison.fiatlux.cinema.Cinema.onWatch(sp, p);
        });
        r.playToClient(SonPayloads.Setup.TYPE, SonPayloads.Setup.STREAM_CODEC,
                (p, ctx) -> com.josephison.fiatlux.client.ClientPayloads.onSonSetup(p));
        r.playToServer(SonPayloads.Choose.TYPE, SonPayloads.Choose.STREAM_CODEC, (p, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || Divinity.get(sp).pilgrim()) return;
            String err = com.josephison.fiatlux.son.TheSon.begin(sp, p.name(), p.variant(), p.town(), p.mother().orElse(null));
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(err != null ? err
                    : "\"Behold, a virgin shall conceive, and bear a son\" - " + p.name() + " is sent. Watch over his mother.")
                    .withStyle(s -> s.withColor(err != null ? 0xE08060 : 0xF5D77A)));
        });
    }
}
