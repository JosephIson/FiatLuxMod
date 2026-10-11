package com.josephison.fiatlux.son;

import com.josephison.fiatlux.entity.MortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Son moving as no man moves, a tick at a time: walking upon the sea (Matthew 14:25), and going up
 * into heaven until a cloud receives him (Acts 1:9).
 */
final class Motion {
    private Motion() {}

    private enum Mode { NONE, WATER, ASCEND }

    private static Mode mode = Mode.NONE;
    @Nullable private static UUID who;
    private static Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private static int ticks, length;

    static boolean walking() {
        return mode == Mode.WATER;
    }

    static boolean rising() {
        return mode == Mode.ASCEND;
    }

    /**
     * He walks across the water at `water` to his disciples on the far shore. Returns false if there is no
     * stretch of water there long enough to walk upon.
     */
    static boolean walkOnWater(ServerLevel level, SonData d, MortalEntity son, BlockPos water) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int run = 0;
            while (run < 18 && level.getFluidState(water.relative(dir, run + 1)).is(FluidTags.WATER)) run++;
            if (run < 8) continue;
            BlockPos shore = water.relative(dir, run + 2);
            BlockPos land = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, shore.getX(), shore.getZ(), 3);
            if (land == null) continue;
            from = Vec3.atBottomCenterOf(water).add(0, 0.95, 0);
            to = Vec3.atBottomCenterOf(water.relative(dir, run)).add(0, 0.95, 0);
            son.teleportTo(from.x, from.y, from.z);
            son.setNoGravity(true);
            son.getNavigation().stop();
            for (MortalEntity m : TheSon.disciples(level, d)) {
                TheSon.go(level, m, land.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2), 2.5, true);
                m.getLookControl().setLookAt(from.x, from.y + 1, from.z);
            }
            if (!TheSon.disciples(level, d).isEmpty()) TheSon.disciples(level, d).getFirst().say("It is a spirit!");
            who = son.getUUID();
            mode = Mode.WATER;
            ticks = 0;
            length = (int) Math.max(80, from.distanceTo(to) * 9);
            TheSon.narrate(level, water, "And in the fourth watch of the night " + d.name + " went unto them, walking on the sea.", "Matthew 14:25");
            return true;
        }
        return false;
    }

    /** "He was taken up; and a cloud received him out of their sight" (Acts 1:9). */
    static void ascend(MortalEntity son, int forTicks) {
        who = son.getUUID();
        mode = Mode.ASCEND;
        ticks = 0;
        length = forTicks;
        son.setNoGravity(true);
        son.getNavigation().stop();
        from = son.position();
    }

    static void tick(ServerLevel level, SonData d) {
        if (mode == Mode.NONE || who == null) return;
        MortalEntity son = TheSon.mortal(level, who);
        if (son == null) {
            mode = Mode.NONE;
            return;
        }
        ticks++;
        switch (mode) {
            case WATER -> {
                double f = Math.min(1, ticks / (double) length);
                Vec3 p = from.lerp(to, f);
                son.setPos(p.x, p.y, p.z);
                son.setDeltaMovement(Vec3.ZERO);
                son.getLookControl().setLookAt(to.x, to.y + 1.5, to.z);
                if (ticks % 3 == 0) level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.05, p.z, 6, 0.3, 0.02, 0.3, 0.02);
                if (ticks % 8 == 0) level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 4, 0.4, 0.05, 0.4, 0.02);
                if (f >= 1) {
                    son.setNoGravity(false);
                    TheSon.speak(level, son, d, "Be of good cheer; it is I; be not afraid.", true);
                    for (MortalEntity m : TheSon.disciples(level, d)) {
                        m.beginAct(MortalEntity.BOW, 60);
                        if (level.random.nextInt(3) == 0) m.say("Of a truth thou art the Son of " + d.creatorName + ".");
                    }
                    mode = Mode.NONE;
                }
            }
            case ASCEND -> {
                double rise = Math.min(0.12, 0.01 + ticks * 0.0006);
                son.setPos(son.getX(), son.getY() + rise, son.getZ());
                son.setDeltaMovement(Vec3.ZERO);
                if (ticks % 2 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD, son.getX(), son.getY() - 0.2, son.getZ(), 6, 0.6, 0.1, 0.6, 0.01);
                    level.sendParticles(com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), son.getX(), son.getY() + 1, son.getZ(), 3, 0.5, 0.8, 0.5, 0.02);
                }
                if (ticks > length - 60) level.sendParticles(ParticleTypes.CLOUD, son.getX(), son.getY() + 1, son.getZ(), 30, 1.4, 0.8, 1.4, 0.02);
                if (ticks >= length) mode = Mode.NONE;
            }
            default -> {
            }
        }
    }
}
