package com.josephison.fiatlux.portal;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The blocks and block light of a box of one world, captured on the server so that a client in
 * another world can draw it through a portal. Blocks that cannot be seen from anywhere (solid on
 * every side) are sent as {@link #HIDDEN}, which keeps the snapshot small; the box is sent run-length
 * encoded.
 */
public final class RegionSnapshot {
    /** Palette index of air, and of a solid block hidden on every side (drawn as nothing, but culls its neighbours' faces). */
    public static final int AIR = 0, HIDDEN = 1;
    public final BlockPos min;
    public final int sx, sy, sz;
    public final BlockState[] palette;
    public final int[] states;
    public final byte[] light;

    public RegionSnapshot(BlockPos min, int sx, int sy, int sz, BlockState[] palette, int[] states, byte[] light) {
        this.min = min;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.palette = palette;
        this.states = states;
        this.light = light;
    }

    public int index(int x, int y, int z) {
        return (y * sz + z) * sx + x;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    /** Captures the box from `centre` out `r` blocks each way, `down` below and `up` above (chunks are loaded as needed). */
    public static RegionSnapshot capture(ServerLevel level, BlockPos centre, int r, int down, int up) {
        BlockPos min = centre.offset(-r, -down, -r);
        int sx = 2 * r + 1, sy = down + up + 1, sz = 2 * r + 1;
        for (int cx = (min.getX() >> 4); cx <= ((min.getX() + sx - 1) >> 4); cx++) {
            for (int cz = (min.getZ() >> 4); cz <= ((min.getZ() + sz - 1) >> 4); cz++) level.getChunk(cx, cz);
        }
        BlockState[] raw = new BlockState[sx * sy * sz];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) {
            raw[(y * sz + z) * sx + x] = level.getBlockState(p.set(min.getX() + x, min.getY() + y, min.getZ() + z));
        }
        List<BlockState> palette = new ArrayList<>(List.of(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        Map<BlockState, Integer> ids = new HashMap<>();
        ids.put(palette.get(0), AIR);
        int[] states = new int[raw.length];
        byte[] light = new byte[raw.length];
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) {
            int i = (y * sz + z) * sx + x;
            BlockState s = raw[i];
            p.set(min.getX() + x, min.getY() + y, min.getZ() + z);
            if (s.isAir()) {
                states[i] = AIR;
                light[i] = (byte) level.getBrightness(LightLayer.BLOCK, p);
                continue;
            }
            if (opaque(level, s, p) && hidden(level, raw, sx, sy, sz, x, y, z, min)) {
                states[i] = HIDDEN;
                continue;
            }
            Integer id = ids.get(s);
            if (id == null) {
                id = palette.size();
                palette.add(s);
                ids.put(s, id);
            }
            states[i] = id;
            light[i] = (byte) level.getBrightness(LightLayer.BLOCK, p);
        }
        return new RegionSnapshot(min, sx, sy, sz, palette.toArray(new BlockState[0]), states, light);
    }

    private static boolean opaque(ServerLevel level, BlockState s, BlockPos p) {
        return s.isSolidRender(level, p);
    }

    private static boolean hidden(ServerLevel level, BlockState[] raw, int sx, int sy, int sz, int x, int y, int z, BlockPos min) {
        int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (int[] o : d) {
            int nx = x + o[0], ny = y + o[1], nz = z + o[2];
            if (nx < 0 || ny < 0 || nz < 0 || nx >= sx || ny >= sy || nz >= sz) continue;   // beyond the box counts as solid
            BlockState ns = raw[(ny * sz + nz) * sx + nx];
            if (!ns.isSolidRender(level, n.set(min.getX() + nx, min.getY() + ny, min.getZ() + nz))) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ on the wire

    public void write(ByteBuf buf) {
        buf.writeLong(min.asLong());
        ByteBufCodecs.VAR_INT.encode(buf, sx);
        ByteBufCodecs.VAR_INT.encode(buf, sy);
        ByteBufCodecs.VAR_INT.encode(buf, sz);
        ByteBufCodecs.VAR_INT.encode(buf, palette.length);
        for (BlockState s : palette) ByteBufCodecs.VAR_INT.encode(buf, Block.getId(s));
        runs(buf, states);
        int[] l = new int[light.length];
        for (int i = 0; i < l.length; i++) l[i] = light[i];
        runs(buf, l);
    }

    public static RegionSnapshot read(ByteBuf buf) {
        BlockPos min = BlockPos.of(buf.readLong());
        int sx = ByteBufCodecs.VAR_INT.decode(buf), sy = ByteBufCodecs.VAR_INT.decode(buf), sz = ByteBufCodecs.VAR_INT.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        BlockState[] palette = new BlockState[n];
        for (int i = 0; i < n; i++) palette[i] = Block.stateById(ByteBufCodecs.VAR_INT.decode(buf));
        int[] states = new int[sx * sy * sz];
        unruns(buf, states);
        int[] l = new int[states.length];
        unruns(buf, l);
        byte[] light = new byte[l.length];
        for (int i = 0; i < l.length; i++) light[i] = (byte) l[i];
        return new RegionSnapshot(min, sx, sy, sz, palette, states, light);
    }

    private static void runs(ByteBuf buf, int[] a) {
        int i = 0;
        while (i < a.length) {
            int j = i;
            while (j < a.length && a[j] == a[i]) j++;
            ByteBufCodecs.VAR_INT.encode(buf, a[i]);
            ByteBufCodecs.VAR_INT.encode(buf, j - i);
            i = j;
        }
    }

    private static void unruns(ByteBuf buf, int[] a) {
        int i = 0;
        while (i < a.length) {
            int v = ByteBufCodecs.VAR_INT.decode(buf), len = ByteBufCodecs.VAR_INT.decode(buf);
            for (int k = 0; k < len && i < a.length; k++) a[i++] = v;
        }
    }
}
