package com.josephison.fiatlux.weapon;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.registry.ModDataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The divine and infernal arsenal (design doc section 4). Every weapon here is a 3D GeckoLib model with
 * an emissive glow layer, bespoke display transforms and its traits in the {@code weapon_traits}
 * data component. Further weapons (Michael's Lance, the Staff of Wonders, the Trumpets of Judgment,
 * infernal tech blades and soul firearms) are registered here as they are built.
 */
public final class DivineWeaponRegistry {
    public static final DeferredRegister.Items WEAPONS = DeferredRegister.createItems(FiatLux.MODID);

    /** Tier of the divine arsenal: unrivalled sharpness, mended with gold. */
    public static final SimpleTier DIVINE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5000, 12f, 6f, 30,
            () -> Ingredient.of(Items.GOLD_BLOCK));

    /** Genesis 3:24 - the Flaming Sword of the Cherubim. */
    public static final DeferredItem<FlamingSwordItem> FLAMING_SWORD = WEAPONS.register("flaming_sword_of_cherubim",
            () -> new FlamingSwordItem(DIVINE, new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(DIVINE, 8, -2.2f))
                    .component(ModDataComponents.WEAPON_TRAITS.get(), WeaponTraits.FRESH)));

    private DivineWeaponRegistry() {}
}
