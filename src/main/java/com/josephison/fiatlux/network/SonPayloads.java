package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Choosing the Son. Clientbound: the towns of the world the Creator stands on, and in each the women
 * who might be his mother. Serverbound: the Creator's choice - his name, his look, his town, his mother.
 */
public final class SonPayloads {
    private SonPayloads() {}

    public record Mother(UUID id, String name, String info) {
        static final StreamCodec<ByteBuf, Mother> CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Mother::id, ByteBufCodecs.STRING_UTF8, Mother::name,
                ByteBufCodecs.STRING_UTF8, Mother::info, Mother::new);
    }

    public record Town(String name, BlockPos centre, List<Mother> mothers) {
        static final StreamCodec<ByteBuf, Town> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Town::name, BlockPos.STREAM_CODEC, Town::centre,
                Mother.CODEC.apply(ByteBufCodecs.list()), Town::mothers, Town::new);
    }

    public record Setup(String world, List<Town> towns) implements CustomPacketPayload {
        public static final Type<Setup> TYPE = new Type<>(FiatLux.id("son_setup"));
        public static final StreamCodec<ByteBuf, Setup> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Setup::world,
                Town.CODEC.apply(ByteBufCodecs.list()), Setup::towns, Setup::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Choose(String name, int variant, BlockPos town, Optional<UUID> mother) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(FiatLux.id("son_choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Choose::name, ByteBufCodecs.VAR_INT,
                Choose::variant, BlockPos.STREAM_CODEC, Choose::town, ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Choose::mother, Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The towns of the world at `level` (those near enough to be seen) and the women of each who might bear him. */
    public static Setup setup(ServerLevel level, String worldName, java.util.UUID planet) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        Generations g = Generations.get(level.getServer());
        List<Town> towns = new ArrayList<>();
        for (Settlement s : c.townsOf(planet)) {
            if (!level.isLoaded(s.centre)) continue;
            List<Mother> mothers = new ArrayList<>();
            for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, new AABB(s.centre).inflate(64),
                    m -> !m.isMale() && !m.isChild() && m.ageYears() < 50 && !m.isPossessed())) {
                Person p = g.person(m.getUUID());
                String line = p != null && !p.line.isEmpty() ? ", of the line of " + p.line.substring(0, 1).toUpperCase() + p.line.substring(1) : "";
                String parents = p != null && p.father != null && g.person(p.father) != null ? ", daughter of " + g.person(p.father).name : "";
                mothers.add(new Mother(m.getUUID(), m.getName().getString(), (int) m.ageYears() + " years" + parents + line
                        + (m.spouseId() == null ? ", unwed" : ", wife of " + (g.person(m.spouseId()) != null ? g.person(m.spouseId()).name : "another"))));
                if (mothers.size() >= 12) break;
            }
            towns.add(new Town(s.name.isEmpty() ? worldName : s.name, s.centre, mothers));
        }
        return new Setup(worldName, towns);
    }
}
