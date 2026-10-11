package com.josephison.fiatlux.civilization;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What mortals say: lines for every moment of their lives, coloured by temperament, trade, age and
 * the state of the cosmos around them. Lines use {world}, {other}, {spouse}, {child} and {era}.
 */
public final class MortalSpeech {
    public enum Topic {
        GREET_CREATOR, BLESSED, WORK, HUNGRY, TIRED, MORNING, NIGHT, PRAYER, CHAT, REPLY, MIRACLE, TERROR, ANGEL, FALLEN,
        BIRTH, WEDDING, MOURNING, QUARREL, CHILD, ELDER, EAT, IDLE
    }

    /** What the speaker knows: who they are, where they live, and the news of the cosmos. */
    public record Context(String world, String era, Profession profession, Trait a, Trait b, boolean child, boolean elder,
                          String other, String spouse, String child_, boolean rebellion, boolean abyss, int worlds, int fallen,
                          boolean awakened, boolean edenLost) {
        boolean has(Trait t) {
            return a == t || b == t;
        }
    }

    private static final Map<Topic, String[]> COMMON = Map.ofEntries(
            Map.entry(Topic.MORNING, new String[]{"A new day - a gift.", "The light returns, as it was promised.", "Up, up! The day will not wait.",
                    "Morning on {world}. Praise be.", "I dreamt of a ladder reaching to heaven."}),
            Map.entry(Topic.NIGHT, new String[]{"The stars are many tonight.", "Rest now; the night belongs to the Creator.", "Keep the fire burning.",
                    "Who set those lights in the sky, I wonder?", "Sleep well, {other}."}),
            Map.entry(Topic.HUNGRY, new String[]{"My stomach groans...", "Is there bread at the hearth?", "Give us this day our daily bread.",
                    "The store runs thin. We must work harder.", "I would trade a day's work for a loaf."}),
            Map.entry(Topic.TIRED, new String[]{"My bones are weary.", "I long for my bed.", "One more task, then sleep."}),
            Map.entry(Topic.EAT, new String[]{"Good bread, good company.", "Thanks be for this food.", "Mmm. Still warm.", "Share with me, {other}."}),
            Map.entry(Topic.PRAYER, new String[]{"Give us this day our daily bread.", "Forgive us, as we forgive.", "Bless my children, Lord of all.",
                    "Holy, holy, holy...", "Watch over {world} tonight.", "Thank you for the rain.", "Keep us from the darkness below.",
                    "The heavens declare Your glory.", "Hear me, Creator of the stars.", "Make me kinder than I was today."}),
            Map.entry(Topic.GREET_CREATOR, new String[]{"Who... who are you, shining one?", "Is it You? Truly You?", "I am not worthy...",
                    "The light of heaven walks among us!", "Welcome, stranger of light."}),
            Map.entry(Topic.BLESSED, new String[]{"I feel... warm. Whole.", "Praise! Praise!", "My heart is light as a feather!",
                    "I will remember this day all my life.", "Thank you. Thank you."}),
            Map.entry(Topic.MIRACLE, new String[]{"A miracle! Did you see it?", "The heavens have touched {world}!", "Light! Light from nowhere!",
                    "Run and tell the others!", "Glory, glory!"}),
            Map.entry(Topic.TERROR, new String[]{"Run! Run!", "Fire from the sky!", "Mercy, mercy on us!", "Hide the children!",
                    "What have we done to deserve this?"}),
            Map.entry(Topic.ANGEL, new String[]{"An angel! An angel of the Lord!", "Fear not, it said... but I fear!", "Its face is like lightning!",
                    "Kneel, everyone, kneel!"}),
            Map.entry(Topic.FALLEN, new String[]{"A demon! Away, away!", "Its wings... like a bat's!", "Protect us, Creator!"}),
            Map.entry(Topic.BIRTH, new String[]{"A child is born to us!", "Look at the little one!", "Welcome to {world}, little {child}.",
                    "Another soul for the Creator!"}),
            Map.entry(Topic.WEDDING, new String[]{"Today {spouse} and I are joined!", "Two become one.", "Dance! Dance at the wedding!"}),
            Map.entry(Topic.MOURNING, new String[]{"I will miss you, {other}.", "Dust to dust...", "May the Creator receive their soul.",
                    "The hearth feels colder tonight."}),
            Map.entry(Topic.QUARREL, new String[]{"That was mine!", "Mind your own field!", "You always think you know best.",
                    "Hmph. Fine.", "Say that again, I dare you."}),
            Map.entry(Topic.CHILD, new String[]{"Catch me if you can!", "Mama, look!", "Tell me about the stars!", "I'm going to be a {era} hero!",
                    "Is the Creator bigger than the sky?", "Can I have honey?"}),
            Map.entry(Topic.ELDER, new String[]{"In my day the field was half this size.", "Listen to your elders, child.",
                    "I have seen many winters on {world}.", "My knees know when rain is coming.", "Remember who made the stars."}),
            Map.entry(Topic.IDLE, new String[]{"Hm-hm-hmm...", "Fine weather.", "What shall I do today?", "I wonder what lies beyond the hills.",
                    "There's always something to do."}),
            Map.entry(Topic.REPLY, new String[]{"Truly!", "I've heard the same.", "Hah! You may be right.", "I'm not so sure.", "May it be so.",
                    "Aye, the Creator watches over us.", "Hush, don't say such things.", "Tell me more!"})
    );

