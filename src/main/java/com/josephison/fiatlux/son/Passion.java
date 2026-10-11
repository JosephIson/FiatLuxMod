package com.josephison.fiatlux.son;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Justice;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Polity;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.SoulRealms;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.entity.SoulEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Matthew 21-27, Luke 22-23, John 12-19: the entry, the supper, the garden, the trial, the cross and the tomb. */
final class Passion {
    private Passion() {}

    /** The chief city of his world: where its ruler sits, or its greatest town. */
    static Settlement capital(ServerLevel level, SonData d) {
        Planet world = TheSon.world(level);
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        List<Settlement> towns = world == null ? List.of() : c.townsOf(world.id);
        Polity g = world == null ? null : Polities.get(level.getServer()).of(world.id);
        if (g != null) for (Settlement t : towns) if (t.name.equals(g.capital) && level.isLoaded(t.centre)) return t;
        Settlement best = towns.stream().filter(t -> level.isLoaded(t.centre)).max(Comparator.comparingInt(t -> t.houses.size()))
                .orElse(towns.isEmpty() ? null : towns.getFirst());
        if (best == null && d.town != null) {   // a people with no towns written down: the town of his birth
            best = new Settlement(d.town);
            best.name = d.townName;
        }
        return best;
    }

    @Nullable
    static MortalEntity priest(ServerLevel level, SonData d, MortalEntity son) {
        Religion faith = Generations.get(level.getServer()).religion(d.faith);
        MortalEntity leader = faith == null ? null : TheSon.mortal(level, faith.leader);
        if (leader != null && !d.disciples.contains(leader.getUUID())) return leader;
        return level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(96), m -> m.profession() == com.josephison.fiatlux.civilization.Profession.PRIEST
                && !d.disciples.contains(m.getUUID()) && m != son).stream().findFirst().orElse(null);
    }

    @Nullable
    static MortalEntity ruler(ServerLevel level, SonData d, MortalEntity son) {
        Planet world = TheSon.world(level);
        Polity g = world == null ? null : Polities.get(level.getServer()).of(world.id);
        MortalEntity r = g == null ? null : TheSon.mortal(level, g.ruler);
        if (r != null && r != son) return r;
        return level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(96), m -> m.isElder() && m.isMale() && m != son
                && !d.disciples.contains(m.getUUID())).stream().findFirst().orElse(null);
    }

    static String rulerTitle(ServerLevel level) {
        Planet world = TheSon.world(level);
        Polity g = world == null ? null : Polities.get(level.getServer()).of(world.id);
        return g != null ? g.rulerTitle() : "the governor";
    }

    // ------------------------------------------------------------------ the entry (Matthew 21)

    static void entry(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) return;
        String g = d.creatorName;
        Settlement cap = capital(level, d);
        if (cap == null) return;
        if (TheSon.at(d, t, 0, 0)) {
            d.capital = cap.centre;
            d.capitalName = cap.name.isEmpty() ? d.world : cap.name;
            BlockPos start = MortalPopulation.safeGround(level, cap.centre.getX() + 40, cap.centre.getZ() + 6, 12);
            if (start == null) start = cap.centre.offset(20, 0, 6);
            son.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            Donkey donkey = EntityType.DONKEY.create(level);
            if (donkey != null) {
                donkey.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 90f, 0);
                donkey.setTamed(true);
                donkey.addTag("fiatlux_son_story");
                level.addFreshEntity(donkey);
                son.startRiding(donkey, true);
            }
            TheSon.narrate(level, cap.centre, "Tell ye the daughter of " + d.capitalName + ", Behold, thy King cometh unto thee, meek, and sitting upon an ass.",
                    "Matthew 21:5");
            // the people line the way
            for (MortalEntity m : TheSon.crowd(level, cap.centre, 48)) {
                if (d.disciples.contains(m.getUUID())) continue;
                double f = level.random.nextDouble();
                BlockPos side = cap.centre.offset((int) (f * 36), 0, level.random.nextBoolean() ? 3 : 9);
                TheSon.go(level, m, side, 2, true);
            }
            return;
        }
        if (d.step == 1) {
            TheSon.follow(level, d, son);
            var mount = son.getVehicle();
            BlockPos target = cap.centre.offset(0, 0, 6);
            if (mount instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
            if (now % 40 == 0) {
                for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 14)) {
                    if (d.disciples.contains(m.getUUID())) continue;
                    if (level.random.nextInt(3) == 0) m.say("Hosanna! Blessed is he that cometh in the name of " + g + "! Hosanna in the highest!");
                    m.beginAct(MortalEntity.CHEER, 40);
                }
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, son.getX(), son.getY() + 1, son.getZ(), 30, 3, 0.5, 3, 0.02);
                level.sendParticles(ParticleTypes.COMPOSTER, son.getX(), son.getY() + 0.2, son.getZ(), 20, 2.5, 0.1, 2.5, 0.0);
            }
            if (son.distanceToSqr(Vec3.atBottomCenterOf(target)) < 25 || t > TheSon.secs(40)) {
                if (son.getVehicle() != null) {
                    var v = son.getVehicle();
                    son.stopRiding();
                    v.discard();
                }
                TheSon.go(level, son, cap.shrine.offset(0, 0, 3), 2.5, true);
                d.step = 2;
                d.since = now;
                d.deed("And a very great multitude spread their garments in the way; others cut down branches from the trees, and strawed them in the way, "
                        + "and cried, Hosanna to the Son of " + g + ".");
                CosmosData.get(level.getServer()).record(level.getServer(), d.name, "entry", "rode into " + d.capitalName
                        + " upon an ass, and the multitude cried Hosanna.");
            }
            return;
        }
        long u = TheSon.since(d, now);
        if (TheSon.at(d, u, 2, 4)) {
            // he cleanses the temple (Matthew 21:12-13)
            TheSon.speak(level, son, d, "It is written, My house shall be called the house of prayer; but ye have made it a den of thieves.", true);
            Fx.send(level, FxKind.SHOCKWAVE, son.position(), 6f, 0xFFFFE0A0, 20);
            for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 10)) if (!d.disciples.contains(m.getUUID())) m.beginAct(MortalEntity.BOW, 40);
            MortalEntity priest = priest(level, d, son);
            if (priest != null) priest.say("By what authority doest thou these things? He must be stopped.");
            d.deed("And he went into the temple, and cast out all them that sold and bought, and overthrew the tables of the moneychangers.");
        } else if (TheSon.at(d, u, 3, 12)) {
            MortalEntity betrayer = TheSon.mortal(level, d.betrayer);
            MortalEntity priest = priest(level, d, son);
            if (betrayer != null) betrayer.say("What will ye give me, and I will deliver him unto you?");
            if (priest != null) priest.say("Thirty pieces of silver.");
        } else if (d.step == 4 && u > TheSon.secs(20)) {
            d.to(SonData.Stage.SUPPER, now);
        }
    }

    // ------------------------------------------------------------------ the last supper (Luke 22)

    static void supper(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null || d.capital == null) return;
        Settlement cap = capital(level, d);
        BlockPos room = cap != null && !cap.houses.isEmpty() ? cap.houses.getFirst() : d.capital;
        if (TheSon.at(d, t, 0, 0)) {
            TheSon.go(level, son, room, 1.5, true);
            int i = 0;
            for (MortalEntity m : TheSon.disciples(level, d)) {
                double a = Math.PI * 2 * i++ / Math.max(1, d.disciples.size());
                TheSon.go(level, m, room.offset((int) Math.round(Math.cos(a) * 2), 0, (int) Math.round(Math.sin(a) * 2)), 1.5, true);
            }
            TheSon.narrate(level, room, "And when the hour was come, he sat down, and the twelve apostles with him.", "Luke 22:14");
            return;
        }
        if (TheSon.at(d, t, 1, 6)) {
            TheSon.speak(level, son, d, "Take, eat: this is my body, which is given for you: this do in remembrance of me.", true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, son.getX(), son.getY() + 1.2, son.getZ(), 10, 1, 0.3, 1, 0);
        } else if (TheSon.at(d, t, 2, 13)) {
            TheSon.speak(level, son, d, "Drink ye all of it; for this is my blood of the new testament, which is shed for many for the remission of sins.", true);
        } else if (TheSon.at(d, t, 3, 20)) {
            TheSon.speak(level, son, d, "Verily I say unto you, that one of you shall betray me.", false);
            for (MortalEntity m : TheSon.disciples(level, d)) if (level.random.nextInt(3) == 0) m.say("Lord, is it I?");
        } else if (TheSon.at(d, t, 4, 26)) {
            MortalEntity betrayer = TheSon.mortal(level, d.betrayer);
            if (betrayer != null) {
                TheSon.speak(level, son, d, "That thou doest, do quickly.", false);
                betrayer.say("...");
                BlockPos away = betrayer.blockPosition().offset(30, 0, 0);
                betrayer.walkTo(away, 0.9);
            }
            TheSon.narrate(level, room, "He then having received the sop went immediately out: and it was night.", "John 13:30");
            d.deed("And as they were eating, he took bread, and blessed it, and brake it, and gave it to the disciples, and said, Take, eat; this is my body.");
        } else if (TheSon.at(d, t, 5, 32)) {
            TheSon.speak(level, son, d, "A new commandment I give unto you, That ye love one another; as I have loved you.", true);
        } else if (d.step == 6 && t > TheSon.secs(38)) {
            d.to(SonData.Stage.GETHSEMANE, now);
        }
    }

    // ------------------------------------------------------------------ the garden (Matthew 26)

    static void gethsemane(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null || d.capital == null) return;
        if (TheSon.at(d, t, 0, 0)) {
            BlockPos garden = MortalPopulation.safeGround(level, d.capital.getX() - 26, d.capital.getZ() + 18, 10);
            d.visiting = garden != null ? garden : d.capital.offset(-20, 0, 12);
            TheSon.go(level, son, d.visiting, 2, true);
            for (MortalEntity m : TheSon.disciples(level, d)) {
                if (m.getUUID().equals(d.betrayer)) continue;
                TheSon.go(level, m, d.visiting.offset(level.random.nextInt(9) - 4, 0, 5 + level.random.nextInt(3)), 2, true);
            }
            return;
        }
        BlockPos garden = d.visiting != null ? d.visiting : son.blockPosition();
        if (TheSon.at(d, t, 1, 5)) {
            son.beginAct(MortalEntity.PRAY, 400);
            TheSon.speak(level, son, d, "O my Father, if it be possible, let this cup pass from me: nevertheless not as I will, but as thou wilt.", true);
            level.sendParticles(ParticleTypes.FALLING_WATER, son.getX(), son.getY() + 1.6, son.getZ(), 20, 0.2, 0.2, 0.2, 0);
            for (MortalEntity m : TheSon.disciples(level, d)) if (!m.getUUID().equals(d.betrayer)) m.beginAct(MortalEntity.SLEEP, 400);
            TheSon.herald(level, com.josephison.fiatlux.registry.ModEntities.ANGEL.get(), son.position().add(1.5, 1.5, 0), "");
            TheSon.narrate(level, garden, "And there appeared an angel unto him from heaven, strengthening him. And being in an agony he prayed more earnestly.",
                    "Luke 22:43-44");
        } else if (TheSon.at(d, t, 2, 15)) {
            TheSon.dismiss(level, garden);
            for (MortalEntity m : TheSon.disciples(level, d)) if (m.act() == MortalEntity.SLEEP) m.beginAct(MortalEntity.NONE, 1);
            TheSon.speak(level, son, d, "What, could ye not watch with me one hour? Watch and pray, that ye enter not into temptation.", true);
        } else if (TheSon.at(d, t, 3, 22)) {
            // the betrayer comes with a band from the priests and the ruler
            MortalEntity betrayer = TheSon.mortal(level, d.betrayer);
            if (betrayer != null) TheSon.go(level, betrayer, garden.offset(2, 0, 1), 2, true);
            for (int i = 0; i < 4; i++) {
                MortalEntity guard = MortalPopulation.bringForth(level, garden.offset(4 + i, 0, -3), d.capital,
                        TheSon.world(level) != null ? TheSon.world(level).era : com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, null, true, 28, null, null, null);
                if (guard != null) {
                    guard.addTag("fiatlux_son_story");
                    guard.say(i == 0 ? "Whom seek ye?" : "...");
                }
            }
            if (betrayer != null) {
                betrayer.say("Hail, master.");
                betrayer.getLookControl().setLookAt(son, 30f, 30f);
            }
            TheSon.speak(level, son, d, (d.betrayerName.isEmpty() ? "Friend" : d.betrayerName) + ", betrayest thou the Son of man with a kiss?", true);
        } else if (TheSon.at(d, t, 4, 30)) {
            List<MortalEntity> ds = TheSon.disciples(level, d);
            MortalEntity bold = ds.stream().filter(m -> !m.getUUID().equals(d.betrayer)).findFirst().orElse(null);
            if (bold != null) bold.say("Lord, shall we smite with the sword?");
            TheSon.speak(level, son, d, "Put up again thy sword into his place: for all they that take the sword shall perish with the sword.", true);
            for (MortalEntity m : ds) {   // "Then all the disciples forsook him, and fled" (Matthew 26:56)
                if (m.getUUID().equals(d.betrayer)) continue;
                m.walkTo(m.blockPosition().offset(level.random.nextInt(41) - 20, 0, level.random.nextInt(41) - 20), 1.2);
            }
            d.deed("And " + d.betrayerName + " came, and with him a great multitude with swords and staves, and he kissed him; and they laid hands on him, and took him.");
            CosmosData.get(level.getServer()).record(level.getServer(), d.betrayerName.isEmpty() ? "One of the twelve" : d.betrayerName, "betrayal",
                    "betrayed " + d.name + " with a kiss in the garden outside " + d.capitalName + ", and all the disciples forsook him, and fled.");
        } else if (d.step == 5 && t > TheSon.secs(36)) {
            d.to(SonData.Stage.TRIAL, now);
        }
    }

    // ------------------------------------------------------------------ the trial (Mark 14-15)

    static void trial(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null || d.capital == null) return;
        String g = d.creatorName;
        BlockPos court = d.capital.offset(4, 0, 4);
        MortalEntity priest = priest(level, d, son);
        MortalEntity ruler = ruler(level, d, son);
        if (TheSon.at(d, t, 0, 0)) {
            TheSon.go(level, son, court, 2, true);
            for (var e : level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(40), e -> e.getTags().contains("fiatlux_son_story"))) {
                TheSon.go(level, e, court.offset(2 + level.random.nextInt(3), 0, 2), 2, true);
            }
            if (priest != null) TheSon.go(level, priest, court.offset(-3, 0, 0), 2, true);
            if (ruler != null) TheSon.go(level, ruler, court.offset(0, 0, -3), 2, true);
            for (MortalEntity m : TheSon.crowd(level, d.capital, 48)) if (!d.disciples.contains(m.getUUID())) TheSon.go(level, m, court.offset(level.random.nextInt(11) - 5, 0, 5 + level.random.nextInt(4)), 2, true);
            return;
        }
        if (TheSon.at(d, t, 1, 4)) {
            if (priest != null) priest.say("Art thou the Son of " + g + ", the Blessed?");
        } else if (TheSon.at(d, t, 2, 9)) {
            TheSon.speak(level, son, d, "I am: and ye shall see the Son of man sitting on the right hand of power, and coming in the clouds of heaven.", true);
            if (priest != null) priest.say("He hath spoken blasphemy! What further need have we of witnesses?");
        } else if (TheSon.at(d, t, 3, 15)) {
            // "Before the cock crow, thou shalt deny me thrice" (Matthew 26:75)
            MortalEntity denier = TheSon.disciples(level, d).stream().filter(m -> !m.getUUID().equals(d.betrayer)).findFirst().orElse(null);
            if (denier != null) {
                denier.say("I know not the man.");
                TheSon.sound(level, denier.blockPosition(), SoundEvents.CHICKEN_AMBIENT, 2f, 0.7f);
                denier.beginAct(MortalEntity.BOW, 80);
            }
            if (ruler != null) ruler.say("Art thou the King of " + d.world + "?");
        } else if (TheSon.at(d, t, 4, 20)) {
            TheSon.speak(level, son, d, "My kingdom is not of this world.", true);
            if (ruler != null) ruler.say("I find in him no fault at all. Whom will ye that I release unto you?");
        } else if (TheSon.at(d, t, 5, 26)) {
            MortalEntity prisoner = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(120),
                    m -> m.sentence() != Justice.Sentence.NONE && m.sentence() != Justice.Sentence.EXILE).stream().findFirst().orElse(null);
            String released = prisoner != null ? prisoner.getName().getString() : "Barabbas";
            for (MortalEntity m : TheSon.crowd(level, court, 24)) {
                if (d.disciples.contains(m.getUUID()) || m == priest || m == ruler) continue;
                m.say(level.random.nextInt(3) == 0 ? "Crucify him!" : released + "! Release unto us " + released + "!");
            }
            if (prisoner != null) {
                prisoner.release();
                prisoner.say("I am free... and he, in my place?");
            }
            d.deed("And they cried, Not this man, but " + released + ". Now " + released + " was a robber.");
        } else if (TheSon.at(d, t, 6, 32)) {
            if (ruler != null) ruler.say("I am innocent of the blood of this just person: see ye to it.");
            for (MortalEntity m : TheSon.crowd(level, court, 24)) if (!d.disciples.contains(m.getUUID()) && m != ruler) m.say("Crucify him! Crucify him!");
            TheSon.narrate(level, court, rulerTitle(level) + " took water, and washed his hands before the multitude.", "Matthew 27:24");
        } else if (d.step == 7 && t > TheSon.secs(38)) {
            d.to(SonData.Stage.CROSS, now);
        }
    }

    // ------------------------------------------------------------------ the cross (Luke 23, John 19)

    static void cross(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null || d.capital == null) return;
        String g = d.creatorName;
        if (TheSon.at(d, t, 0, 0)) {
            String charge = d.name.toUpperCase() + " THE KING OF " + d.world.toUpperCase();
            BlockPos at = Justice.crucify(level, son, d.capital, charge, com.josephison.fiatlux.civilization.Polity.Crime.SEDITION, Long.MAX_VALUE);
            d.cross = at;
            son.setGlory(0);
            // two thieves, the one on his right hand, and the other on his left (Luke 23:33)
            int thieves = 0;
            for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(120),
                    m -> m != son && m.sentence() == Justice.Sentence.PRISON)) {
                if (thieves >= 2) break;
                Justice.crucify(level, m, d.capital, Justice.charge(m, com.josephison.fiatlux.civilization.Polity.Crime.THEFT),
                        com.josephison.fiatlux.civilization.Polity.Crime.THEFT, now + (long) TheSon.secs(150));
                thieves++;
            }
            for (int i = thieves; i < 2; i++) {
                MortalEntity thief = MortalPopulation.bringForth(level, at != null ? at.offset(4, 0, 4) : son.blockPosition(), d.capital,
                        TheSon.world(level) != null ? TheSon.world(level).era : com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL,
                        i == 0 ? "Dismas" : "Gestas", true, 30, null, null, null);
                if (thief != null) Justice.crucify(level, thief, d.capital, Justice.charge(thief, com.josephison.fiatlux.civilization.Polity.Crime.THEFT),
                        com.josephison.fiatlux.civilization.Polity.Crime.THEFT, now + (long) TheSon.secs(150));
            }
            if (at != null) SonFilms.calvary(level, d, son, at);
            TheSon.narrate(level, at, "And when they were come to the place, which is called Calvary, there they crucified him, and the malefactors, "
                    + "one on the right hand, and the other on the left.", "Luke 23:33");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "crucifixion", "was crucified outside " + d.capitalName
                    + " on " + d.world + ", between two thieves, and over him was written " + charge + ".");
            TheSon.tellCreator(level.getServer(), d, "[The Son] " + d.name + " is crucified outside " + d.capitalName + ".");
            return;
        }
        BlockPos at = d.cross != null ? d.cross : son.blockPosition();
        // the darkness over all the land (Luke 23:44)
        if (d.step >= 2 && d.step <= 8 && now % 40 == 0) {
            level.setWeatherParameters(0, 6000, true, true);
            level.sendParticles(ParticleTypes.ASH, at.getX() + 0.5, at.getY() + 6, at.getZ() + 0.5, 60, 16, 4, 16, 0.01);
        }
        String[] words = {
                "Father, forgive them; for they know not what they do.",
                "Verily I say unto thee, To day shalt thou be with me in paradise.",
                "Woman, behold thy son! Behold thy mother!",
                "Eli, Eli, lama sabachthani? My God, my God, why hast thou forsaken me?",
                "I thirst.",
                "It is finished.",
                "Father, into thy hands I commend my spirit."};
        for (int i = 0; i < words.length; i++) {
            if (TheSon.at(d, t, 1 + i, 6 + i * 11)) {
                if (i == 1) {
                    TheSon.narrate(level, at, "And it was about the sixth hour, and there was a darkness over all the earth until the ninth hour.", "Luke 23:44");
                    Fx.send(level, FxKind.SKY_FRACTURE, Vec3.atCenterOf(at).add(0, 30, 0), 40f, 0xFF201010, 120);
                }
                TheSon.speak(level, son, d, words[i], true);
                if (i == 6) die(level, d, son, at, now);
                return;
            }
        }
        if (d.step == 8 && t > TheSon.secs(6 + 7 * 11 + 6)) {
            // taken down, and laid in a tomb hewn out of a rock (Luke 23:53)
            son.setNoGravity(false);
            BlockPos tomb = tomb(level, d, d.capital);
            d.tomb = tomb;
            son.teleportTo(tomb.getX() + 0.5, tomb.getY(), tomb.getZ() + 0.5);
            son.beginAct(MortalEntity.SLEEP, Integer.MAX_VALUE);
            son.setNoAi(true);
            seal(level, tomb, true);
            TheSon.narrate(level, tomb, "And he took it down, and wrapped it in linen, and laid it in a sepulchre that was hewn in stone, wherein never man "
                    + "before was laid. And they rolled a great stone to the door of the sepulchre.", "Luke 23:53, Matthew 27:60");
            level.setWeatherParameters(0, 2400, true, false);
            d.to(SonData.Stage.TOMB, now);
        }
    }

    private static void die(ServerLevel level, SonData d, MortalEntity son, BlockPos at, long now) {
        son.release();
        son.setNoGravity(true);
        son.moveTo(at.getX() + 0.5, at.getY() + Justice.CROSS_FOOT, at.getZ() + 0.5, 180f, 0f);
        son.beginAct(MortalEntity.CRUCIFIED, Integer.MAX_VALUE);
        Fx.send(level, FxKind.SHOCKWAVE, Vec3.atCenterOf(at), 30f, 0xFF402020, 40);
        TheSon.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.4f);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 40, 6, 0.5, 6, 0.01);
        TheSon.narrate(level, at, "And, behold, the veil of the temple was rent in twain from the top to the bottom; and the earth did quake, and the rocks rent.",
                "Matthew 27:51");
        // the veil of the temple
        Settlement cap = capital(level, d);
        if (cap != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, cap.shrine.getX() + 0.5, cap.shrine.getY() + 2, cap.shrine.getZ() + 0.5, 6, 1, 1, 1, 0);
            Fx.send(level, FxKind.SHOCKWAVE, Vec3.atCenterOf(cap.shrine), 12f, 0xFFB02020, 30);
        }
        MortalEntity centurion = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(16),
                m -> m != son && m.sentence() == Justice.Sentence.NONE && !d.disciples.contains(m.getUUID())).stream().findFirst().orElse(null);
        if (centurion != null) {
            centurion.say("Truly this was the Son of " + d.creatorName + ".");
            centurion.beginAct(MortalEntity.BOW, 120);
            Religion faith = Generations.get(level.getServer()).religion(d.faith);
            if (faith != null) centurion.setReligion(faith.id);
        }
        for (MortalEntity m : TheSon.crowd(level, at, 40)) m.setDoubt(Math.min(1, m.doubt() + 0.3));
        d.deed("And when " + d.name + " had cried with a loud voice, he said, Father, into thy hands I commend my spirit: and having said thus, he gave up the ghost.");
        CosmosData.get(level.getServer()).record(level.getServer(), d.name, "crucifixion", "cried, It is finished, and gave up the ghost; "
                + "and the earth did quake, and the veil of the temple was rent in twain.");
    }

    /**
     * A tomb hewn out of a rock (Luke 23:53): carved into a hillside near the city where there is one, its
     * door to the east; where the land is flat, a chamber of stone raised on the bare earth. The story's
     * own tomb is reused if it has one already.
     */
    private static BlockPos tomb(ServerLevel level, SonData d, BlockPos city) {
        if (d.tomb != null) return d.tomb;
        BlockPos door = com.josephison.fiatlux.civilization.Ground.hillside(level, city, 14, 64, 6, 5, 1);
        d.tombHewn = door != null;
        BlockPos g;
        if (door != null) {
            g = door.offset(-3, 0, 0);
        } else {
            g = com.josephison.fiatlux.civilization.Ground.naturalGround(level, city.getX() - 30, city.getZ() - 22, 24);
            if (g == null) g = city.offset(-24, 0, -18);
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -1; dy <= 4; dy++) {
            boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == -1 || dy == 4;
            BlockState s = shell ? (dy == 4 && !d.tombHewn ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE).defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(g.offset(dx, dy, dz), s, 3);
        }
        // the stone bench he is laid on
        level.setBlock(g.offset(-1, 0, 0), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3);
        level.setBlock(g.offset(-1, 0, 1), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3);
        // the doorway, to the east
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) level.setBlock(g.offset(3, dy, dz), Blocks.AIR.defaultBlockState(), 3);
        return g.offset(-1, 1, 0);
    }

    /** Takes the tomb away again (when the story is set aside): a hewn tomb is filled back into the rock, a raised one taken down. */
    static void unbuild(ServerLevel level, BlockPos tomb, boolean hewn) {
        BlockPos g = tomb.offset(1, -1, 0);
        if (hewn) {
            for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 3; dy++) {
                if (dx == 3 && (Math.abs(dz) > 1 || dy > 2)) continue;
                level.setBlock(g.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        } else {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -1; dy <= 4; dy++) {
                level.setBlock(g.offset(dx, dy, dz), (dy == -1 ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState(), 3);
            }
        }
        // the great stone, if it was rolled away
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) {
            BlockPos beside = g.offset(4, dy, dz + 3);
            if (level.getBlockState(beside).is(Blocks.POLISHED_ANDESITE)) level.setBlock(beside, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** The great stone at the door: rolled to (true) or rolled away (false). */
    static void seal(ServerLevel level, BlockPos tomb, boolean shut) {
        BlockPos g = tomb.offset(1, -1, 0);
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) {
            BlockPos door = g.offset(3, dy, dz), beside = g.offset(4, dy, dz + 3);
            level.setBlock(door, shut ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            if (!shut) level.setBlock(beside, Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ the tomb, and the spirits in prison

    static void tomb(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        BlockPos tomb = d.tomb;
        if (tomb == null) return;
        if (TheSon.at(d, t, 0, 4)) {
            // the watch is set (Matthew 27:66)
            for (int i = 0; i < 2; i++) {
                MortalEntity guard = MortalPopulation.bringForth(level, tomb.offset(6, -1, i * 3 - 1), d.capital, TheSon.world(level) != null ? TheSon.world(level).era
                        : com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, null, true, 30, null, null, null);
                if (guard != null) {
                    guard.addTag("fiatlux_son_story");
                    guard.addTag("fiatlux_tomb_guard");
                }
            }
        } else if (TheSon.at(d, t, 1, 10)) {
            // "he went and preached unto the spirits in prison" (1 Peter 3:19)
            int freed = harrow(level, d);
            ServerLevel under = level.getServer().getLevel(FiatLuxDimensions.UNDERWORLD);
            if (under != null) TheSon.narrate(under, null, "By which also he went and preached unto the spirits in prison. Lift up your heads, O ye gates; "
                    + "and be ye lift up, ye everlasting doors.", "1 Peter 3:19, Psalm 24:7");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "harrowing", "went down and preached unto the spirits in prison, and "
                    + freed + " of them were led up out of the Underworld into Heaven.");
        } else if (TheSon.at(d, t, 2, Math.max(11, 75 - SonFilms.tombLeadSeconds()))) {
            // the film of the empty tomb begins at dawn, a little before the stone is rolled away
            if (son != null) SonFilms.risen(level, d, son, tomb);
        } else if (d.step == 3 && t > TheSon.secs(75)) {   // "the third day"
            d.to(SonData.Stage.RISEN, now);
        }
        if (son != null && now % 100 == 0) level.sendParticles(ParticleTypes.WHITE_ASH, tomb.getX() + 0.5, tomb.getY() + 1, tomb.getZ() + 0.5, 4, 1, 1, 1, 0);
    }

    /** Souls in the Underworld are led up into Heaven. Returns how many. */
    private static int harrow(ServerLevel level, SonData d) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        ServerLevel under = level.getServer().getLevel(FiatLuxDimensions.UNDERWORLD);
        int freed = 0;
        if (under != null) {
            for (SoulEntity s : under.getEntitiesOfClass(SoulEntity.class, new AABB(BlockPos.ZERO).inflate(30000))) {
                if (freed >= 24) break;
                SoulRealms.redeem(level.getServer(), d.name, s);
                freed++;
            }
        }
        int unseen = (int) Math.min(c.underworld.souls * 0.25, 500);
        c.underworld.souls -= unseen;
        c.heavenSouls += unseen;
        c.setDirty();
        return freed + unseen;
    }
}
