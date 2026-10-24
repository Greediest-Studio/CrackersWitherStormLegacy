package com.wdcftgg.witherstormmod.common.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.AdvancementResourceConverter;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ExternalAdvancements {

    private static final int MIN_EXPECTED_MAIN_ADVANCEMENTS = 10;

    private ExternalAdvancements() {
    }

    public static int install(MinecraftServer server) throws IOException {
        File advancementDirectory = server.getActiveAnvilConverter().getFile(
                server.getFolderName(), "data/advancements");
        return install(advancementDirectory);
    }

    public static int install(File advancementDirectory) throws IOException {
        File modAdvancementDir = new File(advancementDirectory, Tags.MOD_ID);
        Map<String, String> originalParents = new LinkedHashMap<String, String>();
        Map<String, JsonObject> convertedMain = new LinkedHashMap<String, JsonObject>();
        Set<String> keptMainIds = new LinkedHashSet<String>();

        for (String relativeName : AdvancementResourceConverter.ALL_MAIN_ADVANCEMENTS) {
            String entryName = AdvancementResourceConverter.sourceEntry(relativeName);
            byte[] rawBytes;
            try (InputStream source = UpstreamResourceArchive.open(entryName)) {
                rawBytes = source.readAllBytes();
            }
            String id = AdvancementResourceConverter.advancementId(relativeName);
            String parent = AdvancementResourceConverter.readParent(
                    entryName, new ByteArrayInputStream(rawBytes));
            if (parent != null) {
                originalParents.put(id, parent);
            }
            JsonObject converted = AdvancementResourceConverter.convert(
                    entryName, new ByteArrayInputStream(rawBytes));
            if (converted != null) {
                keptMainIds.add(id);
                convertedMain.put(relativeName, converted);
            }
        }

        if (convertedMain.size() < MIN_EXPECTED_MAIN_ADVANCEMENTS) {
            throw new IOException("Expected at least " + MIN_EXPECTED_MAIN_ADVANCEMENTS
                    + " main advancements but prepared " + convertedMain.size());
        }

        Map<File, GeneratedAdvancement> generated = new LinkedHashMap<File, GeneratedAdvancement>();
        for (Map.Entry<String, JsonObject> entry : convertedMain.entrySet()) {
            String relativeName = entry.getKey();
            JsonObject converted = entry.getValue();
            AdvancementResourceConverter.rewriteParent(converted, keptMainIds, originalParents);
            String entryName = AdvancementResourceConverter.sourceEntry(relativeName);
            File target = new File(modAdvancementDir, relativeName);
            generated.put(target, new GeneratedAdvancement(entryName,
                    AdvancementResourceConverter.serialize(converted)));
        }

        for (String entryName : UpstreamResourceArchive.listEntries(
                AdvancementResourceConverter.ADVANCEMENT_PREFIX, ".json")) {
            String relativeName = entryName.substring(
                    AdvancementResourceConverter.ADVANCEMENT_PREFIX.length());
            if (!relativeName.startsWith("recipes/")) continue;
            JsonObject converted;
            try (InputStream source = UpstreamResourceArchive.open(entryName)) {
                converted = AdvancementResourceConverter.convertRecipeAdvancement(entryName, source);
            }
            if (converted == null) continue;
            File target = new File(modAdvancementDir, relativeName);
            generated.put(target, new GeneratedAdvancement(entryName,
                    AdvancementResourceConverter.serialize(converted)));
        }

        for (Map.Entry<File, GeneratedAdvancement> entry : generated.entrySet()) {
            verifyWritable(entry.getKey(), entry.getValue().sourceEntry);
        }
        removeStaleGeneratedFiles(modAdvancementDir, generated.keySet());
        for (Map.Entry<File, GeneratedAdvancement> entry : generated.entrySet()) {
            writeAtomically(entry.getKey(), entry.getValue().json);
        }
        WitherStormMod.LOGGER.info("Prepared {} advancements from the external Wither Storm archive",
                generated.size());
        return generated.size();
    }

    private static void removeStaleGeneratedFiles(File directory, Set<File> activeTargets)
            throws IOException {
        if (!directory.isDirectory()) return;
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                removeStaleGeneratedFiles(child, activeTargets);
                File[] remaining = child.listFiles();
                if (remaining != null && remaining.length == 0) {
                    child.delete();
                }
            } else if (child.isFile() && child.getName().endsWith(".json")
                    && !activeTargets.contains(child)) {
                try {
                    String json = Files.readString(child.toPath(), StandardCharsets.UTF_8);
                    JsonObject existing = JsonParser.parseString(json).getAsJsonObject();
                    if (AdvancementResourceConverter.isAnyGeneratedFile(existing)) {
                        Files.deleteIfExists(child.toPath());
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private static void verifyWritable(File target, String sourceEntry) throws IOException {
        if (!target.exists()) return;
        if (!target.isFile()) {
            throw new IOException("Advancement target is not a file: " + target.getAbsolutePath());
        }
        JsonObject existing;
        try {
            String json = Files.readString(target.toPath(), StandardCharsets.UTF_8);
            existing = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Refusing to replace an unreadable custom advancement: "
                    + target.getAbsolutePath(), exception);
        }
        if (!AdvancementResourceConverter.isGeneratedFile(existing, sourceEntry)) {
            throw new IOException("Refusing to replace a custom advancement: "
                    + target.getAbsolutePath());
        }
    }

    private static void writeAtomically(File target, String json) throws IOException {
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Unable to create advancement directory: " + parent.getAbsolutePath());
        }
        Path temporary = Files.createTempFile(parent.toPath(), target.getName(), ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class GeneratedAdvancement {
        private final String sourceEntry;
        private final String json;

        private GeneratedAdvancement(String sourceEntry, String json) {
            this.sourceEntry = sourceEntry;
            this.json = json;
        }
    }
}
