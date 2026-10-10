package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.divinity.Divinity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class ModTags {
    /** Beings the Host and holy weapons treat as corrupt: undead, demons, and the like. */
    public static final TagKey<EntityType<?>> CORRUPT = TagKey.create(Registries.ENTITY_TYPE, FiatLux.id("corrupt"));
    /** The Heavenly Host (never harmed by holy fire). */
    public static final TagKey<EntityType<?>> HEAVENLY_HOST = TagKey.create(Registries.ENTITY_TYPE, FiatLux.id("heavenly_host"));

    private ModTags() {}

    public static boolean isCorrupt(LivingEntity e) {
        return e.getType().is(CORRUPT) || Divinity.alignment(e).isCorrupt();
    }
}
