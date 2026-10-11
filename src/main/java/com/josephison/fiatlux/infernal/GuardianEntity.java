package com.josephison.fiatlux.infernal;

import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.MortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * A guardian angel, sent when the Creator says "Go forth and watch over the planets": "Are they not
 * all ministering spirits, sent forth to minister for them who shall be heirs of salvation?" (Heb
 * 1:14); "For he shall give his angels charge over thee, to keep thee in all thy ways" (Ps 91:11).
 *
 * Each keeps one town. No unclean spirit can enter those near it; it seals rifts that open within its
 * sight and casts the spirits that come up out of them into the deep; and it seeks out the faithful
 * of the town and anoints them to cast out devils in the name of their Creator (Mark 16:17), so that
 * the town has those who can set the possessed free when the angel is elsewhere.
 */
public class GuardianEntity extends AngelEntity {
    /** How many the angel anoints in one town. */
    public static final int EXORCISTS_PER_TOWN = 2;
    @Nullable private BlockPos town;
    private String townName = "";
    private int anointTicks;

    public GuardianEntity(EntityType<? extends GuardianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public void keep(BlockPos town, String name) {
        this.town = town;
        this.townName = name;
        setStation(town.above(2));
    }

    @Nullable
    public BlockPos town() {
        return town;
    }

    public String townName() {
        return townName;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl) || town == null || tickCount % 20 != 5) return;
        // rifts within its sight: it goes to seal them (the rift seals itself when a holy angel stands by)
        RiftEntity rift = sl.getEntitiesOfClass(RiftEntity.class, new AABB(town).inflate(96), r -> !r.closing()).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (rift != null) {
            setStation(rift.blockPosition().above());
            return;
        }
        setStation(town.above(2));
        if (++anointTicks % 6 == 0) seekTheFaithful(sl);
    }

    /** Anoints one of the faithful of its town, if it has fewer than it should who can cast out devils. */
    private void seekTheFaithful(ServerLevel level) {
        AABB area = new AABB(town).inflate(64);
        long have = level.getEntitiesOfClass(MortalEntity.class, area, MortalEntity::isExorcist).size();
        if (have >= EXORCISTS_PER_TOWN) return;
        MortalEntity chosen = level.getEntitiesOfClass(MortalEntity.class, area, GuardianEntity::worthy).stream()
                .max(Comparator.comparingDouble(m -> Divinity.alignment(m).virtue() - Divinity.alignment(m).sin() - m.doubt() * 10)).orElse(null);
        if (chosen == null) return;
        if (distanceToSqr(chosen) > 9) {
            Vec3 at = chosen.position().add(0, 2.5, 0);
            moveTo(at.x, at.y, at.z, getYRot(), getXRot());   // it is there in an instant, as at Gideon's threshing floor (Judg 6:11)
        }
        getLookControl().setLookAt(chosen, 30f, 30f);
        chosen.anoint(this);
    }

    /** One fit to be anointed: grown, of the true faith, steadfast, more virtue than sin, free of any spirit. */
    public static boolean worthy(MortalEntity m) {
        if (!m.isAlive() || m.isChild() || m.isExorcist() || m.isPossessed() || m.isNephil() || m.doubt() > 0.35) return false;
        Religion r = Faiths.of(m);
        Alignment a = Divinity.alignment(m);
        return r != null && r.deity.true_ && a.virtue() >= a.sin();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (town != null) tag.put("Town", NbtUtils.writeBlockPos(town));
        tag.putString("TownName", townName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        town = NbtUtils.readBlockPos(tag, "Town").orElse(null);
        townName = tag.getString("TownName");
    }
}
