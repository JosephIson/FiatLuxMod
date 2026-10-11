package com.josephison.fiatlux.command;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.config.PlayMode;
import com.josephison.fiatlux.creation.ChronicleEntry;
import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.power.PowerRegistry;
import com.josephison.fiatlux.power.PowerCasting;
import com.josephison.fiatlux.power.powers.CosmicSparkPower;
import com.josephison.fiatlux.power.powers.PlanetaryGenesisPower;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.PlanetType;
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
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(PowerRegistry.REGISTRY.keySet(), b))
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
                .then(Commands.literal("cosmos").executes(c -> cosmos(c.getSource())))
                .then(Commands.literal("host").executes(c -> host(c.getSource())))
                .then(Commands.literal("lineage").executes(c -> {
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(c.getSource().getPlayerOrException(),
                                    com.josephison.fiatlux.network.LineagePayloads.view(c.getSource().getServer(), null));
                            return 1;
                        })
                        .then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            String name = StringArgumentType.getString(c, "name");
                            var g = com.josephison.fiatlux.civilization.Generations.get(c.getSource().getServer());
                            var person = g.people.values().stream().filter(p -> p.name.equalsIgnoreCase(name))
                                    .sorted((a, b) -> Boolean.compare(b.living(), a.living())).findFirst();
                            if (person.isEmpty()) {
                                c.getSource().sendFailure(Component.literal("No one named " + name + " is written in the Book of Generations."));
                                return 0;
                            }
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(c.getSource().getPlayerOrException(),
                                    com.josephison.fiatlux.network.LineagePayloads.view(c.getSource().getServer(), person.get().id));
                            return 1;
                        })))
                .then(Commands.literal("faiths").executes(c -> {
                    var g = com.josephison.fiatlux.civilization.Generations.get(c.getSource().getServer());
                    var counts = g.followers();
                    c.getSource().sendSuccess(() -> Component.literal("The faiths of the cosmos:").withStyle(s -> s.withColor(0xF5D77A)), false);
                    for (var r : g.religions.values()) {
                        c.getSource().sendSuccess(() -> Component.literal(String.format("%s (%s) - worships %s, founded by %s - %d followers",
                                r.name, r.world, r.deityName, r.founder, counts.getOrDefault(r.id, 0))).withStyle(s -> s.withColor(r.deity.color & 0xFFFFFF)), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("blackhole").requires(FiatLuxCommand::isGod)
                        .then(Commands.literal("create").then(Commands.argument("radius", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(2f, 40f))
                                .executes(c -> {
                                    var p = c.getSource().getPlayerOrException();
                                    if (p.level().dimension() != com.josephison.fiatlux.world.FiatLuxDimensions.VOID) {
                                        c.getSource().sendFailure(Component.translatable("power.fiatlux.bottomless_pit.only_void"));
                                        return 0;
                                    }
                                    float r = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "radius");
                                    var at = p.getEyePosition().add(p.getLookAngle().scale(r * 6 + 20));
                                    var hole = com.josephison.fiatlux.power.powers.BottomlessPitPower.open(c.getSource().getServer(), p.getGameProfile().getName(), at, r);
                                    if (hole == null) return 0;
                                    c.getSource().sendSuccess(() -> Component.literal(hole.name() + " opens in the deep."), true);
                                    return 1;
                                })))
                        .then(Commands.literal("remove").executes(c -> {
                            var p = c.getSource().getPlayerOrException();
                            var cosmos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            var nearest = cosmos.blackHoles.stream().min(java.util.Comparator.comparingDouble(h -> h.center().distanceToSqr(p.position())));
                            if (nearest.isEmpty()) return 0;
                            cosmos.blackHoles.remove(nearest.get());
                            cosmos.setDirty();
                            WorldTicker.syncAll(c.getSource().getServer());
                            c.getSource().sendSuccess(() -> Component.literal(nearest.get().name() + " is sealed."), true);
                            return 1;
                        }))
                        .then(Commands.literal("list").executes(c -> {
                            for (var h : CosmicExpanseSavedData.get(c.getSource().getServer()).blackHoles) {
                                c.getSource().sendSuccess(() -> Component.literal(String.format("%s at %.0f %.0f %.0f, horizon %.1f", h.name(), h.x(), h.y(), h.z(), h.radius())), false);
                            }
                            return 1;
                        })))
                .then(Commands.literal("realm")
                        .then(Commands.literal("heaven").executes(c -> HeavenTravel.toHeaven(c.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("void").executes(c -> com.josephison.fiatlux.realm.RealmTravel.toVoid(c.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("underworld").executes(c -> com.josephison.fiatlux.realm.RealmTravel.toUnderworld(c.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("earth").executes(c -> {
                            HeavenTravel.toEarth(c.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("planet").requires(FiatLuxCommand::isGod)
                        .then(Commands.literal("create").then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(PlanetType.values()).map(PlanetType::getSerializedName), b))
                                .executes(c -> createPlanet(c.getSource(), StringArgumentType.getString(c, "type"), null))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(c -> createPlanet(c.getSource(), StringArgumentType.getString(c, "type"), StringArgumentType.getString(c, "name"))))))
                        .then(Commands.literal("approach").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            var cosmos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            String name = StringArgumentType.getString(c, "name");
                            for (int i = 0; i < cosmos.planets.size(); i++) {
                                if (cosmos.planets.get(i).name.equalsIgnoreCase(name)) {
                                    return com.josephison.fiatlux.realm.RealmTravel.worldToVoid(c.getSource().getPlayerOrException(), i) ? 1 : 0;
                                }
                            }
                            c.getSource().sendFailure(Component.translatable("message.fiatlux.world_unformed"));
                            return 0;
                        })))
                        .then(Commands.literal("visit").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            var cosmos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            String name = StringArgumentType.getString(c, "name");
                            int slot = -1;
                            for (int i = 0; i < Math.min(cosmos.planets.size(), com.josephison.fiatlux.realm.RealmPool.SIZE); i++) {
                                if (cosmos.planets.get(i).name.equalsIgnoreCase(name)) slot = i;
                            }
                            if (slot < 0) {
                                c.getSource().sendFailure(Component.translatable("message.fiatlux.world_unformed"));
                                return 0;
                            }
                            return com.josephison.fiatlux.realm.RealmTravel.toPlanet(c.getSource().getPlayerOrException(), slot) ? 1 : 0;
                        })))
                        .then(Commands.literal("life").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            var cosmos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            var p = cosmos.planet(StringArgumentType.getString(c, "name")).orElse(null);
                            boolean ok = p != null && cosmos.breatheLife(p);
                            if (ok && c.getSource().getEntity() instanceof ServerPlayer giver) {
                                cosmos.lifeGivers.put(p.id, com.mojang.datafixers.util.Pair.of(giver.getUUID(), giver.getGameProfile().getName()));
                            }
                            c.getSource().sendSuccess(() -> Component.translatable(ok ? "command.fiatlux.life" : "command.fiatlux.life.fail"), true);
                            WorldTicker.syncAll(c.getSource().getServer());
                            return ok ? 1 : 0;
                        }))))
                .then(Commands.literal("sim").requires(FiatLuxCommand::isGod)
                        .then(Commands.literal("step").then(Commands.argument("seconds", FloatArgumentType.floatArg(0.05f, 1_000_000f)).executes(c -> {
                            WorldTicker.step(c.getSource().getServer(), FloatArgumentType.getFloat(c, "seconds"));
                            WorldTicker.syncAll(c.getSource().getServer());
                            return cosmos(c.getSource());
                        }))))
                .then(Commands.literal("event").requires(FiatLuxCommand::isGod)
                        .then(Commands.literal("bigbang").executes(c -> CosmicSparkPower.ignite(c.getSource().getServer(), c.getSource().getTextName()) ? 1 : 0))
                        .then(Commands.literal("rebellion").executes(c -> {
                            WorldTicker.startRebellion(c.getSource().getServer(), CosmosData.get(c.getSource().getServer()), CosmicExpanseSavedData.get(c.getSource().getServer()));
                            return 1;
                        }))
                        .then(Commands.literal("fall").executes(c -> {
                            WorldTicker.theFall(c.getSource().getServer(), CosmosData.get(c.getSource().getServer()), CosmicExpanseSavedData.get(c.getSource().getServer()));
                            return 1;
                        }))
                        .then(Commands.literal("castout").executes(c -> {
                            int n = com.josephison.fiatlux.world.RebellionWar.castOutAll(c.getSource().getServer(), 6);
                            c.getSource().sendSuccess(() -> Component.literal(n + " rebel angels are cast out of heaven."), true);
                            return n;
                        }))
                        .then(Commands.literal("cleanse").executes(c -> {
                            WorldTicker.divineCleanse(c.getSource().getServer(), CosmosData.get(c.getSource().getServer()), CosmicExpanseSavedData.get(c.getSource().getServer()),
                                    c.getSource().getServer().overworld().getDayTime() / 24000L);
                            return 1;
                        })))
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

    /** Lists the nine choirs in order, with the loaded members of each (loyal and rebel). */
    private static int host(CommandSourceStack src) {
        java.util.Map<com.josephison.fiatlux.entity.Choir, int[]> counts = new java.util.EnumMap<>(com.josephison.fiatlux.entity.Choir.class);
        for (var level : src.getServer().getAllLevels()) {
            for (var b : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(com.josephison.fiatlux.entity.HeavenlyBeing.class), b -> true)) {
                counts.computeIfAbsent(b.choir(), k -> new int[2])[b.isRebel() ? 1 : 0]++;
            }
        }
        src.sendSuccess(() -> Component.translatable("command.fiatlux.host.header").withStyle(s -> s.withColor(0xF5D77A)), false);
        for (var choir : com.josephison.fiatlux.entity.Choir.values()) {
            int[] n = counts.getOrDefault(choir, new int[2]);
            src.sendSuccess(() -> Component.translatable("command.fiatlux.host.line", choir.rank(), choir.displayName(), choir.sphere(),
                    choir.office(), choir.scripture(), n[0], n[1]), false);
        }
        return 1;
    }

    private static int createPlanet(CommandSourceStack src, String typeName, String name) {
        PlanetType type = PlanetType.byName(typeName);
        if (type == null) {
            src.sendFailure(Component.translatable("command.fiatlux.planet.bad_type", typeName));
            return 0;
        }
        if (!CosmicExpanseSavedData.get(src.getServer()).hasBigBang()) {
            src.sendFailure(Component.translatable("message.fiatlux.no_universe"));
            return 0;
        }
        var p = PlanetaryGenesisPower.create(src.getServer(), src.getTextName(), type, name);
        if (p == null) {
            src.sendFailure(Component.translatable("command.fiatlux.planet.full"));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("message.fiatlux.planet_formed", p.name,
                Component.translatable("planet_type.fiatlux." + p.type.getSerializedName())), true);
        return 1;
    }

    private static int cosmos(CommandSourceStack src) {
        var c = CosmicExpanseSavedData.get(src.getServer());
        var u = c.underworld;
        src.sendSuccess(() -> Component.translatable("command.fiatlux.cosmos.header"), false);
        src.sendSuccess(() -> Component.literal(String.format("Big Bang: %s · galaxies %d · cosmic age %.1f days",
                c.hasBigBang() ? "yes" : "not yet", c.galaxies.size(), c.cosmicAge)).withStyle(s -> s.withColor(0xF5D77A)), false);
        for (var p : c.planets) {
            src.sendSuccess(() -> Component.literal(String.format("  %s (%s) orbit %.2f AU / %.1f d · pop %,.0f · %s · faith %.1f · sin %.1f",
                    p.name, p.type.getSerializedName(), p.orbitRadius, p.orbitPeriodDays, p.population,
                    p.hasLife() ? p.era.getSerializedName() : "lifeless", p.faith, p.sin)).withStyle(s -> s.withColor(0xCFE8FF)), false);
        }
        src.sendSuccess(() -> Component.literal(String.format("Corruption %.0f%% · angelic pride %.0f%% · rebellion %s · fallen %,.0f · souls in Heaven %,.0f",
                c.corruption * 100, c.angelicPride * 100, c.rebellionActive ? "RAGING" : "none", c.fallenAngels, c.heavenSouls)).withStyle(s -> s.withColor(0xFFD0A0)), false);
        src.sendSuccess(() -> Component.literal(String.format("Underworld: %s · knowledge %,.0f · soul power %,.0f · demons %,.0f · sinner souls %,.0f · overlords %d · contracts %,.0f",
                u.era.getSerializedName(), u.knowledge, u.soulPower, u.demons, u.souls, u.overlords, u.contracts)).withStyle(s -> s.withColor(0xFF7A5A)), false);
        return 1;
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
