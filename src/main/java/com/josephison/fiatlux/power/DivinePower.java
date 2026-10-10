package com.josephison.fiatlux.power;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A power the Creator can speak into the world. Powers are registry entries (fiatlux:divine_power)
 * so other mods and datapacks can reference them by id; the cast itself is always server-side.
 */
public abstract class DivinePower {
    private final float cost;
    private final int tier;
    private final int color;

    /**
     * @param cost  Divine Essence spent per cast (before play-mode multipliers)
     * @param tier  Ascension tier required (0 = always available)
     * @param color ARGB accent used by the power wheel and its visual effects
     */
    protected DivinePower(float cost, int tier, int color) {
        this.cost = cost;
        this.tier = tier;
        this.color = color;
    }

    /** Performs the act. Return false to refund (nothing happened). */
    public abstract boolean cast(PowerContext ctx);

    /** Glory the world gains when this power is cast (creation glorifies; destruction does not). */
    public int glory() {
        return 1;
    }

    /** Maximum reach of the caster's gaze for this power. */
    public double reach() {
        return 96.0;
    }

    public float cost() {
        return cost;
    }

    public int tier() {
        return tier;
    }

    public int color() {
        return color;
    }

    public ResourceLocation id() {
        return ModPowers.REGISTRY.getKey(this);
    }

    public Component name() {
        ResourceLocation id = id();
        return Component.translatable("power." + id.getNamespace() + "." + id.getPath());
    }

    public Component description() {
        ResourceLocation id = id();
        return Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
}
