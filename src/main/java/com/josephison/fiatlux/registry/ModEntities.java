package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.CherubEntity;
import com.josephison.fiatlux.entity.OphanEntity;
import com.josephison.fiatlux.entity.SeraphEntity;
import com.josephison.fiatlux.entity.StationedSwordEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, FiatLux.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AngelEntity>> ANGEL = ENTITIES.register("angel",
            () -> EntityType.Builder.of(AngelEntity::new, MobCategory.CREATURE).sized(0.7f, 2.4f).fireImmune()
                    .clientTrackingRange(10).build(FiatLux.id("angel").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SeraphEntity>> SERAPH = ENTITIES.register("seraph",
            () -> EntityType.Builder.of(SeraphEntity::new, MobCategory.CREATURE).sized(1.4f, 3.0f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("seraph").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CherubEntity>> CHERUB = ENTITIES.register("cherub",
            () -> EntityType.Builder.of(CherubEntity::new, MobCategory.CREATURE).sized(1.6f, 4.2f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("cherub").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<OphanEntity>> OPHAN = ENTITIES.register("ophan",
            () -> EntityType.Builder.of(OphanEntity::new, MobCategory.CREATURE).sized(2.4f, 3.4f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("ophan").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<StationedSwordEntity>> STATIONED_SWORD = ENTITIES.register("stationed_flaming_sword",
            () -> EntityType.Builder.<StationedSwordEntity>of(StationedSwordEntity::new, MobCategory.MISC).sized(0.8f, 2.6f).fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build(FiatLux.id("stationed_flaming_sword").toString()));

    private ModEntities() {}
}
