package com.josephison.fiatlux.network;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.config.FiatLuxConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Book of Generations over the network. Serverbound: "show me this person's family" (an empty
 * id asks for the first of all mortals). Clientbound: a family seen from one person - grandparents,
 * parents, the person and their spouse, children and grandchildren - and the faiths of the cosmos.
 */
public final class LineagePayloads {
    private LineagePayloads() {}

    public record Request(Optional<UUID> person) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(FiatLux.id("lineage_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC =
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).map(Request::new, Request::person);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One person in the view. gen: -2 grandparents ... 0 the person and spouse ... 2 grandchildren. */
    public record Node(UUID id, String name, boolean male, int gen, String info, String faith, String soul,
                       Optional<UUID> father, Optional<UUID> mother, boolean focus) {
        static final StreamCodec<ByteBuf, Node> CODEC = new StreamCodec<>() {
            @Override
            public Node decode(ByteBuf b) {
                return new Node(UUIDUtil.STREAM_CODEC.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), b.readBoolean(), b.readByte(),
                        ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b),
                        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).decode(b), ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).decode(b), b.readBoolean());
            }

            @Override
            public void encode(ByteBuf b, Node n) {
                UUIDUtil.STREAM_CODEC.encode(b, n.id);
                ByteBufCodecs.STRING_UTF8.encode(b, n.name);
                b.writeBoolean(n.male);
                b.writeByte(n.gen);
                ByteBufCodecs.STRING_UTF8.encode(b, n.info);
                ByteBufCodecs.STRING_UTF8.encode(b, n.faith);
                ByteBufCodecs.STRING_UTF8.encode(b, n.soul);
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).encode(b, n.father);
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).encode(b, n.mother);
                b.writeBoolean(n.focus);
            }
        };
    }

    public record Faith(String name, String deity, int color, int followers) {
        static final StreamCodec<ByteBuf, Faith> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Faith::name, ByteBufCodecs.STRING_UTF8, Faith::deity, ByteBufCodecs.INT, Faith::color,
                ByteBufCodecs.VAR_INT, Faith::followers, Faith::new);
    }

    public record View(List<Node> nodes, List<Faith> faiths, int living, int total) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(FiatLux.id("lineage_view"));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                Node.CODEC.apply(ByteBufCodecs.list()), View::nodes, Faith.CODEC.apply(ByteBufCodecs.list()), View::faiths,
                ByteBufCodecs.VAR_INT, View::living, ByteBufCodecs.VAR_INT, View::total, View::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Builds the view of one person's family (or of the first mortal, if none is named). */
    public static View view(MinecraftServer server, @Nullable UUID focus) {
        Generations g = Generations.get(server);
        Person f = focus != null ? g.person(focus) : null;
        if (f == null) f = g.people.values().stream().filter(p -> p.father == null && p.mother == null).findFirst()
                .orElse(g.people.values().stream().findFirst().orElse(null));
        List<Node> nodes = new ArrayList<>();
        long now = server.overworld().getGameTime();
        int year = FiatLuxConfig.COMMON.mortalYearTicks.get();
        if (f != null) {
            List<UUID> seen = new ArrayList<>();
            add(nodes, seen, g, f, 0, true, now, year);
            Person spouse = g.person(f.spouse);
            if (spouse != null) add(nodes, seen, g, spouse, 0, false, now, year);
            for (Person parent : new Person[]{g.person(f.father), g.person(f.mother)}) {
                if (parent == null) continue;
                add(nodes, seen, g, parent, -1, false, now, year);
                for (Person gp : new Person[]{g.person(parent.father), g.person(parent.mother)}) if (gp != null) add(nodes, seen, g, gp, -2, false, now, year);
            }
            List<Person> kids = g.childrenOf(f.id);
            for (Person c : kids.subList(0, Math.min(8, kids.size()))) {
                add(nodes, seen, g, c, 1, false, now, year);
                List<Person> grand = g.childrenOf(c.id);
                for (Person gc : grand.subList(0, Math.min(4, grand.size()))) add(nodes, seen, g, gc, 2, false, now, year);
            }
        }
        List<Faith> faiths = new ArrayList<>();
        var counts = g.followers();
        for (Religion r : g.religions.values()) {
            faiths.add(new Faith(r.name + " (" + r.world + ")", r.deityName, r.deity.color, counts.getOrDefault(r.id, 0)));
        }
        faiths.sort((a, b) -> Integer.compare(b.followers(), a.followers()));
        int living = (int) g.people.values().stream().filter(Person::living).count();
        return new View(nodes, faiths.subList(0, Math.min(10, faiths.size())), living, g.people.size());
    }

    private static void add(List<Node> nodes, List<UUID> seen, Generations g, Person p, int gen, boolean focus, long now, int year) {
        if (seen.contains(p.id)) return;
        seen.add(p.id);
        int age = p.living() ? (int) ((now - p.born) / Math.max(1, year)) : p.ageAtDeath;
        String info = (p.living() ? "aged " + age : "died aged " + age) + (p.profession.isEmpty() ? "" : ", " + p.profession) + " - " + p.world;
        Religion r = g.religion(p.religion);
        nodes.add(new Node(p.id, p.name, p.male, gen, info, r == null ? "" : r.name, p.soul,
                Optional.ofNullable(p.father), Optional.ofNullable(p.mother), focus));
    }
}
