package com.josephison.fiatlux.eden;

import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Cain and Abel (Genesis 4). When Adam and Eve's first sons are grown - "Abel was a keeper of sheep,
 * but Cain was a tiller of the ground" - each brings an offering. Abel's is accepted with fire from
 * heaven; Cain's smoke falls back to the earth, "and Cain was very wroth". Sin lies at the door
 * (4:7): a Creator who blesses Cain now lets him rule over it, and Abel lives. Otherwise Cain talks
 * with his brother in the field and slays him. Asked "Where is Abel thy brother?" (or met by his
 * Creator) he answers "Am I my brother's keeper?"; he is marked, so no one will kill him, and goes
 * out to the land of Nod, east of Eden, and builds a city called Enoch (4:16-17).
 */
public final class CainAndAbel {
    public static final int NONE = 0, OFFERED = 1, SLAIN = 2, MARKED = 3, AVERTED = 4;
    @Nullable private static ServerPlayer asker;

    private CainAndAbel() {}

    public static void tick(MinecraftServer server, boolean force) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        if (!c.fallOfMan || c.cainStep == MARKED || c.cainStep == AVERTED) return;
        ServerLevel level = EdenEvents.level(server);
        if (level == null) return;
        MortalEntity cain = find(level, "Cain"), abel = find(level, "Abel");
        if (cain == null) return;
        long now = level.getGameTime();
        long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        boolean watched = !level.getEntitiesOfClass(ServerPlayer.class, cain.getBoundingBox().inflate(96), p -> !p.isSpectator()).isEmpty();
        switch (c.cainStep) {
            case NONE -> {
                if (abel == null || cain.ageYears() < 18 || abel.ageYears() < 18 || !(watched || force)) return;
                offerings(level, c, cain, abel, now);
            }
            case OFFERED -> {
                if (abel == null) return;
                if ((now - c.cainSince > year && watched) || force) slay(level, c, cain, abel, now);
            }
            case SLAIN -> {
                ServerPlayer near = level.getEntitiesOfClass(ServerPlayer.class, cain.getBoundingBox().inflate(12),
                        p -> !p.isSpectator() && !Divinity.get(p).pilgrim()).stream().findFirst().orElse(null);
                if (near != null || asker != null || force || (now - c.cainSince > 2 * year && watched)) mark(level, c, cain, now);
            }
            default -> {
            }
        }
    }

    /** "And in process of time it came to pass..." (Gen 4:3-7). */
    private static void offerings(ServerLevel level, CosmicExpanseSavedData c, MortalEntity cain, MortalEntity abel, long now) {
        BlockPos altar = cain.settlement() != null ? cain.settlement().shrine : cain.blockPosition();
        for (MortalEntity m : new MortalEntity[]{cain, abel}) {
            m.teleportTo(altar.getX() + (m == cain ? -1.5 : 2.5), altar.getY(), altar.getZ() + 2.5);
            m.beginAct(MortalEntity.PRAY, 160);
        }
        Fx.send(level, FxKind.CELESTIAL_PILLAR, abel.position(), 20f, 0xFFFFE0A0, 40);   // the fire of acceptance
        level.sendParticles(ParticleTypes.LARGE_SMOKE, cain.getX(), cain.getY() + 1.5, cain.getZ(), 40, 0.4, 0.2, 0.4, -0.02);
        narrate(level, cain, "Cain brought of the fruit of the ground an offering unto the LORD. And Abel, he also brought of the firstlings of "
                + "his flock. And the LORD had respect unto Abel and to his offering: but unto Cain and to his offering he had not respect. "
                + "And Cain was very wroth, and his countenance fell.", "Genesis 4:3-5");
        narrate(level, cain, "If thou doest well, shalt thou not be accepted? and if thou doest not well, sin lieth at the door. "
                + "And unto thee shall be his desire, and thou shalt rule over him.", "Genesis 4:7");
        cain.say("Why his? Why always his?");
        abel.say("Brother... it was a good offering. Try again.");
        c.cainStep = OFFERED;
        c.cainSince = now;
        c.setDirty();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(cain) < 96 * 96) p.displayClientMessage(Component.translatable("message.fiatlux.cain.warning"), false);
        }
    }

    /** A Creator lays a hand on Cain before the deed: he rules over his sin, and Abel lives. */
    public static boolean onBlessed(ServerLevel level, MortalEntity m) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        if (c.cainStep != OFFERED || !"Cain".equals(m.getName().getString())) return false;
        c.cainStep = AVERTED;
        c.setDirty();
        m.say("I will master it. He is my brother.");
        CosmosData.get(level.getServer()).record(level.getServer(), "Cain", "averted",
                "heeded the warning: sin lay at the door, and he ruled over it. Abel his brother lived.");
        return true;
    }

    /** "And Cain talked with Abel his brother: and ... Cain rose up against Abel his brother, and slew him" (4:8). */
    private static void slay(ServerLevel level, CosmicExpanseSavedData c, MortalEntity cain, MortalEntity abel, long now) {
        cain.say("Let us go into the field.");
        BlockPos field = cain.settlement() != null && cain.settlement().field != null ? cain.settlement().field : abel.blockPosition();
        BlockPos g = MortalPopulation.safeGround(level, field.getX() + 6, field.getZ() + 6, 6);
        if (g != null) {
            abel.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
            cain.teleportTo(g.getX() + 1.5, g.getY(), g.getZ() + 0.5);
        }
        cain.markAsCain();
        cain.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        abel.hurt(level.damageSources().mobAttack(cain), Float.MAX_VALUE);
        narrate(level, cain, "And it came to pass, when they were in the field, that Cain rose up against Abel his brother, and slew him.", "Genesis 4:8");
        CosmosData.get(level.getServer()).record(level.getServer(), "Cain", "first_murder", "rose up against Abel his brother in the field, and slew him.");
        c.cainStep = SLAIN;
        c.cainSince = now;
        c.setDirty();
    }

    /** "Where is Abel thy brother? ... Am I my brother's keeper?" (4:9-16). */
    private static void mark(ServerLevel level, CosmicExpanseSavedData c, MortalEntity cain, long now) {
        String lord = asker != null ? asker.getGameProfile().getName() : "the LORD";
        narrate(level, cain, "And " + lord + " said unto Cain, Where is Abel thy brother?", "Genesis 4:9");
        cain.say("I know not: Am I my brother's keeper?");
        narrate(level, cain, "What hast thou done? the voice of thy brother's blood crieth unto me from the ground. ... A fugitive and a vagabond "
                + "shalt thou be in the earth. ... And the LORD set a mark upon Cain, lest any finding him should kill him.", "Genesis 4:10-15");
        Fx.send(level, FxKind.HALO_SHATTER, cain.position().add(0, 1.6, 0), 1.5f, 0xFFB02020, 20);
        // "And Cain went out ... and dwelt in the land of Nod, on the east of Eden ... and he builded a city, and called the name of the city
        // ... Enoch" (4:16-17)
        BlockPos east = EdenGarden.eastGate(c.edenCentre).offset(110, 0, 20);
        BlockPos site = MortalPopulation.safeGround(level, east.getX(), east.getZ(), 30);
        if (site != null) {
            Settlement enoch = new Settlement(site);
            enoch.name = "Enoch";
            enoch.founders = "Cain";
            enoch.founded = now;
            Planet world = c.edenWorld == null ? null : c.planets.stream().filter(p -> p.id.equals(c.edenWorld)).findFirst().orElse(null);
            MortalPopulation.build(level, enoch, world != null ? world.era : com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER);
            if (world != null) c.addTown(world.id, enoch);
            cain.leaveFor(site);
        }
        CosmosData.get(level.getServer()).record(level.getServer(), "Cain", "mark_of_cain",
                "was marked and went out from the presence of the LORD to the land of Nod, east of Eden, and built a city, Enoch.");
        asker = null;
        c.cainStep = MARKED;
        c.cainSince = now;
        c.setDirty();
    }

    /** "Where is Abel thy brother?" said by a Creator calls Cain to account. */
    public static void onAsked(ServerPlayer player, String said) {
        if (!said.contains("where is abel")) return;
        player.server.execute(() -> {
            if (CosmicExpanseSavedData.get(player.server).cainStep == SLAIN && !Divinity.get(player).pilgrim()) asker = player;
        });
    }

    @Nullable
    private static MortalEntity find(ServerLevel level, String name) {
        for (MortalEntity m : level.getEntities(ModEntities.MORTAL.get(), x -> name.equals(x.getName().getString()) && x.isAlive())) return m;
        return null;
    }

    private static void narrate(ServerLevel level, MortalEntity near, String text, String ref) {
        Component line = Component.literal(text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (" + ref + ")").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(near.blockPosition()).inflate(144))) p.sendSystemMessage(line);
    }
}
