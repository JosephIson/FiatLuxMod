package com.josephison.fiatlux.son;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.prophecy.Prophet;
import com.josephison.fiatlux.prophecy.Prophets;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Matthew 28, Luke 24, John 20, Acts 1-2: the resurrection, the ascension and Pentecost. */
final class Glory {
    private Glory() {}

    static void risen(ServerLevel level, SonData d, long t, long now) {
        BlockPos tomb = d.tomb;
        MortalEntity son = TheSon.mortal(level, d.son);
        if (tomb == null || son == null) return;
        String g = d.creatorName;
        BlockPos door = tomb.offset(5, -1, 0);
        if (TheSon.at(d, t, 0, 0)) {
            Fx.send(level, FxKind.SHOCKWAVE, Vec3.atCenterOf(tomb), 24f, 0xFFFFF0C0, 40);
            TheSon.sound(level, tomb, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.5f);
            Passion.seal(level, tomb, false);
            TheSon.herald(level, ModEntities.ANGEL.get(), Vec3.atBottomCenterOf(door).add(1, 0.2, 2), "Angel of " + g);
            for (var guard : level.getEntitiesOfClass(MortalEntity.class, new net.minecraft.world.phys.AABB(tomb).inflate(12),
                    m -> m.getTags().contains("fiatlux_tomb_guard"))) {
                guard.beginAct(MortalEntity.BOW, 200);
                guard.say("...!");
            }
            son.setNoAi(false);
            son.beginAct(MortalEntity.NONE, 1);
            son.setGlory(2);
            son.teleportTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, son.position(), 60f, 0xFFFFFFFF, 100);
            Fx.send(level, FxKind.GLORY_FLASH, son.position().add(0, 2, 0), 20f, 0xFFFFFFFF, 60);
            level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), son.getX(), son.getY() + 1, son.getZ(), 120, 0.6, 1.4, 0.6, 0.2);
            TheSon.narrate(level, tomb, "And, behold, there was a great earthquake: for the angel of " + g + " descended from heaven, and came and rolled back "
                    + "the stone from the door, and sat upon it. His countenance was like lightning, and his raiment white as snow.", "Matthew 28:2-3");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "resurrection", "rose from the dead on the third day, and the stone was "
                    + "rolled away from the door of the sepulchre outside " + d.capitalName + ".");
            TheSon.tellCreator(level.getServer(), d, "[The Son] He is risen! " + d.name + " has come forth from the tomb.");
            // the betrayer departs (Matthew 27:3-5)
            MortalEntity betrayer = TheSon.mortal(level, d.betrayer);
            if (betrayer != null) {
                betrayer.say("I have sinned in that I have betrayed the innocent blood.");
                betrayer.leaveFor(betrayer.blockPosition().offset(150, 0, 150));
            }
            if (d.betrayer != null) {
                int i = d.disciples.indexOf(d.betrayer);
                if (i >= 0) {
                    d.disciples.remove(i);
                    d.discipleNames.remove(i);
                }
            }
            return;
        }
        List<MortalEntity> women = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(200), m -> !m.isMale() && !m.isChild()
                && (m.getUUID().equals(d.mother) || d.faith != null && d.faith.equals(m.religion()))).stream().limit(3).toList();
        if (d.step == 1 && t < TheSon.secs(12)) {
            for (MortalEntity w : women) TheSon.go(level, w, door.offset(3, 0, level.random.nextInt(3) - 1), 2.5, t > TheSon.secs(8));
        } else if (TheSon.at(d, t, 1, 12)) {
            TheSon.voice(level, tomb, "Angel of " + g, 0xFFF0B0, "Why seek ye the living among the dead? He is not here, but is risen.");
        } else if (TheSon.at(d, t, 2, 19)) {
            TheSon.dismiss(level, tomb);
            MortalEntity first = women.isEmpty() ? null : women.getFirst();
            TheSon.speak(level, son, d, "Woman, why weepest thou? whom seekest thou?", true);
            if (first != null) {
                first.say("Rabboni! Master!");
                first.beginAct(MortalEntity.BOW, 100);
            }
        } else if (TheSon.at(d, t, 3, 27)) {
            for (MortalEntity m : TheSon.disciples(level, d)) TheSon.go(level, m, son.blockPosition().offset(level.random.nextInt(7) - 3, 0, 3 + level.random.nextInt(3)), 2, true);
            TheSon.speak(level, son, d, "Peace be unto you: as my Father hath sent me, even so send I you.", true);
            for (MortalEntity m : TheSon.disciples(level, d)) m.beginAct(MortalEntity.CHEER, 60);
        } else if (TheSon.at(d, t, 4, 35)) {
            MortalEntity thomas = TheSon.disciples(level, d).stream().max(Comparator.comparingDouble(MortalEntity::doubt)).orElse(null);
            if (thomas != null) thomas.say("Except I shall see in his hands the print of the nails, I will not believe.");
            TheSon.speak(level, son, d, "Reach hither thy finger, and behold my hands; and be not faithless, but believing.", true);
            if (thomas != null) {
                thomas.say("My Lord and my God.");
                thomas.beginAct(MortalEntity.BOW, 80);
                thomas.setDoubt(0);
            }
        } else if (TheSon.at(d, t, 5, 43)) {
            int turned = 0;
            Religion faith = Generations.get(level.getServer()).religion(d.faith);
            for (MortalEntity m : level.getEntities(ModEntities.MORTAL.get(), m -> m.isAlive() && !TheSon.isSon(m))) {
                if (faith != null && faith.id.equals(m.religion())) m.setDoubt(0);
                else if (faith != null && level.random.nextDouble() < 0.5) {
                    m.setReligion(faith.id);
                    turned++;
                }
            }
            CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
            c.corruption = Math.max(0, c.corruption - 0.1);
            c.setDirty();
            CosmosData.get(level.getServer()).addGlory(500);
            d.deed("And he shewed himself alive after his passion by many infallible proofs, being seen of them; and " + turned + " believed.");
        } else if (d.step == 6 && t > TheSon.secs(50)) {
            d.to(SonData.Stage.ASCENSION, now);
        }
    }

    static void ascension(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) {
            if (d.step >= 4) {
                if (TheSon.at(d, t, 4, 36)) angels(level, d);
                else if (d.step == 5 && t > TheSon.secs(46)) {
                    TheSon.dismiss(level, d.visiting != null ? d.visiting : d.capital != null ? d.capital : BlockPos.ZERO);
                    d.to(SonData.Stage.PENTECOST, now);
                }
            }
            return;
        }
        if (TheSon.at(d, t, 0, 0)) {
            BlockPos hill = MortalPopulation_highPlace(level, d.capital != null ? d.capital : son.blockPosition());
            d.visiting = hill;
            TheSon.go(level, son, hill, 2, true);
            for (MortalEntity m : TheSon.disciples(level, d)) TheSon.go(level, m, hill.offset(level.random.nextInt(9) - 4, 0, 3 + level.random.nextInt(3)), 2, true);
            return;
        }
        if (TheSon.at(d, t, 1, 6)) {
            TheSon.speak(level, son, d, "All power is given unto me in heaven and in earth. Go ye therefore, and teach all nations, and worlds.", true);
        } else if (TheSon.at(d, t, 2, 14)) {
            TheSon.speak(level, son, d, "And, lo, I am with you alway, even unto the end of the world.", true);
        } else if (TheSon.at(d, t, 3, 20)) {
            Motion.ascend(son, 300);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, son.position(), 80f, 0xFFFFF4D6, 300);
            for (MortalEntity m : TheSon.disciples(level, d)) {
                m.getLookControl().setLookAt(son.getX(), son.getY() + 20, son.getZ());
                m.beginAct(MortalEntity.BOW, 300);
            }
            TheSon.narrate(level, son.blockPosition(), "And when he had spoken these things, while they beheld, he was taken up; and a cloud received him out of their sight.",
                    "Acts 1:9");
        } else if (d.step == 4 && (!Motion.rising() || TheSon.forcing) && t > TheSon.secs(26)) {
            Person p = Generations.get(level.getServer()).person(son.getUUID());
            if (p != null) {
                p.died = level.getGameTime();
                p.soul = "heaven";
                p.translated = true;
                p.ageAtDeath = (int) son.ageYears();
                Generations.get(level.getServer()).setDirty();
            }
            level.sendParticles(ParticleTypes.CLOUD, son.getX(), son.getY() + 1, son.getZ(), 80, 2, 1, 2, 0.02);
            son.discard();
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "ascension", "was taken up into heaven before his disciples on "
                    + d.world + ", and sat on the right hand of " + d.creatorName + ".");
            d.deed("And he led them out, and lifted up his hands, and blessed them; and while he blessed them, he was parted from them, and carried up into heaven.");
            d.since = now - (long) TheSon.secs(30);
            d.setDirty();
        }
    }

    private static void angels(ServerLevel level, SonData d) {
        BlockPos hill = d.visiting != null ? d.visiting : d.capital;
        if (hill == null) return;
        TheSon.herald(level, ModEntities.ANGEL.get(), Vec3.atBottomCenterOf(hill).add(-2, 0.3, 2), "");
        TheSon.herald(level, ModEntities.ANGEL.get(), Vec3.atBottomCenterOf(hill).add(2, 0.3, 2), "");
        TheSon.voice(level, hill, "Two men in white apparel", 0xFFF0B0, "Ye men of " + d.townName + ", why stand ye gazing up into heaven? this same "
                + d.name + ", which is taken up from you into heaven, shall so come in like manner as ye have seen him go into heaven.");
    }

    private static BlockPos MortalPopulation_highPlace(ServerLevel level, BlockPos around) {
        BlockPos best = around;
        int top = Integer.MIN_VALUE;
        for (int dx = -40; dx <= 40; dx += 4) for (int dz = -40; dz <= 40; dz += 4) {
            int x = around.getX() + dx, z = around.getZ() + dz;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y > top && level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                top = y;
                best = new BlockPos(x, y, z);
            }
        }
        return best;
    }

    private static final String[] TONGUES = {
            "Χριστὸς ἀνέστη! (Christ is risen!)", "Surrexit Dominus vere! (The Lord is risen indeed!)", "He is risen!",
            "Il est ressuscité!", "¡Ha resucitado!", "Er ist auferstanden!", "Han har uppstått!", "È risorto!", "Он воскрес!",
            "We do hear them speak in our tongues the wonderful works of God!"};

    static void pentecost(ServerLevel level, SonData d, long t, long now) {
        if (d.capital == null) return;
        Settlement cap = Passion.capital(level, d);
        BlockPos room = cap != null && !cap.houses.isEmpty() ? cap.houses.getFirst() : d.capital;
        List<MortalEntity> apostles = TheSon.disciples(level, d);
        String g = d.creatorName;
        if (TheSon.at(d, t, 0, 0)) {
            for (MortalEntity m : apostles) TheSon.go(level, m, room.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2), 1.5, true);
            TheSon.narrate(level, room, "And when the day of Pentecost was fully come, they were all with one accord in one place.", "Acts 2:1");
        } else if (TheSon.at(d, t, 1, 8)) {
            TheSon.sound(level, room, SoundEvents.ELYTRA_FLYING, 2f, 0.6f);
            level.sendParticles(ParticleTypes.CLOUD, room.getX() + 0.5, room.getY() + 2, room.getZ() + 0.5, 80, 3, 1.5, 3, 0.15);
            for (MortalEntity m : apostles) {
                level.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + m.getBbHeight() + 0.35, m.getZ(), 20, 0.08, 0.15, 0.08, 0.01);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + m.getBbHeight() + 0.4, m.getZ(), 6, 0.05, 0.1, 0.05, 0.005);
                Fx.send(level, FxKind.BLESSING, m.position(), m.getBbHeight(), 0xFFFF9030, 30);
            }
            TheSon.narrate(level, room, "And suddenly there came a sound from heaven as of a rushing mighty wind, and there appeared unto them cloven tongues "
                    + "like as of fire, and it sat upon each of them. And they were all filled with the Holy Ghost.", "Acts 2:2-4");
        } else if (TheSon.at(d, t, 2, 14)) {
            int i = 0;
            for (MortalEntity m : apostles) {
                m.say(TONGUES[i++ % TONGUES.length]);
                level.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + m.getBbHeight() + 0.35, m.getZ(), 10, 0.08, 0.12, 0.08, 0.01);
            }
        } else if (TheSon.at(d, t, 3, 22)) {
            MortalEntity peter = apostles.isEmpty() ? null : apostles.getFirst();
            Religion faith = Generations.get(level.getServer()).religion(d.faith);
            Religion church = new Religion(UUID.randomUUID(), "The Church of " + d.name, Religion.Deity.CREATOR, g, d.creator,
                    peter != null ? peter.getName().getString() : d.name, d.world, level.getGameTime());
            church.parent = d.faith;
            church.doctrine = "over " + d.name + ", whom they called the Son of " + g;
            if (peter != null) {
                church.leader = peter.getUUID();
                church.leaderName = peter.getName().getString();
                church.leaderMale = peter.isMale();
                church.leaders = 1;
                TheSon.speak(level, peter, d, "Repent, and be baptised every one of you in the name of " + d.name + " for the remission of sins.", false);
            }
            Generations.get(level.getServer()).add(church);
            d.church = church.id;
            int added = 0;
            for (MortalEntity m : level.getEntities(ModEntities.MORTAL.get(), m -> m.isAlive())) {
                boolean close = apostles.contains(m) || m.getUUID().equals(d.mother);
                double chance = close ? 1 : faith != null && faith.id.equals(m.religion()) ? 0.55 : 0.25;
                if (level.random.nextDouble() < chance) {
                    m.setReligion(church.id);
                    m.setDoubt(0);
                    added++;
                }
            }
            TheSon.narrate(level, room, "Then they that gladly received his word were baptised: and the same day there were added unto them about "
                    + added + " souls.", "Acts 2:41");
            CosmosData.get(level.getServer()).record(level.getServer(), church.founder, "pentecost", "and the disciples of " + d.name
                    + " were filled with the Holy Ghost at Pentecost, and " + added + " were added unto them: and so began the Church of " + d.name + ".");
        } else if (TheSon.at(d, t, 4, 30)) {
            sendApostles(level, d, apostles);
        } else if (TheSon.at(d, t, 5, 38)) {
            Religion church = Generations.get(level.getServer()).religion(d.church);
            if (church != null) {
                TheSon.gospels(level, d, church);
                CosmosData.get(level.getServer()).record(level.getServer(), church.founder, "scripture", "and the disciples wrote the Gospels of " + d.name
                        + " and the Acts of the Apostles.");
            }
            TheSon.tellCreator(level.getServer(), d, "[The Son] The church is born, the apostles go out, and the Gospels are written. /fiatlux scripture to read them.");
            d.to(SonData.Stage.DONE, now);
        }
    }

    /** "Ye shall be witnesses unto me ... unto the uttermost part of the earth" (Acts 1:8): to every living world. */
    private static void sendApostles(ServerLevel level, SonData d, List<MortalEntity> apostles) {
        Religion church = Generations.get(level.getServer()).religion(d.church);
        if (church == null) return;
        for (MortalEntity a : apostles) {
            Prophet p = Prophets.call(level, church, a, d.creatorName);
            if (p != null) p.deeds.add("And " + p.name + " went forth as an apostle of " + d.name + ", and preached every where.");
        }
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        List<String> sentTo = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < Math.min(RealmPool.SIZE, c.planets.size()); i++) {
            Planet p = c.planets.get(i);
            if (!p.hasLife() || p.name.equals(d.world) || c.townsOf(p.id).isEmpty()) continue;
            ServerLevel there = level.getServer().getLevel(RealmPool.dimension(i));
            if (there == null || k >= apostles.size()) continue;
            MortalEntity a = apostles.get(k++);
            BlockPos at = c.townsOf(p.id).getFirst().centre;
            BlockPos g = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(there, at.getX() + 3, at.getZ() + 6, 8);
            if (g == null) g = at;
            a.say("I go to " + p.name + ".");
            Fx.send(level, FxKind.CELESTIAL_PILLAR, a.position(), 30f, 0xFFFFF0C0, 30);
            a.teleportTo(there, g.getX() + 0.5, g.getY(), g.getZ() + 0.5, Set.of(), a.getYRot(), a.getXRot());
            sentTo.add(p.name);
        }
        if (!sentTo.isEmpty()) {
            CosmosData.get(level.getServer()).record(level.getServer(), "The apostles of " + d.name, "apostles", "went out from " + d.world
                    + " to " + String.join(", ", sentTo) + ", preaching the word.");
        }
    }
}
