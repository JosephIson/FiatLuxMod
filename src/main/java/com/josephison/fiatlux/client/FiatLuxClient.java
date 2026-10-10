package com.josephison.fiatlux.client;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.client.fx.FxManager;
import com.josephison.fiatlux.client.fx.FxOverlay;
import com.josephison.fiatlux.client.fx.GlowParticle;
import com.josephison.fiatlux.client.fx.HeavenSkyEffects;
import com.josephison.fiatlux.client.render.HostRenderer;
import com.josephison.fiatlux.client.render.StationedSwordRenderer;
import com.josephison.fiatlux.client.screen.PowerWheelScreen;
import com.josephison.fiatlux.network.CastPowerPayload;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModItems;
import com.josephison.fiatlux.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client entry point: renderers, particles, sky, HUD, power wheel and live config screen. */
@Mod(value = FiatLux.MODID, dist = Dist.CLIENT)
public class FiatLuxClient {
    public FiatLuxClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(FiatLuxClient::renderers);
        modBus.addListener(FiatLuxClient::particles);
        modBus.addListener(FiatLuxClient::keys);
        modBus.addListener(FiatLuxClient::guiLayers);
        modBus.addListener(FiatLuxClient::skies);
        NeoForge.EVENT_BUS.addListener(FiatLuxClient::clientTick);
        NeoForge.EVENT_BUS.addListener(FxManager::render);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> FxManager.clear());
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.ANGEL.get(), ctx -> new HostRenderer<>(ctx, "angel", 1.0f, 0.5f, true));
        e.registerEntityRenderer(ModEntities.SERAPH.get(), ctx -> new HostRenderer<>(ctx, "seraph", 1.2f, 0.8f, false));
        e.registerEntityRenderer(ModEntities.CHERUB.get(), ctx -> new HostRenderer<>(ctx, "cherub", 1.45f, 1.0f, true));
        e.registerEntityRenderer(ModEntities.OPHAN.get(), ctx -> new HostRenderer<>(ctx, "ophan", 1.5f, 1.2f, false));
        e.registerEntityRenderer(ModEntities.STATIONED_SWORD.get(), StationedSwordRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.HOLY_SPARK.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.SPARK));
        e.registerSpriteSet(ModParticles.DIVINE_FLAME.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.FLAME));
        e.registerSpriteSet(ModParticles.LIGHT_MOTE.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.MOTE));
        e.registerSpriteSet(ModParticles.FEATHER.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.FEATHER));
        e.registerSpriteSet(ModParticles.EMBER.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.EMBER));
        e.registerSpriteSet(ModParticles.RADIANCE.get(), s -> new GlowParticle.Provider(s, GlowParticle.Style.RADIANCE));
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        e.register(Keybinds.POWER_WHEEL);
        e.register(Keybinds.CAST);
    }

    private static void guiLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(FiatLux.id("fx_overlay"), FxOverlay::render);
        e.registerAboveAll(FiatLux.id("divinity_hud"), DivinityHud::render);
    }

    private static void skies(RegisterDimensionSpecialEffectsEvent e) {
        e.register(FiatLux.id("heaven"), new HeavenSkyEffects());
    }

    private static void clientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        FxManager.tick();
        if (mc.player == null || mc.level == null) return;
        while (Keybinds.POWER_WHEEL.consumeClick()) {
            mc.setScreen(new PowerWheelScreen());
        }
        while (Keybinds.CAST.consumeClick()) {
            PacketDistributor.sendToServer(new CastPowerPayload(ClientDivinity.get().state().selectedPower()));
        }
        // a held Flaming Sword sheds living fire along its blade. The local player's own sword is
        // skipped in first person: particles at the camera would swamp the view.
        if (mc.level.getGameTime() % 2 == 0) {
            boolean firstPerson = mc.options.getCameraType().isFirstPerson();
            for (Player p : mc.level.players()) {
                if (p.distanceToSqr(mc.player) > 48 * 48 || (p == mc.player && firstPerson)) continue;
                if (!p.getMainHandItem().is(ModItems.FLAMING_SWORD.get())) continue;
                float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
                Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
                Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
                Vec3 hand = p.position().add(right.scale(0.4)).add(forward.scale(0.25)).add(0, 0.75, 0);
                double along = mc.level.random.nextDouble() * 1.3;
                mc.level.addParticle(ModParticles.DIVINE_FLAME.get(), hand.x + forward.x * along * 0.6, hand.y + along * 0.8,
                        hand.z + forward.z * along * 0.6, 0, 0.015, 0);
            }
        }
    }
}
