package com.josephison.fiatlux.datagen;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, FiatLux.MODID, files);
    }

    @Override
    protected void registerModels() {
        for (String block : new String[]{"lux_block", "golden_street", "jasper", "cloudstone", "pearl",
                "sulfur", "brimstone", "abyssal_stone", "neon_crimson", "neon_violet", "soul_engine", "regolith",
                "cloud_band_amber", "cloud_band_cream", "cloud_band_rust"}) {
            withExistingParent(block, modLoc("block/" + block));
        }
        getBuilder("celestial_crystal").parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("block/celestial_crystal"));
        getBuilder("divine_scepter").parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", modLoc("item/divine_scepter"));
        for (String egg : new String[]{"angel", "seraph", "cherub", "ophan", "fallen"}) {
            withExistingParent(egg + "_spawn_egg", mcLoc("item/template_spawn_egg"));
        }
        // The 3D GeckoLib sword: builtin/entity hands rendering to the GeoItemRenderer. The model is
        // 41px (2.6 blocks) tall with the pommel at the origin, so each view rescales and recentres it.
        getBuilder("flaming_sword_of_cherubim").parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .guiLight(net.minecraft.client.renderer.block.model.BlockModel.GuiLight.FRONT)
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(0, 0, -45).translation(-7.2f, -7.2f, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, -45).translation(-7.2f, -7.2f, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 1, 0).scale(0.3f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, -90, 55).translation(0, 1.5f, 0.5f).scale(0.6f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, -55).translation(0, 1.5f, 0.5f).scale(0.6f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, 25).translation(1.13f, 2.2f, 1.13f).scale(0.4f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, -25).translation(1.13f, 2.2f, 1.13f).scale(0.4f).end()
                .end();
    }
}
