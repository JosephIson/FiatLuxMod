package com.josephison.fiatlux.infernal;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The hierarchy of the fallen, as Scripture and tradition describe it.
 *
 * <p>At its head is Satan, "that old serpent, called the Devil" (Rev 12:9), once Lucifer, "son of the
 * morning" (Isa 14:12). Beneath him stand the princes of hell, each over a deadly sin after Peter
 * Binsfeld's classification (1589) - Beelzebub "the prince of the devils" (Matt 12:24), Mammon
 * (Matt 6:24), Asmodeus (Tobit 3:8), Leviathan (Isa 27:1), Amon, Belphegor (Baal-peor, Num 25:3) - with
 * Abaddon, "the angel of the bottomless pit" (Rev 9:11). Then come the seventy-two spirits of the Ars
 * Goetia (the Lesser Key of Solomon), each with its rank (king, duke, prince, marquis, president,
 * earl, knight), the legions it commands, the form it takes and its office. Beneath them all are the
 * legions themselves: the unclean spirits of the Gospels, who enter men (Mark 5:9, Luke 11:24-26).
 *
 * <p>Paul's order of the powers of darkness (Eph 6:12) gives the tiers: principalities (kings and
 * princes), powers (dukes and marquises), the rulers of the darkness of this world (presidents, earls
 * and knights), and spiritual wickedness in high places (the unclean spirits of their legions).
 */
public final class InfernalHierarchy {
    public enum Rank {
        DEVIL("the Devil"), PRINCE_OF_HELL("Prince of Hell"), ANGEL_OF_THE_ABYSS("Angel of the Bottomless Pit"),
        KING("King"), DUKE("Duke"), PRINCE("Prince"), MARQUIS("Marquis"), PRESIDENT("President"), EARL("Earl"), KNIGHT("Knight");
        public final String title;

        Rank(String title) {
            this.title = title;
        }

        /** The tier of Ephesians 6:12. */
        public Tier tier() {
            return switch (this) {
                case DEVIL, PRINCE_OF_HELL, ANGEL_OF_THE_ABYSS, KING, PRINCE -> Tier.PRINCIPALITIES;
                case DUKE, MARQUIS -> Tier.POWERS;
                default -> Tier.RULERS_OF_DARKNESS;
            };
        }
    }

    /** "For we wrestle not against flesh and blood, but against ..." (Ephesians 6:12). */
    public enum Tier {
        PRINCIPALITIES("principalities"), POWERS("powers"), RULERS_OF_DARKNESS("the rulers of the darkness of this world"),
        SPIRITUAL_WICKEDNESS("spiritual wickedness in high places");
        public final String phrase;

        Tier(String phrase) {
            this.phrase = phrase;
        }
    }

    /** What the spirits of a lord's legions do to the one they possess. */
    public enum Affliction {
        THEFT("they steal from the common store"), DISCORD("they sow quarrels"), VIOLENCE("they strike out at others"),
        SICKNESS("they sicken their host"), SLOTH("they will not work"), FIRE("they set fires"), DECEIT("they lie and lead others astray"),
        MUTENESS("they strike their host deaf and dumb"), DROWNING("they cast their host into water"), DESPAIR("they whisper despair");
        public final String does;

        Affliction(String does) {
            this.does = does;
        }
    }

    /**
     * One of the lords of the fallen.
     *
     * @param number  its number in the Ars Goetia (0 for those not among the seventy-two)
     * @param legions the legions it commands
     * @param sin     the deadly sin a prince of hell is set over ("" for others)
     */
    public record Lord(int number, String name, Rank rank, @Nullable Rank secondRank, int legions, String appears, String office,
                       Affliction affliction, String sin, String source) {
        public String title() {
            return switch (rank) {
                case DEVIL -> name + ", the Devil";
                case PRINCE_OF_HELL -> name + ", Prince of Hell";
                case ANGEL_OF_THE_ABYSS -> name + ", Angel of the Bottomless Pit";
                default -> rank.title + " " + name;
            };
        }

        public String fullRank() {
            return secondRank == null ? rank.title : rank.title + " and " + secondRank.title;
        }
    }

    public static final List<Lord> LORDS_IN_ORDER;
    /** How many lords there are in all. */
    public static final int LORDS;

    private static Lord lord(int n, String name, Rank r, Rank r2, int legions, String appears, String office, Affliction a, String sin, String src) {
        return new Lord(n, name, r, r2, legions, appears, office, a, sin, src);
    }

