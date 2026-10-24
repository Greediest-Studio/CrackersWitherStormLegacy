package com.wdcftgg.witherstormmod.common.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.LootTableResourceConverter;
import com.wdcftgg.witherstormmod.common.resource.BundledResources;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ExternalLootTables {

    private static final int EXPECTED_LOOT_TABLE_COUNT = 14;
    private static final Gson LOOT_GSON = new GsonBuilder()
            .registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer())
            .registerTypeAdapter(LootPool.class, new LootPool.Serializer())
            .registerTypeAdapter(LootTable.class, new LootTable.Serializer())
            .registerTypeHierarchyAdapter(LootEntry.class, new LootEntry.Serializer())
            .registerTypeHierarchyAdapter(LootFunction.class, new LootFunctionManager.Serializer())
            .registerTypeHierarchyAdapter(LootCondition.class, new LootConditionManager.Serializer())
            .registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer())
            .create();
    private static volatile Map<ResourceLocation, String> convertedTables = Collections.emptyMap();

    private ExternalLootTables() {
    }

    public static synchronized void initialize() {
        if (!convertedTables.isEmpty()) return;
        Map<ResourceLocation, String> loaded = new LinkedHashMap<ResourceLocation, String>();
        try {
            for (String entryName : BundledResources.listEntries(
                    LootTableResourceConverter.LOOT_TABLE_PREFIX, ".json")) {
                String relative = entryName.substring(LootTableResourceConverter.LOOT_TABLE_PREFIX.length(),
                        entryName.length() - ".json".length());
                if (!relative.startsWith("entities/") && !relative.startsWith("chests/")) continue;
                JsonObject converted;
                try (InputStream stream = BundledResources.open(entryName)) {
                    converted = LootTableResourceConverter.convert(entryName, stream);
                }
                ResourceLocation name = new ResourceLocation(Tags.MOD_ID, relative);
                if (loaded.put(name, LootTableResourceConverter.serialize(converted)) != null) {
                    throw new IllegalStateException("Duplicate bundled loot table " + name);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load loot tables from the bundled Wither Storm resources", exception);
        }
        if (loaded.size() != EXPECTED_LOOT_TABLE_COUNT) {
            throw new IllegalStateException("Expected " + EXPECTED_LOOT_TABLE_COUNT
                    + " bundled loot tables but loaded " + loaded.size());
        }
        convertedTables = Collections.unmodifiableMap(loaded);
        WitherStormMod.LOGGER.info("Prepared {} loot tables from the bundled Wither Storm resources", loaded.size());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void replacePlaceholderTable(LootTableLoadEvent event) {
        String json = convertedTables.get(event.getName());
        if (json == null) return;
        LootTable table = ForgeHooks.loadLootTable(
                LOOT_GSON, event.getName(), json, true, event.getLootTableManager());
        event.setTable(table);
    }

    static Map<ResourceLocation, String> convertedTablesForTesting() {
        return convertedTables;
    }

    static Gson lootGsonForTesting() {
        return LOOT_GSON;
    }
}
