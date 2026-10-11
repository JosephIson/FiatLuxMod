package com.josephison.fiatlux.eden;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The fruit of one of the two trees in the midst of the garden. The fruit of the tree of life heals
 * and strengthens: whoever eats it may "live for ever" (Gen 3:22). The fruit of the tree of the
 * knowledge of good and evil opens the eyes (Gen 3:5-7): it leaves the eater dazed.
 */
public class EdenFruitItem extends Item {
    private final boolean life;

    public EdenFruitItem(Properties properties, boolean life) {
        super(properties);
        this.life = life;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide) {
            if (life) {
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
                eater.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));
            } else {
                eater.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
            }
            if (eater instanceof Player p) {
                p.displayClientMessage(Component.translatable(life ? "message.fiatlux.eden.ate_life" : "message.fiatlux.eden.ate_knowledge")
                        .withStyle(s -> s.withColor(life ? 0xF5D77A : 0xE07070).withItalic(true)), false);
            }
        }
        return super.finishUsingItem(stack, level, eater);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(life ? "tooltip.fiatlux.fruit_of_life" : "tooltip.fiatlux.fruit_of_knowledge").withStyle(ChatFormatting.GRAY));
    }
}
