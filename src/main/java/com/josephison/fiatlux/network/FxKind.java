package com.josephison.fiatlux.network;

/** Visual events the server can ask clients to render. */
public enum FxKind {
    /** "Let there be light": white-gold flash, expanding light ring and rising sparks. */
    FIAT_LUX,
    /** A pillar of celestial fire/lightning descending from heaven. */
    CELESTIAL_PILLAR,
    /** Ground shockwave ring (earthquakes, trumpet blasts, landings). */
    SHOCKWAVE,
    /** A sweeping arc of holy fire (the Flaming Sword's wave). */
    FIRE_WAVE,
    /** Golden blessing motes spiralling up a target. */
    BLESSING,
    /** Heavy rain of light and rising water mist (the Flood). */
    DELUGE,
    /** A screen-filling glory flash for the caster (theophany). */
    GLORY_FLASH,
    /** Feathers and radiance as one of the Host appears. */
    HOST_ARRIVAL,
    /** The Big Bang: blinding white-out, radiating beams, the sky filling with newborn galaxies. */
    BIG_BANG,
    /** A new world forms in orbit: a ring of creation and rising sparks. */
    PLANET_GENESIS;

    public static FxKind byId(int id) {
        FxKind[] v = values();
        return id >= 0 && id < v.length ? v[id] : FIAT_LUX;
    }
}
