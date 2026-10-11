package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.entity.SoulEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The dead in their realms. Every soul judged is written in the Book of Generations; while a Creator
 * walks Heaven or the Underworld, the souls sent there are brought forth around them - first the
 * mortals who lived and died before one's eyes, then, standing for the multitudes no one counted,
 * souls of the unseen dead of every living world ("a great multitude, which no man could number",
 * Rev 7:9). When a mortal dies while someone is in its realm, its soul arrives there at once.
 */
public final class SoulRealms {
    private static final int INTERVAL = 100;
    private static final int HEAVEN_NEAR = 14, UNDERWORLD_NEAR = 12, MAX_LOADED = 90;

    private SoulRealms() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 37) return;
        populate(server, server.getLevel(FiatLuxDimensions.HEAVEN), true);
        populate(server, server.getLevel(FiatLuxDimensions.UNDERWORLD), false);
    }

    private static void populate(MinecraftServer server, @Nullable ServerLevel realm, boolean heaven) {
        if (realm == null || realm.players().isEmpty()) return;
        Generations gens = Generations.get(server);
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        String fate = heaven ? "heaven" : "underworld";
        int loaded = 0;
        for (var e : realm.getAllEntities()) if (e instanceof SoulEntity) loaded++;
        for (ServerPlayer player : realm.players()) {
            if (loaded >= MAX_LOADED) return;
            int near = realm.getEntitiesOfClass(SoulEntity.class, player.getBoundingBox().inflate(48)).size();
            int want = heaven ? HEAVEN_NEAR : UNDERWORLD_NEAR;
            for (int n = 0; n < Math.min(2, want - near); n++) {
                // first the dead who are written in the Book, most recent first
                Person person = null;
                for (Person p : gens.people.values()) {
                    if (!p.living() && fate.equals(p.soul) && !p.embodied && (person == null || p.died > person.died)) person = p;
                }
                long recorded = gens.people.values().stream().filter(p -> fate.equals(p.soul)).count();
                double multitude = heaven ? cosmos.heavenSouls : cosmos.underworld.souls;
                if (person == null && multitude <= recorded) break;   // every soul there is already walking
                BlockPos at = spot(realm, player.blockPosition(), realm.random);
                if (at == null) break;
                if (person != null) {
                    spawn(realm, person, person.name, person.male, person.world, heaven, at, false);
                } else {
                    boolean male = realm.random.nextBoolean();
                    spawn(realm, null, MortalNames.pick(realm.random, male), male, someWorld(cosmos, realm.random), heaven, at, false);
                }
                loaded++;
            }
        }
    }

    /** A mortal has just died: if anyone is in its realm, its soul arrives there now. */
    public static void onDeath(MinecraftServer server, Person p, boolean heaven) {
        ServerLevel realm = server.getLevel(heaven ? FiatLuxDimensions.HEAVEN : FiatLuxDimensions.UNDERWORLD);
        if (realm == null || realm.players().isEmpty()) return;
        ServerPlayer witness = realm.players().get(realm.random.nextInt(realm.players().size()));
        BlockPos at = spot(realm, witness.blockPosition(), realm.random);
        if (at != null) spawn(realm, p, p.name, p.male, p.world, heaven, at, true);
    }

    @Nullable
    public static SoulEntity spawn(ServerLevel realm, @Nullable Person p, String name, boolean male, String world, boolean heaven,
                                   BlockPos at, boolean arriving) {
        SoulEntity soul = ModEntities.SOUL.get().create(realm);
        if (soul == null) return null;
        soul.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, realm.random.nextFloat() * 360f, 0);
        soul.become(p, name, male, world, heaven);
        realm.addFreshEntity(soul);
        if (p != null) {
            p.embodied = true;
            Generations.get(realm.getServer()).setDirty();
        }
        if (arriving) {
            if (heaven) {
                Fx.send(realm, FxKind.CELESTIAL_PILLAR, soul.position(), 18f, 0xFFFFF0C0, 30);
                soul.say(p != null ? "I am home. I am home!" : "Is this... Heaven?");
            } else {
                Fx.send(realm, FxKind.FALLEN_IMPACT, soul.position(), 3f, 0xFFB03020, 20);
                soul.say("No... no! Not here!");
            }
            for (ServerPlayer pl : realm.players()) {
                if (pl.distanceToSqr(soul) < 64 * 64) {
                    pl.displayClientMessage(Component.translatable(heaven ? "message.fiatlux.soul.arrives_heaven" : "message.fiatlux.soul.arrives_underworld",
                            name, world), true);
                }
            }
        }
        return soul;
    }

    /** Standing ground near a place in a realm. */
    @Nullable
    private static BlockPos spot(ServerLevel realm, BlockPos near, RandomSource r) {
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 20;
            BlockPos g = MortalPopulation.safeGround(realm, near.getX() + (int) (Math.cos(a) * d), near.getZ() + (int) (Math.sin(a) * d), 6);
            if (g != null && Math.abs(g.getY() - near.getY()) < 24) return g;
        }
        return null;
    }

    private static String someWorld(CosmicExpanseSavedData cosmos, RandomSource r) {
        List<String> living = new ArrayList<>();
        for (Planet p : cosmos.planets) if (p.hasLife()) living.add(p.name);
        return living.isEmpty() ? "the earth" : living.get(r.nextInt(living.size()));
    }

    // ------------------------------------------------------------------ redemption

    /** A Creator raises a soul out of the Underworld into Heaven. */
    public static void redeem(ServerPlayer creator, SoulEntity soul) {
        String name = soul.getName().getString();
        redeem(creator.server, creator.getGameProfile().getName(), soul);
        creator.displayClientMessage(Component.translatable("message.fiatlux.soul.redeemed", name), true);
    }

    public static void redeem(MinecraftServer server, String by, SoulEntity soul) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        Person p = soul.person() == null ? null : Generations.get(server).person(soul.person());
        String name = soul.getName().getString();
        Fx.send((ServerLevel) soul.level(), FxKind.CELESTIAL_PILLAR, soul.position(), 30f, 0xFFFFF0C0, 40);
        soul.say("I am lifted up... I am forgiven!");
        cosmos.underworld.souls = Math.max(0, cosmos.underworld.souls - 1);
        cosmos.heavenSouls += 1;
        cosmos.setDirty();
        String world = p != null ? p.world : "the unnumbered dead";
        boolean male = soul.isMale();
        soul.discard();
        if (p != null) {
            p.soul = "heaven";
            p.redeemed = true;
            Generations.get(server).setDirty();
        }
        ServerLevel heaven = server.getLevel(FiatLuxDimensions.HEAVEN);
        if (heaven != null && !heaven.players().isEmpty()) {
            ServerPlayer witness = heaven.players().getFirst();
            BlockPos at = spot(heaven, witness.blockPosition(), heaven.random);
            if (at != null) spawn(heaven, p, name, male, world, true, at, true);
        }
        CosmosData.get(server).record(server, by, "redemption", "redeemed the soul of " + name + " from the Underworld and raised it into Heaven.");
    }

    // ------------------------------------------------------------------ what they say

    private static final String[] HEAVEN = {
            "God shall wipe away all tears from their eyes.", "There is no more death here, nor sorrow, nor crying.",
            "Holy, holy, holy!", "I was a stranger on {world}, and here I am at home.", "The light here has no shadow.",
            "Worthy is the One who made us!", "I see my mother! She is here!", "Every tear I wept on {world} - gone.",
            "Blessed are they that mourn, for they shall be comforted.", "I wish they could see this, back on {world}.",
            "The streets... they are gold, and clear as glass.", "Glory, glory, glory!", "I am {name}. I lived on {world}. I live still."};
    private static final String[] UNDERWORLD = {
            "Water... a drop of water for my tongue...", "Let me go back - let me warn my brothers!", "It burns... it burns...",
            "Why did I not listen?", "I had everything on {world}. Now nothing.", "Is there no end to this?",
            "I remember the shrine on {world}. I never went.", "Mercy... is there any mercy?", "Weeping, and gnashing of teeth...",
            "Do not look at me.", "They took my name. I am {name}... I think.", "Dig. Carry. Dig. Carry. Forever.",
            "If someone would only pray for me..."};

    public static String heavenLine(RandomSource r, String world, String name) {
        return HEAVEN[r.nextInt(HEAVEN.length)].replace("{world}", world.isEmpty() ? "earth" : world).replace("{name}", name);
    }

    public static String underworldLine(RandomSource r, String world, String name) {
        return UNDERWORLD[r.nextInt(UNDERWORLD.length)].replace("{world}", world.isEmpty() ? "earth" : world).replace("{name}", name);
    }

    /** Who this soul's kin were, and where they are now. */
    public static String kin(MinecraftServer server, Person p) {
        Generations g = Generations.get(server);
        List<String> parts = new ArrayList<>();
        Person father = g.person(p.father), mother = g.person(p.mother), spouse = g.person(p.spouse);
        if (father != null || mother != null) {
            parts.add("child of " + (father != null ? father.name + where(father) : "") + (father != null && mother != null ? " and " : "")
                    + (mother != null ? mother.name + where(mother) : ""));
        }
        if (spouse != null) parts.add("wed to " + spouse.name + where(spouse));
        List<Person> children = g.childrenOf(p.id);
        if (!children.isEmpty()) {
            parts.add("parent of " + String.join(", ", children.stream().limit(5).map(c -> c.name + where(c)).toList()));
        }
        return String.join(" · ", parts);
    }

    private static String where(Person p) {
        return p.living() ? " (living)" : "heaven".equals(p.soul) ? " (in Heaven)" : "underworld".equals(p.soul) ? " (in the Underworld)" : "";
    }

    /** Souls near a place (for tests and reports). */
    public static int soulsNear(ServerLevel realm, Vec3 at, double r) {
        return realm.getEntitiesOfClass(SoulEntity.class, new AABB(at, at).inflate(r)).size();
    }
}
