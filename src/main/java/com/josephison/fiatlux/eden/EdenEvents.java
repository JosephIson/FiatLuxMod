package com.josephison.fiatlux.eden;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.CherubEntity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.entity.StationedSwordEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The story of the garden, played out (Genesis 3). Once Satan has fallen, the serpent comes to the
 * woman in Eden and speaks with her; she takes of the fruit of the tree of knowledge and eats, and
 * gives to her husband; their eyes are opened and they hide among the trees. When their Creator comes
 * walking in the garden - or calls "Where art thou?" - they answer, the serpent is cursed, they are
 * clothed in coats of skins and sent out of the garden eastward, and cherubim and a flaming sword are
 * set to keep the way of the tree of life. From that day men die, and children are born, and sin is in
 * the world. A Creator may strike the serpent to drive him away for a while; he returns.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class EdenEvents {
    private static final int RANGE = 96;
    @Nullable private static ServerPlayer voice;

    private EdenEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 11) {
            tick(event.getServer(), false);
            CainAndAbel.tick(event.getServer(), false);
        }
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        if (c.eden == EdenState.NONE || c.edenDim.isEmpty()) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.edenDim)));
    }

    /** One beat of the garden's story. force: skip every wait and walk (for tests and commands). */
    public static void tick(MinecraftServer server, boolean force) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        ServerLevel level = level(server);
        if (level == null || !level.isLoaded(c.edenCentre)) return;
        long now = level.getGameTime();
        long elapsed = force ? Long.MAX_VALUE / 4 : now - c.edenSince;
        switch (c.eden) {
            case INNOCENCE -> {
                long delay = FiatLuxConfig.COMMON.edenTemptationTicks.get();
                boolean watched = !level.getEntitiesOfClass(ServerPlayer.class, new AABB(c.edenCentre).inflate(80), p -> !p.isSpectator()).isEmpty();
                if (c.fallenAngels > 0 && (force || (elapsed > delay && watched)) && person(level, c, "Eve") != null) {
                    SerpentEntity serpent = ModEntities.SERPENT.get().create(level);
                    if (serpent == null) return;
                    BlockPos at = c.knowledgeTree.offset(-2, 0, -2);
                    serpent.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                    level.addFreshEntity(serpent);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, serpent.getX(), serpent.getY() + 0.5, serpent.getZ(), 20, 0.4, 0.3, 0.4, 0.01);
                    serpent.say("Hsss...");
                    narrate(level, c, "Now the serpent was more subtil than any beast of the field which the LORD God had made.", "Genesis 3:1");
                    next(c, EdenState.TEMPTATION, now);
                }
            }
            case TEMPTATION -> temptation(level, c, now, elapsed, force);
            case FALLEN -> {
                hide(level, c, now);
                MortalEntity adam = person(level, c, "Adam");
                ServerPlayer near = adam == null ? null : level.getEntitiesOfClass(ServerPlayer.class, adam.getBoundingBox().inflate(14),
                        p -> !p.isSpectator() && !Divinity.get(p).pilgrim()).stream().findFirst().orElse(null);
                boolean watched = !level.getEntitiesOfClass(ServerPlayer.class, new AABB(c.edenCentre).inflate(RANGE)).isEmpty();
                if (near != null || voice != null || force || (elapsed > 4800 && watched)) {
                    if (voice == null) voice = near;
                    next(c, EdenState.JUDGMENT, now);
                }
            }
            case JUDGMENT -> judgment(level, c, now, elapsed, force);
            case EXILED -> keepTheWay(level, c, now);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ the temptation (Genesis 3:1-7)

    private static void temptation(ServerLevel level, CosmicExpanseSavedData c, long now, long elapsed, boolean force) {
        SerpentEntity serpent = serpent(level, c);
        MortalEntity eve = person(level, c, "Eve"), adam = person(level, c, "Adam");
        if (serpent == null || eve == null) {   // the scene cannot go on: wait for the tempter's return
            if (serpent != null) serpent.discard();
            next(c, EdenState.INNOCENCE, now);
            return;
        }
        serpent.getLookControl().setLookAt(eve, 30f, 30f);
        eve.getLookControl().setLookAt(serpent, 30f, 30f);
        switch (c.edenStep) {
            case 0 -> {
                if (approach(serpent, eve, 3.2, elapsed > 600 || force)) step(c, now);
            }
            case 1 -> say(c, now, elapsed, 120, () -> serpent.say("Yea, hath God said, Ye shall not eat of every tree of the garden?"));
            case 2 -> say(c, now, elapsed, 130, () -> eve.say("We may eat of the fruit of the trees of the garden..."));
            case 3 -> say(c, now, elapsed, 150, () -> eve.say("...but of the tree in the midst of the garden, God hath said, Ye shall not eat of it, neither shall ye touch it, lest ye die."));
            case 4 -> say(c, now, elapsed, 100, () -> serpent.say("Ye shall not surely die."));
            case 5 -> say(c, now, elapsed, 170, () -> serpent.say("For God doth know that in the day ye eat thereof, then your eyes shall be opened, and ye shall be as gods, knowing good and evil."));
            case 6 -> {
                if (elapsed <= 20 || elapsed >= Long.MAX_VALUE / 8) narrate(level, c, "And when the woman saw that the tree was good for food, and that it was pleasant to the eyes, and a tree to be desired to make one wise...", "Genesis 3:6");
                if (approach(eve, c.knowledgeTree, 3.5, elapsed > 600 || force)) step(c, now);
            }
            case 7 -> say(c, now, elapsed, 100, () -> {
                pickFruit(level, c.knowledgeTree);
                eve.beginAct(MortalEntity.EAT, 80);
                eve.say("It is... sweet.");
                narrate(level, c, "...she took of the fruit thereof, and did eat,", "Genesis 3:6");
            });
            case 8 -> {
                if (adam == null || approach(adam, eve, 3.2, elapsed > 600 || force)) step(c, now);
            }
            case 9 -> say(c, now, elapsed, 100, () -> {
                narrate(level, c, "...and gave also unto her husband with her; and he did eat.", "Genesis 3:6");
                if (adam != null) adam.beginAct(MortalEntity.EAT, 80);
            });
            default -> {
                // "And the eyes of them both were opened, and they knew that they were naked" (3:7)
                narrate(level, c, "And the eyes of them both were opened, and they knew that they were naked; and they sewed fig leaves together.", "Genesis 3:7");
                for (MortalEntity m : List.of(eve, adam == null ? eve : adam)) {
                    Fx.send(level, FxKind.SHOCKWAVE, m.position(), 2.5f, 0xFF8A7050, 20);
                    level.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + 1.2, m.getZ(), 12, 0.3, 0.4, 0.3, 0.01);
                }
                if (adam != null) adam.say("I... I know. I know what I am. I know good... and evil.");
                eve.say("We are naked! Hide - hide among the trees!");
                c.fallOfMan = true;
                CosmosData.get(level.getServer()).record(level.getServer(), "Adam and Eve", "fall_of_man",
                        "ate of the fruit of the tree of the knowledge of good and evil, and the eyes of them both were opened.");
                level.playSound(null, c.knowledgeTree, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.AMBIENT, 0.6f, 0.6f);
                next(c, EdenState.FALLEN, now);
            }
        }
    }

    /** "And Adam and his wife hid themselves from the presence of the LORD God amongst the trees of the garden" (3:8). */
    private static void hide(ServerLevel level, CosmicExpanseSavedData c, long now) {
        for (String name : List.of("Adam", "Eve")) {
            MortalEntity m = person(level, c, name);
            if (m == null || m.act() == MortalEntity.BOW) continue;
            BlockPos spot = c.lifeTree.offset(name.equals("Adam") ? -6 : -4, 0, name.equals("Adam") ? 3 : 5);
            if (approach(m, spot, 2.5, false)) m.beginAct(MortalEntity.BOW, 200);
        }
    }

    // ------------------------------------------------------------------ the judgment (Genesis 3:8-24)

    private static void judgment(ServerLevel level, CosmicExpanseSavedData c, long now, long elapsed, boolean force) {
        MortalEntity adam = person(level, c, "Adam"), eve = person(level, c, "Eve");
        SerpentEntity serpent = serpent(level, c);
        String lord = voice != null ? voice.getGameProfile().getName() : "the LORD God";
        if (voice != null) {
            for (MortalEntity m : new MortalEntity[]{adam, eve}) if (m != null && m.act() == MortalEntity.BOW) m.beginAct(MortalEntity.NONE, 1);
        }
        switch (c.edenStep) {
            case 0 -> say(c, now, elapsed, 120, () -> {
                narrate(level, c, "And they heard the voice of " + lord + " walking in the garden in the cool of the day... And "
                        + lord + " called unto Adam, and said unto him, Where art thou?", "Genesis 3:8-9");
                if (voice != null) {
                    if (adam != null) adam.getNavigation().moveTo(voice, 0.6);
                    if (eve != null) eve.getNavigation().moveTo(voice, 0.6);
                }
            });
            case 1 -> say(c, now, elapsed, 160, () -> {
                if (adam != null) adam.say("I heard thy voice in the garden, and I was afraid, because I was naked; and I hid myself.");
            });
            case 2 -> say(c, now, elapsed, 130, () -> narrate(level, c, "Who told thee that thou wast naked? Hast thou eaten of the tree, "
                    + "whereof I commanded thee that thou shouldest not eat?", "Genesis 3:11"));
            case 3 -> say(c, now, elapsed, 150, () -> {
                if (adam != null) adam.say("The woman whom thou gavest to be with me, she gave me of the tree, and I did eat.");
            });
            case 4 -> say(c, now, elapsed, 130, () -> {
                narrate(level, c, "What is this that thou hast done?", "Genesis 3:13");
                if (eve != null) eve.say("The serpent beguiled me, and I did eat.");
            });
            case 5 -> say(c, now, elapsed, 170, () -> {
                narrate(level, c, "Because thou hast done this, thou art cursed above all cattle... upon thy belly shalt thou go, and dust shalt thou eat "
                        + "all the days of thy life: and I will put enmity between thee and the woman, and between thy seed and her seed; "
                        + "it shall bruise thy head, and thou shalt bruise his heel.", "Genesis 3:14-15");
                if (serpent != null) {
                    serpent.curse();
                    Fx.send(level, FxKind.HALO_SHATTER, serpent.position().add(0, 0.5, 0), 2f, 0xFF7A9A40, 20);
                    level.playSound(null, serpent.blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 0.8f, 0.5f);
                }
            });
            case 6 -> say(c, now, elapsed, 130, () -> narrate(level, c, "Unto the woman he said, I will greatly multiply thy sorrow and thy "
                    + "conception; in sorrow thou shalt bring forth children.", "Genesis 3:16"));
            case 7 -> say(c, now, elapsed, 170, () -> narrate(level, c, "And unto Adam he said... cursed is the ground for thy sake... In the sweat "
                    + "of thy face shalt thou eat bread, till thou return unto the ground; for dust thou art, and unto dust shalt thou return.", "Genesis 3:17-19"));
            case 8 -> say(c, now, elapsed, 130, () -> {
                narrate(level, c, "And Adam called his wife's name Eve; because she was the mother of all living. Unto Adam also and to his wife "
                        + "did the LORD God make coats of skins, and clothed them.", "Genesis 3:20-21");
                for (MortalEntity m : new MortalEntity[]{adam, eve}) {
                    if (m != null) Fx.send(level, FxKind.BLESSING, m.position(), m.getBbHeight(), 0xFFE8D8B0, 20);
                }
            });
            default -> expel(level, c, now, adam, eve, serpent);
        }
    }

    /** "So he drove out the man; and he placed at the east of the garden of Eden Cherubims, and a flaming sword" (3:24). */
    private static void expel(ServerLevel level, CosmicExpanseSavedData c, long now, @Nullable MortalEntity adam, @Nullable MortalEntity eve,
                              @Nullable SerpentEntity serpent) {
        narrate(level, c, "Therefore the LORD God sent him forth from the garden of Eden, to till the ground from whence he was taken. So he drove "
                + "out the man; and he placed at the east of the garden of Eden Cherubims, and a flaming sword which turned every way, to keep "
                + "the way of the tree of life.", "Genesis 3:23-24");
        if (serpent != null) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, serpent.getX(), serpent.getY() + 0.3, serpent.getZ(), 30, 0.6, 0.2, 0.6, 0.02);
            serpent.discard();
        }
        // their first home: east of Eden (Gen 4:16)
        BlockPos gate = EdenGarden.eastGate(c.edenCentre);
        BlockPos site = MortalPopulation.safeGround(level, gate.getX() + 34, gate.getZ(), 20);
        if (site != null) {
            Settlement town = new Settlement(site);
            Planet world = c.edenWorld == null ? null : c.planets.stream().filter(p -> p.id.equals(c.edenWorld)).findFirst().orElse(null);
            town.name = world != null ? world.name : "East of Eden";
            town.founded = now;
            town.founders = "Adam, Eve";
            MortalPopulation.build(level, town, world != null ? world.era : com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER);
            if (world != null) c.addTown(world.id, town);
            for (MortalEntity m : new MortalEntity[]{adam, eve}) if (m != null) m.leaveFor(site);
        }
        station(level, c, gate);
        CosmosData.get(level.getServer()).record(level.getServer(), "Adam and Eve", "exile",
                "were driven out of the garden of Eden; cherubim and a flaming sword were set at its east gate to keep the way of the tree of life.");
        voice = null;
        next(c, EdenState.EXILED, now);
    }

    /** Sets (or restores) the cherub and the flaming sword at the east gate. */
    static void station(ServerLevel level, CosmicExpanseSavedData c, BlockPos gate) {
        AABB around = new AABB(gate).inflate(10);
        if (level.getEntitiesOfClass(CherubEntity.class, around).isEmpty()) {
            CherubEntity cherub = ModEntities.CHERUB.get().create(level);
            if (cherub != null) {
                cherub.moveTo(gate.getX() + 0.5, gate.getY(), gate.getZ() - 3.5, -90f, 0);
                cherub.setStation(gate.offset(0, 0, -3));
                cherub.setPersistenceRequired();
                level.addFreshEntity(cherub);
                Fx.send(level, FxKind.HOST_ARRIVAL, cherub.position(), 4f, 0xFFFFE8B0, 30);
            }
        }
        if (level.getEntitiesOfClass(StationedSwordEntity.class, around).isEmpty()) {
            StationedSwordEntity sword = ModEntities.STATIONED_SWORD.get().create(level);
            if (sword != null) {
                sword.moveTo(gate.getX() + 0.5, gate.getY() + 0.5, gate.getZ() + 0.5, 0, 0);
                level.addFreshEntity(sword);
            }
        }
    }

    /** No mortal comes back into the garden; the guardians are kept at the gate. */
    private static void keepTheWay(ServerLevel level, CosmicExpanseSavedData c, long now) {
        BlockPos gate = EdenGarden.eastGate(c.edenCentre);
        for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.edenCentre).inflate(EdenGarden.RADIUS))) {
            if (m.isLeaving() || !EdenGarden.inGarden(c.edenCentre, m.position())) continue;
            BlockPos out = MortalPopulation.safeGround(level, gate.getX() + 8, gate.getZ(), 6);
            if (out != null) m.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
            m.say("The flaming sword turns every way... we may not go back.");
        }
        if (now % 1200 == 11 && level.isLoaded(gate)) station(level, c, gate);
    }

    // ------------------------------------------------------------------ the Creator's part

    /** "Where art thou?" (Gen 3:9): said by a Creator, it calls the hidden pair out to judgment. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String said = event.getRawText().toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        ServerPlayer player = event.getPlayer();
        CainAndAbel.onAsked(player, said);
        com.josephison.fiatlux.flood.Deluge.onChat(player, said);
        if (!said.contains("where art thou") && !said.contains("where are you")) return;
        player.server.execute(() -> {
            CosmicExpanseSavedData c = CosmicExpanseSavedData.get(player.server);
            if (c.eden != EdenState.FALLEN || Divinity.get(player).pilgrim() || level(player.server) != player.level()) return;
            voice = player;
        });
    }

    /** A Creator strikes the serpent: before the fruit is taken, he slinks away (and will return). */
    public static void serpentDrivenOff(ServerLevel level, SerpentEntity serpent, ServerPlayer by) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        if (c.eden != EdenState.TEMPTATION || c.edenStep >= 7) {
            serpent.say(serpent.cursed() ? "Hsss... dust... I eat dust..." : "You cannot unmake what I am.");
            return;
        }
        serpent.say("I will return, in time.");
        level.sendParticles(ParticleTypes.LARGE_SMOKE, serpent.getX(), serpent.getY() + 0.3, serpent.getZ(), 30, 0.6, 0.2, 0.6, 0.02);
        serpent.discard();
        by.displayClientMessage(Component.translatable("message.fiatlux.eden.serpent_driven_off"), true);
        next(c, EdenState.INNOCENCE, level.getGameTime());
    }

    // ------------------------------------------------------------------ helpers

    private static void next(CosmicExpanseSavedData c, EdenState state, long now) {
        c.eden = state;
        c.edenStep = 0;
        c.edenSince = now;
        c.setDirty();
    }

    private static void step(CosmicExpanseSavedData c, long now) {
        c.edenStep++;
        c.edenSince = now;
        c.setDirty();
    }

    /** Runs a line once at the start of a step, then moves on after `ticks`. */
    private static void say(CosmicExpanseSavedData c, long now, long elapsed, int ticks, Runnable line) {
        if (elapsed >= Long.MAX_VALUE / 8) {   // forced: speak and move on at once
            line.run();
            step(c, now);
            return;
        }
        if (elapsed <= 20) line.run();   // the first beat after the step began
        if (elapsed >= ticks) step(c, now);
    }

    /** Walks toward a place; true once there (or, when hurried, after being set down there). */
    private static boolean approach(net.minecraft.world.entity.PathfinderMob who, Entity target, double r, boolean hurry) {
        if (who.distanceTo(target) <= r) return true;
        if (hurry) {
            Vec3 at = target.position().add(target.getLookAngle().multiply(1, 0, 1).normalize().scale(r * 0.7));
            who.teleportTo(at.x, target.getY(), at.z);
            return true;
        }
        if (who.getNavigation().isDone()) who.getNavigation().moveTo(target, 0.7);
        return false;
    }

    private static boolean approach(net.minecraft.world.entity.PathfinderMob who, BlockPos target, double r, boolean hurry) {
        Vec3 t = Vec3.atBottomCenterOf(target);
        if (who.position().distanceTo(t) <= r) return true;
        if (hurry) {
            BlockPos g = MortalPopulation.safeGround((ServerLevel) who.level(), target.getX() - 2, target.getZ() - 2, 4);
            if (g != null) who.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
            return true;
        }
        if (who.getNavigation().isDone()) who.getNavigation().moveTo(t.x, t.y, t.z, 0.65);
        return false;
    }

    private static void pickFruit(ServerLevel level, BlockPos tree) {
        for (BlockPos p : BlockPos.betweenClosed(tree.offset(-6, 4, -6), tree.offset(7, 12, 7))) {
            if (level.getBlockState(p).is(ModBlocks.KNOWLEDGE_FRUIT.get())) {
                level.removeBlock(p, false);
                level.sendParticles(ParticleTypes.CRIT, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                return;
            }
        }
    }

    @Nullable
    public static MortalEntity person(ServerLevel level, CosmicExpanseSavedData c, String name) {
        for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(c.edenCentre).inflate(RANGE))) {
            if (name.equals(m.getName().getString())) return m;
        }
        return null;
    }

    @Nullable
    private static SerpentEntity serpent(ServerLevel level, CosmicExpanseSavedData c) {
        var all = level.getEntitiesOfClass(SerpentEntity.class, new AABB(c.edenCentre).inflate(RANGE));
        return all.isEmpty() ? null : all.getFirst();
    }

    /** Scripture spoken over the garden to those near it. */
    private static void narrate(ServerLevel level, CosmicExpanseSavedData c, String text, String ref) {
        Component line = Component.literal(text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (" + ref + ")").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c.edenCentre).inflate(RANGE * 1.5))) p.sendSystemMessage(line);
    }

    /** A short account of where the story stands, for reports. */
    public static String describe(CosmicExpanseSavedData c) {
        return switch (c.eden) {
            case NONE -> c.fallOfMan ? "No garden stands; mankind is fallen." : "No garden has yet been planted.";
            case INNOCENCE -> c.fallenAngels > 0 ? "Adam and Eve live in innocence. The serpent is in the world."
                    : "Adam and Eve live in innocence. No angel has yet fallen, so there is no tempter.";
            case TEMPTATION -> "The serpent is speaking with the woman.";
            case FALLEN -> "They have eaten of the tree and hide among the trees, waiting for their Creator.";
            case JUDGMENT -> "Their Creator is calling them to account.";
            case EXILED -> "Mankind was driven out; cherubim and a flaming sword keep the way of the tree of life.";
        };
    }
}