    private static final Affliction TH = Affliction.THEFT, DI = Affliction.DISCORD, VI = Affliction.VIOLENCE, SI = Affliction.SICKNESS,
            SL = Affliction.SLOTH, FI = Affliction.FIRE, DE = Affliction.DECEIT, MU = Affliction.MUTENESS, DR = Affliction.DROWNING,
            DS = Affliction.DESPAIR;
    private static final Rank K = Rank.KING, D = Rank.DUKE, P = Rank.PRINCE, M = Rank.MARQUIS, PR = Rank.PRESIDENT, E = Rank.EARL, KN = Rank.KNIGHT;

    static {
        List<Lord> l = new ArrayList<>();
        // the Devil and the princes of hell
        l.add(lord(0, "Satan", Rank.DEVIL, null, 0, "the great dragon, that old serpent",
                "the accuser of the brethren, who deceiveth the whole world", DE, "pride", "Rev 12:9-10; Isa 14:12-15"));
        l.add(lord(0, "Beelzebub", Rank.PRINCE_OF_HELL, null, 0, "the lord of the flies", "prince of the devils", SI, "gluttony", "Matt 12:24; 2 Kings 1:2"));
        l.add(lord(0, "Mammon", Rank.PRINCE_OF_HELL, null, 0, "a hoarder bent over his gold", "the master no man can serve beside God", TH, "greed", "Matt 6:24"));
        l.add(lord(32, "Asmodeus", Rank.PRINCE_OF_HELL, K, 72, "three heads, of a bull, a man and a ram, riding an infernal dragon",
                "the evil spirit of Tobit; teaches arithmetic and astronomy, and finds hidden treasure", DI, "lust", "Tobit 3:8; Ars Goetia 32"));
        l.add(lord(0, "Leviathan", Rank.PRINCE_OF_HELL, null, 0, "the piercing serpent, the dragon that is in the sea",
                "king over all the children of pride", DR, "envy", "Isa 27:1; Job 41"));
        l.add(lord(7, "Amon", Rank.PRINCE_OF_HELL, M, 40, "a wolf with a serpent's tail, vomiting flame",
                "tells of things past and to come, and reconciles friends and foes", VI, "wrath", "Ars Goetia 7"));
        l.add(lord(0, "Belphegor", Rank.PRINCE_OF_HELL, null, 0, "Baal of Peor", "the idol Israel joined itself to", SL, "sloth", "Num 25:3"));
        l.add(lord(0, "Abaddon", Rank.ANGEL_OF_THE_ABYSS, null, 0, "a king over locusts out of the smoke of the pit",
                "the destroyer, called Apollyon in the Greek", DS, "", "Rev 9:1-11"));
        // the seventy-two spirits of the Ars Goetia
        Object[][] g = {
                {1, "Bael", K, null, 66, "a cat, a toad or a man, or all at once", "makes men invisible", DE},
                {2, "Agares", D, null, 31, "an old man riding a crocodile, with a goshawk on his fist", "teaches all tongues and causes earthquakes", DS},
                {3, "Vassago", P, null, 26, "as Agares", "declares things past and to come, and finds what is lost", DE},
                {4, "Samigina", M, null, 30, "a little horse or ass", "teaches the liberal sciences and gives account of the souls who died in sin", DS},
                {5, "Marbas", PR, null, 36, "a great lion", "answers truly of hidden things, and causes and cures diseases", SI},
                {6, "Valefor", D, null, 10, "a lion with an ass's head", "tempts men to steal", TH},
                {8, "Barbatos", D, null, 30, "an archer with four kings and their companies", "gives understanding of the songs of birds and the voices of beasts", DE},
                {9, "Paimon", K, null, 200, "a crowned man on a dromedary, with trumpets before him", "teaches all arts and sciences and secret things", DE},
                {10, "Buer", PR, null, 50, "a wheel of five lion's legs", "teaches philosophy and heals infirmities", SI},
                {11, "Gusion", D, null, 40, "a xenophilus", "tells all things past, present and to come", DE},
                {12, "Sitri", P, null, 60, "a leopard's head and a griffin's wings", "inflames men with desire", DI},
                {13, "Beleth", K, null, 85, "a king on a pale horse, with trumpets before him", "causes all the love that may be", DI},
                {14, "Leraje", M, null, 30, "an archer clad in green", "causes great battles and makes arrow-wounds putrefy", VI},
                {15, "Eligos", D, null, 60, "a goodly knight with lance, ensign and serpent", "discovers hidden things and knows of wars", VI},
                {16, "Zepar", D, null, 26, "a soldier in red apparel and armour", "makes women love men, and makes them barren", DI},
                {17, "Botis", PR, E, 60, "an ugly viper", "tells of things past and to come, and reconciles friends and foes", DI},
                {18, "Bathin", D, null, 30, "a strong man with a serpent's tail, on a pale horse", "knows the virtues of herbs and stones and carries men away", DS},
                {19, "Sallos", D, null, 30, "a gallant soldier riding a crocodile", "causes the love of women to men", DI},
                {20, "Purson", K, null, 22, "a man with a lion's face carrying a viper, riding a bear", "knows hidden things and finds treasure", DE},
                {21, "Marax", E, PR, 30, "a great bull with a man's face", "teaches astronomy and the virtues of herbs", SL},
                {22, "Ipos", E, P, 36, "an angel with a lion's head, goose's feet and a hare's tail", "makes men witty and bold", DE},
                {23, "Aim", D, null, 26, "three heads, of a serpent, a man and a calf, riding a viper, bearing a firebrand", "sets cities, castles and great places on fire", FI},
                {24, "Naberius", M, null, 19, "a black crane", "makes men cunning in all arts", DE},
                {25, "Glasya-Labolas", PR, E, 36, "a dog with a griffin's wings", "the author of bloodshed and manslaughter", VI},
                {26, "Bune", D, null, 30, "a dragon with three heads, of a dog, a griffin and a man", "changes the places of the dead", DS},
                {27, "Ronove", M, E, 19, "a monster", "teaches rhetoric and tongues", DE},
                {28, "Berith", D, null, 26, "a soldier in red on a red horse, with a crown of gold", "turns all metals into gold, and is a great liar", DE},
                {29, "Astaroth", D, null, 40, "a hurtful angel riding an infernal dragon, a viper in his right hand", "knows all things past and to come, and of the fall of the spirits", DS},
                {30, "Forneus", M, null, 29, "a great sea-monster", "teaches rhetoric and tongues and makes men loved by their foes", DR},
                {31, "Foras", PR, null, 29, "a strong man", "teaches the virtues of herbs and stones, logic and ethics", SL},
                {33, "Gaap", PR, P, 66, "a man with great bat's wings", "makes men insensible and ignorant", MU},
                {34, "Furfur", E, null, 26, "a hart with a fiery tail", "raises thunder, lightning and great winds, and never speaks truth unless compelled", FI},
                {35, "Marchosias", M, null, 30, "a wolf with a griffin's wings and a serpent's tail, breathing fire", "a strong fighter; he hoped to return to the seventh throne", VI},
                {36, "Stolas", P, null, 26, "a raven", "teaches astronomy and the virtues of herbs and precious stones", SL},
                {37, "Phenex", M, null, 20, "a phoenix with the voice of a child", "an excellent poet; he hoped to return to the seventh throne", DS},
                {38, "Halphas", E, null, 26, "a stock-dove", "builds towers, furnishes them with weapons, and sends men to war", VI},
                {39, "Malphas", PR, null, 40, "a crow", "builds houses and high towers, and casts down the walls of enemies", DI},
                {40, "Raum", E, null, 30, "a crow", "steals treasure out of kings' houses and destroys cities", TH},
                {41, "Focalor", D, null, 30, "a man with a griffin's wings", "drowns men and overthrows ships of war", DR},
                {42, "Vepar", D, null, 29, "a mermaid", "governs the waters, raises storms at sea, and makes wounds putrefy", DR},
                {43, "Sabnock", M, null, 50, "an armed soldier with a lion's head on a pale horse", "builds towers and afflicts men with wounds", VI},
                {44, "Shax", M, null, 30, "a stock-dove with a hoarse voice", "takes away the sight, hearing and understanding of any man, and steals money", MU},
                {45, "Vine", K, E, 36, "a lion riding a black horse, bearing a viper", "builds towers, throws down walls and makes the waters rough", FI},
                {46, "Bifrons", E, null, 6, "a monster", "removes the dead and lights seeming candles on their graves", DS},
                {47, "Vual", D, null, 37, "a great dromedary", "procures the love of women and friendship between friends and foes", DI},
                {48, "Haagenti", PR, null, 33, "a mighty bull with a griffin's wings", "turns metals into gold and water into wine", DE},
                {49, "Crocell", D, null, 48, "an angel", "makes the noise of rushing waters, and warms waters", DR},
                {50, "Furcas", KN, null, 20, "a cruel old man with a long beard, on a pale horse, bearing a sharp weapon", "teaches philosophy, logic and pyromancy", FI},
                {51, "Balam", K, null, 40, "three heads, of a bull, a man and a ram, riding a bear", "tells all things past and to come, and makes men invisible", DE},
                {52, "Alloces", D, null, 36, "a soldier on a great horse, with a lion's face and flaming eyes", "teaches astronomy and the liberal sciences", VI},
                {53, "Camio", PR, null, 30, "a thrush, then a man bearing a sharp sword", "gives understanding of the voices of birds, beasts and waters", MU},
                {54, "Murmur", D, E, 30, "a warrior riding a griffin, crowned", "makes the souls of the dead appear and answer", DS},
                {55, "Orobas", P, null, 20, "a horse", "tells true answers of things past and to come", DE},
                {56, "Gremory", D, null, 26, "a beautiful woman crowned as a duchess, riding a camel", "tells of hidden treasures and procures the love of women", DI},
                {57, "Ose", PR, null, 3, "a leopard", "makes men believe they are changed into another shape", DE},
                {58, "Amy", PR, null, 36, "a flaming fire", "teaches astrology and gives good familiars", FI},
                {59, "Orias", M, null, 30, "a lion with a serpent's tail, riding a horse, holding two hissing serpents", "teaches the virtues of the stars and transforms men", DE},
                {60, "Vapula", D, null, 36, "a lion with a griffin's wings", "makes men knowing in handicrafts and philosophy", SL},
                {61, "Zagan", K, PR, 33, "a bull with a griffin's wings", "turns wine into water and blood into wine, and makes fools wise", DE},
                {62, "Volac", PR, null, 38, "a child with angel's wings riding a two-headed dragon", "tells where serpents may be found", DE},
                {63, "Andras", M, null, 30, "an angel with a raven's head riding a black wolf, with a sharp sword", "sows discord, and may kill those who call him", DI},
                {64, "Haures", D, null, 36, "a terrible leopard", "destroys enemies by fire, and speaks of the creation and the fall of spirits", FI},
                {65, "Andrealphus", M, null, 30, "a peacock", "teaches geometry and measurement", SL},
                {66, "Cimejes", M, null, 20, "a valiant warrior riding a black horse", "finds lost things and hidden treasure", TH},
                {67, "Amdusias", D, null, 29, "a unicorn", "causes music to be heard and trees to bend", DS},
                {68, "Belial", K, null, 50, "two beautiful angels in a chariot of fire", "distributes preferments; created next after Lucifer", DE},
                {69, "Decarabia", M, null, 30, "a star in a pentacle", "knows the virtues of birds and stones", DE},
                {70, "Seere", P, null, 26, "a beautiful man on a winged horse", "brings all things to pass suddenly and carries them away", TH},
                {71, "Dantalion", D, null, 36, "a man with many faces of men and women, a book in his right hand", "knows the thoughts of all men and women, and can change them", DE},
                {72, "Andromalius", E, null, 36, "a man holding a great serpent", "brings back thieves and stolen goods, and discovers wickedness", TH},
        };
        for (Object[] r : g) {
            l.add(lord((Integer) r[0], (String) r[1], (Rank) r[2], (Rank) r[3], (Integer) r[4], (String) r[5], (String) r[6],
                    (Affliction) r[7], "", "Ars Goetia " + r[0]));
        }
        LORDS_IN_ORDER = Collections.unmodifiableList(l);
        LORDS = l.size();
    }

