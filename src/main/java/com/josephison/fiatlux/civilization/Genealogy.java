package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The generations of Genesis 4, 5 and 11, kept as they are born: the eldest son of each line is given
 * the next name in it. From Adam's son Seth: Enos, Cainan, Mahalaleel, Jared, Enoch - who "walked with
 * God: and he was not; for God took him" (5:24) - Methuselah, Lamech and Noah, whose sons are Shem,
 * Ham and Japheth (5:32); from Shem: Arphaxad, Salah, Eber, Peleg, Reu, Serug, Nahor, Terah and Abram
 * (11:10-26). And from Cain: Enoch, Irad, Mehujael, Methusael and Lamech, whose sons were Jabal, "the
 * father of such as dwell in tents", Jubal, "of all such as handle the harp and organ", and Tubal-cain,
 * "an instructer of every artificer in brass and iron" (4:17-22).
 */
public final class Genealogy {
    public static final String SETH = "seth", SHEM = "shem", CAIN = "cain";
    private static final String[] OF_SETH = {"Adam", "Seth", "Enos", "Cainan", "Mahalaleel", "Jared", "Enoch", "Methuselah", "Lamech", "Noah"};
    private static final String[] OF_SHEM = {"Noah", "Shem", "Arphaxad", "Salah", "Eber", "Peleg", "Reu", "Serug", "Nahor", "Terah", "Abram"};
    private static final String[] OF_CAIN = {"Cain", "Enoch", "Irad", "Mehujael", "Methusael", "Lamech"};
    private static final String[] NOAHS_SONS = {"Shem", "Ham", "Japheth"};
    private static final String[] LAMECHS_SONS = {"Jabal", "Jubal", "Tubal-cain"};
    private static final String[] LAMECHS_SONS_WORK = {"the father of such as dwell in tents, and of such as have cattle",
            "the father of all such as handle the harp and organ", "an instructer of every artificer in brass and iron"};

    public record Heir(String name, String line) {}

    private Genealogy() {}

    /** The name the father's next son is to bear, if he carries one of the lines and it goes on through this son. */
    @Nullable
    public static Heir nextSon(MinecraftServer server, MortalEntity father) {
        Generations g = Generations.get(server);
        Person dad = g.person(father.getUUID());
        if (dad == null || dad.line.isEmpty()) return null;
        int sons = (int) g.childrenOf(dad.id).stream().filter(p -> p.male && father.getUUID().equals(p.father)).count();
        String name = dad.name;
        switch (dad.line) {
            case SETH -> {
                if (name.equals("Noah") && sons < 3) return new Heir(NOAHS_SONS[sons], sons == 0 ? SHEM : "");
                int i = Arrays.asList(OF_SETH).indexOf(name);
                if (i >= 1 && i < OF_SETH.length - 1 && sons == 0) return new Heir(OF_SETH[i + 1], SETH);
            }
            case SHEM -> {
                int i = Arrays.asList(OF_SHEM).indexOf(name);
                if (i >= 1 && i < OF_SHEM.length - 1 && sons == 0) return new Heir(OF_SHEM[i + 1], SHEM);
            }
            case CAIN -> {
                if (name.equals("Lamech") && sons < 3) return new Heir(LAMECHS_SONS[sons], "");
                int i = Arrays.asList(OF_CAIN).indexOf(name);
                if (i >= 0 && i < OF_CAIN.length - 1 && sons == 0) return new Heir(OF_CAIN[i + 1], CAIN);
            }
            default -> {
            }
        }
        return null;
    }

    /** Writes a newborn into its line: Adam's sons Seth and Cain begin theirs; heirs carry them on. */
    public static void enter(MinecraftServer server, MortalEntity child, @Nullable MortalEntity father, @Nullable Heir heir) {
        Person p = Generations.get(server).person(child.getUUID());
        if (p == null) return;
        String name = p.name;
        boolean ofAdam = father != null && "Adam".equals(father.getName().getString());
        if (ofAdam && name.equals("Seth")) p.line = SETH;
        else if (ofAdam && name.equals("Cain")) p.line = CAIN;
        else if (heir != null) p.line = heir.line();
        else return;
        Generations.get(server).setDirty();
        if (heir != null) {
            int i = Arrays.asList(LAMECHS_SONS).indexOf(name);
            String what = i >= 0 ? "was born, son of Lamech of the line of Cain: " + LAMECHS_SONS_WORK[i] + "."
                    : "was born, son of " + (father != null ? father.getName().getString() : "his father") + ", of the line of "
                    + (p.line.equals(CAIN) ? "Cain" : p.line.equals(SHEM) ? "Shem" : "Seth") + ".";
            CosmosData.get(server).record(server, name, "generations", what);
        }
    }

    /**
     * "And Enoch walked with God: and he was not; for God took him" (Gen 5:24). Once Enoch of the line of
     * Seth has begotten his son and walked long with God, he is taken up living into Heaven.
     */
    public static boolean walkWithGod(ServerLevel level, MortalEntity m) {
        if (!"Enoch".equals(m.getName().getString()) || m.ageYears() < 40 || m.isRemoved()) return false;
        Generations g = Generations.get(level.getServer());
        Person p = g.person(m.getUUID());
        if (p == null || !SETH.equals(p.line) || g.childrenOf(p.id).isEmpty() || level.random.nextDouble() > 0.02) return false;
        p.died = level.getGameTime();
        p.soul = "heaven";
        p.translated = true;
        p.ageAtDeath = (int) m.ageYears();
        g.setDirty();
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        c.heavenSouls += 1;
        c.setDirty();
        Fx.send(level, FxKind.CELESTIAL_PILLAR, m.position(), 40f, 0xFFFFF4D6, 60);
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), m.getX(), m.getY() + 1, m.getZ(), 80, 0.4, 1.6, 0.4, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1, m.getZ(), 40, 0.3, 2.0, 0.3, 0.1);
        Component line = Component.literal("And Enoch walked with God: and he was not; for God took him.").withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (Genesis 5:24)").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer pl : level.players()) if (pl.distanceToSqr(m) < 200 * 200) pl.sendSystemMessage(line);
        CosmosData.get(level.getServer()).record(level.getServer(), "Enoch", "translated",
                "walked with God: and he was not; for God took him. He was taken up living into Heaven, and did not see death.");
        SoulRealms.onDeath(level.getServer(), p, true);
        m.discard();
        return true;
    }
}
