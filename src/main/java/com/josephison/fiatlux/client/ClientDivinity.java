package com.josephison.fiatlux.client;

import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.divinity.DivineState;
import com.josephison.fiatlux.network.SyncDivinityPayload;

/** The latest divine state the server sent this client. */
public final class ClientDivinity {
    private static SyncDivinityPayload last = new SyncDivinityPayload(DivineState.DEFAULT, PlayMode.UNBOUND, 0, 0, 1000f);

    private ClientDivinity() {}

    public static void set(SyncDivinityPayload p) {
        last = p;
    }

    public static SyncDivinityPayload get() {
        return last;
    }
}
