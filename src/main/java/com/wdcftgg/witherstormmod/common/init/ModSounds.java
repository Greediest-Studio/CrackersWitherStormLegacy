package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModSounds {
    private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<String, SoundEvent>();
    private static final String[] NAMES = ModRegistryNames.soundNames();

    static {
        for (String name : NAMES) {
            ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
            SOUNDS.put(name, new SoundEvent(id).setRegistryName(id));
        }
    }

    private ModSounds() {
    }

    public static void bootstrap() {
    }

    public static SoundEvent get(String name) {
        return SOUNDS.get(name);
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(SOUNDS.values().toArray(new SoundEvent[0]));
    }
}
