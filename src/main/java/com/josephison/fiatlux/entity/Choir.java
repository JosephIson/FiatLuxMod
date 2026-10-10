package com.josephison.fiatlux.entity;

import net.minecraft.network.chat.Component;

/**
 * The nine choirs of the celestial hierarchy in three spheres, highest first (Pseudo-Dionysius,
 * Celestial Hierarchy 6-9; Gregory the Great, Homily 34). Scripture names every one of them:
 * seraphim (Isa 6:2), cherubim (Gen 3:24, Ezek 10), thrones (Col 1:16; the ophanim, Ezek 1:15-21),
 * dominions, principalities and powers (Col 1:16, Eph 1:21), virtues/mights (Eph 1:21, 1 Pet 3:22),
 * archangels (1 Thess 4:16, Jude 1:9) and angels.
 *
 * Rank orders the Host: the higher choirs govern the lower (a Dominion strengthens those beneath
 * it), the rebellion musters across every choir, and Michael the archangel leads the Powers.
 */
public enum Choir {
    SERAPHIM("seraphim", 1, "Isaiah 6:2-3"),
    CHERUBIM("cherubim", 1, "Ezekiel 10:14-21"),
    THRONES("thrones", 1, "Colossians 1:16; Ezekiel 1:15-21"),
    DOMINIONS("dominions", 2, "Colossians 1:16"),
    VIRTUES("virtues", 2, "Ephesians 1:21; 1 Peter 3:22"),
    POWERS("powers", 2, "Ephesians 6:12; Colossians 2:15"),
    PRINCIPALITIES("principalities", 3, "Colossians 1:16; Daniel 10:13"),
    ARCHANGELS("archangels", 3, "1 Thessalonians 4:16; Jude 1:9"),
    ANGELS("angels", 3, "Hebrews 1:14");

    private final String id;
    private final int sphere;
    private final String scripture;

    Choir(String id, int sphere, String scripture) {
        this.id = id;
        this.sphere = sphere;
        this.scripture = scripture;
    }

    /** 1 (Seraphim, nearest the throne) .. 9 (Angels, nearest creation). */
    public int rank() {
        return ordinal() + 1;
    }

    /** 1 = counsellors of contemplation, 2 = governors of creation, 3 = messengers to it. */
    public int sphere() {
        return sphere;
    }

    public String id() {
        return id;
    }

    public String scripture() {
        return scripture;
    }

    public boolean outranks(Choir other) {
        return rank() < other.rank();
    }

    public Component displayName() {
        return Component.translatable("choir.fiatlux." + id);
    }

    public Component office() {
        return Component.translatable("choir.fiatlux." + id + ".office");
    }
}
