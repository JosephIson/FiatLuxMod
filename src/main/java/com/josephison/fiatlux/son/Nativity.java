package com.josephison.fiatlux.son;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Polity;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Luke 1-2, Matthew 1-2: the annunciation, the birth, the shepherds and the wise men, and the boy in the temple. */
final class Nativity {
    private Nativity() {}

    static void annunciation(ServerLevel level, SonData d, long t, long now) {
        MortalEntity mother = TheSon.mortal(level, d.mother);
        if (mother == null) {
            Settlement town = d.town == null ? null : CosmicExpanseSavedData.get(level.getServer()).townNear(TheSon.world(level) != null ? TheSon.world(level).id : null, d.town);
            Religion faith = Generations.get(level.getServer()).religion(d.faith);
            mother = town != null && faith != null ? TheSon.chooseMother(level, town, faith) : null;
            if (mother == null) return;
            d.mother = mother.getUUID();
            d.motherName = mother.getName().getString();
        }
        String g = d.creatorName;
        Vec3 by = mother.position().add(1.8, 1.2, 0.6);
        if (TheSon.at(d, t, 0, 2)) {
            TheSon.herald(level, ModEntities.ARCHANGEL.get(), by, "Gabriel");
            mother.beginAct(MortalEntity.BOW, 400);
            mother.getLookControl().setLookAt(by.x, by.y + 1, by.z);
            TheSon.narrate(level, mother.blockPosition(), "And the angel Gabriel was sent from " + g + " unto a city of " + d.world + ", named "
                    + d.townName + ", to a woman whose name was " + d.motherName + ".", "Luke 1:26-27");
        } else if (TheSon.at(d, t, 1, 7)) {
            TheSon.voice(level, mother.blockPosition(), "Gabriel", 0xFFF0B0, "Hail, thou that art highly favoured, " + g
                    + " is with thee: blessed art thou among women.");
        } else if (TheSon.at(d, t, 2, 13)) {
            TheSon.voice(level, mother.blockPosition(), "Gabriel", 0xFFF0B0, "Fear not, " + d.motherName + ": for thou hast found favour with "
                    + g + ". And, behold, thou shalt bring forth a son, and shalt call his name " + d.name + ".");
        } else if (TheSon.at(d, t, 3, 19)) {
            mother.say("How shall this be?");
            TheSon.voice(level, mother.blockPosition(), "Gabriel", 0xFFF0B0, "The power of the Highest shall overshadow thee: therefore that holy "
                    + "thing which shall be born of thee shall be called the Son of " + g + ". For with " + g + " nothing shall be impossible.");
        } else if (TheSon.at(d, t, 4, 26)) {
            mother.say("Behold the handmaid of " + g + "; be it unto me according to thy word.");
        } else if (TheSon.at(d, t, 5, 31)) {
            TheSon.dismiss(level, mother.blockPosition());
            // "Joseph her husband, being a just man ... took unto him his wife" (Matthew 1:19, 24)
            MortalEntity husband = TheSon.mortal(level, mother.spouseId());
            if (husband == null && mother.spouseId() == null) {
                MortalEntity m2 = mother;
                husband = level.getEntitiesOfClass(MortalEntity.class, mother.getBoundingBox().inflate(64),
                                m -> m.isMale() && !m.isChild() && m.spouseId() == null && m.sentence() == com.josephison.fiatlux.civilization.Justice.Sentence.NONE
                                        && !m.isPossessed() && m.religion() != null && m.religion().equals(m2.religion()))
                        .stream().max(Comparator.comparingDouble(m -> (m.hasTrait(com.josephison.fiatlux.civilization.Trait.KIND) ? 2 : 0)
                                + (m.profession() == Profession.BUILDER ? 2 : 0) + level.random.nextDouble())).orElse(null);
                if (husband != null) {
                    TheSon.narrate(level, mother.blockPosition(), "Then " + husband.getName().getString() + " being raised from sleep did as the angel "
                            + "of " + g + " had bidden him, and took unto him his wife.", "Matthew 1:24");
                    husband.wed(level, mother, "after the angel had spoken to them both");
                }
            }
            if (husband != null) {
                d.father = husband.getUUID();
                d.fatherName = husband.getName().getString();
            }
            CosmosData.get(level.getServer()).record(level.getServer(), "Gabriel", "annunciation", "was sent to " + d.motherName + " of "
                    + d.townName + ", saying, Thou shalt bring forth a son, and shalt call his name " + d.name + ".");
        } else if (d.step == 6 && t >= TheSon.secs(80)) {
            d.to(SonData.Stage.NATIVITY, now);
        }
    }

