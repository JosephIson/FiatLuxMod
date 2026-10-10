package com.josephison.fiatlux.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Shared base of the choirs of the second and third spheres, whose GeckoLib models are generated
 * from one figure: each has animation.NAME.idle / .move / .judge, and a periodic act of office
 * performed every {@link #officeInterval()} ticks besides its act of judgment.
 */
public abstract class ChoirBeing extends HeavenlyBeing {
    private final RawAnimation idle;
    private final RawAnimation move;
    private final RawAnimation judge;

    protected ChoirBeing(EntityType<? extends ChoirBeing> type, Level level, String name) {
        super(type, level);
        this.idle = RawAnimation.begin().thenLoop("animation." + name + ".idle");
        this.move = RawAnimation.begin().thenLoop("animation." + name + ".move");
        this.judge = RawAnimation.begin().thenPlay("animation." + name + ".judge");
    }

    /** Ticks between acts of office; 0 for none. */
    protected int officeInterval() {
        return 0;
    }

    /** The choir's periodic office (server side). */
    protected void office(ServerLevel level) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        int every = officeInterval();
        if (every > 0 && !isBeingCastOut() && level() instanceof ServerLevel sl && (tickCount + getId()) % every == 0) {
            office(sl);
        }
    }

    /** Living beings within r on this being's side of the war (the Host, and players for the loyal). */
    protected List<LivingEntity> alliesWithin(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(r), e -> e != this && e.isAlive()
                && (e instanceof HeavenlyBeing b ? b.isRebel() == isRebel() : !isRebel() && e instanceof net.minecraft.world.entity.player.Player));
    }

    /** Living beings within r that this being's side wars against. */
    protected List<LivingEntity> enemiesWithin(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(r), e -> e != this && e.isAlive()
                && (isRebel() ? e instanceof HeavenlyBeing b && !b.isRebel() : com.josephison.fiatlux.registry.ModTags.isCorrupt(e)));
    }

    @Override
    protected RawAnimation idleAnimation() {
        return idle;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return move;
    }

    @Override
    protected RawAnimation judgmentAnimation() {
        return judge;
    }
}
