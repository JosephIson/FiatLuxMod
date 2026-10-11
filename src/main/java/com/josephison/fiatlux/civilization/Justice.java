package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.civilization.Polity.Crime;
import com.josephison.fiatlux.civilization.Polity.Form;
import com.josephison.fiatlux.civilization.Polity.Law;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.entity.PunishmentPropEntity;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Crime and punishment among mortals. When a mortal steals, strikes another, kills, worships what the
 * state forbids or plots against the crown, and someone sees it, and the people have a law against it,
 * the offender is judged by the ruler and punished as their age punishes: restitution and exile in
 * clans and chiefdoms ("a fugitive and a vagabond shalt thou be", Gen 4:12), the stocks and prison
 * in kingdoms, and under empires the cross, "without the gate" (Heb 13:12) - shown with dignity, not
 * gore. Faithful mortals persecuted by a pagan state die as martyrs and go up into Heaven. A Creator
 * may deliver any condemned mortal: "the angel of the Lord by night opened the prison doors" (Acts 5:19).
 */
public final class Justice {
    public enum Sentence {
        NONE("free"), RESTITUTION("made to restore what was taken"), STOCKS("in the stocks"), PRISON("in prison"),
        EXILE("in exile"), CROSS("upon a cross");
        public final String words;

        Sentence(String words) {
            this.words = words;
        }
    }

    private Justice() {}

    /** What a people's government does to one who commits this crime. */
    public static Sentence punishment(Form form, Crime crime) {
        boolean tribal = form == Form.CLAN || form == Form.CHIEFDOM || form == Form.JUDGES;
        boolean modern = form == Form.REPUBLIC || form == Form.FEDERATION;
        return switch (crime) {
            case THEFT -> modern ? Sentence.PRISON : tribal ? Sentence.RESTITUTION : Sentence.STOCKS;
            case VIOLENCE -> modern ? Sentence.PRISON : tribal ? Sentence.RESTITUTION : Sentence.STOCKS;
            case IDOLATRY -> form == Form.JUDGES ? Sentence.EXILE : Sentence.RESTITUTION;
            case MURDER, HERESY, SEDITION -> tribal ? Sentence.EXILE : form == Form.EMPIRE ? Sentence.CROSS : Sentence.PRISON;
        };
    }

    /** A crime has been done. If it is seen and there is a law against it, the offender is judged. */
    public static void report(ServerLevel level, MortalEntity offender, Crime crime, @Nullable MortalEntity victim) {
        Planet world = offender.planet();
        if (world == null || offender.sentence() != Sentence.NONE || offender.isChild() || offender.isMarked()) return;   // Cain is the Creator's to judge
        if (com.josephison.fiatlux.son.TheSon.isSon(offender)) return;   // his hour is not yet come (John 7:30)
        Polity g = Polities.get(level.getServer()).of(world.id);
        if (g == null || g.laws.stream().noneMatch(l -> l.crime == crime)) return;
        if (offender.getUUID().equals(g.ruler)) return;   // the king is above his own law (and the people murmur)
        if (offender.isNephil()) return;   // none dare lay hands on the mighty men (Gen 6:4)
        List<MortalEntity> witnesses = level.getEntitiesOfClass(MortalEntity.class, offender.getBoundingBox().inflate(12),
                m -> m != offender && m != victim && !m.isChild() && m.isAlive());
        if (witnesses.isEmpty() && level.random.nextBoolean()) return;   // no one saw
        judge(level, g, world, offender, crime);
    }

