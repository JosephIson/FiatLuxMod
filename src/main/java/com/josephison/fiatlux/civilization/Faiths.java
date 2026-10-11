package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * How faith lives among mortals: the true faith of a world's Creator, prayers and petitions sent up
 * to that player, the doubt that grows when heaven is silent, the false faiths doubters found, and
 * the conversions that follow conversation and miracles.
 */
public final class Faiths {
    /** Ticks without a sign before doubt begins to grow. */
    public static final long SILENCE = 3 * 24000L;
    /** Ticks a petition waits for an answer before it is given up. */
    public static final long PETITION_LIFE = 6000L;

    public enum Petition {
        BREAD("bread"), HEALING("healing"), CHILD("child"), PROTECTION("protection"), SIGN("sign");
        public final String id;

        Petition(String id) {
            this.id = id;
        }
    }

    private Faiths() {}

    // ------------------------------------------------------------------ founding and schism

    /** A doubter founds a faith of its own, turning to what it can see. */
    public static Religion schism(ServerLevel level, MortalEntity founder, String world) {
        RandomSource r = level.random;
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        Religion.Deity deity;
        if (cosmos.corruption > 0.4 || founder.hasTrait(Trait.GREEDY) && r.nextBoolean()) {
            deity = r.nextBoolean() ? Religion.Deity.IDOL : Religion.Deity.FALLEN;
        } else if (founder.profession() == Profession.SCHOLAR || founder.hasTrait(Trait.CURIOUS)) {
            deity = Religion.Deity.STARS;
        } else if (founder.isElder()) {
            deity = Religion.Deity.ANCESTORS;
        } else {
            deity = r.nextInt(3) == 0 ? Religion.Deity.IDOL : Religion.Deity.SUN;
        }
        String[] names = switch (deity) {
            case SUN -> new String[]{"Shemesh", "the Burning Eye", "the Golden Disc", "Baal"};
            case STARS -> new String[]{"the Host of Heaven", "the Seven Lights", "the Watchers"};
            case ANCESTORS -> new String[]{"the Fathers", "the Old Ones", "the First Mothers"};
            case IDOL -> new String[]{"the Golden Calf", "Mammon", "the Bronze Serpent"};
            case FALLEN -> new String[]{"Moloch", "the Morning Star", "the Deep One"};
            default -> new String[]{"the Unknown"};
        };
        String deityName = names[r.nextInt(names.length)];
        String name = switch (deity) {
            case SUN -> "Children of " + deityName;
            case STARS -> "Watchers of " + deityName;
            case ANCESTORS -> "House of " + deityName;
            case IDOL -> "Cult of " + deityName;
            case FALLEN -> "Covenant of " + deityName;
            default -> "Seekers";
        };
        Religion rel = new Religion(UUID.randomUUID(), name, deity, deityName, null, founder.getName().getString(), world, level.getGameTime());
        Generations.get(level.getServer()).add(rel);
        Religion old = Generations.get(level.getServer()).religion(founder.religion());
        CosmosData.get(level.getServer()).record(level.getServer(), founder.getName().getString(), "schism",
                "of " + world + " turned " + (old != null ? "from " + old.deityName + " " : "") + "and founded the " + name + ", worshipping " + deityName + ".");
        if (old != null && old.creator != null) {
            ServerPlayer creator = level.getServer().getPlayerList().getPlayer(old.creator);
            if (creator != null) {
                creator.sendSystemMessage(Component.translatable("message.fiatlux.faith.schism", founder.getName(), world, name, deityName)
                        .withStyle(s -> s.withColor(0xE0A060)));
            }
        }
        founder.setReligion(rel.id);
        return rel;
    }

    /**
     * In conversation, the listener may take up the speaker's faith: more readily when the speaker
     * is a priest, when the listener doubts, and when the listener's temperament leans that way.
     */
    public static boolean maybeConvert(MortalEntity speaker, MortalEntity listener, RandomSource r) {
        if (speaker.religion() == null || speaker.religion().equals(listener.religion())) return false;
        Religion faith = Generations.get(speaker.getServer()).religion(speaker.religion());
        if (faith == null) return false;
        double chance = 0.04 + listener.doubt() * 0.25 + (speaker.profession() == Profession.PRIEST ? 0.08 : 0);
        if (faith.deity.true_ && listener.hasTrait(Trait.PIOUS)) chance += 0.1;
        if (!faith.deity.true_ && (listener.hasTrait(Trait.GREEDY) || listener.hasTrait(Trait.PROUD))) chance += 0.08;
        if (r.nextDouble() >= chance) return false;
        listener.setReligion(faith.id);
        listener.setDoubt(0.1);
        return true;
    }

    // ------------------------------------------------------------------ signs, miracles and petitions

