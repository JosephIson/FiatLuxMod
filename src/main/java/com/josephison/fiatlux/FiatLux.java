package com.josephison.fiatlux;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.CherubEntity;
import com.josephison.fiatlux.entity.OphanEntity;
import com.josephison.fiatlux.entity.SeraphEntity;
import com.josephison.fiatlux.network.ModNetwork;
import com.josephison.fiatlux.power.PowerRegistry;
import com.josephison.fiatlux.registry.ModAttachments;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.registry.ModCreativeTabs;
import com.josephison.fiatlux.registry.ModDataComponents;
import com.josephison.fiatlux.registry.ModEffects;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModItems;
import com.josephison.fiatlux.registry.ModParticles;
import com.josephison.fiatlux.registry.ModSounds;
import com.josephison.fiatlux.registry.ModWorldgen;
import com.josephison.fiatlux.weapon.DivineWeaponRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

/**
 * FIAT LUX - "Let there be light." A sandbox where the player is the Creator.
 */
@Mod(FiatLux.MODID)
public class FiatLux {
    public static final String MODID = "fiatlux";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FiatLux(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        DivineWeaponRegistry.WEAPONS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        PowerRegistry.POWERS.register(modBus);

        modBus.addListener(ModNetwork::register);
        modBus.addListener(FiatLux::attributes);
        modBus.addListener(FiatLux::spawnPlacements);

        container.registerConfig(ModConfig.Type.COMMON, FiatLuxConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, FiatLuxConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, FiatLuxConfig.SERVER_SPEC);
        LOGGER.info("Fiat lux - let there be light.");
    }

    /** The Fallen haunt the dark places of the Underworld (spawners in worldgen/biome/underworld.json). */
    private static void spawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.FALLEN.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGEL.get(), AngelEntity.createAttributes().build());
        event.put(ModEntities.SERAPH.get(), SeraphEntity.createAttributes().build());
        event.put(ModEntities.CHERUB.get(), CherubEntity.createAttributes().build());
        event.put(ModEntities.OPHAN.get(), OphanEntity.createAttributes().build());
        event.put(ModEntities.DOMINION.get(), com.josephison.fiatlux.entity.DominionEntity.createAttributes().build());
        event.put(ModEntities.VIRTUE.get(), com.josephison.fiatlux.entity.VirtueEntity.createAttributes().build());
        event.put(ModEntities.POWER.get(), com.josephison.fiatlux.entity.PowerEntity.createAttributes().build());
        event.put(ModEntities.PRINCIPALITY.get(), com.josephison.fiatlux.entity.PrincipalityEntity.createAttributes().build());
        event.put(ModEntities.ARCHANGEL.get(), com.josephison.fiatlux.entity.ArchangelEntity.createAttributes().build());
        event.put(ModEntities.MORTAL.get(), com.josephison.fiatlux.entity.MortalEntity.createAttributes().build());
        event.put(ModEntities.SERPENT.get(), com.josephison.fiatlux.eden.SerpentEntity.createAttributes().build());
        event.put(ModEntities.SOUL.get(), com.josephison.fiatlux.entity.SoulEntity.createAttributes().build());
        event.put(ModEntities.FALLEN.get(), com.josephison.fiatlux.entity.FallenEntity.createAttributes().build());
        event.put(ModEntities.WATCHER.get(), AngelEntity.createAttributes().build());
        event.put(ModEntities.BIRD.get(), com.josephison.fiatlux.flood.BirdEntity.createAttributes().build());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
