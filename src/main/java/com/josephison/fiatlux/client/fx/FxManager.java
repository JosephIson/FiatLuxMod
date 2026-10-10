package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.network.FxPayload;
import com.josephison.fiatlux.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side director of divine spectacle. Server events (FxPayload) are composed from particle
 * bursts, additive light geometry (expanding rings, celestial pillars) and screen flashes.
 */
public final class FxManager {
    private static final class Ring {
        final Vec3 c; final float maxR; final int color; final int life; final float width; final boolean wall;
        int age;
        Ring(Vec3 c, float maxR, int color, int life, float width, boolean wall) {
            this.c = c; this.maxR = maxR; this.color = color; this.life = life; this.width = width; this.wall = wall;
        }
    }

    private static final class Beam {
        final Vec3 base; final float height; final float width; final int color; final int life;
        int age;
        Beam(Vec3 base, float height, float width, int color, int life) {
            this.base = base; this.height = height; this.width = width; this.color = color; this.life = life;
        }
    }

    /** A jagged fracture in the sky (the Fall): a polyline drawn as a camera-facing ribbon. */
    private static final class Rift {
        final Vec3[] pts; final float width; final int color; final int life;
        int age;
        Rift(Vec3[] pts, float width, int color, int life) {
            this.pts = pts; this.width = width; this.color = color; this.life = life;
        }
    }

    private static final List<Rift> RIFTS = new ArrayList<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static float flash;
    private static int flashColor = 0xFFFFF4D6;

    private FxManager() {}

    public static float flashAlpha() {
        return flash;
    }

    public static int flashColor() {
        return flashColor;
    }

    // ------------------------------------------------------------------ events

