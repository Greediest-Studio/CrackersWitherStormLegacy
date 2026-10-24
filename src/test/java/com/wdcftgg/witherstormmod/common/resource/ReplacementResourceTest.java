package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.common.config.ConfiguredListMatcher;
import com.wdcftgg.witherstormmod.common.init.ModRegistryNames;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReplacementResourceTest {

    private void initialize() throws Exception {
        String archive = System.getenv("WITHERSTORM_TEST_ARCHIVE");
        assumeTrue(archive != null, "外部资源不随仓库分发；设置 WITHERSTORM_TEST_ARCHIVE 后运行资源转换测试。");
        UpstreamResourceArchive.initializeArchive(new File(archive));
    }

    @Test
    void actualArchiveConvertsRetainedCraftingAndStonecuttingRecipes() throws Exception {
        initialize();
        int crafting = 0;
        int stonecutting = 0;
        List<String> entries = UpstreamResourceArchive.listEntries(RecipeResourceConverter.RECIPE_PREFIX, ".json");
        assertEquals(117, entries.size());
        for (String name : entries) {
            try (InputStream stream = UpstreamResourceArchive.open(name)) {
                if (RecipeResourceConverter.convert(name, stream) != null) crafting++;
            }
            try (InputStream stream = UpstreamResourceArchive.open(name)) {
                if (RecipeResourceConverter.convertStonecutting(name, stream) != null) stonecutting++;
            }
        }
        assertTrue(crafting > 0 && crafting < 45);
        assertEquals(13, stonecutting);

        for (String removed : Arrays.asList(
                "golden_apple_stew.json",
                "phasometer.json",
                "amulet.json",
                "amulet_lock.json",
                "super_beacon.json",
                "super_support_beacon.json",
                "tainted_flesh_block.json",
                "command_block_sword_anvil.json")) {
            String path = RecipeResourceConverter.RECIPE_PREFIX + removed;
            if (entries.contains(path)) {
                try (InputStream stream = UpstreamResourceArchive.open(path)) {
                    assertNull(RecipeResourceConverter.convert(path, stream), "Expected null for " + removed);
                }
            }
        }
    }

    @Test
    void registryNamesRetainOnlyExpectedItemsBlocksAndSounds() {
        assertArrayEquals(
                new String[]{"withered_bone", "command_block_book", "withered_nether_star"},
                ModRegistryNames.itemNames());

        assertFalse(ModRegistryNames.isRegisteredBlock("witherstormmod:super_beacon"));
        assertFalse(ModRegistryNames.isRegisteredBlock("witherstormmod:super_support_beacon"));
        assertTrue(ModRegistryNames.isRegisteredBlock("witherstormmod:tainted_flesh_block"));
        assertTrue(ModRegistryNames.isRegisteredBlock("witherstormmod:formidibomb"));
        assertTrue(ModRegistryNames.isRegisteredBlock("witherstormmod:super_tnt"));
        assertTrue(ModRegistryNames.isRegisteredBlock("minecraft:obsidian"));

        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:amulet"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:phasometer"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:formidi_blade"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:eye_of_the_storm"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:golden_apple_stew"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:withered_flesh"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:withered_spider_eye"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:command_block_sword"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:wooden_command_block_hoe"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:super_beacon"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:super_support_beacon"));
        assertFalse(ModRegistryNames.isRegisteredItem("witherstormmod:tainted_hanging_sign"));

        assertTrue(ModRegistryNames.isRegisteredItem("witherstormmod:command_block_book"));
        assertTrue(ModRegistryNames.isRegisteredItem("witherstormmod:withered_bone"));
        assertTrue(ModRegistryNames.isRegisteredItem("witherstormmod:withered_nether_star"));
        assertTrue(ModRegistryNames.isRegisteredItem("witherstormmod:tainted_flesh_block"));
        assertTrue(ModRegistryNames.isRegisteredItem("witherstormmod:formidibomb"));
        assertTrue(ModRegistryNames.isRegisteredItem("minecraft:diamond"));

        assertFalse(ModRegistryNames.isRegisteredSoundName("withered_beacon_activate"));
        assertFalse(ModRegistryNames.isRegisteredSoundName("amulet_bind"));
        assertFalse(ModRegistryNames.isRegisteredSoundName("formidi_blade_charging"));
        assertTrue(ModRegistryNames.isRegisteredSoundName("wither_storm_roar"));
    }

    @Test
    void recipeConverterRejectsRemovedResultsAndRemovedIngredients() throws Exception {
        String beaconRecipe = "{\"type\":\"minecraft:crafting_shaped\","
                + "\"pattern\":[\"###\"],"
                + "\"key\":{\"#\":{\"item\":\"minecraft:obsidian\"}},"
                + "\"result\":{\"item\":\"witherstormmod:super_beacon\"}}";
        assertNull(RecipeResourceConverter.convert(
                "super_beacon.json",
                new ByteArrayInputStream(beaconRecipe.getBytes(StandardCharsets.UTF_8))));

        String fleshBlockRecipe = "{\"type\":\"minecraft:crafting_shaped\","
                + "\"pattern\":[\"###\",\"###\",\"###\"],"
                + "\"key\":{\"#\":{\"item\":\"witherstormmod:withered_flesh\"}},"
                + "\"result\":{\"item\":\"witherstormmod:tainted_flesh_block\"}}";
        assertNull(RecipeResourceConverter.convert(
                "tainted_flesh_block.json",
                new ByteArrayInputStream(fleshBlockRecipe.getBytes(StandardCharsets.UTF_8))));

        String superTntRecipe = "{\"type\":\"minecraft:crafting_shapeless\","
                + "\"ingredients\":[{\"item\":\"minecraft:tnt\"},{\"item\":\"minecraft:gunpowder\"}],"
                + "\"result\":{\"item\":\"witherstormmod:super_tnt\"}}";
        JsonObject kept = RecipeResourceConverter.convert(
                "super_tnt.json",
                new ByteArrayInputStream(superTntRecipe.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(kept);
        assertEquals("witherstormmod:super_tnt", kept.getAsJsonObject("result").get("item").getAsString());

        JsonElement cureTag = RecipeResourceConverter.convertIngredient(
                JsonParser.parseString("{\"tag\":\"witherstormmod:cure_ingredient\"}"));
        String cureTagStr = cureTag.toString();
        assertTrue(cureTagStr.contains("futuremc:wither_rose"));
        assertTrue(cureTagStr.contains("witherstormmod:withered_bone"));
        assertFalse(cureTagStr.contains("withered_flesh"));
        assertFalse(cureTagStr.contains("withered_spider_eye"));
    }

    @Test
    void lootSoundLanguageAdvancementAndResourcePackFilterRemovedContent() throws Exception {
        String lootJson = "{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:item\",\"name\":\"witherstormmod:withered_flesh\"},"
                + "{\"type\":\"minecraft:item\",\"name\":\"witherstormmod:withered_bone\"}"
                + "]}]}";
        JsonObject convertedLoot = LootTableResourceConverter.convert(
                "entities/sickened_zombie.json",
                new ByteArrayInputStream(lootJson.getBytes(StandardCharsets.UTF_8)));
        String lootStr = convertedLoot.toString();
        assertFalse(lootStr.contains("withered_flesh"));
        assertTrue(lootStr.contains("witherstormmod:withered_bone"));

        String soundsJson = "{\"withered_beacon_activate\":{\"category\":\"blocks\",\"sounds\":[]},"
                + "\"formidi_blade_charging\":{\"category\":\"players\",\"sounds\":[]},"
                + "\"wither_storm_roar\":{\"category\":\"hostile\",\"sounds\":[]}}";
        JsonObject convertedSounds = JsonParser.parseString(
                new String(SoundResourceConverter.convert(soundsJson.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(convertedSounds.has("withered_beacon_activate"));
        assertFalse(convertedSounds.has("formidi_blade_charging"));
        assertTrue(convertedSounds.has("wither_storm_roar"));

        String langJson = "{"
                + "\"block.witherstormmod.super_beacon\":\"Super Beacon\","
                + "\"block.witherstormmod.tainted_flesh_block\":\"Tainted Flesh Block\","
                + "\"item.witherstormmod.amulet\":\"Amulet\","
                + "\"item.witherstormmod.command_block_sword\":\"Command Block Sword\","
                + "\"item.witherstormmod.command_block_book\":\"Command Block Book\","
                + "\"item.witherstormmod.sickened_pig_spawn_egg\":\"Sickened Pig Spawn Egg\","
                + "\"subtitles.witherstormmod.amulet_bind\":\"Amulet binds\","
                + "\"gui.witherstormmod.config.autoSpawnWitherStorm.title\":\"Auto Spawn\","
                + "\"item.minecraft.potion.effect.wither\":\"Potion of Decay\""
                + "}";
        byte[] langBytes = langJson.getBytes(StandardCharsets.UTF_8);
        String convertedLang = new String(
                LanguageResourceConverter.convert(langBytes, langBytes),
                StandardCharsets.UTF_8);
        assertFalse(convertedLang.contains("super_beacon"));
        assertFalse(convertedLang.contains("item.amulet.name"));
        assertFalse(convertedLang.contains("command_block_sword"));
        assertFalse(convertedLang.contains("amulet_bind"));
        assertFalse(convertedLang.contains("autoSpawnWitherStorm"));
        assertFalse(convertedLang.contains("potion.effect.wither="));
        assertTrue(convertedLang.contains("tile.tainted_flesh_block.name=Tainted Flesh Block"));
        assertTrue(convertedLang.contains("item.command_block_book.name=Command Block Book"));
        assertTrue(convertedLang.contains("item.sickened_pig_spawn_egg.name=Sickened Pig Spawn Egg"));

        String amuletAdv = "{\"parent\":\"witherstormmod:main/summon_wither_storm\","
                + "\"criteria\":{\"has\":{\"trigger\":\"minecraft:inventory_changed\","
                + "\"conditions\":{\"items\":[{\"item\":\"witherstormmod:amulet\"}]}}}}";
        assertNull(AdvancementResourceConverter.convert(
                "data/witherstormmod/advancements/main/amulet.json",
                new ByteArrayInputStream(amuletAdv.getBytes(StandardCharsets.UTF_8))));

        JsonObject childAdv = JsonParser.parseString(
                "{\"parent\":\"witherstormmod:main/harbinger_of_cataclysmic_fates\"}").getAsJsonObject();
        Map<String, String> parents = new HashMap<String, String>();
        parents.put("witherstormmod:main/harbinger_of_cataclysmic_fates", "witherstormmod:main/infinite_potential");
        Set<String> keptMain = new HashSet<String>(Collections.singletonList("witherstormmod:main/infinite_potential"));
        AdvancementResourceConverter.rewriteParent(childAdv, keptMain, parents);
        assertEquals("witherstormmod:main/infinite_potential", childAdv.get("parent").getAsString());

        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/blockstates/super_beacon.json"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/block/super_beacon.json"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/blocks/super_beacon_top.png"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/gui/super_beacon.png"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/misc/phasometer_scope.png"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/item/amulet.json"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/items/command_block_sword.png"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/item/wooden_command_block_hoe.json"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/item/withered_flesh.png"));
        assertTrue(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/sounds/block/withered_beacon_activate.ogg"));

        assertFalse(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/textures/painting/amulet.png"));
        assertFalse(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/item/command_block_book.json"));
        assertFalse(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/block/tainted_flesh_block.json"));
        assertFalse(ModRegistryNames.isRemovedAssetResource("assets/witherstormmod/models/item/formidibomb.json"));

        assertFalse(ModelResourceConverter.handles("assets/witherstormmod/models/item/phasometer.json"));
        assertFalse(ModelResourceConverter.handles("assets/witherstormmod/blockstates/super_beacon.json"));
        assertTrue(ModelResourceConverter.handles("assets/witherstormmod/models/item/command_block_book.json"));

        String curedAdv = "{\"display\":{\"icon\":{\"item\":\"witherstormmod:golden_apple_stew\"}},"
                + "\"criteria\":{\"cured\":{\"trigger\":\"witherstormmod:cured_sickened_mob\","
                + "\"conditions\":{}}}}";
        JsonObject convertedCured = AdvancementResourceConverter.convert(
                "data/witherstormmod/advancements/main/cured_sickened_mob.json",
                new ByteArrayInputStream(curedAdv.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(convertedCured);
        assertEquals("minecraft:golden_apple",
                convertedCured.getAsJsonObject("display").getAsJsonObject("icon").get("item").getAsString());
    }

    @Test
    void preciseBossRestrictionsLeaveSickenedOrdinaryMobsAvailable() {
        String[] bosses = {"witherstormmod:wither_storm", "witherstormmod:command_block", "witherstormmod:tentacle"};
        assertTrue(ConfiguredListMatcher.matches("witherstormmod:tentacle", bosses));
        assertFalse(ConfiguredListMatcher.matches("witherstormmod:sickened_cow", bosses));
        assertTrue(ConfiguredListMatcher.matches("modularmachinery:controller", new String[]{"modularmachinery:*"}));
    }
}