    private InfernalHierarchy() {}

    /** The first n lords risen in the Underworld (the Devil and his princes first, then the Goetia in order). */
    public static List<Lord> risen(int n) {
        return LORDS_IN_ORDER.subList(0, Math.max(0, Math.min(n, LORDS)));
    }

    @Nullable
    public static Lord byName(String name) {
        for (Lord l : LORDS_IN_ORDER) if (l.name().equalsIgnoreCase(name)) return l;
        return null;
    }

    /** A lord of the Goetia among the risen who sends out his legions (never the Devil himself). */
    @Nullable
    public static Lord sender(int risen, RandomSource random) {
        List<Lord> r = risen(risen);
        if (r.size() <= 1) return null;
        return r.get(1 + random.nextInt(r.size() - 1));
    }

    /** Legions commanded by all the risen lords together (a legion being six thousand, after the Roman legion of Mark 5:9). */
    public static long legions(int risen) {
        long n = 0;
        for (Lord l : risen(risen)) n += l.legions();
        return n;
    }

    /** Names the unclean spirits give themselves when asked, as Legion did (Mark 5:9). */
    private static final String[] SPIRIT_NAMES = {"Legion", "Infirmity", "Divination", "Dumb", "Unclean", "Wormwood", "Gnawer", "Whisperer",
            "Night-hag", "Ruin", "Liar", "Lurker", "Stumbling", "Envy", "Dread", "Thief"};

    public static String spiritName(RandomSource random) {
        return SPIRIT_NAMES[random.nextInt(SPIRIT_NAMES.length)];
    }
}