    public static void handle(FxPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 at = new Vec3(p.x(), p.y(), p.z());
        float r = p.radius();
        int col = p.color();
        RandomSource rand = level.random;
        switch (FxKind.byId(p.kind())) {
            case FIAT_LUX -> {
                RINGS.add(new Ring(at, r * 1.2f, 0xFFFFC850, 34, 0.9f, true));
                RINGS.add(new Ring(at, r * 0.65f, 0xFFFFF0B0, 24, 0.5f, false));
                BEAMS.add(new Beam(at, 6f, r * 0.12f, col, 16));
                burstSphere(level, ModParticles.HOLY_SPARK.get(), at, 140, 0.25, 0.7);
                burstSphere(level, ModParticles.RADIANCE.get(), at, 18, 0.05, 0.15);
                scatter(level, ModParticles.LIGHT_MOTE.get(), at, r, 60);
                flashIfNear(at, r * 3f, 0.6f, col);
            }
            case CELESTIAL_PILLAR -> {
                BEAMS.add(new Beam(at, Math.max(24f, r), 1.6f, col, p.duration() + 12));
                RINGS.add(new Ring(at.add(0, 0.05, 0), 5f, col, 22, 0.7f, true));
                for (int i = 0; i < dens(70); i++) {
                    double h = rand.nextDouble() * Math.min(r, 60);
                    level.addParticle(ModParticles.HOLY_SPARK.get(), at.x + rand.nextGaussian() * 0.6, at.y + h,
                            at.z + rand.nextGaussian() * 0.6, rand.nextGaussian() * 0.05, 0.05 + rand.nextDouble() * 0.2, rand.nextGaussian() * 0.05);
                }
                burstSphere(level, ModParticles.RADIANCE.get(), at.add(0, 0.5, 0), 10, 0.05, 0.12);
                burstSphere(level, ModParticles.EMBER.get(), at.add(0, 0.2, 0), 30, 0.1, 0.35);
                flashIfNear(at, 40f, 0.35f, col);
            }
            case SHOCKWAVE -> {
                RINGS.add(new Ring(at.add(0, 0.1, 0), r, col, 24, 1.2f, true));
                RINGS.add(new Ring(at.add(0, 0.1, 0), r * 0.6f, 0xFFFFFFFF, 18, 0.6f, false));
                for (int i = 0; i < dens(48); i++) {
                    double a = rand.nextDouble() * Mth.TWO_PI;
                    level.addParticle(ModParticles.HOLY_SPARK.get(), at.x, at.y + 0.2, at.z,
                            Math.cos(a) * r * 0.06, 0.04 + rand.nextDouble() * 0.06, Math.sin(a) * r * 0.06);
                }
                level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 0, 0, 0);
            }
            case FIRE_WAVE -> {
                RINGS.add(new Ring(at, r, col, 14, 0.9f, true));
                burstSphere(level, ModParticles.DIVINE_FLAME.get(), at, dens(80), r * 0.04, r * 0.09);
                burstSphere(level, ModParticles.EMBER.get(), at, dens(40), 0.1, 0.4);
            }
            case BLESSING -> {
                for (int i = 0; i < dens(48); i++) {
                    double a = i * 0.55;
                    double y = (i / 48.0) * Math.max(1.0, r);
                    level.addParticle(ModParticles.HOLY_SPARK.get(), at.x + Math.cos(a) * 0.75, at.y + y, at.z + Math.sin(a) * 0.75,
                            -Math.sin(a) * 0.03, 0.03, Math.cos(a) * 0.03);
                }
                burstSphere(level, ModParticles.RADIANCE.get(), at.add(0, r * 0.5, 0), 6, 0.0, 0.05);
            }
            case DELUGE -> {
                RINGS.add(new Ring(at, r, 0xFF6FA8FF, 60, 1.5f, true));
                for (int i = 0; i < dens(400); i++) {
                    double dx = (rand.nextDouble() - 0.5) * 2 * r, dz = (rand.nextDouble() - 0.5) * 2 * r;
                    level.addParticle(ParticleTypes.FALLING_WATER, at.x + dx, at.y + 10 + rand.nextDouble() * 16, at.z + dz, 0, -0.5, 0);
                    if (i % 4 == 0) level.addParticle(ParticleTypes.SPLASH, at.x + dx, at.y + 0.2, at.z + dz, 0, 0.1, 0);
                }
                flashIfNear(at, r * 3f, 0.25f, 0xFF9CC4FF);
            }
            case GLORY_FLASH -> {
                burstSphere(level, ModParticles.RADIANCE.get(), at, dens(20), 0.05, 0.2);
                burstSphere(level, ModParticles.HOLY_SPARK.get(), at, dens(60), 0.15, 0.45);
                flashIfNear(at, r * 4f, 0.75f, col);
            }
            case BIG_BANG -> {
                flash = 1f;
                flashColor = 0xFFFFFFFF;
                BEAMS.add(new Beam(at.subtract(0, 2, 0), 220f, 3.5f, 0xFFFFFFFF, 60));
                RINGS.add(new Ring(at.subtract(0, 1, 0), 48f, 0xFFFFE6A0, 80, 3f, true));
                RINGS.add(new Ring(at.subtract(0, 1, 0), 30f, 0xFFB48CFF, 70, 2f, true));
                RINGS.add(new Ring(at.subtract(0, 1, 0), 18f, 0xFF7FD0FF, 60, 1.5f, false));
                burstSphere(level, ModParticles.RADIANCE.get(), at, dens(60), 0.3, 1.2);
                burstSphere(level, ModParticles.HOLY_SPARK.get(), at, dens(300), 0.4, 1.6);
            }
            case PLANET_GENESIS -> {
                RINGS.add(new Ring(at.subtract(0, 1, 0), 10f, col, 40, 1.2f, true));
                for (int i = 0; i < dens(90); i++) {
                    double a = rand.nextDouble() * Mth.TWO_PI, rr = 3 + rand.nextDouble() * 4;
                    level.addParticle(ModParticles.LIGHT_MOTE.get(), at.x + Math.cos(a) * rr, at.y - 0.5, at.z + Math.sin(a) * rr,
                            -Math.cos(a) * 0.05, 0.08 + rand.nextDouble() * 0.06, -Math.sin(a) * 0.05);
                }
                burstSphere(level, ModParticles.HOLY_SPARK.get(), at, dens(60), 0.1, 0.4);
                flashIfNear(at, 20f, 0.35f, col);
            }
            case HALO_SHATTER -> {
                boolean mutation = ((col >> 8) & 255) < 80;   // the second burst: wings tearing as it becomes a fallen one
                if (!mutation) {
                    RINGS.add(new Ring(at, r * 1.6f, 0xFFFFE08A, 16, 0.35f, false));
                    for (int i = 0; i < dens(70); i++) {
                        Vec3 d = new Vec3(rand.nextGaussian(), rand.nextGaussian() * 0.5 + 0.4, rand.nextGaussian()).normalize();
                        double sp = 0.15 + rand.nextDouble() * 0.35;
                        level.addParticle(ModParticles.HALO_SHARD.get(), at.x, at.y, at.z, d.x * sp, d.y * sp, d.z * sp);
                    }
                    burstSphere(level, ModParticles.HOLY_SPARK.get(), at, dens(60), 0.2, 0.6);
                    burstSphere(level, ParticleTypes.ELECTRIC_SPARK, at, dens(40), 0.2, 0.7);
                    flashIfNear(at, 48f, 0.55f, 0xFFFFFFFF);
                } else {
                    burstSphere(level, ParticleTypes.LARGE_SMOKE, at, dens(40), 0.05, 0.25);
                    burstSphere(level, ModParticles.EMBER.get(), at, dens(60), 0.1, 0.5);
                    flashIfNear(at, 32f, 0.35f, 0xFFB01A10);
                }
                for (int i = 0; i < dens(mutation ? 70 : 40); i++) {
                    level.addParticle(ModParticles.TORN_FEATHER.get(), at.x + rand.nextGaussian() * 0.5, at.y + rand.nextGaussian() * 0.5,
                            at.z + rand.nextGaussian() * 0.5, rand.nextGaussian() * 0.12, rand.nextDouble() * 0.15, rand.nextGaussian() * 0.12);
                }
            }
            case SKY_FRACTURE -> {
                Vec3[] trunk = jagged(rand, at.add(0, 80, 0), at.subtract(0, 60, 0), 34, 1.6);
                RIFTS.add(new Rift(trunk, 1.2f, col, p.duration()));
                for (int b = 0; b < 5; b++) {   // splinters branching off the main fracture
                    Vec3 from = trunk[4 + rand.nextInt(trunk.length - 10)];
                    Vec3 to = from.add(rand.nextGaussian() * 12, -6 - rand.nextDouble() * 14, rand.nextGaussian() * 12);
                    RIFTS.add(new Rift(jagged(rand, from, to, 8, 0.9), 0.45f, col, p.duration() - 10));
                }
                for (int i = 0; i < dens(120); i++) {
                    Vec3 q = trunk[rand.nextInt(trunk.length)];
                    level.addParticle(ModParticles.EMBER.get(), q.x + rand.nextGaussian() * 0.6, q.y, q.z + rand.nextGaussian() * 0.6,
                            rand.nextGaussian() * 0.04, -0.05 - rand.nextDouble() * 0.2, rand.nextGaussian() * 0.04);
                }
                burstSphere(level, ParticleTypes.LARGE_SMOKE, at, dens(30), 0.05, 0.2);
                flashIfNear(at, 80f, 0.45f, 0xFFC0201A);
            }
            case FALLEN_IMPACT -> {
                RINGS.add(new Ring(at.add(0, 0.1, 0), r * 1.8f, col, 34, 1.6f, true));
                RINGS.add(new Ring(at.add(0, 0.1, 0), r, 0xFFFFA040, 24, 0.8f, true));
                BEAMS.add(new Beam(at, 26f, 1.4f, col, 22));
                for (int i = 0; i < dens(50); i++) {
                    double a = rand.nextDouble() * Mth.TWO_PI;
                    level.addParticle(ParticleTypes.LAVA, at.x + Math.cos(a), at.y + 0.3, at.z + Math.sin(a), 0, 0, 0);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, at.x + Math.cos(a) * 2, at.y + 0.4, at.z + Math.sin(a) * 2,
                            Math.cos(a) * 0.25, 0.1 + rand.nextDouble() * 0.2, Math.sin(a) * 0.25);
                }
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.EXPLOSION, at.x + rand.nextGaussian() * 2, at.y + 0.5, at.z + rand.nextGaussian() * 2, 0, 0, 0);
                }
                burstSphere(level, ModParticles.EMBER.get(), at.add(0, 0.5, 0), dens(90), 0.2, 0.8);
                flashIfNear(at, 40f, 0.5f, 0xFFD0301A);
            }
            case HOST_ARRIVAL -> {
                BEAMS.add(new Beam(at.subtract(0, 1, 0), 40f, 1.1f, col, 26));
                RINGS.add(new Ring(at.subtract(0, 0.9, 0), r * 2f + 2f, col, 26, 0.8f, true));
                for (int i = 0; i < dens(44); i++) {
                    level.addParticle(ModParticles.FEATHER.get(), at.x + rand.nextGaussian() * r, at.y + 2 + rand.nextDouble() * 3,
                            at.z + rand.nextGaussian() * r, rand.nextGaussian() * 0.02, -0.01, rand.nextGaussian() * 0.02);
                }
                burstSphere(level, ModParticles.RADIANCE.get(), at, dens(16), 0.05, 0.2);
                burstSphere(level, ModParticles.HOLY_SPARK.get(), at, dens(80), 0.2, 0.5);
                flashIfNear(at, 24f, 0.4f, col);
            }
        }
    }

    /** A jagged lightning-like polyline from a to b: a bounded random walk around the straight line. */
    private static Vec3[] jagged(RandomSource rand, Vec3 a, Vec3 b, int n, double amp) {
        Vec3[] pts = new Vec3[n + 1];
        double ox = 0, oz = 0;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            ox = Mth.clamp(ox + rand.nextGaussian() * amp, -amp * 3, amp * 3);
            oz = Mth.clamp(oz + rand.nextGaussian() * amp, -amp * 3, amp * 3);
            double taper = Math.sin(t * Math.PI);
            pts[i] = a.lerp(b, t).add(ox * taper, 0, oz * taper);
        }
        return pts;
    }

    private static int dens(int n) {
        return Math.max(1, (int) (n * FiatLuxConfig.CLIENT.particleDensity.get()));
    }

    private static void burstSphere(ClientLevel level, ParticleOptions type, Vec3 at, int n, double minSpeed, double maxSpeed) {
        RandomSource r = level.random;
        for (int i = 0; i < dens(n); i++) {
            Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
            double s = minSpeed + r.nextDouble() * (maxSpeed - minSpeed);
            level.addParticle(type, at.x, at.y, at.z, d.x * s, d.y * s, d.z * s);
        }
    }

    private static void scatter(ClientLevel level, ParticleOptions type, Vec3 at, float radius, int n) {
        RandomSource r = level.random;
        for (int i = 0; i < dens(n); i++) {
            level.addParticle(type, at.x + (r.nextDouble() - 0.5) * 2 * radius, at.y + (r.nextDouble() - 0.5) * radius,
                    at.z + (r.nextDouble() - 0.5) * 2 * radius, 0, 0.005, 0);
        }
    }

    private static void flashIfNear(Vec3 at, float range, float strength, int color) {
        var player = Minecraft.getInstance().player;
        if (player == null || !FiatLuxConfig.CLIENT.screenFlashes.get()) return;
        double d = player.position().distanceTo(at);
        if (d > range) return;
        float s = (float) (strength * (1.0 - d / range) * FiatLuxConfig.CLIENT.screenFlashStrength.get());
        if (s > flash) {
            flash = s;
            flashColor = color;
        }
    }

    // ------------------------------------------------------------------ lifecycle

    public static void tick() {
        flash *= 0.86f;
        if (flash < 0.004f) flash = 0f;
        for (Iterator<Ring> it = RINGS.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            if (++r.age > r.life) it.remove();
        }
        for (Iterator<Beam> it = BEAMS.iterator(); it.hasNext(); ) {
            Beam b = it.next();
            if (++b.age > b.life) it.remove();
        }
        for (Iterator<Rift> it = RIFTS.iterator(); it.hasNext(); ) {
            Rift r = it.next();
            if (++r.age > r.life) it.remove();
        }
    }

    public static void clear() {
        RINGS.clear();
        BEAMS.clear();
        RIFTS.clear();
        flash = 0;
    }

    // ------------------------------------------------------------------ rendering (additive light geometry)

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (RINGS.isEmpty() && BEAMS.isEmpty() && RIFTS.isEmpty())) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        // pass 1 (alpha-blended): a deep-gold rim so light still reads against Heaven's bright sky
        VertexConsumer rim = buffers.getBuffer(RenderType.debugQuads());
        for (Ring r : RINGS) drawRingRim(rim, m, r, partial);
        for (Rift r : RIFTS) drawRift(rim, m, r, cam, partial, true);   // the dark wound in the sky
        buffers.endBatch(RenderType.debugQuads());
        // pass 2 (additive): the light itself
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Beam b : BEAMS) drawBeam(vc, m, b, partial);
        for (Ring r : RINGS) drawRing(vc, m, r, partial);
        for (Rift r : RIFTS) drawRift(vc, m, r, cam, partial, false);  // its burning edges
        buffers.endBatch(RenderType.lightning());
        ps.popPose();
    }

    private static void drawBeam(VertexConsumer vc, Matrix4f m, Beam b, float partial) {
        float t = (b.age + partial) / b.life;
        float fade = Math.min(1f, (b.age + partial) / 3f) * (t > 0.6f ? (1f - t) / 0.4f : 1f);
        if (fade <= 0) return;
        float spin = (b.age + partial) * 0.08f;
        // core, glow, halo: three nested prisms of decreasing intensity
        prism(vc, m, b.base, b.height, b.width * 0.28f, 0xFFFFFFFF, 0.95f * fade, spin, 8);
        prism(vc, m, b.base, b.height, b.width * (0.8f + 0.1f * Mth.sin(spin * 5)), b.color, 0.45f * fade, -spin, 10);
        prism(vc, m, b.base, b.height, b.width * 2.2f, b.color, 0.14f * fade, spin * 0.5f, 12);
    }

    private static void prism(VertexConsumer vc, Matrix4f m, Vec3 base, float height, float radius, int color, float alpha,
                              float rot, int sides) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, bl = color & 255;
        int a0 = (int) (alpha * 255), a1 = 0;
        float x = (float) base.x, y = (float) base.y, z = (float) base.z;
        for (int i = 0; i < sides; i++) {
            float ang0 = rot + Mth.TWO_PI * i / sides, ang1 = rot + Mth.TWO_PI * (i + 1) / sides;
            float x0 = x + Mth.cos(ang0) * radius, z0 = z + Mth.sin(ang0) * radius;
            float x1 = x + Mth.cos(ang1) * radius, z1 = z + Mth.sin(ang1) * radius;
            // bright at the base, dissolving into the heavens at the top
            vc.addVertex(m, x0, y, z0).setColor(r, g, bl, a0);
            vc.addVertex(m, x1, y, z1).setColor(r, g, bl, a0);
            vc.addVertex(m, x1, y + height, z1).setColor(r, g, bl, a1);
            vc.addVertex(m, x0, y + height, z0).setColor(r, g, bl, a1);
            vc.addVertex(m, x0, y + height, z0).setColor(r, g, bl, a1);
            vc.addVertex(m, x1, y + height, z1).setColor(r, g, bl, a1);
            vc.addVertex(m, x1, y, z1).setColor(r, g, bl, a0);
            vc.addVertex(m, x0, y, z0).setColor(r, g, bl, a0);
        }
    }

    /**
     * A rift opens fast, holds, and seals. Dark pass: a near-black crimson core (alpha-blended so it
     * shows even against a bright sky). Light pass: a wide crimson glow and a thin white-hot seam.
     */
    private static void drawRift(VertexConsumer vc, Matrix4f m, Rift rift, Vec3 cam, float partial, boolean dark) {
        float t = (rift.age + partial) / rift.life;
        float open = Math.min(1f, t / 0.12f) * (t > 0.75f ? Math.max(0f, (1f - t) / 0.25f) : 1f);
        if (open <= 0) return;
        if (dark) {
            ribbon(vc, m, rift.pts, cam, rift.width * open, 0x1A0406, 0.92f * open);
        } else {
            ribbon(vc, m, rift.pts, cam, rift.width * 3.2f * open, rift.color, 0.35f * open);
            ribbon(vc, m, rift.pts, cam, rift.width * 1.4f * open, rift.color, 0.7f * open);
            ribbon(vc, m, rift.pts, cam, rift.width * 0.25f * open, 0xFFF0C8, 0.95f * open);
        }
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3[] pts, Vec3 cam, float width, int color, float alpha) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255, a = (int) (alpha * 255);
        for (int i = 0; i < pts.length - 1; i++) {
            Vec3 p0 = pts[i], p1 = pts[i + 1];
            Vec3 dir = p1.subtract(p0);
            Vec3 side = dir.cross(p0.subtract(cam)).normalize();
            float w0 = width * (0.35f + 0.65f * Mth.sin((float) Math.PI * i / (pts.length - 1)));
            float w1 = width * (0.35f + 0.65f * Mth.sin((float) Math.PI * (i + 1) / (pts.length - 1)));
            Vec3 a0 = p0.add(side.scale(w0)), b0 = p0.subtract(side.scale(w0));
            Vec3 a1 = p1.add(side.scale(w1)), b1 = p1.subtract(side.scale(w1));
            quad2(vc, m, a0, a1, b1, b0, r, g, b, a);
        }
    }

    private static void quad2(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int r, int g, int b, int a) {
        vc.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        vc.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
    }

    private static void drawRingRim(VertexConsumer vc, Matrix4f m, Ring ring, float partial) {
        float t = Math.min(1f, (ring.age + partial) / ring.life);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        float outer = Math.max(0.05f, ring.maxR * ease);
        float inner = Math.max(0f, outer - ring.width * 0.6f);
        int r = (int) (((ring.color >> 16) & 255) * 0.85f), g = (int) (((ring.color >> 8) & 255) * 0.62f), b = (int) ((ring.color & 255) * 0.25f);
        int a = (int) ((1f - t) * 140);
        float cx = (float) ring.c.x, cy = (float) ring.c.y + 0.04f, cz = (float) ring.c.z;
        for (int i = 0; i < 72; i++) {
            float a0 = Mth.TWO_PI * i / 72, a1 = Mth.TWO_PI * (i + 1) / 72;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            vc.addVertex(m, cx + c0 * inner, cy, cz + s0 * inner).setColor(r, g, b, 0);
            vc.addVertex(m, cx + c1 * inner, cy, cz + s1 * inner).setColor(r, g, b, 0);
            vc.addVertex(m, cx + c1 * outer, cy, cz + s1 * outer).setColor(r, g, b, a);
            vc.addVertex(m, cx + c0 * outer, cy, cz + s0 * outer).setColor(r, g, b, a);
        }
    }

    private static void drawRing(VertexConsumer vc, Matrix4f m, Ring ring, float partial) {
        float t = Math.min(1f, (ring.age + partial) / ring.life);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        float outer = Math.max(0.05f, ring.maxR * ease);
        float inner = Math.max(0f, outer - ring.width * (1f + t));
        float alpha = (1f - t) * 0.85f;
        int r = (ring.color >> 16) & 255, g = (ring.color >> 8) & 255, b = ring.color & 255;
        int aIn = 0, aOut = (int) (alpha * 255);
        float cx = (float) ring.c.x, cy = (float) ring.c.y + 0.06f, cz = (float) ring.c.z;
        int seg = 72;
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            // flat annulus, soft inner edge
            vc.addVertex(m, cx + c0 * inner, cy, cz + s0 * inner).setColor(r, g, b, aIn);
            vc.addVertex(m, cx + c1 * inner, cy, cz + s1 * inner).setColor(r, g, b, aIn);
            vc.addVertex(m, cx + c1 * outer, cy, cz + s1 * outer).setColor(r, g, b, aOut);
            vc.addVertex(m, cx + c0 * outer, cy, cz + s0 * outer).setColor(r, g, b, aOut);
            vc.addVertex(m, cx + c0 * outer, cy, cz + s0 * outer).setColor(r, g, b, aOut);
            vc.addVertex(m, cx + c1 * outer, cy, cz + s1 * outer).setColor(r, g, b, aOut);
            vc.addVertex(m, cx + c1 * inner, cy, cz + s1 * inner).setColor(r, g, b, aIn);
            vc.addVertex(m, cx + c0 * inner, cy, cz + s0 * inner).setColor(r, g, b, aIn);
            if (ring.wall) {
                // a short wall of light rising from the ring's edge
                float h = 1.4f * (1f - t);
                vc.addVertex(m, cx + c0 * outer, cy, cz + s0 * outer).setColor(r, g, b, aOut);
                vc.addVertex(m, cx + c1 * outer, cy, cz + s1 * outer).setColor(r, g, b, aOut);
                vc.addVertex(m, cx + c1 * outer, cy + h, cz + s1 * outer).setColor(r, g, b, 0);
                vc.addVertex(m, cx + c0 * outer, cy + h, cz + s0 * outer).setColor(r, g, b, 0);
                vc.addVertex(m, cx + c0 * outer, cy + h, cz + s0 * outer).setColor(r, g, b, 0);
                vc.addVertex(m, cx + c1 * outer, cy + h, cz + s1 * outer).setColor(r, g, b, 0);
                vc.addVertex(m, cx + c1 * outer, cy, cz + s1 * outer).setColor(r, g, b, aOut);
                vc.addVertex(m, cx + c0 * outer, cy, cz + s0 * outer).setColor(r, g, b, aOut);
            }
        }
    }
}
