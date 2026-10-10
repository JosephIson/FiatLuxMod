package com.josephison.fiatlux.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Closes to judgment range of the current target and executes judgment when ready. */
public class JudgeGoal extends Goal {
    private final HeavenlyBeing being;

    public JudgeGoal(HeavenlyBeing being) {
        this.being = being;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = being.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = being.getTarget();
        if (t == null) return;
        being.getLookControl().setLookAt(t, 30f, 30f);
        double range = being.judgmentRange() * 0.8;
        if (being.distanceToSqr(t) > range * range) {
            being.getNavigation().moveTo(t.getX(), t.getY() + 1.5, t.getZ(), 1.3);
        } else {
            being.getNavigation().stop();
        }
        being.tryJudge(t);
    }
}
