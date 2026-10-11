package com.josephison.fiatlux.son;

import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Justice;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.Trait;
import com.josephison.fiatlux.civilization.Weddings;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.entity.SoulEntity;
import com.josephison.fiatlux.flood.BirdEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.prophecy.ProphecyData;
import com.josephison.fiatlux.prophecy.Prophet;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Matthew 3-17, Mark 1-9, Luke 3-9, John 1-11: the baptism, the temptation, and the ministry in the towns. */
final class Ministry {
    private Ministry() {}

    // ------------------------------------------------------------------ the baptism (Matthew 3)

    static void baptism(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) return;
        String g = d.creatorName;
        if (TheSon.at(d, t, 0, 0)) {
            BlockPos water = TheSon.water(level, d.town != null ? d.town : son.blockPosition(), 56);
            d.visiting = water != null ? water : d.town;
            MortalEntity baptist = baptist(level, d, son);
            if (baptist != null) {
                d.baptist = baptist.getUUID();
                d.baptistName = baptist.getName().getString();
                baptist.say("Repent ye: for the kingdom of " + g + " is at hand.");
                TheSon.narrate(level, baptist.blockPosition(), "In those days came " + d.baptistName + " the Baptist, preaching, and saying, Repent ye: "
                        + "for the kingdom of " + g + " is at hand. Then went out to " + (d.baptistName.endsWith("s") ? "him" : "him") + " all the land, and were baptised.",
                        "Matthew 3:1-6");
            }
            d.setDirty();
            return;
        }
        BlockPos site = d.visiting != null ? d.visiting : son.blockPosition();
        MortalEntity baptist = TheSon.mortal(level, d.baptist);
        if (d.step == 1) {
            boolean hurry = t > TheSon.secs(25);
            boolean a = TheSon.go(level, son, site, 2.5, hurry);
            boolean b = baptist == null || TheSon.go(level, baptist, site.offset(1, 0, 1), 2.5, hurry);
            if (a && b && t > TheSon.secs(8)) {
                d.step = 2;
                d.since = now;
                if (baptist != null) baptist.say("Behold the Lamb of " + g + ", which taketh away the sin of the world.");
            }
            return;
        }
        if (TheSon.at(d, TheSon.since(d, now), 2, 6)) {
            son.beginAct(MortalEntity.BOW, 60);
            level.sendParticles(ParticleTypes.SPLASH, son.getX(), son.getY() + 1, son.getZ(), 60, 0.6, 0.6, 0.6, 0.1);
            level.sendParticles(ParticleTypes.BUBBLE, son.getX(), son.getY() + 0.5, son.getZ(), 30, 0.5, 0.4, 0.5, 0.05);
            Fx.send(level, FxKind.GLORY_FLASH, son.position().add(0, 6, 0), 14f, 0xFFFFF0C0, 40);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, son.position(), 40f, 0xFFFFF4D6, 60);
            BirdEntity dove = ModEntities.BIRD.get().create(level);
            if (dove != null) {
                Vec3 from = son.position().add(4, 28, 3);
                dove.send(BirdEntity.DOVE, from, son.position().add(0, 2.4, 0), BirdEntity.AWAY, false);
                level.addFreshEntity(dove);
            }
            TheSon.narrate(level, son.blockPosition(), "And " + d.name + ", when he was baptised, went up straightway out of the water: and, lo, "
                    + "the heavens were opened unto him, and he saw the Spirit descending like a dove, and lighting upon him.", "Matthew 3:16");
            askVoice(level, d, son, "baptism", "This is my beloved Son, in whom I am well pleased.");
        } else if (d.step == 3) {
            if (waitVoice(now, d)) return;
            heavenSpeaks(level, d, son, "This is my beloved Son, in whom I am well pleased.");
            for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 32)) {
                m.beginAct(MortalEntity.BOW, 60);
                m.setDoubt(m.doubt() - 0.4);
            }
            d.deed("And " + d.name + " was baptised of " + d.baptistName + ", and the heavens were opened, and a voice came from heaven.");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "baptism", "was baptised" + (d.baptistName.isEmpty() ? "" : " of " + d.baptistName)
                    + ", and the Spirit descended upon him like a dove, and a voice came from heaven.");
            d.step = 4;
            d.since = now;
        } else if (d.step == 4 && TheSon.since(d, now) > TheSon.secs(8)) {
            d.to(SonData.Stage.TEMPTATION, now);
        }
    }

    /** The prophet of his day, if one lives on his world; else one of the most devout. */
    @Nullable
    private static MortalEntity baptist(ServerLevel level, SonData d, MortalEntity son) {
        for (Prophet p : ProphecyData.get(level.getServer()).prophets.values()) {
            if (p.living() && !p.false_ && p.world.equals(d.world)) {
                MortalEntity m = TheSon.mortal(level, p.id);
                if (m != null) return m;
            }
        }
        return level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(96),
                m -> m != son && !m.isChild() && m.isMale() && d.faith != null && d.faith.equals(m.religion()) && !m.getUUID().equals(d.father))
                .stream().max(Comparator.comparingDouble(m -> (m.hasTrait(Trait.PIOUS) ? 3 : 0) + (m.profession() == Profession.PRIEST ? 1 : 0)
                        + level.random.nextDouble())).orElse(null);
    }

    /** The Creator is asked to speak the words from heaven, and the story waits a little for them. */
    static void askVoice(ServerLevel level, SonData d, MortalEntity son, String what, String words) {
        ServerPlayer creator = d.creator == null ? null : level.getServer().getPlayerList().getPlayer(d.creator);
        d.step++;
        d.since = level.getGameTime();
        d.setDirty();
        TheSon.spoken = "";
        if (creator != null && creator.level() == level && creator.distanceToSqr(son) < 160 * 160) {
            TheSon.awaitingVoice = what;
            TheSon.awaitingUntil = level.getGameTime() + 600;
            creator.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Heaven] Speak from heaven over your Son - say: \"" + words + "\"")
                    .withStyle(s -> s.withColor(0xFFE9A8)));
        } else {
            TheSon.awaitingVoice = "";
        }
    }

    static boolean waitVoice(long now, SonData d) {
        return !TheSon.forcing && !TheSon.awaitingVoice.isEmpty() && TheSon.spoken.isEmpty() && now < TheSon.awaitingUntil;
    }

    static void heavenSpeaks(ServerLevel level, SonData d, MortalEntity son, String words) {
        String said = TheSon.spoken.isEmpty() ? words : TheSon.spoken;
        TheSon.awaitingVoice = "";
        TheSon.spoken = "";
        TheSon.voice(level, son.blockPosition(), "A voice from heaven (" + d.creatorName + ")", 0xFFE070, said);
        TheSon.sound(level, son.blockPosition(), SoundEvents.BEACON_ACTIVATE, 2f, 0.6f);
        Fx.send(level, FxKind.GLORY_FLASH, son.position().add(0, 3, 0), 10f, 0xFFFFF0C0, 30);
    }

    // ------------------------------------------------------------------ the temptation (Matthew 4)

    static void temptation(ServerLevel level, SonData d, long t, long now) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) return;
        String g = d.creatorName;
        if (TheSon.at(d, t, 0, 0)) {
            BlockPos from = d.town != null ? d.town : son.blockPosition();
            double a = level.random.nextDouble() * Math.PI * 2;
            BlockPos w = MortalPopulation.safeGround(level, from.getX() + (int) (Math.cos(a) * 70), from.getZ() + (int) (Math.sin(a) * 70), 16);
            d.visiting = w != null ? w : from.offset(40, 0, 0);
            TheSon.narrate(level, son.blockPosition(), "Then was " + d.name + " led up of the Spirit into the wilderness to be tempted of the devil. "
                    + "And when he had fasted forty days and forty nights, he was afterward an hungred.", "Matthew 4:1-2");
            return;
        }
        if (d.step == 1) {
            if (TheSon.go(level, son, d.visiting, 3, t > TheSon.secs(20)) && t > TheSon.secs(6)) {
                d.step = 2;
                d.since = now;
                son.beginAct(MortalEntity.PRAY, 200);
            }
            return;
        }
        long u = TheSon.since(d, now);
        Vec3 by = son.position().add(3, 0, 1);
        if (TheSon.at(d, u, 2, 4)) {
            var devil = TheSon.herald(level, ModEntities.FALLEN.get(), by, "the Devil");
            if (devil != null) {
                devil.getLookControl().setLookAt(son, 30f, 30f);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, by.x, by.y + 1, by.z, 30, 0.4, 0.8, 0.4, 0.02);
            }
            TheSon.voice(level, son.blockPosition(), "The Devil", 0xC04030, "If thou be the Son of " + g + ", command that these stones be made bread.");
        } else if (TheSon.at(d, u, 3, 9)) {
            TheSon.speak(level, son, d, "It is written, Man shall not live by bread alone, but by every word that proceedeth out of the mouth of " + g + ".", true);
        } else if (TheSon.at(d, u, 4, 15)) {
            TheSon.voice(level, son.blockPosition(), "The Devil", 0xC04030, "If thou be the Son of " + g + ", cast thyself down: for it is written, "
                    + "He shall give his angels charge concerning thee.");
        } else if (TheSon.at(d, u, 5, 20)) {
            TheSon.speak(level, son, d, "It is written again, Thou shalt not tempt " + g + " thy God.", true);
        } else if (TheSon.at(d, u, 6, 26)) {
            TheSon.voice(level, son.blockPosition(), "The Devil", 0xC04030, "All the kingdoms of " + d.world + ", and the glory of them, will I give thee, "
                    + "if thou wilt fall down and worship me.");
        } else if (TheSon.at(d, u, 7, 31)) {
            TheSon.speak(level, son, d, "Get thee hence, Satan: for it is written, Thou shalt worship " + g + " thy God, and him only shalt thou serve.", true);
        } else if (TheSon.at(d, u, 8, 36)) {
            for (var e : level.getEntitiesOfClass(com.josephison.fiatlux.entity.FallenEntity.class, son.getBoundingBox().inflate(12),
                    e -> e.getTags().contains("fiatlux_son_story"))) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1, e.getZ(), 40, 0.4, 1, 0.4, 0.03);
                e.discard();
            }
            TheSon.herald(level, ModEntities.ANGEL.get(), son.position().add(-2, 0.5, 2), "");
            TheSon.herald(level, ModEntities.ANGEL.get(), son.position().add(2, 0.5, -2), "");
            TheSon.narrate(level, son.blockPosition(), "Then the devil leaveth him, and, behold, angels came and ministered unto him.", "Matthew 4:11");
            d.deed("And " + d.name + " was tempted of the devil in the wilderness, and overcame him by the word of " + g + ".");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "temptation", "was tempted of the devil in the wilderness of "
                    + d.world + ", and said, Get thee hence, Satan.");
        } else if (d.step == 9 && u > TheSon.secs(44)) {
            TheSon.dismiss(level, son.blockPosition());
            d.visiting = null;
            d.to(SonData.Stage.MINISTRY, now);
        }
    }

    // ------------------------------------------------------------------ the ministry

    private static final String[] TEACHING = {
            "Blessed are the poor in spirit: for theirs is the kingdom of heaven.",
            "Blessed are they that mourn: for they shall be comforted.",
            "Blessed are the meek: for they shall inherit the earth.",
            "Blessed are the merciful: for they shall obtain mercy.",
            "Blessed are the pure in heart: for they shall see God.",
            "Blessed are the peacemakers: for they shall be called the children of God.",
            "Ye are the light of the world. A city that is set on an hill cannot be hid.",
            "Love your enemies, bless them that curse you, do good to them that hate you.",
            "Ask, and it shall be given you; seek, and ye shall find; knock, and it shall be opened unto you.",
            "Judge not, that ye be not judged.",
            "Come unto me, all ye that labour and are heavy laden, and I will give you rest.",
            "I am the way, the truth, and the life.",
            "I am the light of the world: he that followeth me shall not walk in darkness.",
            "I am the good shepherd: the good shepherd giveth his life for the sheep.",
            "Thou shalt love thy neighbour as thyself.",
            "Lay not up for yourselves treasures upon earth, where moth and rust doth corrupt.",
            "Suffer the little children to come unto me, and forbid them not.",
            "Whosoever shall smite thee on thy right cheek, turn to him the other also."};
    private static final String[] PARABLES = {
            "A sower went forth to sow; and some seeds fell by the way side, and some upon stony places, and other fell into good ground, and brought forth fruit.",
            "The kingdom of heaven is like to a grain of mustard seed, which is the least of all seeds: but when it is grown, it is the greatest among herbs.",
            "What man of you, having an hundred sheep, if he lose one of them, doth not leave the ninety and nine, and go after that which is lost?",
            "A certain man had two sons: and the younger gathered all together, and wasted his substance with riotous living; and when he came to himself, he arose, and came to his father.",
            "A certain man went down from the town, and fell among thieves; and a stranger came where he was, and had compassion on him, and bound up his wounds.",
            "The kingdom of heaven is like unto treasure hid in a field; the which when a man hath found, he selleth all that he hath, and buyeth that field.",
            "Whosoever heareth these sayings of mine, and doeth them, I will liken him unto a wise man, which built his house upon a rock.",
            "The kingdom of heaven is like unto leaven, which a woman took, and hid in three measures of meal, till the whole was leavened.",
            "Ten virgins took their lamps, and went forth to meet the bridegroom: five of them were wise, and five were foolish."};

    static void ministry(ServerLevel level, SonData d, long t, long now, boolean force) {
        MortalEntity son = TheSon.mortal(level, d.son);
        if (son == null) return;
        double length = TheSon.secs(FiatLuxConfig.COMMON.sonMinistrySeconds.get());
        if (son.tickCount % 60 == 0) TheSon.glory(level, son, 1);
        if (force) {
            if (d.town != null) TheSon.go(level, son, d.town, 3, true);
            callDisciples(level, d, son, 12);
            if (!d.transfigured) d.transfigured = true;
            d.to(SonData.Stage.ENTRY, now);
            return;
        }
        // the twelve
        if (d.disciples.size() < 12 && now - d.lastSign > 60 && (d.disciples.size() < 4 || level.random.nextInt(12) == 0)) {
            d.lastSign = now;
            callDisciples(level, d, son, 1);
        }
        TheSon.follow(level, d, son);
        // the mount of transfiguration, midway
        if (!d.transfigured && t > length * 0.55) {
            if (transfiguration(level, d, son, now)) return;
        }
        if (d.step >= 100) {   // still upon the mountain
            transfiguration(level, d, son, now);
            return;
        }
        if (Motion.walking()) return;
        Settlement town = journey(level, d, son, now);
        if (town == null) return;
        if (now - d.lastWonder > TheSon.secs(22) && sign(level, d, son, town)) {
            d.lastWonder = now;
            d.setDirty();
            return;
        }
        if (now - d.lastWord > TheSon.secs(16)) {
            d.lastWord = now;
            teach(level, d, son, town);
        }
        if (t > length) d.to(SonData.Stage.ENTRY, now);
    }

    /** "Follow me, and I will make you fishers of men" (Matthew 4:19). */
    static void callDisciples(ServerLevel level, SonData d, MortalEntity son, int count) {
        ProphecyData pd = ProphecyData.get(level.getServer());
        for (int k = 0; k < count && d.disciples.size() < 12; k++) {
            MortalEntity m = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(48), x -> x != son && x.isMale() && !x.isChild()
                            && !x.isElder() && !d.disciples.contains(x.getUUID()) && !x.isPossessed() && x.sentence() == Justice.Sentence.NONE
                            && !x.getUUID().equals(d.father) && pd.prophet(x.getUUID()) == null && !x.isNephil())
                    .stream().max(Comparator.comparingDouble(x -> (x.profession() == Profession.HUNTER || x.profession() == Profession.GATHERER ? 2 : 0)
                            + (d.faith != null && d.faith.equals(x.religion()) ? 1 : 0) + level.random.nextDouble() * 2)).orElse(null);
            if (m == null) {   // the town has no more to give: the rest come from the crowds
                if (level.random.nextInt(4) != 0 || d.disciples.size() >= 12) return;
                m = MortalPopulation.bringForth(level, son.blockPosition().offset(3, 0, 3), son.blockPosition(),
                        TheSon.world(level) != null ? TheSon.world(level).era : com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN, null, true, 24, null, null, d.faith);
                if (m == null) return;
            }
            d.disciples.add(m.getUUID());
            d.discipleNames.add(m.getName().getString());
            if (d.faith != null) m.setReligion(d.faith);
            m.setDoubt(0);
            TheSon.speak(level, son, d, "Follow me, " + m.getName().getString() + ", and I will make you fishers of men.", false);
            m.say("Master, I will follow thee whithersoever thou goest.");
            m.beginAct(MortalEntity.BOW, 40);
            // the one who will betray him: the one who loves silver
            Alignment a = Divinity.alignment(m);
            if (d.betrayer == null && (m.hasTrait(Trait.GREEDY) || m.hasTrait(Trait.PROUD) || a.sin() > a.virtue() + 2)) {
                d.betrayer = m.getUUID();
                d.betrayerName = m.getName().getString();
            }
            d.setDirty();
        }
        if (d.betrayer == null && d.disciples.size() >= 12) {
            int i = d.disciples.size() - 1;
            d.betrayer = d.disciples.get(i);
            d.betrayerName = d.discipleNames.get(i);
        }
    }

    /** From town to town (Matthew 9:35). Returns the town once he is in it. */
    @Nullable
    private static Settlement journey(ServerLevel level, SonData d, MortalEntity son, long now) {
        Planet world = TheSon.world(level);
        if (world == null) return null;
        List<Settlement> towns = CosmicExpanseSavedData.get(level.getServer()).townsOf(world.id);
        if (towns.isEmpty()) return null;
        Settlement current = d.visiting == null ? null : towns.stream().filter(x -> x.centre.equals(d.visiting)).findFirst().orElse(null);
        if (current == null || now - d.arrived > TheSon.secs(70) && d.arrived > 0) {
            List<Settlement> loaded = towns.stream().filter(x -> level.isLoaded(x.centre) && x != current).toList();
            Settlement next = loaded.isEmpty() ? (current != null ? current : towns.getFirst()) : loaded.get(level.random.nextInt(loaded.size()));
            d.visiting = next.centre;
            d.arrived = 0;
            d.setDirty();
            return null;
        }
        if (!TheSon.go(level, son, current.centre.offset(0, 0, 5), 8, false)) return null;
        if (d.arrived == 0) {
            d.arrived = now;
            son.setHome(current.centre);
            d.setDirty();
        }
        return current;
    }

    private static void teach(ServerLevel level, SonData d, MortalEntity son, Settlement town) {
        boolean parable = level.random.nextInt(5) < 2;
        String[] from = parable ? PARABLES : TEACHING;
        List<String> unsaid = new ArrayList<>();
        for (String s : from) if (!d.sayings.contains(s)) unsaid.add(s);
        String words = unsaid.isEmpty() ? from[level.random.nextInt(from.length)] : unsaid.get(level.random.nextInt(unsaid.size()));
        TheSon.speak(level, son, d, words, true);
        if (parable) d.parables++;
        int turned = 0;
        Religion faith = Generations.get(level.getServer()).religion(d.faith);
        for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 26)) {
            if (m.act() == MortalEntity.NONE) m.getLookControl().setLookAt(son, 30f, 30f);
            if (faith != null && !faith.id.equals(m.religion()) && level.random.nextDouble() < (m.hasTrait(Trait.PROUD) ? 0.1 : 0.3)) {
                m.setReligion(faith.id);
                turned++;
            }
            if (faith != null && faith.id.equals(m.religion())) m.setDoubt(m.doubt() - 0.2);
            m.addVirtue(1);
        }
        if (turned > 0) d.deed("And in " + (town.name.isEmpty() ? d.world : town.name) + " " + turned + " believed on him because of his word.");
        // the priests and the rulers murmur
        if (faith != null && faith.leader != null && level.getEntity(faith.leader) instanceof MortalEntity leader && leader.distanceToSqr(son) < 40 * 40
                && com.josephison.fiatlux.civilization.Churches.corrupt(leader) && level.random.nextInt(3) == 0) {
            leader.say("By what authority doest thou these things? and who gave thee this authority?");
        }
    }

    // ------------------------------------------------------------------ signs and wonders

    /** A miracle where it is needed. Returns whether one was worked. */
    private static boolean sign(ServerLevel level, SonData d, MortalEntity son, Settlement town) {
        String place = town.name.isEmpty() ? d.world : town.name;
        String g = d.creatorName;
        // an unclean spirit
        MortalEntity possessed = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(28), m -> m.isPossessed() && !m.beingCastOut())
                .stream().findFirst().orElse(null);
        if (possessed != null) {
            TheSon.go(level, son, possessed.blockPosition().offset(2, 0, 0), 3, true);
            boolean legion = possessed.spiritName().equals("Legion");
            possessed.say(legion ? "My name is Legion: for we are many." : "What have we to do with thee? I know thee who thou art, the Holy One of " + g + ".");
            TheSon.speak(level, son, d, "Hold thy peace, and come out of " + (possessed.isMale() ? "him." : "her."), false);
            possessed.exorcise(d.name);
            if (legion) swine(level, possessed.blockPosition());
            d.deed("And in " + place + " he cast out " + (legion ? "Legion, and the devils went into the swine, and the herd ran violently into the sea" :
                    "an unclean spirit from " + possessed.getName().getString()) + ".");
            miracle(level, d, son, possessed.position());
            return true;
        }
        // the sick
        MortalEntity sick = level.getEntitiesOfClass(MortalEntity.class, son.getBoundingBox().inflate(24), m -> m != son && m.getHealth() < m.getMaxHealth() * 0.85)
                .stream().findFirst().orElse(null);
        if (sick != null) {
            TheSon.go(level, son, sick.blockPosition().offset(1, 0, 1), 2.5, true);
            TheSon.speak(level, son, d, "Thy faith hath made thee whole; go in peace.", false);
            sick.heal(sick.getMaxHealth());
            sick.say("I am healed! Glory to " + g + "!");
            d.deed("And in " + place + " he healed " + sick.getName().getString() + ", saying, Thy faith hath made thee whole.");
            miracle(level, d, son, sick.position());
            return true;
        }
        // the storm
        if (level.isThundering() || level.isRaining() && level.random.nextInt(2) == 0) {
            TheSon.speak(level, son, d, "Peace, be still.", false);
            level.setWeatherParameters(12000, 0, false, false);
            for (MortalEntity m : TheSon.disciples(level, d)) m.say("What manner of man is this, that even the winds and the sea obey him!");
            d.deed("And there arose a great storm of wind over " + place + "; and he rebuked the wind, and said, Peace, be still. And there was a great calm.");
            miracle(level, d, son, son.position());
            return true;
        }
        // the wine at a wedding (John 2)
        if (!d.wine && Weddings.feastingNear(level, son.blockPosition(), 40)) {
            d.wine = true;
            TheSon.speak(level, son, d, "Fill the waterpots with water. Draw out now, and bear unto the governor of the feast.", false);
            for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 24)) if (level.random.nextInt(3) == 0) m.say("Thou hast kept the good wine until now!");
            d.deed("And there was a marriage in " + place + "; and he turned the water into wine. This beginning of miracles did he, and manifested forth his glory.");
            miracle(level, d, son, son.position());
            return true;
        }
        // the dead (John 11)
        if (d.raised < 2 && level.random.nextInt(3) == 0) {
            MortalEntity raised = raise(level, d, son, town);
            if (raised != null) {
                d.raised++;
                d.deed("And in " + place + " he cried with a loud voice, " + raised.getName().getString() + ", come forth. And he that was dead came forth.");
                miracle(level, d, son, raised.position());
                return true;
            }
        }
        // the hungry multitude (Matthew 14:13-21)
        if (town.food < 25 && d.fed < 3) {
            TheSon.speak(level, son, d, "They need not depart; give ye them to eat. Bring me the five loaves and two fishes.", false);
            town.food += 60;
            CosmicExpanseSavedData.get(level.getServer()).setDirty();
            int n = 0;
            for (MortalEntity m : TheSon.crowd(level, son.blockPosition(), 30)) {
                m.feast();
                n++;
            }
            d.fed++;
            d.deed("And in " + place + " he took the five loaves and the two fishes, and blessed, and brake, and they did all eat, " + n
                    + " of them, and were filled: and they took up of the fragments that remained twelve baskets full.");
            miracle(level, d, son, son.position());
            return true;
        }
        // upon the sea (Matthew 14:25)
        if (!d.walkedOnWater && level.random.nextInt(3) == 0) {
            BlockPos water = TheSon.water(level, son.blockPosition(), 40);
            if (water != null && Motion.walkOnWater(level, d, son, water)) {
                d.walkedOnWater = true;
                d.deed("And in the fourth watch of the night " + d.name + " went unto them, walking on the sea. And they cried out for fear; "
                        + "but straightway he spake unto them, saying, Be of good cheer; it is I; be not afraid.");
                d.miracles++;
                return true;
            }
        }
        return false;
    }

    private static void miracle(ServerLevel level, SonData d, MortalEntity son, Vec3 at) {
        d.miracles++;
        Fx.send(level, FxKind.BLESSING, at, 2.6f, 0xFFFFF0C0, 30);
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), at.x, at.y + 1, at.z, 30, 0.5, 0.8, 0.5, 0.06);
        Religion faith = Generations.get(level.getServer()).religion(d.faith);
        for (MortalEntity w : TheSon.crowd(level, BlockPos.containing(at), 24)) {
            w.witness(false);
            if (faith != null && !faith.id.equals(w.religion()) && level.random.nextDouble() < 0.5) w.setReligion(faith.id);
        }
        CosmosData.get(level.getServer()).record(level.getServer(), d.name, "miracle", d.deeds.isEmpty() ? "worked a sign." :
                d.deeds.getLast().replaceFirst("^And ", "").replace(" he ", " " + d.name + " "));
        d.setDirty();
    }

    /** "Lazarus, come forth" (John 11:43): one of the faithful dead of this world lives again. */
    @Nullable
    private static MortalEntity raise(ServerLevel level, SonData d, MortalEntity son, Settlement town) {
        Generations g = Generations.get(level.getServer());
        long now = level.getGameTime();
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        Person dead = g.people.values().stream()
                .filter(p -> !p.living() && p.world.equals(d.world) && !p.translated && !p.nephil && !p.redeemed && now - p.died < 6 * year
                        && !p.id.equals(d.son) && level.getEntity(p.id) == null)
                .max(Comparator.comparingLong(p -> p.died)).orElse(null);
        if (dead == null) return null;
        MortalEntity m = ModEntities.MORTAL.get().create(level);
        if (m == null) return null;
        BlockPos at = MortalPopulation.safeGround(level, son.getBlockX() + 3, son.getBlockZ() + 2, 4);
        if (at == null) return null;
        m.setUUID(dead.id);
        Planet world = TheSon.world(level);
        if (world != null) m.setEra(world.era);
        m.restore(dead);
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        m.setHome(town.centre);
        level.addFreshEntity(m);
        // the soul comes back from its realm
        for (var key : List.of(FiatLuxDimensions.HEAVEN, FiatLuxDimensions.UNDERWORLD)) {
            ServerLevel realm = level.getServer().getLevel(key);
            if (realm == null) continue;
            for (SoulEntity s : realm.getEntitiesOfClass(SoulEntity.class, new AABB(BlockPos.ZERO).inflate(30000), s -> dead.id.equals(s.person()))) s.discard();
        }
        if ("heaven".equals(dead.soul)) CosmicExpanseSavedData.get(level.getServer()).heavenSouls -= 1;
        dead.died = -1;
        dead.soul = "";
        dead.embodied = false;
        g.setDirty();
        TheSon.speak(level, son, d, "I am the resurrection, and the life. " + dead.name + ", come forth!", true);
        m.say("I was dead... and I live!");
        Fx.send(level, FxKind.CELESTIAL_PILLAR, m.position(), 20f, 0xFFFFF4D6, 40);
        CosmosData.get(level.getServer()).record(level.getServer(), d.name, "raised", "raised " + dead.name + " from the dead in "
                + (town.name.isEmpty() ? d.world : town.name) + ".");
        return m;
    }

    /** "And the unclean spirits went out, and entered into the swine: and the herd ran violently down a steep place into the sea" (Mark 5:13). */
    private static void swine(ServerLevel level, BlockPos near) {
        BlockPos water = TheSon.water(level, near, 48);
        for (int i = 0; i < 6; i++) {
            Pig pig = EntityType.PIG.create(level);
            if (pig == null) continue;
            pig.moveTo(near.getX() + level.random.nextInt(7) - 3, near.getY(), near.getZ() + level.random.nextInt(7) - 3, 0, 0);
            pig.addTag("fiatlux_son_story");
            level.addFreshEntity(pig);
            if (water != null) pig.getNavigation().moveTo(water.getX(), water.getY(), water.getZ(), 1.6);
            level.sendParticles(ParticleTypes.SMOKE, pig.getX(), pig.getY() + 0.8, pig.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // ------------------------------------------------------------------ the transfiguration (Matthew 17)

    /** Returns true while it is happening. */
    private static boolean transfiguration(ServerLevel level, SonData d, MortalEntity son, long now) {
        List<MortalEntity> three = TheSon.disciples(level, d).stream().limit(3).toList();
        if (d.step < 100) {
            BlockPos high = highPlace(level, son.blockPosition(), 40);
            d.visiting = high;
            d.step = 100;
            d.since = now;
            TheSon.narrate(level, high, "And after six days " + d.name + " taketh " + (three.isEmpty() ? "his disciples" : names(three))
                    + ", and bringeth them up into an high mountain apart.", "Matthew 17:1");
            return true;
        }
        long u = TheSon.since(d, now);
        BlockPos high = d.visiting != null ? d.visiting : son.blockPosition();
        if (d.step == 100) {
            boolean there = TheSon.go(level, son, high, 2.5, u > TheSon.secs(15));
            for (int i = 0; i < three.size(); i++) TheSon.go(level, three.get(i), high.offset(-3 + i * 3, 0, 4), 2.5, u > TheSon.secs(15));
            if (there && u > TheSon.secs(5)) {
                d.step = 101;
                d.since = now;
            }
            return true;
        }
        if (d.step == 101) {
            son.setGlory(2);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, son.position(), 50f, 0xFFFFFFFF, 80);
            Fx.send(level, FxKind.GLORY_FLASH, son.position().add(0, 2, 0), 16f, 0xFFFFFFFF, 40);
            List<String> elders = elders(level, d);
            for (int i = 0; i < elders.size(); i++) {
                BlockPos at = high.offset(i == 0 ? -2 : 2, 0, 0);
                SoulEntity soul = com.josephison.fiatlux.civilization.SoulRealms.spawn(level, null, elders.get(i), true, d.world, true, at, false);
                if (soul != null) {
                    soul.addTag("fiatlux_son_story");
                    soul.setNoAi(true);
                }
            }
            for (MortalEntity m : three) m.beginAct(MortalEntity.BOW, 300);
            TheSon.narrate(level, high, "And he was transfigured before them: and his face did shine as the sun, and his raiment was white as the light. "
                    + "And, behold, there appeared unto them " + String.join(" and ", elders) + " talking with him.", "Matthew 17:2-3");
            askVoice(level, d, son, "transfiguration", "This is my beloved Son, in whom I am well pleased; hear ye him.");
            d.step = 102;
            return true;
        }
        if (d.step == 102) {
            if (waitVoice(now, d) && u < TheSon.secs(40)) return true;
            heavenSpeaks(level, d, son, "This is my beloved Son, in whom I am well pleased; hear ye him.");
            d.step = 103;
            d.since = now;
            return true;
        }
        if (d.step == 103 && u > TheSon.secs(10)) {
            son.setGlory(1);
            TheSon.dismiss(level, high);
            TheSon.speak(level, son, d, "Arise, and be not afraid. Tell the vision to no man, until the Son be risen again from the dead.", true);
            d.deed("And he was transfigured before them on a high mountain, and his face did shine as the sun; and a voice out of the cloud said, Hear ye him.");
            CosmosData.get(level.getServer()).record(level.getServer(), d.name, "transfiguration", "was transfigured on a high mountain of "
                    + d.world + ", and his face did shine as the sun.");
            d.transfigured = true;
            d.step = 1;
            d.visiting = null;
            d.setDirty();
            return false;
        }
        return true;
    }

    private static String names(List<MortalEntity> ms) {
        List<String> n = new ArrayList<>();
        for (MortalEntity m : ms) n.add(m.getName().getString());
        return String.join(", ", n);
    }

    /** The two who appear with him: the great prophets who went up, or failing them the fathers of old. */
    private static List<String> elders(ServerLevel level, SonData d) {
        List<String> out = new ArrayList<>();
        for (Prophet p : ProphecyData.get(level.getServer()).prophets.values()) {
            if (!p.false_ && p.world.equals(d.world) && !p.living() && out.size() < 2) out.add(p.name);
        }
        for (Person p : Generations.get(level.getServer()).people.values()) {
            if (out.size() >= 2) break;
            if (p.world.equals(d.world) && (p.translated || p.name.equals("Noah")) && !out.contains(p.name)) out.add(p.name);
        }
        if (out.isEmpty()) out.add("Moses");
        if (out.size() < 2) out.add("Elijah");
        return out;
    }

    private static BlockPos highPlace(ServerLevel level, BlockPos around, int r) {
        BlockPos best = around;
        int top = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += 4) for (int dz = -r; dz <= r; dz += 4) {
            int x = around.getX() + dx, z = around.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y > top && level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                top = y;
                best = new BlockPos(x, y, z);
            }
        }
        return best;
    }
}
