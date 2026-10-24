package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.common.init.ModRegistryNames;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class BundledResourcePack implements IResourcePack {

    private static final byte[] PACK_METADATA = ("{\"pack\":{\"pack_format\":3,\"description\":\"Cracker's Wither Storm Legacy upstream resources\"}}")
            .getBytes(StandardCharsets.UTF_8);
    private static final String[] PADDED_TEXTURES = {"flesh_skele", "flesh_skull_e", "flesh_zomb", "flesh_zomb_e"};

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        return getInputStreamByName("assets/" + location.getNamespace() + "/" + location.getPath());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return hasResourceName("assets/" + location.getNamespace() + "/" + location.getPath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return Set.of("minecraft", "witherstormmod");
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return serializer.parseMetadataSection(section,
                JsonParser.parseString(new String(PACK_METADATA, StandardCharsets.UTF_8)).getAsJsonObject());
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "Wither Storm bundled resources";
    }

    protected InputStream getInputStreamByName(String name) throws IOException {
        if (ModRegistryNames.isRemovedAssetResource(name)) {
            throw new FileNotFoundException(name);
        }
        if ("pack.mcmeta".equals(name)) {
            return new ByteArrayInputStream(PACK_METADATA);
        }
        if (ModelResourceConverter.handles(name)) {
            String sourceName = ModelResourceConverter.sourceName(name);
            try (InputStream source = BundledResources.open(sourceName)) {
                return new ByteArrayInputStream(
                        ModelResourceConverter.convert(name, source.readAllBytes()));
            }
        }
        if (LanguageResourceConverter.handles(name)) {
            byte[] localized = readResource(LanguageResourceConverter.sourceName(name));
            byte[] english = readResource(LanguageResourceConverter.englishSourceName());
            return new ByteArrayInputStream(LanguageResourceConverter.convert(localized, english));
        }
        if (SoundResourceConverter.handles(name)) {
            return new ByteArrayInputStream(SoundResourceConverter.convert(readResource(name)));
        }
        if (isModernDefinition(name)) {
            throw new FileNotFoundException(name);
        }
        String mappedName = mapLegacyTexturePath(name);
        if (isLegacyParticleTexture(mappedName)) {
            InputStream normalized = normalizedParticleTexture(mappedName);
            if (normalized != null) {
                return normalized;
            }
        }
        if (isBlockTexture(mappedName)) {
            InputStream normalized = normalizedBlockTexture(mappedName);
            if (normalized != null) {
                return normalized;
            }
        }
        return BundledResources.open(mappedName);
    }

    public boolean hasResourceName(String name) {
        if (ModRegistryNames.isRemovedAssetResource(name)) {
            return false;
        }
        if ("pack.mcmeta".equals(name)) {
            return true;
        }
        if (ModelResourceConverter.handles(name)) {
            return BundledResources.contains(ModelResourceConverter.sourceName(name));
        }
        if (LanguageResourceConverter.handles(name)) {
            return BundledResources.contains(LanguageResourceConverter.sourceName(name))
                    && BundledResources.contains(LanguageResourceConverter.englishSourceName());
        }
        if (SoundResourceConverter.handles(name)) {
            return BundledResources.contains(name);
        }
        if (isModernDefinition(name)) {
            return false;
        }
        String mappedName = mapLegacyTexturePath(name);
        if (isLegacyParticleTexture(mappedName)) {
            return hasResourceNameDirect(mappedName);
        }
        if (isBlockTexture(mappedName) && hasNormalizedBlockTexture(mappedName)) {
            return true;
        }
        return BundledResources.contains(mappedName);
    }

    private InputStream normalizedBlockTexture(String name) throws IOException {
        if (name.endsWith(".mcmeta") || !hasResourceNameDirect(name)) {
            return null;
        }
        String metadataName = name + ".mcmeta";
        if (hasResourceNameDirect(metadataName)) {
            return null;
        }
        BufferedImage image;
        try (InputStream source = BundledResources.open(name)) {
            image = ImageIO.read(source);
        }
        if (image == null) {
            return null;
        }
        int largestDimension = Math.max(image.getWidth(), image.getHeight());
        int targetSize = 1;
        while (targetSize < largestDimension) {
            targetSize <<= 1;
        }
        if (image.getWidth() == targetSize && image.getHeight() == targetSize) {
            image.flush();
            return BundledResources.open(name);
        }
        BufferedImage normalized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (isPaddedStaticTexture(name)) {
                graphics.drawImage(image, 0, 0, null);
            } else {
                graphics.drawImage(image, 0, 0, targetSize, targetSize, null);
            }
        } finally {
            graphics.dispose();
            image.flush();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            normalized.flush();
        }
    }

    private InputStream normalizedParticleTexture(String name) throws IOException {
        if (!hasResourceNameDirect(name)) {
            return null;
        }
        BufferedImage image;
        try (InputStream source = BundledResources.open(name)) {
            image = ImageIO.read(source);
        }
        if (image == null || image.getWidth() <= 0 || image.getHeight() % image.getWidth() != 0) {
            if (image != null) image.flush();
            return BundledResources.open(name);
        }

        int frameCount = image.getHeight() / image.getWidth();
        int frameSize = Math.max(16, nextPowerOfTwo(image.getWidth()));
        if (image.getWidth() == frameSize) {
            image.flush();
            return BundledResources.open(name);
        }

        BufferedImage normalized = new BufferedImage(
                frameSize, frameSize * frameCount, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(image, 0, 0, normalized.getWidth(), normalized.getHeight(), null);
        } finally {
            graphics.dispose();
            image.flush();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            normalized.flush();
        }
    }

    private boolean hasNormalizedBlockTexture(String name) {
        if (!hasResourceNameDirect(name)) {
            return false;
        }
        return !hasResourceNameDirect(name + ".mcmeta");
    }

    private boolean hasResourceNameDirect(String name) {
        return BundledResources.contains(name);
    }

    private static boolean isBlockTexture(String name) {
        return name.startsWith("assets/")
                && (name.contains("/textures/blocks/") || name.contains("/textures/block/"))
                && name.endsWith(".png");
    }

    private static boolean isLegacyParticleTexture(String name) {
        if (!name.startsWith("assets/witherstormmod/textures/particle/") || !name.endsWith(".png")) {
            return false;
        }
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        return "command_block.png".equals(fileName)
                || "command_block_1.png".equals(fileName)
                || "command_block_2.png".equals(fileName)
                || "command_block_3.png".equals(fileName)
                || "phlegm.png".equals(fileName)
                || "tractor_beam.png".equals(fileName);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) result <<= 1;
        return result;
    }

    private static String mapLegacyTexturePath(String name) {
        return name.replace("/textures/blocks/", "/textures/block/")
                .replace("/textures/items/", "/textures/item/");
    }

    private static boolean isPaddedStaticTexture(String name) {
        int separator = name.lastIndexOf('/');
        String base = separator < 0 ? name : name.substring(separator + 1);
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        for (String padded : PADDED_TEXTURES) {
            if (padded.equals(base)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isModernDefinition(String name) {
        return name.startsWith("assets/witherstormmod/lang/")
                || name.startsWith("assets/minecraft/models/")
                || name.startsWith("assets/minecraft/blockstates/");
    }

    private byte[] readResource(String name) throws IOException {
        try (InputStream input = BundledResources.open(name)) {
            return input.readAllBytes();
        }
    }
}
