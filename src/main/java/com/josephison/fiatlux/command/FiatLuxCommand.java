package com.josephison.fiatlux.command;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.creation.ChronicleEntry;
import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.power.ModPowers;
import com.josephison.fiatlux.power.PowerCasting;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.HeavenTravel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;

/** /fiatlux - administration of the creation. */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class FiatLuxCommand {
    private FiatLuxCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fiatlux")
                .then(Commands.literal("heaven").executes(c -> {
                    HeavenTravel.toggle(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("cast").then(Commands.argument("power", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ModPowers.REGISTRY.keySet(), b))
                        .executes(c -> {
                            ResourceLocation id = ResourceLocationArgument.getId(c, "power");
                            PowerCasting.Result r = PowerCasting.cast(c.getSource().getPlayerOrException(), id);
                            c.getSource().sendSuccess(() -> Component.translatable("command.fiatlux.cast", id.toString(), r.name()), false);
                            return r == PowerCasting.Result.CAST ? 1 : 0;
                        })))
                .then(Commands.literal("undo").executes(c -> {
                    boolean ok = EditQueue.undo(c.getSource().getPlayerOrException().getUUID(), c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable(ok ? "command.fiatlux.undo" : "command.fiatlux.undo.none"), false);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("chronicle").executes(c -> chronicle(c.getSource(), 10))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                .executes(c -> chronicle(c.getSource(), IntegerArgumentType.getInteger(c, "count")))))
                .then(Commands.literal("mode").requires(FiatLuxCommand::isGod)
                        .executes(c -> {
                            PlayMode m = CosmosData.get(c.getSource().getServer()).playMode();
                            c.getSource().sendSuccess(() -> Component.translatable("command.fiatlux.mode.get", m.getSerializedName()), false);
                            return 1;
                        })
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(PlayMode.values()).map(PlayMode::getSerializedName), b))
                                .executes(c -> {
                                    String name = StringArgumentType.getString(c, "mode");
                                    PlayMode mode = Arrays.stream(PlayMode.values()).filter(p -> p.getSerializedName().equals(name)).findFirst().orElse(null);
                                    if (mode == null) return 0;
                                    CosmosData.get(c.getSource().getServer()).setPlayMode(mode);
                                    c.getSource().getServer().getPlayerList().getPlayers().forEach(Divinity::sync);
                                    c.getSource().sendSuccess(() -> Component.translatable("command.fiatlux.mode.set", name), true);
                                    return 1;
                                })))
                .then(Commands.literal("glory").requires(FiatLuxCommand::isGod)
                        .then(Commands.literal("add").then(Commands.argument("amount", LongArgumentType.longArg())
                                .executes(c -> {
                                    CosmosData cos = CosmosData.get(c.getSource().getServer());
                                    cos.addGlory(LongArgumentType.getLong(c, "amount"));
                                    c.getSource().sendSuccess(() -> Component.translatable("command.fiatlux.glory", cos.glory(), cos.gloryTier()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("essence").requires(FiatLuxCommand::isGod)
                        .then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "target");
                                    Divinity.set(p, Divinity.get(p).withEssence(FloatArgumentType.getFloat(c, "amount")));
                                    Divinity.sync(p);
                                    return 1;
                                }))))
                .then(Commands.literal("pilgrim").requires(FiatLuxCommand::isGod)
                        .then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("mortal", BoolArgumentType.bool())
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "target");
                                    boolean mortal = BoolArgumentType.getBool(c, "mortal");
                                    Divinity.set(p, Divinity.get(p).withPilgrim(mortal));
                                    Divinity.sync(p);
                                    c.getSource().sendSuccess(() -> Component.translatable(mortal ? "command.fiatlux.pilgrim.on" : "command.fiatlux.pilgrim.off",
                                            p.getDisplayName()), true);
                                    return 1;
                                })))));
    }

    private static boolean isGod(CommandSourceStack s) {
        return s.hasPermission(FiatLuxConfig.SERVER.godPermissionLevel.get());
    }

    private static int chronicle(CommandSourceStack src, int count) {
        List<ChronicleEntry> all = CosmosData.get(src.getServer()).chronicle();
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("command.fiatlux.chronicle.empty"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("command.fiatlux.chronicle.header"), false);
        for (ChronicleEntry e : all.subList(Math.max(0, all.size() - count), all.size())) {
            long day = e.gameTime() / 24000L + 1;
            src.sendSuccess(() -> Component.literal("Day " + day + " - " + e.actor() + " " + e.text()).withStyle(s -> s.withColor(0xF5D77A)), false);
        }
        return all.size();
    }
}
