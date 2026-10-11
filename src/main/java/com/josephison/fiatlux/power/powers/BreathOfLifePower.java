package com.josephison.fiatlux.power.powers;

import net.minecraft.core.BlockPos;

import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.power.AbstractDivinePower;
import com.josephison.fiatlux.power.PowerContext;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.network.chat.Component;

/**
 * "And the LORD God formed man of the dust of the ground, and breathed into his nostrils the breath
 * of life; and man became a living soul." (Genesis 2:7)
 *
 * Standing on a created world, it breathes life upon that world: its first people rise from the
 * dust where the Creator gazes (the first ever are Adam and Eve), and from then on its civilisation
 * lives on without the Creator's hand. On the earth (or any world already living) it forms one more
 * mortal from the dust. From Heaven or the Void it seeds life on the most recently formed habitable
 * world that has none.
 */
public class BreathOfLifePower extends AbstractDivinePower {
    public BreathOfLifePower() {
        super(150f, 2, 0xFF9CE38C);
    }

    @Override
    public boolean cast(PowerContext ctx) {
        var server = ctx.level().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        var dim = ctx.level().dimension();
        boolean above = dim == com.josephison.fiatlux.world.FiatLuxDimensions.HEAVEN || dim == com.josephison.fiatlux.world.FiatLuxDimensions.VOID
                || dim == com.josephison.fiatlux.world.FiatLuxDimensions.UNDERWORLD;
        if (!above) return formFromDust(ctx, cosmos);
        Planet target = null;
        for (int i = cosmos.planets.size() - 1; i >= 0 && target == null; i--) {
            Planet p = cosmos.planets.get(i);
            if (p.capacity() > 0 && !p.hasLife()) target = p;
        }
        if (target == null) {
            ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.no_lifeless_world"), true);
            return false;
        }
        cosmos.breatheLife(target);
        cosmos.lifeGivers.put(target.id, com.mojang.datafixers.util.Pair.of(ctx.caster().getUUID(), ctx.caster().getGameProfile().getName()));
        CosmosData.get(server).record(server, ctx.caster().getGameProfile().getName(), "life",
                "breathed the breath of life upon " + target.name + ", and mortals became living souls.");
        Fx.send(ctx.level(), FxKind.BLESSING, ctx.caster().position(), 3f, color(), 40);
        WorldTicker.syncAll(server);
        return true;
    }

    /** On a world or the earth: life rises from the dust where the Creator gazes. */
    private boolean formFromDust(PowerContext ctx, CosmicExpanseSavedData cosmos) {
        var server = ctx.level().getServer();
        int slot = com.josephison.fiatlux.realm.RealmPool.slotOf(ctx.level().dimension());
        Planet world = slot >= 0 && slot < cosmos.planets.size() ? cosmos.planets.get(slot) : null;
        if (world != null && world.capacity() <= 0) {
            ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.no_lifeless_world"), true);
            return false;
        }
        net.minecraft.core.BlockPos ground = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(ctx.level(),
                ctx.targetBlock().getX(), ctx.targetBlock().getZ(), 6);
        if (ground == null) return false;
        var era = world != null ? world.era : com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER;
        String worldName = world != null ? world.name : "the earth";
        var faith = com.josephison.fiatlux.civilization.Generations.get(server)
                .faithOf(ctx.caster().getUUID(), ctx.caster().getGameProfile().getName(), worldName, ctx.level().getGameTime()).id;
        boolean firstOfWorld = world != null && !world.hasLife();
        // the very first man and woman are put into a garden planted for them (Gen 2:8)
        boolean eden = !cosmos.firstPairBorn && cosmos.eden == com.josephison.fiatlux.eden.EdenState.NONE;
        com.josephison.fiatlux.civilization.Settlement settlement = world != null ? cosmos.townNear(world.id, ground) : null;
        if (eden) {
            com.josephison.fiatlux.eden.EdenGarden.plant(ctx.level(), ground, world != null ? world.id : null);
            CosmosData.get(server).record(server, ctx.caster().getGameProfile().getName(), "eden",
                    "planted a garden eastward in Eden, and there put the man whom they had formed.");
            ctx.caster().sendSystemMessage(Component.translatable("message.fiatlux.eden.planted").withStyle(st -> st.withColor(0xF5D77A).withItalic(true)));
        }
        if (firstOfWorld) {
            cosmos.breatheLife(world);
            cosmos.lifeGivers.put(world.id, com.mojang.datafixers.util.Pair.of(ctx.caster().getUUID(), ctx.caster().getGameProfile().getName()));
            if (settlement == null && !eden) {
                settlement = new com.josephison.fiatlux.civilization.Settlement(ground);
                settlement.name = world.name;
                settlement.founded = ctx.level().getGameTime();
                com.josephison.fiatlux.civilization.MortalPopulation.build(ctx.level(), settlement, world.era);
                cosmos.addTown(world.id, settlement);
            }
            CosmosData.get(server).record(server, ctx.caster().getGameProfile().getName(), "life",
                    "breathed the breath of life upon " + world.name + ", and mortals became living souls.");
        }
        BlockPos home = settlement != null ? settlement.centre : ground;
        if (firstOfWorld || !cosmos.firstPairBorn) {
            // "male and female created he them" (Gen 1:27)
            boolean first = !cosmos.firstPairBorn;
            com.josephison.fiatlux.civilization.MortalPopulation.bringForth(ctx.level(), ground, home, era, first ? "Adam" : null, true, 24, null, null, faith);
            com.josephison.fiatlux.civilization.MortalPopulation.bringForth(ctx.level(), ground.east(), home, era, first ? "Eve" : null, false, 22, null, null, faith);
            cosmos.firstPairBorn = true;
        } else {
            com.josephison.fiatlux.civilization.MortalPopulation.bringForth(ctx.level(), ground, home, era, null, null, 20, null, null, faith);
        }
        cosmos.setDirty();
        ctx.caster().displayClientMessage(Component.translatable("message.fiatlux.formed_from_dust"), false);
        WorldTicker.syncAll(server);
        return true;
    }

    @Override
    public int glory() {
        return 15;
    }
}