    private static final Map<Profession, String[]> WORK = Map.ofEntries(
            Map.entry(Profession.GATHERER, new String[]{"Berries, roots, and a little luck.", "The earth gives, if you know where to look.", "These are ripe!"}),
            Map.entry(Profession.HUNTER, new String[]{"Quiet... quiet...", "We will eat well tonight.", "Tracks! Fresh ones."}),
            Map.entry(Profession.FARMER, new String[]{"The wheat is golden - the Creator provides.", "Sow in tears, reap in joy.", "Rain, please, rain.",
                    "Another row and I'm done."}),
            Map.entry(Profession.SHEPHERD, new String[]{"Come along, come along.", "Not one of you shall be lost.", "Easy, little one."}),
            Map.entry(Profession.BUILDER, new String[]{"One more stone for the house of my children.", "Measure twice, cut once.",
                    "A town needs good paths.", "This will stand a hundred years."}),
            Map.entry(Profession.PRIEST, new String[]{"Come, all of you, and pray.", "The altar must be kept clean.", "Blessed are the meek.",
                    "Remember the One who made {world}."}),
            Map.entry(Profession.SCHOLAR, new String[]{"If the stars move, what moves the stars?", "I have counted {worlds} worlds in the sky.",
                    "Every answer hides a question.", "Writing it down, so it is never lost."}),
            Map.entry(Profession.MERCHANT, new String[]{"Fine wares! Fair prices!", "A fair weight is a delight to the Lord.", "Business is good."}),
            Map.entry(Profession.SMITH, new String[]{"Strike while it's hot!", "Ploughshares, not swords.", "The fire tells the iron what to be."}),
            Map.entry(Profession.ENGINEER, new String[]{"The engines hum like a choir.", "Gears within gears, like the wheels of Ezekiel.",
                    "If it works, don't touch it."}),
            Map.entry(Profession.PILOT, new String[]{"Next launch: the {world} moon!", "Up there, you can see the Creator's handiwork.",
                    "Flight check complete."})
    );

    private MortalSpeech() {}

    /** Words of the garden, before the fruit was eaten. */
    public static String innocentLine(RandomSource random, String name) {
        String[] common = {"Everything the Creator made is very good.", "The river is cool, and the fruit is sweet.", "Come, walk with me among the trees!",
                "Not a single thing here is afraid of us.", "The Creator walks with us in the cool of the day.", "Look at the flowers - every colour!"};
        String[] adam = {"This is now bone of my bones, and flesh of my flesh.", "Eve! Come and see what I have named!", "I shall dress the garden and keep it."};
        String[] eve = {"Adam, look at the light on the water!", "Every tree here is good for food... every one but one.", "How the birds sing here!"};
        String[] own = "Adam".equals(name) ? adam : "Eve".equals(name) ? eve : new String[0];
        int n = random.nextInt(common.length + own.length);
        return n < common.length ? common[n] : own[n - common.length];
    }

