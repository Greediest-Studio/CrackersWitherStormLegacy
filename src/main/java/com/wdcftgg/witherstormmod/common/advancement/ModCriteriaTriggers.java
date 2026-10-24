package com.wdcftgg.witherstormmod.common.advancement;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.util.ResourceLocation;

public final class ModCriteriaTriggers {

    public static final EntityTrigger PLAY_DEAD = entityTrigger("wither_storm_play_dead", "entity");
    public static final EntityTrigger REVIVAL = entityTrigger("wither_storm_revival", "entity");
    public static final EntityTrigger ESCAPE_WITHER_STORM = entityTrigger("escape_wither_storm", "entity");
    public static final EntityTrigger RING_BELL_NEAR_STORM = entityTrigger("ring_bell_near_storm", "entity");
    public static final EntityTrigger NEARLY_KILL_WITHER_STORM = entityTrigger("nearly_kill_wither_storm", "entity");
    public static final CuredSickenedMobTrigger CURED_SICKENED_MOB =
            new CuredSickenedMobTrigger(id("cured_sickened_mob"));
    private static boolean registered;

    private ModCriteriaTriggers() {
    }

    public static synchronized void register() {
        if (registered) return;
        CriteriaTriggers.register(PLAY_DEAD);
        CriteriaTriggers.register(REVIVAL);
        CriteriaTriggers.register(ESCAPE_WITHER_STORM);
        CriteriaTriggers.register(RING_BELL_NEAR_STORM);
        CriteriaTriggers.register(NEARLY_KILL_WITHER_STORM);
        CriteriaTriggers.register(CURED_SICKENED_MOB);
        registered = true;
    }

    private static EntityTrigger entityTrigger(String path, String conditionKey) {
        return new EntityTrigger(id(path), conditionKey);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Tags.MOD_ID, path);
    }
}