    static void nativity(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        String g = d.creatorName;
        if (d.town != null) star(level, d.town.above(48));
        if (TheSon.at(d, t, 0, 0)) {
            MortalEntity mother = TheSon.mortal(level, d.mother);
            if (mother == null) {
                d.to(SonData.Stage.ANNUNCIATION, now);
                return;
            }
            MortalEntity father = TheSon.mortal(level, d.father);
            Planet world = TheSon.world(level);
            MortalEra era = world != null ? world.era : MortalEra.AGRARIAN;
            son = MortalPopulation.bringForth(level, mother.blockPosition(), d.town, era, d.name, true, 0, father, mother, d.faith);
            if (son == null) return;
            son.setVariant(d.variant);
            son.setLifespan(1000);
            son.setGlory(1);
            d.son = son.getUUID();
            d.born = now;
            Person p = Generations.get(level.getServer()).person(son.getUUID());
            if (p != null) {
                p.line = "son";
                Generations.get(level.getServer()).setDirty();
            }
            mother.beginAct(MortalEntity.BOW, 200);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, son.position(), 40f, 0xFFFFF4D6, 60);
            TheSon.narrate(level, son.blockPosition(), "And she brought forth her firstborn son, and wrapped him in swaddling clothes, and laid "
                    + "him in a manger; because there was no room for them in the inn.", "Luke 2:7");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "nativity", "was born of " + d.motherName + " in " + d.townName
                    + " on " + d.world + ", and a star stood over the place where the young child was.");
            TheSon.tellCreator(level.getServer(), d, "[The Son] " + d.name + " is born in " + d.townName + ". A star stands over the town.");
            d.setDirty();
            return;
        }
        if (son == null) return;
        MortalEntity child = son;
        List<MortalEntity> shepherds = level.getEntitiesOfClass(MortalEntity.class, new AABB(son.blockPosition()).inflate(96),
                m -> !m.isChild() && m != child && (m.profession() == Profession.SHEPHERD || m.profession() == Profession.FARMER
                        || m.profession() == Profession.HUNTER || m.profession() == Profession.GATHERER)).stream().limit(3).toList();
        BlockPos field = shepherds.isEmpty() ? son.blockPosition().offset(20, 0, 12) : shepherds.getFirst().blockPosition();
        if (TheSon.at(d, t, 1, 8)) {
            var angel = TheSon.herald(level, ModEntities.ANGEL.get(), Vec3.atCenterOf(field).add(0, 5, 0), "Angel of " + g);
            for (MortalEntity s : shepherds) s.beginAct(MortalEntity.BOW, 200);
            TheSon.narrate(level, field, "And there were in the same country shepherds abiding in the field, keeping watch over their flock by night. "
                    + "And, lo, the angel of " + g + " came upon them, and the glory of " + g + " shone round about them.", "Luke 2:8-9");
            if (angel != null) Fx.send(level, FxKind.GLORY_FLASH, angel.position(), 12f, 0xFFFFF0C0, 40);
        } else if (TheSon.at(d, t, 2, 14)) {
            TheSon.voice(level, field, "Angel of " + g, 0xFFF0B0, "Fear not: for, behold, I bring you good tidings of great joy, which shall be "
                    + "to all people. For unto you is born this day in " + d.townName + " a Saviour.");
        } else if (TheSon.at(d, t, 3, 20)) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                TheSon.herald(level, ModEntities.ANGEL.get(), Vec3.atCenterOf(field).add(Math.cos(a) * 7, 7 + (i % 3), Math.sin(a) * 7), "");
            }
            TheSon.voice(level, field, "The Heavenly Host", 0xFFF0B0, "Glory to " + g + " in the highest, and on earth peace, good will toward men.");
        } else if (TheSon.at(d, t, 4, 28)) {
            TheSon.dismiss(level, field);
            for (MortalEntity s : shepherds) {
                s.say("Let us now go even unto " + d.townName + ", and see this thing which is come to pass.");
            }
        } else if (d.step == 5 && t < TheSon.secs(45)) {
            for (MortalEntity s : shepherds) if (TheSon.go(level, s, son.blockPosition().offset(2, 0, 2), 3, false)) s.beginAct(MortalEntity.BOW, 60);
        } else if (TheSon.at(d, t, 5, 45)) {
            for (MortalEntity s : shepherds) s.say("Glory to " + g + ", for all the things that we have heard and seen!");
            d.deed("And the shepherds came with haste, and found " + d.motherName + ", and the babe lying in a manger.");
        } else if (d.step == 6 && t < TheSon.secs(70)) {
            for (MortalEntity w : wiseMen(level, d, son)) TheSon.go(level, w, son.blockPosition().offset(-2, 0, 2), 3, t > TheSon.secs(62));
        } else if (TheSon.at(d, t, 6, 70)) {
            List<MortalEntity> wise = wiseMen(level, d, son);
            for (MortalEntity w : wise) {
                TheSon.go(level, w, son.blockPosition().offset(-2, 0, 2), 3, true);
                w.beginAct(MortalEntity.BOW, 120);
            }
            if (!wise.isEmpty()) wise.getFirst().say("Where is he that is born King? for we have seen his star in the east, and are come to worship him.");
            Vec3 at = son.position();
            TheSon.drop(level, at.add(1, 0, 0), TheSon.gift(Items.GOLD_INGOT, "Gold"));
            TheSon.drop(level, at.add(-1, 0, 0), TheSon.gift(Items.GLOWSTONE_DUST, "Frankincense"));
            TheSon.drop(level, at.add(0, 0, 1), TheSon.gift(Items.HONEYCOMB, "Myrrh"));
            TheSon.narrate(level, son.blockPosition(), "And when they were come into the house, they saw the young child with " + d.motherName
                    + ", and fell down, and worshipped him: and they presented unto him gifts; gold, and frankincense, and myrrh.", "Matthew 2:11");
            d.deed("And wise men came from the east to " + d.townName + ", and worshipped him, and opened their treasures: gold, and frankincense, and myrrh.");
        } else if (TheSon.at(d, t, 7, 80)) {
            Planet world = TheSon.world(level);
            Polity g2 = world == null ? null : Polities.get(level.getServer()).of(world.id);
            if (g2 != null) {
                CosmosData.get(level.getServer()).record(level.getServer(), g2.rulerTitle() + " of the " + g2.name, "nativity",
                        "heard of a child born King in " + d.townName + ", and was troubled, and all the land with him.");
            }
            d.to(SonData.Stage.CHILDHOOD, now);
        }
    }

    /** The star over the place where the young child was (Matthew 2:9): seen from afar. */
    static void star(ServerLevel level, BlockPos at) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(at) > 400 * 400) continue;
            level.sendParticles(p, ParticleTypes.END_ROD, true, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.005);
            level.sendParticles(p, com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), true, at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                    4, 0.6, 0.6, 0.6, 0.01);
        }
    }

    /** Three of the wise, from the other towns if there are others. */
    private static List<MortalEntity> wiseMen(ServerLevel level, SonData d, MortalEntity son) {
        return level.getEntitiesOfClass(MortalEntity.class, new AABB(son.blockPosition()).inflate(256),
                        m -> !m.isChild() && m != son && m.isMale() && (m.isElder() || m.profession() == Profession.SCHOLAR || m.profession() == Profession.PRIEST))
                .stream().sorted(Comparator.comparingDouble(m -> -m.distanceToSqr(son))).limit(3).toList();
    }

    static void childhood(ServerLevel level, SonData d, long t, long now, boolean force) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) return;
        double length = TheSon.secs(240);
        double age = force ? 30 : Math.min(30, 30 * t / length);
        if (son.ageYears() < age) son.setAgeYears(age);
        if (son.tickCount % 80 == 0) TheSon.glory(level, son, 1);
        if (d.step == 0 && age >= 12) {
            d.step = 1;
            // "they found him in the temple, sitting in the midst of the doctors" (Luke 2:46)
            Settlement s = son.settlement();
            if (s != null) TheSon.go(level, son, s.shrine.offset(0, 0, 3), 3, true);
            for (MortalEntity e : level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(24), m -> m.isElder() || m.profession() == Profession.PRIEST)) {
                e.beginAct(MortalEntity.BOW, 60);
                e.say("How knoweth this child these things?");
            }
            TheSon.speak(level, son, d, "How is it that ye sought me? wist ye not that I must be about my Father's business?", true);
            d.deed("And when he was twelve years old, they found him in the temple, sitting in the midst of the teachers, both hearing them, and asking them questions.");
        }
        if (age >= 30) d.to(SonData.Stage.BAPTISM, now);
    }
}