    /**
     * A Creator worked a wonder: mortals near it who saw it react, the doubting turn back to the
     * Creator who did it, and the petitions of a settlement nearby are answered.
     */
    public static void onMiracle(ServerLevel level, net.minecraft.world.phys.Vec3 at, ServerPlayer caster, boolean wrathful) {
        var mortals = level.getEntitiesOfClass(MortalEntity.class, new AABB(BlockPos.containing(at)).inflate(40));
        if (mortals.isEmpty()) return;
        Generations gens = Generations.get(level.getServer());
        String world = mortals.getFirst().worldName();
        Religion faith = gens.faithOf(caster.getUUID(), caster.getGameProfile().getName(), world, level.getGameTime());
        for (MortalEntity m : mortals) {
            m.witness(wrathful);
            if (!wrathful && !faith.id.equals(m.religion()) && level.random.nextDouble() < (m.hasTrait(Trait.PROUD) ? 0.3 : 0.7)) {
                m.setReligion(faith.id);
            }
            if (!wrathful) m.setDoubt(Math.max(0, m.doubt() - 0.5));
        }
        // answered prayers
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        for (Settlement s : cosmos.settlements.values()) {
            if (s.centre.distSqr(BlockPos.containing(at)) > 56 * 56) continue;
            s.lastSign = level.getGameTime();
            if (wrathful || s.petitions.isEmpty()) continue;
            for (Map.Entry<Petition, String> p : s.petitions.entrySet()) {
                switch (p.getKey()) {
                    case BREAD -> s.food += 25;
                    case HEALING -> mortals.forEach(m -> m.heal(m.getMaxHealth()));
                    case CHILD -> s.blessedWithChild = true;
                    default -> {
                    }
                }
            }
            CosmosData.get(level.getServer()).record(level.getServer(), caster.getGameProfile().getName(), "answered",
                    "answered the prayers of " + world + " (" + String.join(", ", s.petitions.keySet().stream().map(x -> x.id).toList()) + ").");
            caster.sendSystemMessage(Component.translatable("message.fiatlux.faith.answered", world).withStyle(st -> st.withColor(0xF5D77A)));
            s.petitions.clear();
            Fx.send(level, FxKind.GLORY_FLASH, net.minecraft.world.phys.Vec3.atCenterOf(s.shrine).add(0, 2, 0), 10f, 0xFFFFF0C0, 30);
            cosmos.setDirty();
        }
    }

    /** A praying mortal of the true faith lays a need before its Creator (one open petition of a kind per settlement). */
    public static void petition(ServerLevel level, MortalEntity m, Settlement s, Petition kind, String words) {
        if (s.petitions.containsKey(kind) || level.getGameTime() - s.lastPetition < 1200) return;
        Religion faith = Generations.get(level.getServer()).religion(m.religion());
        if (faith == null || faith.creator == null) return;
        s.petitions.put(kind, words);
        s.petitionsSince = s.petitions.size() == 1 ? level.getGameTime() : s.petitionsSince;
        s.lastPetition = level.getGameTime();
        ServerPlayer creator = level.getServer().getPlayerList().getPlayer(faith.creator);
        if (creator != null) {
            creator.sendSystemMessage(Component.translatable("message.fiatlux.faith.petition", m.getName(), m.worldName(), words)
                    .withStyle(st -> st.withColor(0xC8E0FF).withItalic(true)));
        }
        CosmicExpanseSavedData.get(level.getServer()).setDirty();
    }

    /** Petitions left unanswered too long are given up, and the people doubt. */
    public static void expirePetitions(ServerLevel level, Settlement s, java.util.List<MortalEntity> people) {
        if (s.petitions.isEmpty() || level.getGameTime() - s.petitionsSince < PETITION_LIFE) return;
        s.petitions.clear();
        for (MortalEntity m : people) m.setDoubt(Math.min(1, m.doubt() + 0.2));
        CosmicExpanseSavedData.get(level.getServer()).setDirty();
    }

    /** The shrine is crowned according to what most of its people worship. */
    public static void crownShrine(ServerLevel level, Settlement s, java.util.List<MortalEntity> people) {
        if (people.isEmpty() || !level.isLoaded(s.shrine)) return;
        Map<UUID, Integer> votes = new java.util.HashMap<>();
        for (MortalEntity m : people) if (m.religion() != null) votes.merge(m.religion(), 1, Integer::sum);
        UUID top = votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        Religion r = Generations.get(level.getServer()).religion(top);
        if (r == null) return;
        BlockPos crown = s.shrine.above();
        var want = r.deity.shrineCrown().defaultBlockState();
        if (!level.getBlockState(crown).is(want.getBlock())) {
            level.setBlock(crown, want, net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (r.deity == Religion.Deity.FALLEN) level.setBlock(crown.above(), net.minecraft.world.level.block.Blocks.SOUL_FIRE.defaultBlockState(), 3);
            else if (level.getBlockState(crown.above()).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) level.removeBlock(crown.above(), false);
        }
    }

    @Nullable
    public static Religion of(MortalEntity m) {
        return m.getServer() == null ? null : Generations.get(m.getServer()).religion(m.religion());
    }
}
