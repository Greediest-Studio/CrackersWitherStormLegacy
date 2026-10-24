package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.common.config.ConfiguredListMatcher;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReplacementResourceTest {
    private void initialize() throws Exception {
        String archive = System.getenv("WITHERSTORM_TEST_ARCHIVE");
        assumeTrue(archive != null, "外部资源不随仓库分发；设置 WITHERSTORM_TEST_ARCHIVE 后运行资源转换测试。");
        UpstreamResourceArchive.initializeArchive(new File(archive));
    }

    @Test
    void actualArchiveConvertsAllCraftingStonecuttingAndAnvilRecipes() throws Exception {
        initialize();
        int crafting = 0;
        int stonecutting = 0;
        int anvil = 0;
        List<String> entries = UpstreamResourceArchive.listEntries(RecipeResourceConverter.RECIPE_PREFIX, ".json");
        assertEquals(117, entries.size());
        for (String name : entries) {
            try (InputStream stream = UpstreamResourceArchive.open(name)) {
                if (RecipeResourceConverter.convert(name, stream) != null) crafting++;
            }
            try (InputStream stream = UpstreamResourceArchive.open(name)) {
                if (RecipeResourceConverter.convertStonecutting(name, stream) != null) stonecutting++;
            }
            try (InputStream stream = UpstreamResourceArchive.open(name)) {
                if (RecipeResourceConverter.convertAnvil(name, stream) != null) anvil++;
            }
        }
        assertEquals(45, crafting);
        assertEquals(13, stonecutting);
        assertEquals(25, anvil);
    }

    @Test
    void firstKillRecipesUseAvailableLegacyInputs() throws Exception {
        initialize();
        JsonObject stew;
        try (InputStream stream = UpstreamResourceArchive.open(RecipeResourceConverter.RECIPE_PREFIX + "golden_apple_stew.json")) {
            stew = RecipeResourceConverter.convert("golden_apple_stew", stream);
        }
        assertTrue(stew.toString().contains("futuremc:suspicious_stew"));
        JsonObject phasometer;
        try (InputStream stream = UpstreamResourceArchive.open(RecipeResourceConverter.RECIPE_PREFIX + "phasometer.json")) {
            phasometer = RecipeResourceConverter.convert("phasometer", stream);
        }
        assertTrue(phasometer.toString().contains("minecraft:glass_bottle"));
        JsonObject sword;
        try (InputStream stream = UpstreamResourceArchive.open(RecipeResourceConverter.RECIPE_PREFIX + "command_block_sword_anvil.json")) {
            sword = RecipeResourceConverter.convertAnvil("command_block_sword", stream);
        }
        assertTrue(sword.toString().contains("minecraft:diamond_sword"));
        assertTrue(sword.toString().contains("witherstormmod:command_block_book"));
        assertEquals(1, sword.get("cost").getAsInt());
    }

    @Test
    void preciseBossRestrictionsLeaveSickenedOrdinaryMobsAvailable() {
        String[] bosses = {"witherstormmod:wither_storm", "witherstormmod:command_block", "witherstormmod:tentacle"};
        assertTrue(ConfiguredListMatcher.matches("witherstormmod:tentacle", bosses));
        assertFalse(ConfiguredListMatcher.matches("witherstormmod:sickened_cow", bosses));
        assertTrue(ConfiguredListMatcher.matches("modularmachinery:controller", new String[]{"modularmachinery:*"}));
    }
}
