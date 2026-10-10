package com.josephison.fiatlux;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.CherubEntity;
import com.josephison.fiatlux.entity.OphanEntity;
import com.josephison.fiatlux.entity.SeraphEntity;
import com.josephison.fiatlux.network.ModNetwork;
import com.josephison.fiatlux.power.ModPowers;
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
        ModDataComponents.COMPONENTS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        ModPowers.POWERS.register(modBus);

        modBus.addListener(ModNetwork::register);
        modBus.addListener(FiatLux::attributes);

        container.registerConfig(ModConfig.Type.COMMON, FiatLuxConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, FiatLuxConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, FiatLuxConfig.SERVER_SPEC);
        LOGGER.info("Fiat lux - let there be light.");
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGEL.get(), AngelEntity.createAttributes().build());
        event.put(ModEntities.SERAPH.get(), SeraphEntity.createAttributes().build());
        event.put(ModEntities.CHERUB.get(), CherubEntity.createAttributes().build());
        event.put(ModEntities.OPHAN.get(), OphanEntity.createAttributes().build());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
