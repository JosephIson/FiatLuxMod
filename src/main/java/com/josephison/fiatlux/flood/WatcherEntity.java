package com.josephison.fiatlux.flood;

import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.AngelEntity;
import com.josephison.fiatlux.entity.JudgeGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One of the Watchers, "the sons of God" who "saw the daughters of men that they were fair; and they
 * took them wives of all which they chose" (Genesis 6:2). The Book of Enoch tells how two hundred came
 * down on Mount Hermon under Shemihazah, bound by a mutual oath, and taught men forbidden arts (1 Enoch
 * 6-8); Jude 1:6 and 2 Peter 2:4 tell how they were "reserved in everlasting chains under darkness".
 *
 * An angel of the lowest choir, its glory already dimming: it lives among men beside its wife and
 * judges no one. Its Creator may bind it at a stroke; the Flood binds the rest.
 */
public class WatcherEntity extends AngelEntity {
    @Nullable private UUID wife;
    private String wifeName = "";
    private String teaching = "";

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // it has come down to live among men, not to judge them
        goalSelector.removeAllGoals(g -> g instanceof JudgeGoal);
        targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal<?>);
    }

    public void takeWife(UUID wife, String name) {
        this.wife = wife;
        this.wifeName = name;
    }

    @Nullable
    public UUID wife() {
        return wife;
    }

    public String wifeName() {
        return wifeName;
    }

    public void setTeaching(String teaching) {
        this.teaching = teaching;
    }

    public String teaching() {
        return teaching;
    }

    /** A Creator's stroke binds it at once and casts it down (2 Peter 2:4). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer p && level() instanceof ServerLevel sl && !Divinity.get(p).pilgrim()) {
            Nephilim.bind(sl, this, p.getGameProfile().getName());
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (wife != null) tag.putUUID("Wife", wife);
        tag.putString("WifeName", wifeName);
        tag.putString("Teaching", teaching);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wife = tag.hasUUID("Wife") ? tag.getUUID("Wife") : null;
        wifeName = tag.getString("WifeName");
        teaching = tag.getString("Teaching");
    }
}