    public static String say(Topic topic, Context c, RandomSource random) {
        List<String> pool = new ArrayList<>();
        if (topic == Topic.WORK) {
            pool.addAll(List.of(WORK.getOrDefault(c.profession(), COMMON.get(Topic.IDLE))));
        } else if (topic == Topic.CHAT) {
            pool.addAll(chatOpeners(c));
        } else {
            pool.addAll(List.of(COMMON.getOrDefault(topic, COMMON.get(Topic.IDLE))));
        }
        // temperament colours the words
        if (topic == Topic.GREET_CREATOR) {
            if (c.has(Trait.PIOUS)) pool.addAll(List.of("Holy, holy, holy! You have come among us!", "My Lord and my Maker!"));
            if (c.has(Trait.PROUD)) pool.addAll(List.of("You're the one they pray to? Hm.", "I bow to no one... but I will listen."));
            if (c.has(Trait.FEARFUL)) pool.add("D-don't strike me, please!");
            if (c.has(Trait.CURIOUS)) pool.add("Tell me - how did You make the stars?");
            if (c.has(Trait.JOYFUL)) pool.add("Welcome, welcome! Come sit by our fire!");
        }
        if (topic == Topic.PRAYER && c.has(Trait.PROUD)) pool.add("If You're there... prove it.");
        if (topic == Topic.IDLE) {
            if (c.has(Trait.LAZY)) pool.addAll(List.of("Work can wait. The sun is warm.", "*yawn*"));
            if (c.has(Trait.JOYFUL)) pool.addAll(List.of("What a day to be alive!", "La la la..."));
            if (c.has(Trait.GREEDY)) pool.add("If only I had a bigger house...");
            if (c.child()) pool.addAll(List.of(COMMON.get(Topic.CHILD)));
            if (c.elder()) pool.addAll(List.of(COMMON.get(Topic.ELDER)));
        }
        String line = pool.get(random.nextInt(pool.size()));
        return line.replace("{world}", c.world()).replace("{other}", c.other()).replace("{spouse}", c.spouse())
                .replace("{child}", c.child_()).replace("{era}", c.era()).replace("{worlds}", Integer.toString(c.worlds()));
    }

    /** Conversation openers, including the news of the cosmos as mortals hear it. */
    private static List<String> chatOpeners(Context c) {
        List<String> l = new ArrayList<>(List.of("Have you seen the lights over the shrine?", "How is your family, {other}?",
                "The harvest looks good this year.", "Did you hear {other}'s child singing?", "I saw a falling star last night.",
                "Shall we pray together tonight?", "They say the Creator walks among us sometimes."));
        if (c.rebellion()) l.addAll(List.of("They say there is war in the heavens...", "The sky trembles. The angels are fighting!"));
        if (c.fallen() > 0) l.add("The old ones say some angels fell, far below, into the dark.");
        if (c.abyss()) l.add("The scholars speak of a pit in the sky that swallows light.");
        if (c.worlds() > 1) l.add("Do you think there are people on the other worlds?");
        if (c.has(Trait.CURIOUS)) l.add("Why do you think the sun never falls?");
        if (c.awakened()) {   // knowing good and evil (Gen 3:22), they wonder who they are
            l.addAll(List.of("Why were we made, do you think?", "What becomes of us when we die?", "I know good, and I know evil. Which am I?",
                    "We are dust, and to dust we shall return.", "Do you ever feel... watched over?", "Who made the One who made us?",
                    "Sometimes I feel I was made for more than this."));
        }
        if (c.edenLost()) {
            l.addAll(List.of("My grandmother told of a garden, before the beginning of sorrows.", "They say a flaming sword guards the way to the tree of life.",
                    "If our first parents had never eaten of that tree... would we live for ever?"));
        }
        if (c.has(Trait.GREEDY)) l.add("Your field is bigger than mine. Hardly fair.");
        return l;
    }
}
