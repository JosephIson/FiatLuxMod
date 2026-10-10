package com.josephison.fiatlux.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** "He shall give his angels charge over thee, to keep thee in all thy ways." (Psalm 91:11) */
public class FollowCreatorGoal extends Goal {
    private final HeavenlyBeing being;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    private Player creator;

    public FollowCreatorGoal(HeavenlyBeing being, double speed, float startDistance, float stopDistance) {
        this.being = being;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Player p = being.getCreator();
        if (p == null || p.isSpectator() || being.getTarget() != null) return false;
        if (being.distanceToSqr(p) < startDistance * startDistance) return false;
        creator = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return creator != null && being.getTarget() == null && being.distanceToSqr(creator) > stopDistance * stopDistance;
    }

    @Override
    public void tick() {
        if (being.distanceToSqr(creator) > 48 * 48) {
            being.moveTo(creator.getX(), creator.getY() + 2, creator.getZ());   // ascend/descend to keep pace
            return;
        }
        being.getNavigation().moveTo(creator.getX(), creator.getY() + 2.0, creator.getZ(), speed);
    }

    @Override
    public void stop() {
        creator = null;
        being.getNavigation().stop();
    }
}
