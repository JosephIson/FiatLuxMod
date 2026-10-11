package com.josephison.fiatlux.item;

import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.LineagePayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "This is the book of the generations of Adam" (Genesis 5:1). Use it on a mortal to read their
 * family; use it in the air to open it at the first of all mortals.
 */
public class BookOfGenerationsItem extends Item {
    public BookOfGenerationsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, LineagePayloads.view(sp.server, null));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MortalEntity mortal)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, LineagePayloads.view(sp.server, mortal.getUUID()));
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