    public static void judge(ServerLevel level, Polity g, Planet world, MortalEntity offender, Crime crime) {
        Sentence s = punishment(g.form, crime);
        g.judged++;
        g.punished++;
        MortalEntity judge = ruler(level, g, offender);
        String name = offender.getName().getString();
        String verdict = switch (s) {
            case RESTITUTION -> name + ", you shall restore what you took, twice over.";
            case STOCKS -> name + ", for " + crime.word + ", a day in the stocks.";
            case PRISON -> name + ", for " + crime.word + ", you shall be bound in prison.";
            case EXILE -> name + ", for " + crime.word + ", you are cast out from among us.";
            case CROSS -> name + ", for " + crime.word + ", you shall be crucified outside the city.";
            default -> name + ", go, and sin no more.";
        };
        if (judge != null) judge.say(verdict);
        else for (MortalEntity w : level.getEntitiesOfClass(MortalEntity.class, offender.getBoundingBox().inflate(12), m -> m != offender)) {
            w.say("By order of " + g.rulerTitle() + ": " + verdict);
            break;
        }
        Religion faith = Faiths.of(offender);
        boolean martyr = crime == Crime.HERESY && faith != null && faith.deity.true_;
        offender.say(martyr ? "I will not bow down. My God made the heavens." : switch (s) {
            case CROSS -> "Mercy! Have mercy on me!";
            case EXILE -> "Where will I go? Who will take me in?";
            default -> "I... I am sorry.";
        });
        Settlement town = offender.settlement();
        BlockPos base = town != null ? town.centre : offender.blockPosition();
        long now = level.getGameTime();
        int year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        switch (s) {
            case RESTITUTION -> {
                if (town != null) town.food += 4;
                offender.beginAct(MortalEntity.BOW, 60);
            }
            case STOCKS -> {
                BlockPos at = stocks(level, base, charge(offender, crime));
                if (at != null) offender.sentence(s, crime, now + year, at);
            }
            case PRISON -> {
                BlockPos at = prison(level, base);
                if (at != null) offender.sentence(s, crime, now + (crime == Crime.THEFT || crime == Crime.VIOLENCE ? 2L : 6L) * year, at);
            }
            case EXILE -> exile(level, offender, crime);
            case CROSS -> {
                BlockPos at = cross(level, base, charge(offender, crime));
                if (at != null) offender.sentence(s, crime, now + 3000, at);
            }
            default -> {
            }
        }
        if (martyr) offender.addVirtue(20);   // "Blessed are they which are persecuted for righteousness' sake" (Matt 5:10)
        String line = "judged " + name + " of " + world.name + " for " + crime.word + ": " + s.words + ".";
        g.annal(g.rulerTitle() + " " + line);
        if (s == Sentence.EXILE || s == Sentence.CROSS || s == Sentence.PRISON && crime != Crime.THEFT && crime != Crime.VIOLENCE) {
            CosmosData.get(level.getServer()).record(level.getServer(), g.rulerTitle() + " of the " + g.name, "justice", line);
        }
        Polities.get(level.getServer()).setDirty();
    }

    /** "A fugitive and a vagabond shalt thou be in the earth" (Gen 4:12). */
    public static void exile(ServerLevel level, MortalEntity m, Crime crime) {
        double a = level.random.nextDouble() * Math.PI * 2;
        BlockPos far = MortalPopulation.safeGround(level, m.getBlockX() + (int) (Math.cos(a) * 140), m.getBlockZ() + (int) (Math.sin(a) * 140), 30);
        if (far == null) return;
        m.sentence(Sentence.EXILE, crime, Long.MAX_VALUE, far);
        m.leaveFor(far);
    }

    /** Holds a condemned mortal to its sentence, each tick; releases it (or lets it die) when the time is up. */
    public static void hold(ServerLevel level, MortalEntity m) {
        Sentence s = m.sentence();
        BlockPos at = m.sentencePlace();
        if (at == null) {
            m.release();
            return;
        }
        long now = level.getGameTime();
        switch (s) {
            case CROSS -> {
                m.setNoGravity(true);
                m.setDeltaMovement(Vec3.ZERO);
                m.moveTo(at.getX() + 0.5, at.getY() + CROSS_FOOT, at.getZ() + 0.5, 180f, 0f);
                m.setYBodyRot(180f);
                m.setYHeadRot(180f);
                if (m.act() != MortalEntity.CRUCIFIED) m.beginAct(MortalEntity.CRUCIFIED, Integer.MAX_VALUE);
                if (now >= m.sentenceUntil()) {
                    Religion faith = Faiths.of(m);
                    m.say(faith != null && faith.deity.true_ ? "Into thy hands I commend my spirit." : "It is... finished.");
                    for (MortalEntity w : level.getEntitiesOfClass(MortalEntity.class, m.getBoundingBox().inflate(16), x -> x != m)) {
                        w.beginAct(MortalEntity.BOW, 80);
                    }
                    m.setNoGravity(false);
                    m.release();
                    m.hurt(level.damageSources().magic(), Float.MAX_VALUE);
                }
            }
            case STOCKS -> {
                m.setDeltaMovement(Vec3.ZERO);
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
                m.setYBodyRot(180f);
                m.setYHeadRot(180f);
                if (m.act() != MortalEntity.STOCKS) m.beginAct(MortalEntity.STOCKS, Integer.MAX_VALUE);
                if (now >= m.sentenceUntil()) {
                    m.release();
                    m.say("Free... I have paid for it.");
                }
            }
            case PRISON -> {
                if (m.distanceToSqr(Vec3.atBottomCenterOf(at)) > 1.6) m.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                if (m.act() != MortalEntity.IMPRISONED) m.beginAct(MortalEntity.IMPRISONED, Integer.MAX_VALUE);
                if (level.random.nextInt(1200) == 0) m.say(level.random.nextBoolean() ? "Let me out..." : "How long, O Lord? How long?");
                if (now >= m.sentenceUntil()) {
                    m.release();
                    BlockPos out = MortalPopulation.safeGround(level, at.getX() + 4, at.getZ(), 4);
                    if (out != null) m.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
                    m.say("Free at last!");
                }
            }
            default -> {
            }
        }
    }

