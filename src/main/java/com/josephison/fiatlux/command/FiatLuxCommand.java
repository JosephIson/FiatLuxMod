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
import net.minecraft.core.BlockPos;
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
                .then(Commands.literal("flood").executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal(com.josephison.fiatlux.flood.Deluge.describe(c.getSource().getServer()))
                                    .withStyle(s -> s.withColor(0x9AB8E8)), false);
                            return 1;
                        })
                        .then(Commands.literal("ark").requires(FiatLuxCommand::isGod).executes(c -> {
                            // "Make thee an ark of gopher wood" (Gen 6:14): the most righteous man near you begins to build
                            var p = c.getSource().getPlayerOrException();
                            c.getSource().sendSuccess(() -> com.josephison.fiatlux.flood.Deluge.begin(p.serverLevel(), p.blockPosition()), true);
                            return 1;
                        }))
                        .then(Commands.literal("bow").requires(FiatLuxCommand::isGod).executes(c -> {
                            // "I do set my bow in the cloud" (Gen 9:13): a rainbow in the sky ahead of you, to the south
                            var p = c.getSource().getPlayerOrException();
                            com.josephison.fiatlux.flood.Deluge.setBow(p.serverLevel(), p.blockPosition().offset(0, 0, -20), 1200);
                            return 1;
                        }))
                        .then(Commands.literal("next").requires(FiatLuxCommand::isGod).executes(c -> {
                            // hasten the story to its next stage
                            com.josephison.fiatlux.flood.Deluge.tick(c.getSource().getServer(), true);
                            c.getSource().sendSuccess(() -> Component.literal(com.josephison.fiatlux.flood.Deluge.describe(c.getSource().getServer()))
                                    .withStyle(s -> s.withColor(0x9AB8E8)), true);
                            return 1;
                        })))
                .then(Commands.literal("nephilim").requires(FiatLuxCommand::isGod).executes(c -> {
                    // the Watchers come down on the town nearest you now (Gen 6:1-4)
                    var p = c.getSource().getPlayerOrException();
                    var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                    int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(p.level().dimension());
                    var world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot) : null;
                    var town = world == null ? null : cos.townNear(world.id, p.blockPosition());
                    if (town == null) {
                        c.getSource().sendFailure(Component.translatable("message.fiatlux.nephilim.none"));
                        return 0;
                    }
                    int n = com.josephison.fiatlux.flood.Nephilim.descend(p.serverLevel(), com.josephison.fiatlux.flood.FloodData.get(c.getSource().getServer()),
                            world.id, world.name, town.centre);
                    c.getSource().sendSuccess(() -> Component.translatable("message.fiatlux.nephilim.descended", n, world.name), true);
                    return n;
                }))
                .then(Commands.literal("polity").executes(c -> {
                    var src = c.getSource();
                    var all = com.josephison.fiatlux.civilization.Polities.get(src.getServer()).byWorld.values();
                    if (all.isEmpty()) src.sendSuccess(() -> Component.literal("No people has yet set a ruler over itself."), false);
                    for (var g : all) {
                        src.sendSuccess(() -> Component.literal("The " + g.name + " - " + g.rulerTitle() + ", from " + g.capital)
                                .withStyle(s -> s.withColor(0xE8C890)), false);
                        for (var law : g.laws) src.sendSuccess(() -> Component.literal("  " + law.text).withStyle(s -> s.withColor(0xA0A0B0)), false);
                    }
                    return all.size();
                }))
                .then(Commands.literal("faiths").executes(c -> {
                    // every faith of the world you stand on: its followers, its leader, and the branches that split from it
                    var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                    int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(c.getSource().getLevel().dimension());
                    var world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot) : null;
                    if (world == null) {
                        c.getSource().sendFailure(Component.literal("Stand on a created world to see its faiths."));
                        return 0;
                    }
                    var lines = com.josephison.fiatlux.civilization.Churches.describe(c.getSource().getServer(), world);
                    c.getSource().sendSuccess(() -> Component.literal("The faiths of " + world.name + ":").withStyle(s -> s.withColor(0xF5D77A)), false);
                    for (String l : lines) c.getSource().sendSuccess(() -> Component.literal(l).withStyle(s -> s.withColor(0xC8C0B0)), false);
                    return lines.size();
                })
                        .then(Commands.literal("split").requires(FiatLuxCommand::isGod).executes(c -> {
                            // the faith of the mortal nearest you splits now
                            var level = c.getSource().getLevel();
                            var m = level.getNearestEntity(com.josephison.fiatlux.entity.MortalEntity.class,
                                    net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().ignoreLineOfSight(), null,
                                    c.getSource().getPosition().x, c.getSource().getPosition().y, c.getSource().getPosition().z,
                                    new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(64));
                            var faith = m == null ? null : com.josephison.fiatlux.civilization.Faiths.of(m);
                            var world = m == null ? null : m.planet();
                            var b = faith == null || world == null ? null : com.josephison.fiatlux.civilization.Churches.branch(level, faith, world, level.random);
                            if (b == null) {
                                c.getSource().sendFailure(Component.literal("There is no faith here with a dissenter to split it."));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.literal(b.founder + " founded the " + b.name + ", split from the " + faith.name + " " + b.doctrine + ".")
                                    .withStyle(s -> s.withColor(0xE0A060)), true);
                            return 1;
                        })))
                .then(Commands.literal("son").requires(FiatLuxCommand::isGod).executes(c -> {
                    // "in the fulness of the time": choose the Son's name, his likeness, his town and his mother
                    var player = c.getSource().getPlayerOrException();
                    var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                    int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(player.serverLevel().dimension());
                    var world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot) : null;
                    if (world == null || !world.hasLife()) {
                        c.getSource().sendFailure(Component.literal("Stand on a living world you have made."));
                        return 0;
                    }
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                            com.josephison.fiatlux.network.SonPayloads.setup(player.serverLevel(), world.name, world.id));
                    return 1;
                })
                        .then(Commands.literal("begin").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            var player = c.getSource().getPlayerOrException();
                            String err = com.josephison.fiatlux.son.TheSon.begin(player, StringArgumentType.getString(c, "name"),
                                    player.getRandom().nextInt(com.josephison.fiatlux.entity.MortalEntity.VARIANTS), player.blockPosition(), null);
                            if (err != null) {
                                c.getSource().sendFailure(Component.literal(err));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.literal("The Son is sent.").withStyle(st -> st.withColor(0xF5D77A)), true);
                            return 1;
                        })))
                        .then(Commands.literal("status").executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal(com.josephison.fiatlux.son.TheSon.describe(c.getSource().getServer()))
                                    .withStyle(st -> st.withColor(0xF5D77A)), false);
                            return 1;
                        }))
                        .then(Commands.literal("next").executes(c -> {
                            // hasten the story to its next part
                            var server = c.getSource().getServer();
                            var sd = com.josephison.fiatlux.son.SonData.get(server);
                            var level = sd.dim.isEmpty() ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(sd.dim)));
                            if (!sd.active() || level == null) {
                                c.getSource().sendFailure(Component.literal("The Son's story is not under way."));
                                return 0;
                            }
                            var from = sd.stage;
                            for (int i = 0; i < 200 && sd.stage == from; i++) com.josephison.fiatlux.son.TheSon.tick(level, sd, true);
                            c.getSource().sendSuccess(() -> Component.literal("The story moves on to the " + sd.stage.name().toLowerCase() + ".")
                                    .withStyle(st -> st.withColor(0xF5D77A)), true);
                            return 1;
                        })))
                .then(Commands.literal("prophets").executes(c -> {
                    var lines = com.josephison.fiatlux.prophecy.Prophets.describe(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("The prophets:").withStyle(st -> st.withColor(0xF5D77A)), false);
                    for (String l : lines) c.getSource().sendSuccess(() -> Component.literal(l).withStyle(st -> st.withColor(0xE8D7B0)), false);
                    return lines.size();
                }))
                .then(Commands.literal("prophet").requires(FiatLuxCommand::isGod).then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    // "Before I formed thee in the belly I knew thee ... and I ordained thee a prophet" (Jer 1:5)
                    String name = StringArgumentType.getString(c, "name");
                    var level = c.getSource().getLevel();
                    var m = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class,
                            new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(128),
                            x -> x.getName().getString().equalsIgnoreCase(name) && !x.isChild()).stream().findFirst().orElse(null);
                    var faith = m == null ? null : com.josephison.fiatlux.civilization.Faiths.of(m);
                    var p = faith == null ? null : com.josephison.fiatlux.prophecy.Prophets.call(level, faith, m,
                            faith.deity.true_ ? faith.deityName : faith.deityName);
                    if (p == null) {
                        c.getSource().sendFailure(Component.literal(m == null ? "No grown mortal named " + name + " is near." : name + " cannot be called."));
                        return 0;
                    }
                    return 1;
                })))
                .then(Commands.literal("scripture").executes(c -> {
                    // the books of the faith most held where you stand (your own faith there, if you are its Creator)
                    var src = c.getSource();
                    var server = src.getServer();
                    var gens = com.josephison.fiatlux.civilization.Generations.get(server);
                    var cos = CosmicExpanseSavedData.get(server);
                    int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(src.getLevel().dimension());
                    var world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot) : null;
                    var player = src.getPlayerOrException();
                    com.josephison.fiatlux.civilization.Religion faith = null;
                    var counts = gens.followers();
                    for (var r : gens.religions.values()) {
                        if (world == null || !r.world.equals(world.name)) continue;
                        if (player.getUUID().equals(r.creator) && r.parent == null) { faith = r; break; }
                        if (faith == null || counts.getOrDefault(r.id, 0) > counts.getOrDefault(faith.id, 0)) faith = r;
                    }
                    var canon = faith == null ? java.util.List.<com.josephison.fiatlux.prophecy.Book>of() : com.josephison.fiatlux.prophecy.Scripture.canon(server, faith);
                    if (canon.isEmpty()) {
                        src.sendFailure(Component.literal("No book has yet been written here. A people writes once it farms and has a priest or a scribe."));
                        return 0;
                    }
                    for (var b : canon) player.getInventory().placeItemBackInInventory(com.josephison.fiatlux.prophecy.Scripture.item(b, faith.name));
                    var f = faith;
                    src.sendSuccess(() -> Component.literal("The writings of " + f.name + ": " + canon.size() + (canon.size() == 1 ? " book." : " books."))
                            .withStyle(st -> st.withColor(0xF5D77A)), false);
                    return canon.size();
                })
                        .then(Commands.literal("write").requires(FiatLuxCommand::isGod).executes(c -> {
                            // the scribes of the world you stand on write what is not yet written
                            var level = c.getSource().getLevel();
                            var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(level.dimension());
                            if (slot < 0 || slot >= cos.planets.size()) {
                                c.getSource().sendFailure(Component.literal("Stand on a created world."));
                                return 0;
                            }
                            var pd = com.josephison.fiatlux.prophecy.ProphecyData.get(c.getSource().getServer());
                            int before = pd.books.size();
                            for (int i = 0; i < 6; i++) com.josephison.fiatlux.prophecy.Scripture.worldTick(level, cos, cos.planets.get(slot));
                            int n = pd.books.size() - before;
                            c.getSource().sendSuccess(() -> Component.literal(n + " new " + (n == 1 ? "book was" : "books were") + " written.")
                                    .withStyle(st -> st.withColor(0xF5D77A)), true);
                            return n;
                        })))
                .then(Commands.literal("wed").requires(FiatLuxCommand::isGod).then(Commands.argument("man", StringArgumentType.word())
                        .then(Commands.argument("woman", StringArgumentType.word()).executes(c -> {
                            // betroth two mortals near you; they are wed at the shrine of their town by day
                            var level = c.getSource().getLevel();
                            var box = new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(128);
                            String a = StringArgumentType.getString(c, "man"), b = StringArgumentType.getString(c, "woman");
                            var man = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class, box,
                                    x -> x.getName().getString().equalsIgnoreCase(a) && x.spouseId() == null).stream().findFirst().orElse(null);
                            var woman = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class, box,
                                    x -> x.getName().getString().equalsIgnoreCase(b) && x.spouseId() == null && x != man).stream().findFirst().orElse(null);
                            if (man == null || woman == null || man.isMale() == woman.isMale()) {
                                c.getSource().sendFailure(Component.literal("There are no two unwed of " + a + " and " + b + " near you to be wed."));
                                return 0;
                            }
                            com.josephison.fiatlux.civilization.Weddings.betroth(level, man, woman);
                            c.getSource().sendSuccess(() -> Component.literal(a + " and " + b + " are betrothed; they will be wed at the shrine.")
                                    .withStyle(st -> st.withColor(0xF5C8D8)), true);
                            return 1;
                        }))))
                .then(Commands.literal("build").requires(FiatLuxCommand::isGod).executes(c -> {
                    // the builders of the town nearest you build anew for their age, all at once
                    var level = c.getSource().getLevel();
                    var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                    int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(level.dimension());
                    var world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot) : null;
                    var town = world == null ? null : cos.townNear(world.id, BlockPos.containing(c.getSource().getPosition()));
                    if (town == null) {
                        c.getSource().sendFailure(Component.literal("There is no town here."));
                        return 0;
                    }
                    int done = 0;
                    for (int i = 0; i < 40; i++) {
                        String what = com.josephison.fiatlux.civilization.MortalPopulation.renew(level, town, world.era);
                        if (what.isEmpty()) break;
                        done++;
                    }
                    cos.setDirty();
                    int n = done;
                    c.getSource().sendSuccess(() -> Component.literal((town.name.isEmpty() ? "The town" : town.name) + " is built anew for the "
                            + world.era.getSerializedName().replace('_', '-') + " age (" + n + " works).").withStyle(st -> st.withColor(0xE8C890)), true);
                    return n;
                }))
                .then(Commands.literal("spirits").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(com.josephison.fiatlux.infernal.Infernal.describe(c.getSource().getServer()))
                            .withStyle(s -> s.withColor(0xD08070)), false);
                    return 1;
                }))
                .then(Commands.literal("watch").requires(FiatLuxCommand::isGod).executes(c -> {
                    // "Go forth and watch over the planets": a guardian angel to every town (Ps 91:11)
                    com.josephison.fiatlux.infernal.Infernal.decree(c.getSource().getServer(), c.getSource().getTextName());
                    return 1;
                }))
                .then(Commands.literal("rift").requires(FiatLuxCommand::isGod).executes(c -> {
                    // a rift from the Underworld opens near you now
                    var rift = com.josephison.fiatlux.infernal.Infernal.openRift(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), null);
                    if (rift == null) {
                        c.getSource().sendFailure(Component.literal("There is no ground here for a rift."));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("possess").requires(FiatLuxCommand::isGod).then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    // let an unclean spirit of the legions enter a mortal near you
                    String name = StringArgumentType.getString(c, "name");
                    var level = c.getSource().getLevel();
                    var m = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class,
                            new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(128),
                            x -> x.getName().getString().equalsIgnoreCase(name)).stream().findFirst().orElse(null);
                    if (m == null || m.isPossessed()) {
                        c.getSource().sendFailure(Component.literal(m == null ? "No one named " + name + " is near." : name + " is possessed already."));
                        return 0;
                    }
                    var lord = com.josephison.fiatlux.infernal.InfernalHierarchy.sender(Math.max(2,
                            CosmicExpanseSavedData.get(c.getSource().getServer()).underworld.overlords), level.random);
                    var affliction = lord != null ? lord.affliction() : com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction.DISCORD;
                    m.possess(com.josephison.fiatlux.infernal.InfernalHierarchy.spiritName(level.random), lord == null ? "" : lord.title(), affliction, false);
                    return 1;
                })))
                .then(Commands.literal("judge").requires(FiatLuxCommand::isGod).then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("crime", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(com.josephison.fiatlux.civilization.Polity.Crime.values())
                                        .map(x -> x.name().toLowerCase(java.util.Locale.ROOT)), b))
                                .executes(c -> {
                                    // bring a mortal before the judgment of their own people's law
                                    String name = StringArgumentType.getString(c, "name");
                                    com.josephison.fiatlux.civilization.Polity.Crime crime;
                                    try {
                                        crime = com.josephison.fiatlux.civilization.Polity.Crime.valueOf(StringArgumentType.getString(c, "crime").toUpperCase(java.util.Locale.ROOT));
                                    } catch (IllegalArgumentException e) {
                                        c.getSource().sendFailure(Component.literal("No such crime."));
                                        return 0;
                                    }
                                    var level = c.getSource().getLevel();
                                    var m = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class,
                                            new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(128),
                                            x -> x.getName().getString().equalsIgnoreCase(name)).stream().findFirst().orElse(null);
                                    var g = m == null ? null : com.josephison.fiatlux.civilization.Justice.polityOf(level, m);
                                    if (m == null || g == null) {
                                        c.getSource().sendFailure(Component.literal(m == null ? "No one named " + name + " is near." : name + "'s people have no government."));
                                        return 0;
                                    }
                                    com.josephison.fiatlux.civilization.Justice.judge(level, g, m.planet(), m, crime);
                                    return 1;
                                }))))
                .then(Commands.literal("anoint").requires(FiatLuxCommand::isGod).then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    // "Then Samuel took the horn of oil, and anointed him in the midst of his brethren" (1 Sam 16:13)
                    String name = StringArgumentType.getString(c, "name");
                    var server = c.getSource().getServer();
                    var gens = com.josephison.fiatlux.civilization.Generations.get(server);
                    var person = gens.people.values().stream().filter(x -> x.living() && x.name.equalsIgnoreCase(name)).findFirst().orElse(null);
                    if (person == null) {
                        c.getSource().sendFailure(Component.literal("No one living is named " + name + "."));
                        return 0;
                    }
                    var cos = CosmicExpanseSavedData.get(server);
                    var world = cos.planets.stream().filter(pl -> pl.name.equals(person.world)).findFirst().orElse(null);
                    var g = world == null ? null : com.josephison.fiatlux.civilization.Polities.get(server).of(world.id);
                    if (g == null) {
                        c.getSource().sendFailure(Component.literal(person.world + " has no government yet."));
                        return 0;
                    }
                    com.josephison.fiatlux.civilization.Government.crown(gens, g, person, c.getSource().getLevel().getGameTime());
                    com.josephison.fiatlux.civilization.Government.record(server, g, c.getSource().getTextName(),
                            "anointed " + person.name + " " + g.form.title(person.male) + " over the " + g.name + ".");
                    c.getSource().sendSuccess(() -> Component.literal(person.name + " is anointed " + g.rulerTitle() + "."), true);
                    return 1;
                })))
                .then(Commands.literal("eden").executes(c -> {
                            var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            c.getSource().sendSuccess(() -> Component.literal(com.josephison.fiatlux.eden.EdenEvents.describe(cos)
                                    + (cos.eden == com.josephison.fiatlux.eden.EdenState.NONE ? "" : " The garden is at " + cos.edenCentre.toShortString()
                                    + " in " + cos.edenDim + ".")).withStyle(s -> s.withColor(0x9CE38C)), false);
                            return 1;
                        })
                        .then(Commands.literal("plant").requires(FiatLuxCommand::isGod).executes(c -> {
                            var p = c.getSource().getPlayerOrException();
                            var ground = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(p.serverLevel(), p.getBlockX(), p.getBlockZ(), 8);
                            if (ground == null) return 0;
                            int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(p.level().dimension());
                            var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            java.util.UUID world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot).id : null;
                            com.josephison.fiatlux.eden.EdenGarden.plant(p.serverLevel(), ground, world);
                            c.getSource().sendSuccess(() -> Component.translatable("message.fiatlux.eden.planted"), true);
                            return 1;
                        }))
                        .then(Commands.literal("begin").requires(FiatLuxCommand::isGod).executes(c -> {
                            // "In the beginning": the garden here, the first pair in it, and mankind not yet fallen
                            var p = c.getSource().getPlayerOrException();
                            var level = p.serverLevel();
                            var ground = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, p.getBlockX(), p.getBlockZ(), 8);
                            if (ground == null) return 0;
                            var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(level.dimension());
                            java.util.UUID world = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot).id : null;
                            cos.fallOfMan = false;
                            com.josephison.fiatlux.eden.EdenGarden.plant(level, ground, world);
                            if (slot >= 0 && slot < cos.planets.size()) {   // the garden's world is a living world, and this Creator gave it life
                                var planet = cos.planets.get(slot);
                                cos.breatheLife(planet);
                                cos.lifeGivers.putIfAbsent(planet.id, com.mojang.datafixers.util.Pair.of(p.getUUID(), p.getGameProfile().getName()));
                            }
                            var era = slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot).era : com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER;
                            var faith = com.josephison.fiatlux.civilization.Generations.get(c.getSource().getServer())
                                    .faithOf(p.getUUID(), p.getGameProfile().getName(), slot >= 0 && slot < cos.planets.size() ? cos.planets.get(slot).name : "the earth",
                                            level.getGameTime()).id;
                            for (String name : new String[]{"Adam", "Eve"}) {
                                var found = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class, p.getBoundingBox().inflate(512),
                                        m -> name.equals(m.getName().getString())).stream().findFirst();
                                var at = ground.offset(name.equals("Adam") ? 3 : 4, 0, name.equals("Adam") ? 4 : 6);
                                if (found.isPresent()) {
                                    found.get().teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                                    found.get().setHome(ground);
                                } else {
                                    com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at, ground, era, name, name.equals("Adam"),
                                            name.equals("Adam") ? 24 : 22, null, null, faith);
                                }
                            }
                            cos.firstPairBorn = true;
                            cos.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal("In the beginning... The garden is planted, and the man and the woman are in it, "
                                    + "and not ashamed. No one is born and no one dies until the fruit is eaten.").withStyle(st -> st.withColor(0x9CE38C)), true);
                            return 1;
                        }))
                        .then(Commands.literal("tempt").requires(FiatLuxCommand::isGod).executes(c -> {
                            var cos = CosmicExpanseSavedData.get(c.getSource().getServer());
                            if (cos.eden != com.josephison.fiatlux.eden.EdenState.INNOCENCE) {
                                c.getSource().sendFailure(Component.literal("There is no garden in innocence for the serpent to enter."));
                                return 0;
                            }
                            if (cos.fallenAngels <= 0) {
                                c.getSource().sendFailure(Component.literal("No angel has yet fallen: there is no tempter. (/fiatlux event rebellion)"));
                                return 0;
                            }
                            cos.edenSince = -1_000_000_000L;   // his time has come
                            cos.setDirty();
                            return 1;
                        })))
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

    /** Opens the Cosmos screen for a player; anyone else (the console) is told in a few plain lines. */
    private static int cosmos(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, com.josephison.fiatlux.network.CosmosReport.build(src.getServer()));
            return 1;
        }
        var c = CosmicExpanseSavedData.get(src.getServer());
        var u = c.underworld;
        var census = com.josephison.fiatlux.civilization.Generations.get(src.getServer()).census();
        src.sendSuccess(() -> Component.translatable("command.fiatlux.cosmos.header"), false);
        src.sendSuccess(() -> Component.literal(String.format("Universe: %s, %d galaxies, %.1f days old",
                c.hasBigBang() ? "lit" : "dark", c.galaxies.size(), c.cosmicAge)), false);
        for (var p : c.planets) {
            src.sendSuccess(() -> Component.literal(p.hasLife()
                    ? String.format("%s (%s): %d living, %s in all, %s age, %d towns, faith %d%%, sin %d%%", p.name, p.type.getSerializedName(),
                    census.getOrDefault(p.name, 0), com.josephison.fiatlux.network.CosmosReport.num(p.population), p.era.getSerializedName(),
                    c.townsOf(p.id).size(), Math.round(p.faith * 100), Math.round(p.sin * 100))
                    : String.format("%s (%s): without life", p.name, p.type.getSerializedName())), false);
        }
        src.sendSuccess(() -> Component.literal(String.format("Heaven: %s souls. Underworld: %s era, %s fallen, %s sinner souls, %d lords risen.",
                com.josephison.fiatlux.network.CosmosReport.num(c.heavenSouls), u.era.getSerializedName(),
                com.josephison.fiatlux.network.CosmosReport.num(u.demons), com.josephison.fiatlux.network.CosmosReport.num(u.souls), u.overlords)), false);
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
