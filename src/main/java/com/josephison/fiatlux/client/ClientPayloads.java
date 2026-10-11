package com.josephison.fiatlux.client;

import com.josephison.fiatlux.client.fx.FxManager;
import com.josephison.fiatlux.network.FxPayload;
import com.josephison.fiatlux.network.SyncDivinityPayload;

/** Clientbound payload handlers (only ever loaded on the physical client). */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void onSync(SyncDivinityPayload p) {
        ClientDivinity.set(p);
    }

    public static void onCosmos(com.josephison.fiatlux.network.SyncCosmosPayload p) {
        ClientCosmos.set(p);
    }

    public static void onFx(FxPayload p) {
        FxManager.handle(p);
    }

    public static void onLineage(com.josephison.fiatlux.network.LineagePayloads.View view) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof com.josephison.fiatlux.client.screen.GenerationsScreen screen) screen.show(view);
        else mc.setScreen(new com.josephison.fiatlux.client.screen.GenerationsScreen(view));
    }
}