    /** A Creator delivers a condemned mortal. */
    public static void deliver(ServerPlayer creator, MortalEntity m) {
        ServerLevel level = creator.serverLevel();
        Sentence s = m.sentence();
        m.setNoGravity(false);
        m.release();
        BlockPos out = MortalPopulation.safeGround(level, m.getBlockX() + 2, m.getBlockZ() + 2, 5);
        if (out != null) m.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
        m.say("I am free! The Lord has delivered me!");
        m.beginAct(MortalEntity.CHEER, 60);
        Fx.send(level, FxKind.CELESTIAL_PILLAR, m.position(), 12f, 0xFFFFF0C0, 30);
        Faiths.onMiracle(level, m.position(), creator, false);
        CosmosData.get(level.getServer()).record(level.getServer(), creator.getGameProfile().getName(), "deliverance",
                "delivered " + m.getName().getString() + " from " + (s == Sentence.CROSS ? "the cross" : s == Sentence.PRISON ? "prison" : "punishment") + ".");
    }

    /** The proud rise against the crown: they seize it, or are taken for sedition. */
    public static void plot(ServerLevel level, Polity g, Planet world) {
        Generations gens = Generations.get(level.getServer());
        MortalEntity rebel = null;
        for (Settlement t : CosmicExpanseSavedData.get(level.getServer()).townsOf(world.id)) {
            if (!level.isLoaded(t.centre)) continue;
            for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(t.centre).inflate(80),
                    m -> m.hasTrait(Trait.PROUD) && !m.isChild() && !m.getUUID().equals(g.ruler) && m.sentence() == Sentence.NONE)) {
                rebel = m;
                break;
            }
            if (rebel != null) break;
        }
        if (rebel == null) return;
        Person p = gens.person(rebel.getUUID());
        if (p == null) return;
        if (level.random.nextBoolean()) {
            String was = g.rulerTitle();
            MortalEntity old = level.getEntity(g.ruler) instanceof MortalEntity o ? o : null;
            Government.crown(gens, g, p, level.getGameTime());
            rebel.say("The crown is mine now!");
            Government.record(level.getServer(), g, p.name, "rose against " + was + " and seized the throne of the " + g.name + ".");
            if (old != null) exile(level, old, Crime.SEDITION);
        } else {
            rebel.say("Down with the " + g.form.title(g.rulerMale) + "!");
            judge(level, g, world, rebel, Crime.SEDITION);
        }
    }

    @Nullable
    private static MortalEntity ruler(ServerLevel level, Polity g, Entity near) {
        if (g.ruler == null) return null;
        Entity e = level.getEntity(g.ruler);
        return e instanceof MortalEntity m && m.distanceToSqr(near) < 64 * 64 ? m : null;
    }

    // ------------------------------------------------------------------ the places of punishment

    /** The height of the cross's footrest, where the condemned's feet rest (model units; see punishment.py). */
    public static final double CROSS_FOOT = 14.9 / 16.0;

    /** The stocks, in the town square. */
    @Nullable
    private static BlockPos stocks(ServerLevel level, BlockPos centre, String charge) {
        BlockPos g = MortalPopulation.safeGround(level, centre.getX() + 5, centre.getZ() + 5, 4);
        return g == null ? null : prop(level, g, PunishmentPropEntity.STOCKS, charge);
    }

    /** A small stone prison at the edge of town, with barred windows and an iron door. */
    @Nullable
    private static BlockPos prison(ServerLevel level, BlockPos centre) {
        BlockPos g = MortalPopulation.safeGround(level, centre.getX() - 13, centre.getZ() + 11, 6);
        if (g == null) return null;
        if (!level.getBlockState(g.offset(-2, 0, -2)).is(Blocks.COBBLESTONE)) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(g.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.setBlock(g.offset(dx, 3, dz), Blocks.COBBLESTONE_SLAB.defaultBlockState(), 3);
                boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int dy = 0; dy < 3; dy++) {
                    BlockState s = !wall ? Blocks.AIR.defaultBlockState()
                            : dy == 1 && (dx == 0 || dz == 0) ? Blocks.IRON_BARS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                    level.setBlock(g.offset(dx, dy, dz), s, 3);
                }
            }
            BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
            level.setBlock(g.offset(0, 0, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
            level.setBlock(g.offset(0, 1, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        }
        return g;
    }

    /** Crucifies `m` outside the town at `centre` with `charge` written over it, until `until` (or for ever). */
    @Nullable
    public static BlockPos crucify(ServerLevel level, MortalEntity m, BlockPos centre, String charge, Crime crime, long until) {
        BlockPos at = cross(level, centre, charge);
        if (at != null) m.sentence(Sentence.CROSS, crime, until, at);
        return at;
    }

    /** A cross of rough timber on the rise outside the town, beside any others standing there (Luke 23:33). */
    @Nullable
    private static BlockPos cross(ServerLevel level, BlockPos centre, String charge) {
        for (int k = 0; k < 3; k++) {
            BlockPos g = MortalPopulation.safeGround(level, centre.getX() + 28 + k * 3, centre.getZ(), 6);
            if (g == null) continue;
            boolean taken = !level.getEntitiesOfClass(MortalEntity.class, new AABB(g).inflate(0.8, 4, 0.8),
                    m -> m.sentence() == Sentence.CROSS).isEmpty();
            if (taken) continue;
            return prop(level, g, PunishmentPropEntity.CROSS, charge);
        }
        return null;
    }

    /** Takes down the cross (or stocks) standing at a place, and any left empty beside it. */
    public static void takeDown(ServerLevel level, BlockPos at) {
        for (PunishmentPropEntity prop : level.getEntitiesOfClass(PunishmentPropEntity.class, new AABB(at).inflate(8, 4, 8))) {
            BlockPos g = prop.blockPosition();
            boolean occupied = !level.getEntitiesOfClass(MortalEntity.class, new AABB(g).inflate(0.8, 4, 0.8),
                    m -> m.sentence() == Sentence.CROSS || m.sentence() == Sentence.STOCKS).isEmpty();
            if (!occupied) prop.discard();
        }
    }

    /** The prop at a place (raised if missing), with the charge written on it. */
    private static BlockPos prop(ServerLevel level, BlockPos g, int kind, String charge) {
        PunishmentPropEntity prop = level.getEntitiesOfClass(PunishmentPropEntity.class, new AABB(g).inflate(0.6, 2, 0.6),
                e -> e.kind() == kind).stream().findFirst().orElse(null);
        if (prop == null) {
            prop = ModEntities.PUNISHMENT_PROP.get().create(level);
            if (prop == null) return g;
            prop.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 180f, 0f);
            prop.setKind(kind);
            level.addFreshEntity(prop);
            level.sendParticles(ParticleTypes.CLOUD, g.getX() + 0.5, g.getY() + 1, g.getZ() + 0.5, 6, 0.3, 0.5, 0.3, 0.01);
        }
        prop.setInscription(charge);
        return g;
    }

    /** The charge as it is written up: the name, and the crime. */
    public static String charge(MortalEntity m, Crime crime) {
        String word = switch (crime) {
            case THEFT -> "THIEF";
            case VIOLENCE -> "VIOLENT";
            case MURDER -> "MURDERER";
            case IDOLATRY -> "IDOLATER";
            case HERESY -> "BLASPHEMER";
            case SEDITION -> "REBEL";
        };
        return m.getName().getString().toUpperCase(java.util.Locale.ROOT) + "\n" + word;
    }

    /** What a condemned or exiled mortal's state reads as, for its owner and reports. */
    public static Component describe(MortalEntity m) {
        return Component.literal(m.sentence() == Sentence.NONE ? "" : "Condemned for " + m.sentenceCrime().word + ": " + m.sentence().words + ".");
    }

    /** Whether a law applies on this mortal's world. */
    public static boolean lawOf(ServerLevel level, MortalEntity m, Law law) {
        Planet w = m.planet();
        Polity g = w == null ? null : Polities.get(level.getServer()).of(w.id);
        return g != null && g.laws.contains(law);
    }

    @Nullable
    public static Polity polityOf(ServerLevel level, MortalEntity m) {
        Planet w = m.planet();
        return w == null ? null : Polities.get(level.getServer()).of(w.id);
    }
}
