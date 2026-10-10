package com.josephison.fiatlux.client.fx;

import com.josephison.fiatlux.client.fx.FxRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * One particle class drives every Fiat Lux particle; the Style decides its life, motion, size
 * curve, colour and blend. All styles except feathers are full-bright and additively blended.
 */
public class GlowParticle extends TextureSheetParticle {
    public enum Style { SPARK, FLAME, MOTE, FEATHER, EMBER, RADIANCE }

    private final SpriteSet sprites;
    private final Style style;
    private final float baseSize;
    private final float spin;
    private final float phase;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, Style style) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.style = style;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.hasPhysics = false;
        switch (style) {
            case SPARK -> {
                lifetime = 18 + random.nextInt(18);
                baseSize = 0.06f + random.nextFloat() * 0.06f;
                gravity = -0.01f;
                friction = 0.92f;
                setColor(1f, 0.92f + random.nextFloat() * 0.08f, 0.62f + random.nextFloat() * 0.3f);
                spin = 0f;
            }
            case FLAME -> {
                lifetime = 14 + random.nextInt(10);
                baseSize = 0.16f + random.nextFloat() * 0.12f;
                gravity = -0.025f;
                friction = 0.9f;
                setColor(1f, 0.85f + random.nextFloat() * 0.15f, 0.55f + random.nextFloat() * 0.25f);
                spin = (random.nextFloat() - 0.5f) * 0.1f;
            }
            case MOTE -> {
                lifetime = 80 + random.nextInt(80);
                baseSize = 0.05f + random.nextFloat() * 0.08f;
                gravity = -0.0015f;
                friction = 0.98f;
                xd += (random.nextFloat() - 0.5f) * 0.01f;
                zd += (random.nextFloat() - 0.5f) * 0.01f;
                setColor(1f, 0.95f, 0.78f + random.nextFloat() * 0.2f);
                spin = 0f;
            }
            case FEATHER -> {
                lifetime = 60 + random.nextInt(60);
                baseSize = 0.12f + random.nextFloat() * 0.06f;
                gravity = 0.006f;
                friction = 0.95f;
                setColor(1f, 0.98f, 0.92f);
                spin = (random.nextFloat() - 0.5f) * 0.12f;
                roll = random.nextFloat() * Mth.TWO_PI;
            }
            case EMBER -> {
                lifetime = 24 + random.nextInt(30);
                baseSize = 0.05f + random.nextFloat() * 0.05f;
                gravity = -0.004f;
                friction = 0.94f;
                setColor(1f, 0.45f + random.nextFloat() * 0.35f, 0.12f);
                spin = 0f;
            }
            default -> { // RADIANCE
                lifetime = 20 + random.nextInt(16);
                baseSize = 0.5f + random.nextFloat() * 0.5f;
                gravity = 0f;
                friction = 0.9f;
                setColor(1f, 0.94f, 0.75f);
                spin = 0f;
            }
        }
        this.quadSize = baseSize;
        this.oRoll = roll;
        if (style == Style.FLAME) setSpriteFromAge(sprites);
        else pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        float t = age / (float) lifetime;
        switch (style) {
            case FLAME -> setSpriteFromAge(sprites);
            case FEATHER -> {
                // swaying fall
                xd += Mth.sin(age * 0.18f + phase) * 0.004f;
                zd += Mth.cos(age * 0.15f + phase) * 0.004f;
            }
            case MOTE -> xd += Mth.sin(age * 0.05f + phase) * 0.0008f;
            case EMBER -> {
                xd += (random.nextFloat() - 0.5f) * 0.004f;
                zd += (random.nextFloat() - 0.5f) * 0.004f;
            }
            default -> {
            }
        }
        alpha = switch (style) {
            case SPARK -> (1f - t) * (0.6f + 0.4f * Mth.sin(age * 1.3f + phase)); // twinkle
            case MOTE -> Mth.sin(t * Mth.PI) * (0.55f + 0.45f * Mth.sin(age * 0.2f + phase));
            case FEATHER -> t < 0.8f ? 1f : (1f - t) * 5f;
            case EMBER -> (1f - t) * (0.7f + 0.3f * random.nextFloat());
            case RADIANCE -> Mth.sin(t * Mth.PI) * 0.45f;
            default -> 1f - t * t;
        };
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = (age + partialTick) / lifetime;
        return switch (style) {
            case FLAME -> baseSize * (1f - t * 0.6f);
            case RADIANCE -> baseSize * (0.6f + t * 0.8f);
            case SPARK -> baseSize * (1f - t * 0.5f);
            default -> baseSize;
        };
    }

    @Override
    public ParticleRenderType getRenderType() {
        return style == Style.FEATHER ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : FxRenderTypes.ADDITIVE_GLOW;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites, Style style) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, style);
        }
    }
}
