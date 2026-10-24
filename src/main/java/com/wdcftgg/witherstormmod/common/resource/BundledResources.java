package com.wdcftgg.witherstormmod.common.resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 同时支持开发目录和发布 JAR；索引由构建任务根据资源目录自动生成。 */
public final class BundledResources {
    private static final String ROOT = "/upstream/";
    private static volatile Set<String> entryNames;

    private BundledResources() {
    }

    public static synchronized void initialize() throws IOException {
        if (entryNames != null) return;
        Set<String> names = new LinkedHashSet<>();
        try (InputStream stream = openClasspath("index.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!line.isEmpty()) names.add(line);
            }
        }
        if (names.isEmpty()) throw new IOException("The bundled resource index is empty");
        entryNames = Collections.unmodifiableSet(names);
    }

    public static InputStream open(String name) throws IOException {
        initialize();
        if (!entryNames.contains(name)) throw new FileNotFoundException("Missing bundled resource: " + name);
        // 声音保持流式读取，避免把所有音频永久缓存到内存。
        return openClasspath(name);
    }

    public static boolean contains(String name) {
        try {
            initialize();
            return entryNames.contains(name);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize bundled Wither Storm resources", exception);
        }
    }

    public static List<String> listEntries(String prefix, String suffix) throws IOException {
        initialize();
        return entryNames.stream().filter(name -> name.startsWith(prefix) && name.endsWith(suffix)).toList();
    }

    private static InputStream openClasspath(String name) throws IOException {
        InputStream stream = BundledResources.class.getResourceAsStream(ROOT + name);
        if (stream == null) throw new FileNotFoundException("Missing bundled resource: " + name);
        return stream;
    }
}
