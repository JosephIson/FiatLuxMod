package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Marriage is honourable in all" (Heb 13:4). Two who come to love one another are betrothed, and
 * then wed at the shrine of their town by day: the couple go up to the altar and the town gathers
 * behind them; whoever leads their faith (or a priest, or the eldest) joins them with the words of
 * their faith - "What therefore God hath joined together, let not man put asunder" (Mark 10:9), a
 * handfasting under the trees, an oath sworn in blood, a contract under the law - and they speak
 * their vows; then the people cheer and the bell rings, and all go down to the hearth to feast
 * (John 2:1-10).
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Weddings {
    private enum Stage { WAITING, PROCESSION, VOWS, FEAST }

    private static final class Ceremony {
        final ResourceKey<Level> dim;
        final UUID groom, bride;
        final BlockPos shrine, centre;
        final String town;
        @Nullable UUID officiant;
        String officiantName = "";
        final List<UUID> guests = new ArrayList<>();
        Stage stage = Stage.WAITING;
        int ticks, line;

        Ceremony(ResourceKey<Level> dim, UUID groom, UUID bride, BlockPos shrine, BlockPos centre, String town) {
            this.dim = dim;
            this.groom = groom;
            this.bride = bride;
            this.shrine = shrine;
            this.centre = centre;
            this.town = town;
        }
    }

    /** Ceremonies under way, by each of the couple. */
    private static final Map<UUID, Ceremony> ACTIVE = new HashMap<>();
    private static final int PERIOD = 10;

    private Weddings() {}

    public static boolean betrothed(MortalEntity m) {
        return ACTIVE.containsKey(m.getUUID());
    }

    /** Two who love one another are betrothed; the wedding is held at the shrine of their town. */
    public static void betroth(ServerLevel level, MortalEntity a, MortalEntity b) {
        if (betrothed(a) || betrothed(b) || a.spouseId() != null || b.spouseId() != null) return;
        MortalEntity groom = a.isMale() ? a : b, bride = a.isMale() ? b : a;
        Settlement town = groom.settlement();
        a.say(a.isMale() ? "Will you be my wife, " + b.getName().getString() + "?" : "Will you be my husband, " + b.getName().getString() + "?");
        b.say("Yes! With all my heart.");
        level.sendParticles(ParticleTypes.HEART, groom.getX(), groom.getY() + 2, groom.getZ(), 4, 0.3, 0.2, 0.3, 0);
        level.sendParticles(ParticleTypes.HEART, bride.getX(), bride.getY() + 2, bride.getZ(), 4, 0.3, 0.2, 0.3, 0);
        if (town == null || town.shrine == null) {   // no town to wed in: they make their promises to each other
            groom.wed(level, bride, "with no shrine but the open sky");
            return;
        }
        Ceremony c = new Ceremony(level.dimension(), groom.getUUID(), bride.getUUID(), town.shrine, town.centre, town.name.isEmpty() ? groom.worldName() : town.name);
        ACTIVE.put(c.groom, c);
        ACTIVE.put(c.bride, c);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (ACTIVE.isEmpty() || server.getTickCount() % PERIOD != 4) return;
        for (Ceremony c : new ArrayList<>(new java.util.LinkedHashSet<>(ACTIVE.values()))) {
            ServerLevel level = server.getLevel(c.dim);
            MortalEntity groom = level != null && level.getEntity(c.groom) instanceof MortalEntity m && m.isAlive() ? m : null;
            MortalEntity bride = level != null && level.getEntity(c.bride) instanceof MortalEntity m && m.isAlive() ? m : null;
            if (groom == null || bride == null || groom.spouseId() != null && c.stage != Stage.FEAST || bride.spouseId() != null && c.stage != Stage.FEAST
                    || groom.sentence() != Justice.Sentence.NONE || bride.sentence() != Justice.Sentence.NONE) {
                end(c);
                continue;
            }
            c.ticks += PERIOD;
            tick(level, c, groom, bride);
        }
    }

    private static void end(Ceremony c) {
        ACTIVE.remove(c.groom);
        ACTIVE.remove(c.bride);
    }

    // ------------------------------------------------------------------ the ceremony

    /** Where the couple stand: before the altar, facing it (the shrine's front looks toward the hearth). */
    private static BlockPos altar(Ceremony c) {
        return c.shrine.offset(0, 0, 3);
    }

    private static void tick(ServerLevel level, Ceremony c, MortalEntity groom, MortalEntity bride) {
        long time = level.getDayTime() % 24000L;
        boolean day = time < 11500 || time > 23500;
        switch (c.stage) {
            case WAITING -> {
                // the wedding is held in the light of day, when the people are about
                if (!day || groom.isPraying() || bride.isPraying()) return;
                c.stage = Stage.PROCESSION;
                c.ticks = 0;
                chooseOfficiant(level, c, groom);
                for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.centre).inflate(48),
                        m -> m != groom && m != bride && !c.guests.contains(m.getUUID()) && m.sentence() == Justice.Sentence.NONE && !m.isLeaving())) {
                    if (m.getUUID().equals(c.officiant)) continue;
                    c.guests.add(m.getUUID());
                }
                groom.say("Today " + bride.getName().getString() + " and I are to be wed!");
                for (UUID g : c.guests) if (level.getEntity(g) instanceof MortalEntity m && level.random.nextInt(3) == 0) m.say("A wedding! To the shrine!");
            }
            case PROCESSION -> {
                BlockPos at = altar(c);
                boolean watched = level.getNearestPlayer(groom.getX(), groom.getY(), groom.getZ(), 48, false) != null;
                boolean gHere = place(level, groom, at.west(), c.ticks, watched), bHere = place(level, bride, at.east(), c.ticks, watched);
                MortalEntity off = officiant(level, c);
                boolean oHere = off == null || place(level, off, c.shrine.offset(0, 0, 1), c.ticks, watched);
                int i = 0;
                for (UUID g : c.guests) {
                    if (!(level.getEntity(g) instanceof MortalEntity m)) continue;
                    // the guests stand in rows behind the couple
                    int row = i / 5, col = i % 5 - 2;
                    place(level, m, at.offset(col * 2 + (row % 2), 0, 3 + row * 2), c.ticks, watched);
                    i++;
                }
                if (gHere && bHere && oHere || c.ticks > 900) {
                    c.stage = Stage.VOWS;
                    c.ticks = 0;
                    c.line = 0;
                }
            }
            case VOWS -> {
                if (c.ticks % 60 != 0) return;
                MortalEntity off = officiant(level, c);
                face(groom, c.shrine);
                face(bride, c.shrine);
                if (off != null) off.getLookControl().setLookAt(groom.getX(), groom.getEyeY(), groom.getZ());
                List<String[]> words = vows(level, c, groom, bride, off);
                if (c.line < words.size()) {
                    String[] w = words.get(c.line++);
                    MortalEntity who = w[0].equals("o") ? off : w[0].equals("g") ? groom : bride;
                    if (who != null) {
                        who.say(w[1]);
                        if (who == off) off.beginAct(MortalEntity.TALK, 55);
                    }
                    if (w[0].equals("g") || w[0].equals("b")) {
                        groom.getLookControl().setLookAt(bride, 30f, 30f);
                        bride.getLookControl().setLookAt(groom, 30f, 30f);
                    }
                    return;
                }
                union(level, c, groom, bride, off);
                c.stage = Stage.FEAST;
                c.ticks = 0;
            }
            case FEAST -> {
                if (c.ticks == PERIOD) {
                    Settlement s = groom.settlement();
                    if (s != null) s.food = Math.max(0, s.food - Math.min(4, s.food));
                }
                List<MortalEntity> all = new ArrayList<>(List.of(groom, bride));
                for (UUID g : c.guests) if (level.getEntity(g) instanceof MortalEntity m) all.add(m);
                if (officiant(level, c) != null) all.add(officiant(level, c));
                for (MortalEntity m : all) {
                    if (m.distanceToSqr(Vec3.atBottomCenterOf(c.centre)) > 36) m.walkTo(c.centre, 0.7);
                    else if (m.act() == MortalEntity.NONE && level.random.nextInt(4) == 0) m.feast();
                }
                if (c.ticks % 80 == 0 && !all.isEmpty()) {
                    String[] toasts = {"To " + groom.getName().getString() + " and " + bride.getName().getString() + "!", "Long life and many children!",
                            "Thou hast kept the good wine until now!", "May your house be full of laughter.", "Eat, drink, and be merry!"};
                    all.get(level.random.nextInt(all.size())).say(toasts[level.random.nextInt(toasts.length)]);
                }
                if (c.ticks % 40 == 0) level.sendParticles(ParticleTypes.NOTE, c.centre.getX() + 0.5, c.centre.getY() + 2.5, c.centre.getZ() + 0.5, 3, 2, 0.5, 2, 1);
                if (c.ticks > 600) end(c);
            }
        }
    }

    /** Walks one toward its place; if the way is long and no one watches, it is simply there. Returns whether it has arrived. */
    private static boolean place(ServerLevel level, MortalEntity m, BlockPos spot, int ticks, boolean watched) {
        BlockPos g = MortalPopulation.safeGround(level, spot.getX(), spot.getZ(), 2);
        if (g == null) g = spot;
        Vec3 to = Vec3.atBottomCenterOf(g);
        if (m.distanceToSqr(to) < 2.0) return true;
        if (m.act() == MortalEntity.SLEEP || m.act() == MortalEntity.WORK || m.act() == MortalEntity.EAT) m.beginAct(MortalEntity.NONE, 1);
        if (!watched && ticks > 200 || ticks > 700) {
            m.teleportTo(to.x, to.y, to.z);
            return true;
        }
        m.walkTo(g, 0.75);
        return false;
    }

    private static void face(MortalEntity m, BlockPos p) {
        m.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
    }

    /** The leader of their faith, if near; else a priest of the town; else its eldest. */
    private static void chooseOfficiant(ServerLevel level, Ceremony c, MortalEntity groom) {
        Religion faith = Faiths.of(groom);
        List<MortalEntity> near = level.getEntitiesOfClass(MortalEntity.class, new AABB(c.centre).inflate(96),
                m -> !m.getUUID().equals(c.groom) && !m.getUUID().equals(c.bride) && !m.isChild() && m.sentence() == Justice.Sentence.NONE);
        MortalEntity pick = null;
        if (faith != null && faith.leader != null) pick = near.stream().filter(m -> m.getUUID().equals(faith.leader)).findFirst().orElse(null);
        if (pick == null) pick = near.stream().filter(m -> m.profession() == Profession.PRIEST && (faith == null || faith.id.equals(m.religion())))
                .findFirst().orElse(null);
        if (pick == null) pick = near.stream().max(java.util.Comparator.comparingDouble(MortalEntity::ageYears)).orElse(null);
        if (pick != null) {
            c.officiant = pick.getUUID();
            String title = pick.title();
            c.officiantName = (title.isEmpty() ? (pick.profession() == Profession.PRIEST ? "the priest " : "") : title + " ") + pick.getName().getString();
        }
    }

    @Nullable
    private static MortalEntity officiant(ServerLevel level, Ceremony c) {
        return c.officiant != null && level.getEntity(c.officiant) instanceof MortalEntity m && m.isAlive() ? m : null;
    }

    /** The words spoken, in order: who speaks ("o" officiant, "g" groom, "b" bride) and what they say. */
    private static List<String[]> vows(ServerLevel level, Ceremony c, MortalEntity groom, MortalEntity bride, @Nullable MortalEntity off) {
        Religion faith = Faiths.of(groom);
        Planet p = groom.planet();
        MortalEra era = p != null ? p.era : MortalEra.HUNTER_GATHERER;
        String g = groom.getName().getString(), b = bride.getName().getString();
        String god = faith != null ? faith.deityName : "";
        List<String[]> w = new ArrayList<>();
        Religion.Deity d = faith == null ? null : faith.deity;
        if (d == null || off == null) {
            w.add(new String[]{"g", b + ", before all these, I take thee to be my wife."});
            w.add(new String[]{"b", g + ", and I take thee to be my husband."});
            return w;
        }
        switch (d) {
            case CREATOR -> {
                w.add(new String[]{"o", "Dearly beloved, we are gathered together before " + god + " to join this man and this woman."});
                w.add(new String[]{"o", "For this cause shall a man leave father and mother, and cleave to his wife; and the two shall be one flesh."});
                w.add(new String[]{"g", "Set me as a seal upon thine heart, " + b + ": for love is strong as death."});
                w.add(new String[]{"b", "Whither thou goest, I will go; thy people shall be my people, and thy God my God."});
                if (era.ordinal() >= MortalEra.MEDIEVAL.ordinal()) w.add(new String[]{"g", "With this ring I thee wed."});
                w.add(new String[]{"o", "What therefore " + god + " hath joined together, let not man put asunder."});
            }
            case SUN -> {
                w.add(new String[]{"o", "Under the burning eye of " + god + ", who sees all, come forward."});
                w.add(new String[]{"g", "As the sun keeps the day, so shall I keep thee, " + b + "."});
                w.add(new String[]{"b", "As the day follows the sun, so shall I follow thee."});
                w.add(new String[]{"o", god + " has seen it. Let no shadow fall between you."});
            }
            case STARS -> {
                w.add(new String[]{"o", "The lights of heaven were set in their courses long ago, and these two with them."});
                w.add(new String[]{"g", "Thou art my star, " + b + ", and my course is set by thee."});
                w.add(new String[]{"b", "And I by thine, as long as the heavens turn."});
                w.add(new String[]{"o", "So it is written in the stars. Go, and be one."});
            }
            case ANCESTORS -> {
                w.add(new String[]{"o", "Fathers and mothers who went before us, receive a new house among your children."});
                w.add(new String[]{"g", "I will honour thy fathers as my own, " + b + "."});
                w.add(new String[]{"b", "And our children shall remember them both."});
                w.add(new String[]{"o", "The fathers are pleased. Two lines are become one."});
            }
            case IDOL -> {
                w.add(new String[]{"o", "Bring gold before " + god + ", that your house may prosper."});
                w.add(new String[]{"g", "All I have I give to thee, " + b + ", and a portion to " + god + "."});
                w.add(new String[]{"b", "And I to thee, and to " + god + "."});
                w.add(new String[]{"o", god + " accepts your offering. Be fruitful and grow rich."});
            }
            case FALLEN -> {
                w.add(new String[]{"o", "Before " + god + " an oath is sworn in blood, and never broken."});
                w.add(new String[]{"g", "By my blood I bind myself to thee."});
                w.add(new String[]{"b", "And by mine, until the deep shall take us."});
                w.add(new String[]{"o", "It is sealed. " + god + " will hold you to it."});
            }
            case REASON -> {
                w.add(new String[]{"o", "Under the law of this people, two free persons come to make a contract."});
                w.add(new String[]{"g", "I, " + g + ", freely promise faith and care to " + b + "."});
                w.add(new String[]{"b", "I, " + b + ", freely promise the same to " + g + "."});
                w.add(new String[]{"o", "Witnessed and recorded. You are husband and wife."});
            }
            case NATURE -> {
                w.add(new String[]{"o", "With this cord I bind your hands, as the vine binds the oak."});
                w.add(new String[]{"g", "As the seasons turn, I will turn with thee, " + b + "."});
                w.add(new String[]{"b", "As the earth bears fruit, so let our love."});
                w.add(new String[]{"o", "The earth is your witness. You are handfasted."});
            }
            case EMPEROR -> {
                w.add(new String[]{"o", "In the name of the divine " + god + ", lord of the earth, a household is founded."});
                w.add(new String[]{"g", "I take " + b + " to wife, and swear it by the genius of " + god + "."});
                w.add(new String[]{"b", "I take " + g + " to husband, and swear it likewise."});
                w.add(new String[]{"o", "It is lawful. Go, and give sons to the empire."});
            }
        }
        return w;
    }

    /** They are joined: the bell rings, the people cheer, petals fall. */
    private static void union(ServerLevel level, Ceremony c, MortalEntity groom, MortalEntity bride, @Nullable MortalEntity off) {
        String how = "at the shrine of " + c.town + (off != null ? " before " + c.officiantName : "");
        groom.wed(level, bride, how);
        Religion faith = Faiths.of(groom);
        int color = faith != null ? faith.deity.color : 0xFFFFF0C0;
        Vec3 mid = groom.position().add(bride.position()).scale(0.5);
        Fx.send(level, FxKind.BLESSING, mid, 2.4f, color, 40);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, mid.x, mid.y + 3, mid.z, 60, 2.5, 1, 2.5, 0.02);
        level.sendParticles(ParticleTypes.HEART, mid.x, mid.y + 2.2, mid.z, 10, 0.8, 0.3, 0.8, 0);
        level.playSound(null, c.shrine, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2f, 1f);
        level.playSound(null, c.shrine, SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 1.5f, 1f);
        groom.say("My wife!");
        bride.say("My husband!");
        for (UUID g : c.guests) {
            if (level.getEntity(g) instanceof MortalEntity m) {
                m.beginAct(MortalEntity.CHEER, 80);
                if (level.random.nextInt(3) == 0) m.say(level.random.nextBoolean() ? "Hurrah!" : "Blessings on you both!");
            }
        }
        groom.addVirtue(1);
        bride.addVirtue(1);
    }
}
