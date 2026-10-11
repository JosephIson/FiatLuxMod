package com.josephison.fiatlux.client.portal;

import com.josephison.fiatlux.infernal.RiftEntity;
import com.josephison.fiatlux.portal.PortalPayload;
import com.josephison.fiatlux.portal.Portals;
import com.josephison.fiatlux.portal.RegionSnapshot;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws what lies beyond each open portal, inside its opening only, the way the Immersive Portals mod
 * does: the opening is marked in the stencil buffer where nothing nearer hides it; there the depth is
 * pushed back to the far plane and the far side's haze laid down; the far side's blocks (built once
 * per side from the snapshot into vertex buffers) are drawn through the mask in their true place
 * relative to the opening, with the far side's fog; and the opening's own depth is put back so what
 * stands in front of or behind it is drawn rightly afterwards. Only blocks beyond the opening's plane,
 * on the side away from the onlooker, are drawn.
 */
public final class PortalViews {
    private static final Map<Integer, View> VIEWS = new HashMap<>();
    private static boolean stencilAsked;

    private PortalViews() {}

    public static void receive(PortalPayload p) {
        View old = VIEWS.put(p.entity(), new View(p));
        if (old != null) old.free();
    }

    /** Whether the far side of this entity's opening is ready to be drawn (else its renderer shows a dark opening). */
    public static boolean showing(Entity e) {
        View v = VIEWS.get(e.getId());
        return v != null && Minecraft.getInstance().getMainRenderTarget().isStencilEnabled();
    }

    /** Stencil bits are asked for between frames, the first time the client ticks. */
    public static void tick(ClientTickEvent.Post event) {
        if (!stencilAsked) {
            stencilAsked = true;
            Minecraft.getInstance().getMainRenderTarget().enableStencil();
        }
        var level = Minecraft.getInstance().level;
        for (Iterator<Map.Entry<Integer, View>> it = VIEWS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Entity carrier = level == null ? null : level.getEntity(e.getKey());
            if (carrier == null || carrier.isRemoved()) {
                if (++e.getValue().missing > 40) {
                    e.getValue().free();
                    it.remove();
                }
            } else {
                e.getValue().missing = 0;
            }
        }
    }

    public static void clear() {
        VIEWS.values().forEach(View::free);
        VIEWS.clear();
    }

