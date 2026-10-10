package com.josephison.fiatlux.creation;

import com.josephison.fiatlux.config.FiatLuxConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The creation engine's work queue. Large acts (raising land, floods, sculpting) are expressed
 * as lazy iterators of block changes and applied a few thousand blocks per tick so the server
 * never stalls. Each job records the states it replaces so its author can undo it.
 */
public final class EditQueue {
    /** A single queued change: position and a function from the current state to the new one (null = skip). */
    public record Change(BlockPos pos, Function<BlockState, BlockState> rule) {}

    private static final class Job {
        final UUID owner;
        final ResourceKey<Level> dimension;
        final Iterator<Change> changes;
        final List<Snapshot> undo = new ArrayList<>();
        final boolean recordUndo;
        final Runnable onComplete;
        int applied;

        Job(UUID owner, ResourceKey<Level> dimension, Iterator<Change> changes, boolean recordUndo, Runnable onComplete) {
            this.owner = owner;
            this.dimension = dimension;
            this.changes = changes;
            this.recordUndo = recordUndo;
            this.onComplete = onComplete;
        }
    }

    private record Snapshot(BlockPos pos, BlockState state) {}

    private record UndoRecord(ResourceKey<Level> dimension, List<Snapshot> states) {}

    private static final Deque<Job> JOBS = new ArrayDeque<>();
    private static final Map<UUID, Deque<UndoRecord>> HISTORY = new HashMap<>();

    private EditQueue() {}

    public static void submit(UUID owner, ServerLevel level, Iterator<Change> changes, boolean recordUndo, Runnable onComplete) {
        JOBS.add(new Job(owner, level.dimension(), changes, recordUndo, onComplete));
    }

    public static int pendingJobs() {
        return JOBS.size();
    }

    /** Called every server tick: spends the block budget round-robin across jobs. */
    public static void tick(MinecraftServer server) {
        int budget = FiatLuxConfig.COMMON.editBlocksPerTick.get();
        int maxVolume = FiatLuxConfig.COMMON.maxEditVolume.get();
        while (budget > 0 && !JOBS.isEmpty()) {
            Job job = JOBS.poll();
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                continue;
            }
            int slice = Math.min(budget, 1024);
            while (slice-- > 0 && job.changes.hasNext() && job.applied < maxVolume) {
                Change c = job.changes.next();
                if (!level.isLoaded(c.pos())) {
                    continue;
                }
                BlockState current = level.getBlockState(c.pos());
                BlockState next = c.rule().apply(current);
                if (next == null || next == current) {
                    continue;
                }
                if (job.recordUndo) {
                    job.undo.add(new Snapshot(c.pos().immutable(), current));
                }
                level.setBlock(c.pos(), next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                job.applied++;
                budget--;
            }
            if (job.changes.hasNext() && job.applied < maxVolume) {
                JOBS.add(job);
            } else {
                finish(job);
            }
        }
    }

    private static void finish(Job job) {
        if (job.recordUndo && !job.undo.isEmpty()) {
            Deque<UndoRecord> h = HISTORY.computeIfAbsent(job.owner, k -> new ArrayDeque<>());
            h.push(new UndoRecord(job.dimension, job.undo));
            while (h.size() > FiatLuxConfig.COMMON.undoHistory.get()) {
                h.removeLast();
            }
        }
        if (job.onComplete != null) {
            job.onComplete.run();
        }
    }

    /** Queues the reversal of the owner's most recent act. Returns false if there is nothing to undo. */
    public static boolean undo(UUID owner, MinecraftServer server) {
        Deque<UndoRecord> h = HISTORY.get(owner);
        if (h == null || h.isEmpty()) {
            return false;
        }
        UndoRecord rec = h.pop();
        ServerLevel level = server.getLevel(rec.dimension());
        if (level == null) {
            return false;
        }
        List<Snapshot> states = new ArrayList<>(rec.states());
        java.util.Collections.reverse(states);
        Iterator<Change> it = states.stream().map(s -> new Change(s.pos(), cur -> s.state())).iterator();
        submit(owner, level, it, false, null);
        return true;
    }

    public static void clear() {
        JOBS.clear();
        HISTORY.clear();
    }
}
