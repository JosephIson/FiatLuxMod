package com.josephison.fiatlux.weapon;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.power.DivinePower;
import com.josephison.fiatlux.power.ModPowers;
import com.josephison.fiatlux.power.PowerCasting;
import com.josephison.fiatlux.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The Creator's scepter: speaks the selected power where the Creator looks. Sneak-use cycles the
 * selected power (the power wheel, default key V, offers a radial choice).
 */
public class DivineScepterItem extends Item {
    public DivineScepterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) {
                List<ResourceLocation> ids = new ArrayList<>(ModPowers.REGISTRY.keySet());
                ids.sort(null);
                ResourceLocation cur = Divinity.get(sp).selectedPower();
                ResourceLocation next = ids.get((ids.indexOf(cur) + 1) % ids.size());
                Divinity.set(sp, Divinity.get(sp).withSelectedPower(next));
                Divinity.sync(sp);
                DivinePower p = ModPowers.REGISTRY.get(next);
                if (p != null) sp.displayClientMessage(p.name().copy().withColor(p.color()), true);
            } else {
                ResourceLocation id = stack.has(ModDataComponents.BOUND_POWER.get())
                        ? stack.get(ModDataComponents.BOUND_POWER.get()) : Divinity.get(sp).selectedPower();
                PowerCasting.cast(sp, id);
                sp.getCooldowns().addCooldown(this, 6);
            }
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fiatlux.divine_scepter.verse").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.fiatlux.scepter.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