    // ------------------------------------------------------------------ drawing

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || VIEWS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.getMainRenderTarget().isStencilEnabled()) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (var e : VIEWS.entrySet()) {
            Entity carrier = mc.level.getEntity(e.getKey());
            if (carrier == null || carrier.distanceToSqr(cam) > 160 * 160) continue;
            List<Vector3f> tris = opening(carrier, e.getValue().shape, partial);
            if (tris.isEmpty()) continue;
            draw(event, mc, cam, carrier, e.getValue(), tris);
        }
    }

    /** The opening as triangles in its own frame (x across, y up, z = 0), or none if it is shut. */
    static List<Vector3f> opening(Entity carrier, String shape, float partial) {
        List<Vector3f> out = new ArrayList<>();
        if (carrier instanceof RiftEntity rift && shape.equals("rift")) {
            float open = rift.openness(partial);
            if (open < 0.05f) return out;
            float[][] o = RiftEntity.outline(rift.getId(), open);
            int n = o[0].length - 1;
            for (int i = 0; i < n; i++) {
                float y0 = RiftEntity.HEIGHT * i / n, y1 = RiftEntity.HEIGHT * (i + 1) / n;
                float l0 = o[0][i] - o[1][i] * 0.8f, r0 = o[0][i] + o[1][i] * 0.8f, l1 = o[0][i + 1] - o[1][i + 1] * 0.8f, r1 = o[0][i + 1] + o[1][i + 1] * 0.8f;
                out.add(new Vector3f(l0, y0, 0));
                out.add(new Vector3f(r0, y0, 0));
                out.add(new Vector3f(r1, y1, 0));
                out.add(new Vector3f(l0, y0, 0));
                out.add(new Vector3f(r1, y1, 0));
                out.add(new Vector3f(l1, y1, 0));
            }
        } else if (shape.startsWith("door")) {   // "door:<width>:<height>"
            String[] p = shape.split(":");
            float w = p.length > 1 ? Float.parseFloat(p[1]) : 1f, h = p.length > 2 ? Float.parseFloat(p[2]) : 2f;
            out.add(new Vector3f(-w / 2, 0, 0));
            out.add(new Vector3f(w / 2, 0, 0));
            out.add(new Vector3f(w / 2, h, 0));
            out.add(new Vector3f(-w / 2, 0, 0));
            out.add(new Vector3f(w / 2, h, 0));
            out.add(new Vector3f(-w / 2, h, 0));
        }
        return out;
    }

    private static void draw(RenderLevelStageEvent event, Minecraft mc, Vec3 cam, Entity carrier, View view, List<Vector3f> tris) {
        Vec3 origin = carrier.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        float yaw = carrier.getYRot();
        Vec3 f = Portals.facing(yaw), a = Portals.across(yaw);
        int side = cam.subtract(origin).dot(f) >= 0 ? 1 : -1;
        // the opening, in camera-relative world space
        List<Vector3f> world = new ArrayList<>(tris.size());
        for (Vector3f t : tris) {
            world.add(new Vector3f((float) (origin.x - cam.x + a.x * t.x + f.x * t.z), (float) (origin.y - cam.y + t.y),
                    (float) (origin.z - cam.z + a.z * t.x + f.z * t.z)));
        }
        Matrix4f viewMat = new Matrix4f(event.getModelViewMatrix());
        Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
        var mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.set(viewMat);
        RenderSystem.applyModelViewMatrix();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        // 1. mark the opening wherever nothing nearer hides it
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        polygon(world, view.fog, 255);
        // 2. inside it: the far side's haze, and the depth pushed back to the far plane
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        GL11.glDepthRange(1.0, 1.0);
        polygon(world, view.fog, 255);
        GL11.glDepthRange(0.0, 1.0);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        // 3. the far side itself, in its place behind the opening, in its own haze
        Matrix4f m = new Matrix4f(viewMat).translate((float) (origin.x - cam.x), (float) (origin.y - cam.y), (float) (origin.z - cam.z));
        // far-side coordinates -> world: rotate the far facing onto this one, about the anchor
        float turn = (float) Math.toRadians(yaw - view.destYaw);
        m.rotateY(-turn);
        m.translate((float) (view.snapshot.min.getX() - view.ax), (float) (view.snapshot.min.getY() - view.ay), (float) (view.snapshot.min.getZ() - view.az));
        float[] fogColour = RenderSystem.getShaderFogColor().clone();
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        var fogShape = RenderSystem.getShaderFogShape();
        float d = (float) cam.distanceTo(origin);
        RenderSystem.setShaderFogColor(((view.fog >> 16) & 255) / 255f, ((view.fog >> 8) & 255) / 255f, (view.fog & 255) / 255f, 1f);
        RenderSystem.setShaderFogStart(d + 4);
        RenderSystem.setShaderFogEnd(d + 34);
        RenderSystem.setShaderFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        Map<RenderType, VertexBuffer> mesh = view.mesh(side * (view.destYaw == yaw ? 1 : 1));
        for (RenderType type : RenderType.chunkBufferLayers()) {
            VertexBuffer vb = mesh.get(type);
            if (vb == null) continue;
            type.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                shader.setDefaultUniforms(VertexFormat.Mode.QUADS, m, proj, mc.getWindow());
                shader.apply();
                if (shader.CHUNK_OFFSET != null) {
                    shader.CHUNK_OFFSET.set(0f, 0f, 0f);
                    shader.CHUNK_OFFSET.upload();
                }
                vb.bind();
                vb.draw();
                shader.clear();
            }
            VertexBuffer.unbind();
            type.clearRenderState();
        }
        RenderSystem.setShaderFogColor(fogColour[0], fogColour[1], fogColour[2], fogColour[3]);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderFogShape(fogShape);
        // 4. the opening's own depth back, so the world before and behind it is drawn rightly
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        polygon(world, view.fog, 255);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableCull();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        mvStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void polygon(List<Vector3f> tris, int rgb, int alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableBlend();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255;
        for (Vector3f v : tris) b.addVertex(v.x, v.y, v.z).setColor(r, g, bl, alpha);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    // ------------------------------------------------------------------ one portal's far side

    static final class View {
        final String shape;
        final double ax, ay, az;
        final float destYaw;
        final int fog;
        final RegionSnapshot snapshot;
        final Map<Integer, Map<RenderType, VertexBuffer>> meshes = new HashMap<>();
        int missing;

        View(PortalPayload p) {
            shape = p.shape();
            ax = p.ax();
            ay = p.ay();
            az = p.az();
            destYaw = p.destYaw();
            fog = p.fog();
            snapshot = p.snapshot();
        }

        /** The far side as seen from `side` of the opening: only what lies beyond its plane, built once. */
        Map<RenderType, VertexBuffer> mesh(int side) {
            return meshes.computeIfAbsent(side, this::build);
        }

        private Map<RenderType, VertexBuffer> build(int side) {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blocks = mc.getBlockRenderer();
            SnapshotLevel level = new SnapshotLevel(snapshot, fog == 0x3A0E0A ? "fiatlux:underworld" : null, 3);
            Vec3 f = Portals.facing(destYaw);
            // the anchor in snapshot coordinates
            double cx = ax - snapshot.min.getX(), cz = az - snapshot.min.getZ();
            Map<RenderType, ByteBufferBuilder> memory = new LinkedHashMap<>();
            Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
            PoseStack pose = new PoseStack();
            RandomSource random = RandomSource.create();
            ModelBlockRenderer.enableCaching();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int y = 0; y < snapshot.sy; y++) for (int z = 0; z < snapshot.sz; z++) for (int x = 0; x < snapshot.sx; x++) {
                int idx = snapshot.states[snapshot.index(x, y, z)];
                if (idx == RegionSnapshot.AIR || idx == RegionSnapshot.HIDDEN) continue;
                // beyond the opening's plane, away from the onlooker
                double along = (x + 0.5 - cx) * f.x + (z + 0.5 - cz) * f.z;
                if (along * side > -0.4) continue;
                BlockState state = snapshot.palette[idx];
                p.set(x, y, z);
                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty()) {
                    RenderType t = ItemBlockRenderTypes.getRenderLayer(fluid);
                    BufferBuilder b = builder(builders, memory, t);
                    // the liquid renderer writes positions within the 16-block section
                    pose.pushPose();
                    renderLiquidAt(blocks, level, b, state, fluid, p.immutable());
                    pose.popPose();
                }
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                var model = blocks.getBlockModel(state);
                random.setSeed(state.getSeed(p));
                for (RenderType t : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                    BufferBuilder b = builder(builders, memory, t);
                    pose.pushPose();
                    pose.translate(x, y, z);
                    blocks.renderBatched(state, p, level, pose, b, true, random, ModelData.EMPTY, t);
                    pose.popPose();
                }
            }
            ModelBlockRenderer.clearCache();
            Map<RenderType, VertexBuffer> out = new HashMap<>();
            for (var e : builders.entrySet()) {
                MeshData data = e.getValue().build();
                if (data != null) {
                    VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    vb.bind();
                    vb.upload(data);
                    VertexBuffer.unbind();
                    out.put(e.getKey(), vb);
                }
                memory.get(e.getKey()).close();
            }
            return out;
        }

        /** Liquids are written relative to their section; shift them into place with a wrapping consumer. */
        private static void renderLiquidAt(BlockRenderDispatcher blocks, SnapshotLevel level, BufferBuilder b, BlockState state, FluidState fluid, BlockPos p) {
            float ox = p.getX() & ~15, oy = p.getY() & ~15, oz = p.getZ() & ~15;
            blocks.renderLiquid(p, level, new com.mojang.blaze3d.vertex.VertexConsumer() {
                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
                    b.addVertex(x + ox, y + oy, z + oz);
                    return this;
                }

                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int bl, int a) {
                    b.setColor(r, g, bl, a);
                    return this;
                }

                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
                    b.setUv(u, v);
                    return this;
                }

                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
                    b.setUv1(u, v);
                    return this;
                }

                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
                    b.setUv2(u, v);
                    return this;
                }

                @Override
                public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
                    b.setNormal(x, y, z);
                    return this;
                }
            }, state, fluid);
        }

        private static BufferBuilder builder(Map<RenderType, BufferBuilder> builders, Map<RenderType, ByteBufferBuilder> memory, RenderType t) {
            return builders.computeIfAbsent(t, k -> {
                ByteBufferBuilder bytes = new ByteBufferBuilder(k.bufferSize());
                memory.put(k, bytes);
                return new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            });
        }

        void free() {
            RenderSystem.recordRenderCall(() -> {
                meshes.values().forEach(m -> m.values().forEach(VertexBuffer::close));
                meshes.clear();
            });
        }
    }
}
