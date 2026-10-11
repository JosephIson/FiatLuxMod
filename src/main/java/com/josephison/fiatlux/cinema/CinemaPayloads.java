package com.josephison.fiatlux.cinema;

import com.josephison.fiatlux.FiatLux;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The films' traffic: a film to play, cues added while it plays, and the watcher's word that it has begun or ended. */
public final class CinemaPayloads {
    private CinemaPayloads() {}

    /** Clientbound: play this film. */
    public record Play(Film film) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(FiatLux.id("film_play"));
        public static final StreamCodec<ByteBuf, Play> STREAM_CODEC = Film.CODEC.map(Play::new, Play::film);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Clientbound: a cue for the film now playing, at once (narration as the story speaks it). */
    public record Live(Film.Cue cue) implements CustomPacketPayload {
        public static final Type<Live> TYPE = new Type<>(FiatLux.id("film_live"));
        public static final StreamCodec<ByteBuf, Live> STREAM_CODEC = Film.Cue.CODEC.map(Live::new, Live::cue);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Serverbound: the film has begun (`started`) or is over, ended or skipped (`!started`). */
    public record Watch(String film, boolean started) implements CustomPacketPayload {
        public static final Type<Watch> TYPE = new Type<>(FiatLux.id("film_watch"));
        public static final StreamCodec<ByteBuf, Watch> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Watch::film,
                ByteBufCodecs.BOOL, Watch::started, Watch::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
