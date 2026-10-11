package com.josephison.fiatlux.civilization;

import net.minecraft.util.RandomSource;

/** Names for the mortals of the created worlds, drawn from the peoples of Scripture. */
public final class MortalNames {
    private static final String[] MEN = {"Abel", "Seth", "Enoch", "Jared", "Lamech", "Noah", "Shem", "Japheth", "Eber", "Asher",
            "Boaz", "Caleb", "Ezra", "Gideon", "Hiram", "Jonah", "Micah", "Nathan", "Obed", "Reuel", "Silas", "Tobias",
            "Zadok", "Amos", "Elias", "Jesse", "Joel", "Levi", "Hosea", "Barak"};
    private static final String[] WOMEN = {"Ada", "Zillah", "Naamah", "Sarai", "Rebekah", "Leah", "Rachel", "Dinah", "Miriam", "Ruth",
            "Naomi", "Hannah", "Deborah", "Abigail", "Esther", "Tamar", "Judith", "Lydia", "Martha", "Phoebe", "Susanna",
            "Keturah", "Orpah", "Zipporah", "Jael", "Elisheba", "Bilhah", "Basemath"};

    private MortalNames() {}

    public static String pick(RandomSource random, boolean male) {
        String[] pool = male ? MEN : WOMEN;
        return pool[random.nextInt(pool.length)];
    }
}
