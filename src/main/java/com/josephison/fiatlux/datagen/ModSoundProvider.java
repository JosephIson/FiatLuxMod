package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import java.util.function.Supplier;

/** Placeholder soundscape layered from vanilla events until bespoke recordings exist. */
public class ModSoundProvider extends SoundDefinitionsProvider {
    public ModSoundProvider(PackOutput out, ExistingFileHelper files) {
        super(out, FiatLux.MODID, files);
    }

    @Override
    public void registerSounds() {
        layered(ModSounds.FIAT_LUX, "block.beacon.activate", "block.amethyst_block.resonate");
        layered(ModSounds.SMITE, "entity.lightning_bolt.thunder", "block.beacon.power_select");
        layered(ModSounds.SWORD_WAVE, "item.firecharge.use", "entity.blaze.shoot");
        layered(ModSounds.HOLY, "block.bell.resonate", "block.beacon.power_select");
        layered(ModSounds.WINGS, "entity.ender_dragon.flap");
        layered(ModSounds.WHEEL, "block.conduit.ambient");
        layered(ModSounds.HOST_ARRIVE, "block.beacon.activate", "item.trident.riptide_3");
    }

    private void layered(Supplier<SoundEvent> event, String... vanilla) {
        String path = event.get().getLocation().getPath();
        SoundDefinition def = definition().subtitle("subtitles." + FiatLux.MODID + "." + path);
        for (String v : vanilla) {
            def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(v), SoundDefinition.SoundType.EVENT));
        }
        add(event.get(), def);
    }
}
