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

    public static void onFx(FxPayload p) {
        FxManager.handle(p);
    }
}
