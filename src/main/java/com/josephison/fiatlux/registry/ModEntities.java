package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.CherubEntity;
import com.josephison.fiatlux.entity.DominionEntity;
import com.josephison.fiatlux.entity.VirtueEntity;
import com.josephison.fiatlux.entity.PowerEntity;
import com.josephison.fiatlux.entity.PrincipalityEntity;
import com.josephison.fiatlux.entity.ArchangelEntity;
import com.josephison.fiatlux.entity.FallenEntity;
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
    public static final DeferredHolder<EntityType<?>, EntityType<DominionEntity>> DOMINION = ENTITIES.register("dominion",
            () -> EntityType.Builder.of(DominionEntity::new, MobCategory.CREATURE).sized(0.8f, 2.7f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("dominion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VirtueEntity>> VIRTUE = ENTITIES.register("virtue",
            () -> EntityType.Builder.of(VirtueEntity::new, MobCategory.CREATURE).sized(0.8f, 2.7f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("virtue").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<PowerEntity>> POWER = ENTITIES.register("power",
            () -> EntityType.Builder.of(PowerEntity::new, MobCategory.CREATURE).sized(0.9f, 2.8f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("power").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<PrincipalityEntity>> PRINCIPALITY = ENTITIES.register("principality",
            () -> EntityType.Builder.of(PrincipalityEntity::new, MobCategory.CREATURE).sized(0.8f, 2.7f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("principality").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ArchangelEntity>> ARCHANGEL = ENTITIES.register("archangel",
            () -> EntityType.Builder.of(ArchangelEntity::new, MobCategory.CREATURE).sized(0.9f, 2.8f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("archangel").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.entity.MortalEntity>> MORTAL = ENTITIES.register("mortal",
            () -> EntityType.Builder.of(com.josephison.fiatlux.entity.MortalEntity::new, MobCategory.CREATURE).sized(0.6f, 1.85f)
                    .clientTrackingRange(10).build(FiatLux.id("mortal").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.entity.SoulEntity>> SOUL = ENTITIES.register("soul",
            () -> EntityType.Builder.of(com.josephison.fiatlux.entity.SoulEntity::new, MobCategory.MISC).sized(0.6f, 1.85f).fireImmune()
                    .clientTrackingRange(10).build(FiatLux.id("soul").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.eden.SerpentEntity>> SERPENT = ENTITIES.register("serpent",
            () -> EntityType.Builder.of(com.josephison.fiatlux.eden.SerpentEntity::new, MobCategory.MISC).sized(1.0f, 1.3f).fireImmune()
                    .clientTrackingRange(10).build(FiatLux.id("serpent").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.entity.PunishmentPropEntity>> PUNISHMENT_PROP =
            ENTITIES.register("punishment_prop", () -> EntityType.Builder.<com.josephison.fiatlux.entity.PunishmentPropEntity>of(
                    com.josephison.fiatlux.entity.PunishmentPropEntity::new, MobCategory.MISC).sized(1.3f, 3.6f).fireImmune()
                    .clientTrackingRange(10).updateInterval(40).build(FiatLux.id("punishment_prop").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<FallenEntity>> FALLEN = ENTITIES.register("fallen",
            () -> EntityType.Builder.of(FallenEntity::new, MobCategory.MONSTER).sized(0.8f, 2.4f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("fallen").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<StationedSwordEntity>> STATIONED_SWORD = ENTITIES.register("stationed_flaming_sword",
            () -> EntityType.Builder.<StationedSwordEntity>of(StationedSwordEntity::new, MobCategory.MISC).sized(0.8f, 2.6f).fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build(FiatLux.id("stationed_flaming_sword").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.flood.WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(com.josephison.fiatlux.flood.WatcherEntity::new, MobCategory.MISC).sized(0.7f, 2.2f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("watcher").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.flood.BirdEntity>> BIRD = ENTITIES.register("ark_bird",
            () -> EntityType.Builder.of(com.josephison.fiatlux.flood.BirdEntity::new, MobCategory.MISC).sized(0.5f, 0.5f).fireImmune()
                    .clientTrackingRange(12).updateInterval(2).build(FiatLux.id("ark_bird").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.infernal.RiftEntity>> RIFT = ENTITIES.register("rift",
            () -> EntityType.Builder.<com.josephison.fiatlux.infernal.RiftEntity>of(com.josephison.fiatlux.infernal.RiftEntity::new, MobCategory.MISC)
                    .sized(1.2f, 4.8f).fireImmune().clientTrackingRange(12).updateInterval(20).build(FiatLux.id("rift").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.infernal.SpiritEntity>> SPIRIT = ENTITIES.register("unclean_spirit",
            () -> EntityType.Builder.of(com.josephison.fiatlux.infernal.SpiritEntity::new, MobCategory.MISC).sized(0.7f, 2.0f).fireImmune()
                    .clientTrackingRange(10).updateInterval(2).build(FiatLux.id("unclean_spirit").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.josephison.fiatlux.infernal.GuardianEntity>> GUARDIAN = ENTITIES.register("guardian_angel",
            () -> EntityType.Builder.of(com.josephison.fiatlux.infernal.GuardianEntity::new, MobCategory.MISC).sized(0.7f, 2.2f).fireImmune()
                    .clientTrackingRange(12).build(FiatLux.id("guardian_angel").toString()));

    private ModEntities() {}
}
