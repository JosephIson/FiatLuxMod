package com.josephison.fiatlux.weapon;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.StationedSwordEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModDataComponents;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModSounds;
import com.josephison.fiatlux.registry.ModTags;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Flaming Sword of the Cherubim (Genesis 3:24). Ignites what it strikes and slays the corrupt
 * in one stroke; use releases a wave of holy fire; sneak-use on the ground stations it as a
 * guardian "which turned every way, to keep the way of the tree of life".
 */
public class FlamingSwordItem extends SwordItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.flaming_sword_of_cherubim.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FlamingSwordItem(Tier tier, Properties props) {
        super(tier, props);
    }

    public static WeaponTraits traits(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WEAPON_TRAITS.get(), WeaponTraits.FRESH);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(8);
        if (ModTags.isCorrupt(target) && FiatLuxConfig.COMMON.swordSlaysCorruptInstantly.get()) {
            target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), Math.max(1000f, target.getMaxHealth() * 4f));
        }
        if (!target.isAlive() || target.isDeadOrDying()) {
            stack.set(ModDataComponents.WEAPON_TRAITS.get(), traits(stack).recordSmite());
        }
        if (attacker.level() instanceof ServerLevel sl) {
            Fx.send(sl, FxKind.FIRE_WAVE, target.position().add(0, target.getBbHeight() * 0.5, 0), 1.5f, 0xFFFFB040, 8);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level instanceof ServerLevel sl) {
            WeaponTraits t = traits(stack);
            double range = FiatLuxConfig.COMMON.swordFireWaveRange.get() + t.kindled();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player)) {
                Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
                if (to.length() > range || to.normalize().dot(look) < 0.55 || e.isAlliedTo(player)) continue;
                e.hurt(player.damageSources().indirectMagic(player, player), 8f + 3f * t.kindled());
                e.igniteForSeconds(6);
                if (ModTags.isCorrupt(e)) e.hurt(player.damageSources().magic(), 20f);
            }
            Fx.send(sl, FxKind.FIRE_WAVE, eye.add(look.scale(1.5)), (float) range, 0xFFFFA030, 16);
            sl.playSound(null, player.blockPosition(), ModSounds.SWORD_WAVE.get(), SoundSource.PLAYERS, 1.2f, 0.9f + sl.random.nextFloat() * 0.2f);
            player.getCooldowns().addCooldown(this, 24);
        }
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Server-side time of each wielder's last swing wave (prevents spamming waves by flailing). */
    private static final java.util.Map<java.util.UUID, Long> LAST_SWING_WAVE = new java.util.WeakHashMap<>();

    /**
     * "emits waves of divine fire on swing" (design doc section 4): every swing sweeps a short arc of holy fire in
     * front of the wielder; the full wave is reserved for use (right-click).
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity.level() instanceof ServerLevel sl && hand == InteractionHand.MAIN_HAND) {
            long now = sl.getGameTime();
            Long last = LAST_SWING_WAVE.get(entity.getUUID());
            if (last == null || now - last >= 10) {
                LAST_SWING_WAVE.put(entity.getUUID(), now);
                WeaponTraits t = traits(stack);
                double range = (FiatLuxConfig.COMMON.swordFireWaveRange.get() + t.kindled()) * 0.5;
                Vec3 eye = entity.getEyePosition();
                Vec3 look = entity.getLookAngle();
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(range), e -> e != entity)) {
                    Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
                    if (to.length() > range || to.normalize().dot(look) < 0.4 || e.isAlliedTo(entity)) continue;
                    e.hurt(entity.damageSources().indirectMagic(entity, entity), 4f + 2f * t.kindled());
                    e.igniteForSeconds(4);
                }
                Fx.send(sl, FxKind.FIRE_WAVE, eye.add(look.scale(1.4)), (float) range * 0.6f, 0xFFFFB040, 10);
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (ctx.getLevel() instanceof ServerLevel sl) {
            StationedSwordEntity guard = ModEntities.STATIONED_SWORD.get().create(sl);
            if (guard == null) return InteractionResult.FAIL;
            Vec3 at = Vec3.atBottomCenterOf(ctx.getClickedPos().relative(ctx.getClickedFace()));
            guard.moveTo(at.x, at.y, at.z, player.getYRot(), 0f);
            guard.setup(player.getUUID(), traits(ctx.getItemInHand()));
            sl.addFreshEntity(guard);
            Fx.send(sl, FxKind.CELESTIAL_PILLAR, at, 40f, 0xFFFFA040, 20);
            if (player instanceof ServerPlayer sp) {
                CosmosData.get(sl.getServer()).record(sl.getServer(), sp.getGameProfile().getName(), "eden_guard",
                        "placed a flaming sword which turned every way, to keep the way of the tree of life.");
            }
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WeaponTraits t = traits(stack);
        tooltip.add(Component.translatable("item.fiatlux.flaming_sword_of_cherubim.verse").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.fiatlux.kindled", t.kindled()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.fiatlux.smitten", t.smitten()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fiatlux.flaming_sword.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fire", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = com.josephison.fiatlux.client.render.FlamingSwordRenderer.create();
                return renderer;
            }
        });
    }
}
