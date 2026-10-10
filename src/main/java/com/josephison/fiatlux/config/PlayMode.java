package com.josephison.fiatlux.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** How divine power is granted in a world. Chosen per world and changeable at any time. */
public enum PlayMode implements StringRepresentable {
    /** Every power, brush and editor is available immediately; essence costs are scaled by config (default free). */
    UNBOUND("unbound"),
    /** Powers unlock as world Glory rises through creation, faith and realm building. */
    ASCENSION("ascension"),
    /** Multiplayer: players marked as pilgrims are mortals bound to physical law under a God-player. */
    MORTAL_PILGRIM("mortal_pilgrim");

    public static final Codec<PlayMode> CODEC = StringRepresentable.fromEnum(PlayMode::values);
    private final String id;

    PlayMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
