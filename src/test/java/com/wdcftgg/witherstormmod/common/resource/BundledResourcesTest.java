package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.common.init.ModRegistryNames;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BundledResourcesTest {
    private static final String ASSETS = "assets/witherstormmod/";

    @Test
    void everyIndexedResourceIsReadableWithoutAnExternalArchive() throws Exception {
        var entries = BundledResources.listEntries("", "");
        assertFalse(entries.isEmpty());
        for (String name : entries) {
            assertTrue(name.startsWith("assets/") || name.startsWith("data/"), name);
            assertFalse(name.endsWith(".class") || name.endsWith(".jar"), name);
            try (InputStream stream = BundledResources.open(name)) {
                assertTrue(stream.read() >= 0, name);
            }
        }
        assertEquals(52, BundledResources.listEntries("data/witherstormmod/tainting/block/", ".json").size());
        assertEquals(22, BundledResources.listEntries("data/witherstormmod/tainting/entity/", ".json").size());
        assertTrue(BundledResources.contains("assets/minecraft/textures/entity/signs/tainted.png"));
        assertTrue(BundledResources.contains("assets/minecraft/textures/entity/signs/hanging/tainted.png"));
        assertFalse(BundledResources.contains(ASSETS + "textures/item/amulet.png"));
        assertFalse(BundledResources.contains(ASSETS + "textures/entity/boat/tainted_boat.png"));
    }

    @Test
    void registeredModelsAndConvertedDependenciesArePresent() throws Exception {
        for (String name : ModRegistryNames.itemNames()) require(ASSETS + "models/item/" + name + ".json");
        for (String name : ModRegistryNames.blockNames()) {
            // 悬挂告示牌由 TileEntity 渲染，没有 JSON 方块模型。
            if (name.equals("tainted_hanging_sign") || name.equals("tainted_wall_hanging_sign")) continue;
            require(ASSETS + "blockstates/" + name + ".json");
            if (!ModRegistryNames.isItemlessBlock(name)) require(ASSETS + "models/item/" + name + ".json");
        }
        Set<String> reachableModels = new HashSet<>();
        for (String path : BundledResources.listEntries(ASSETS + "models/item/", ".json")) visitModel(path, reachableModels);
        for (String path : BundledResources.listEntries(ASSETS + "blockstates/", ".json")) {
            visitState(convertModel(path), reachableModels);
        }
        assertEquals(new HashSet<>(BundledResources.listEntries(ASSETS + "models/", ".json")), reachableModels);
    }

    private static void visitState(JsonElement value, Set<String> reachable) throws Exception {
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) visitState(child, reachable);
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("model")) {
                String id = object.get("model").getAsString();
                if (id.startsWith("witherstormmod:")) {
                    String name = id.substring("witherstormmod:".length());
                    if (!name.startsWith("block/")) name = "block/" + name;
                    visitModel(ASSETS + "models/" + name + ".json", reachable);
                }
            }
            for (JsonElement child : object.asMap().values()) visitState(child, reachable);
        }
    }

    private static void visitModel(String path, Set<String> reachable) throws Exception {
        if (!reachable.add(path)) return;
        require(path);
        JsonObject model = convertModel(path);
        if (model.has("parent")) {
            String id = model.get("parent").getAsString();
            if (id.startsWith("witherstormmod:")) visitModel(ASSETS + "models/" + id.split(":", 2)[1] + ".json", reachable);
        }
        if (model.has("textures")) {
            for (JsonElement value : model.getAsJsonObject("textures").asMap().values()) {
                String id = value.getAsString();
                if (!id.startsWith("witherstormmod:")) continue;
                String texture = id.split(":", 2)[1].replace("blocks/", "block/").replace("items/", "item/");
                require(ASSETS + "textures/" + texture + ".png");
            }
        }
    }

    @Test
    void registeredSoundsResolveToBundledAudio() throws Exception {
        JsonObject sounds = JsonParser.parseString(new String(SoundResourceConverter.convert(read(ASSETS + "sounds.json")), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(new HashSet<>(Arrays.asList(ModRegistryNames.soundNames())), sounds.keySet());
        Set<String> audio = new HashSet<>();
        for (JsonElement value : sounds.asMap().values()) {
            for (JsonElement sound : value.getAsJsonObject().getAsJsonArray("sounds")) {
                JsonObject object = sound.isJsonObject() ? sound.getAsJsonObject() : null;
                String id = object == null ? sound.getAsString() : object.get("name").getAsString();
                if (object != null && object.has("type") && "event".equals(object.get("type").getAsString())) {
                    if (id.startsWith("witherstormmod:")) assertTrue(sounds.has(id.split(":", 2)[1]), id);
                } else if (!id.contains(":") || id.startsWith("witherstormmod:")) {
                    String path = ASSETS + "sounds/" + id.replace("witherstormmod:", "") + ".ogg";
                    require(path);
                    assertEquals("OggS", new String(read(path), 0, 4, StandardCharsets.US_ASCII));
                    audio.add(path);
                }
            }
        }
        assertEquals(new HashSet<>(BundledResources.listEntries(ASSETS + "sounds/", ".ogg")), audio);
    }

    @Test
    void recipesLootAndAdvancementsStillConvert() throws Exception {
        int crafting = 0;
        int stonecutting = 0;
        for (String name : BundledResources.listEntries(RecipeResourceConverter.RECIPE_PREFIX, ".json")) {
            try (InputStream stream = BundledResources.open(name)) {
                if (RecipeResourceConverter.convert(name, stream) != null) crafting++;
            }
            try (InputStream stream = BundledResources.open(name)) {
                if (RecipeResourceConverter.convertStonecutting(name, stream) != null) stonecutting++;
            }
        }
        assertEquals(37, crafting);
        assertEquals(13, stonecutting);
        var lootTables = BundledResources.listEntries(LootTableResourceConverter.LOOT_TABLE_PREFIX, ".json");
        assertEquals(14, lootTables.size());
        for (String name : lootTables) {
            try (InputStream stream = BundledResources.open(name)) {
                assertNotNull(LootTableResourceConverter.convert(name, stream));
            }
        }
        for (String name : BundledResources.listEntries(AdvancementResourceConverter.ADVANCEMENT_PREFIX, ".json")) {
            try (InputStream stream = BundledResources.open(name)) {
                JsonObject converted = name.contains("/main/")
                        ? AdvancementResourceConverter.convert(name, stream)
                        : AdvancementResourceConverter.convertRecipeAdvancement(name, stream);
                assertNotNull(converted, name);
                if (converted.has("parent")) {
                    String parent = converted.get("parent").getAsString();
                    if (parent.startsWith("witherstormmod:")) require(AdvancementResourceConverter.ADVANCEMENT_PREFIX + parent.split(":", 2)[1] + ".json");
                }
            }
        }
    }

    @Test
    void allLanguagesConvertAndRemovedCosmeticsStayAbsent() throws Exception {
        byte[] english = read(ASSETS + "lang/en_us.json");
        for (String name : BundledResources.listEntries(ASSETS + "lang/", ".json")) {
            String legacy = new String(LanguageResourceConverter.convert(read(name), english), StandardCharsets.UTF_8);
            assertTrue(legacy.contains("tile.formidibomb.name="), name);
            assertFalse(legacy.contains("patronCosmetic"), name);
        }
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.wdcftgg.witherstormmod.client.util.Contributors"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.wdcftgg.witherstormmod.client.util.UpstreamSplashes"));
    }

    @Test
    void clientPackConvertsTexturesAndAllowsOverrides(@TempDir Path directory) throws Exception {
        BundledResourcePack pack = new BundledResourcePack();
        ResourceLocation particle = new ResourceLocation("witherstormmod", "textures/particle/command_block.png");
        assertTrue(pack.resourceExists(particle));
        try (InputStream stream = pack.getInputStream(particle)) {
            var texture = javax.imageio.ImageIO.read(stream);
            assertTrue(texture.getWidth() >= 16);
            assertEquals(0, texture.getHeight() % texture.getWidth());
        }
        ResourceLocation block = new ResourceLocation("witherstormmod", "textures/blocks/flesh_skele.png");
        try (InputStream stream = pack.getInputStream(block)) {
            var texture = javax.imageio.ImageIO.read(stream);
            assertEquals(texture.getWidth(), texture.getHeight());
            assertEquals(0, texture.getWidth() & (texture.getWidth() - 1));
        }
        ResourceLocation lang = new ResourceLocation("witherstormmod", "lang/en_us.lang");
        Path override = directory.resolve("assets/witherstormmod/lang/en_us.lang");
        Files.createDirectories(override.getParent());
        Files.writeString(override, "tile.formidibomb.name=override\n");
        var manager = new SimpleReloadableResourceManager(new MetadataSerializer());
        manager.reloadResources(java.util.List.of(pack, new FolderResourcePack(directory.toFile())));
        try (var resource = manager.getResource(lang)) {
            assertEquals(Files.readString(override), new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
        try (var resource = manager.getResource(particle)) {
            assertNotNull(javax.imageio.ImageIO.read(resource.getInputStream()));
        }
    }

    @Test
    void isolatedJarClassLoaderReadsTheSameResources(@TempDir Path directory) throws Exception {
        Path jar = directory.resolve("bundled.jar");
        String classPath = BundledResources.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(classPath));
            try (InputStream source = BundledResources.class.getResourceAsStream("/" + classPath)) { source.transferTo(output); }
            output.closeEntry();
            var paths = new java.util.ArrayList<>(BundledResources.listEntries("", ""));
            paths.add("index.txt");
            for (String path : paths) {
                output.putNextEntry(new JarEntry("upstream/" + path));
                try (InputStream source = BundledResources.class.getResourceAsStream("/upstream/" + path)) { source.transferTo(output); }
                output.closeEntry();
            }
        }
        // 只有这个临时 JAR 和 JDK 可见，不能回退到项目目录或原版 JAR。
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> resources = loader.loadClass(BundledResources.class.getName());
            var open = resources.getMethod("open", String.class);
            for (String path : BundledResources.listEntries("", "")) {
                try (InputStream input = (InputStream) open.invoke(null, path)) {
                    assertArrayEquals(read(path), input.readAllBytes(), path);
                }
            }
        }
    }

    private static void require(String name) {
        assertTrue(BundledResources.contains(name), "缺少资源依赖：" + name);
    }

    private static byte[] read(String name) throws Exception {
        try (InputStream stream = BundledResources.open(name)) { return stream.readAllBytes(); }
    }

    private static JsonObject convertModel(String name) throws Exception {
        return JsonParser.parseString(new String(ModelResourceConverter.convert(name, read(name)), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
