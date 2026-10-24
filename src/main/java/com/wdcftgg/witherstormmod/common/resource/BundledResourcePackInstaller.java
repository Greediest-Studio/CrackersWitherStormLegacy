package com.wdcftgg.witherstormmod.common.resource;

import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;

import java.util.List;

public final class BundledResourcePackInstaller {

    private BundledResourcePackInstaller() {
    }

    @SuppressWarnings("deprecation")
    public static void install() {
        Minecraft minecraft = Minecraft.getMinecraft();
        addDefaultResourcePack(minecraft);

        minecraft.refreshResources();
        WitherStormMod.LOGGER.info("Mounted bundled Wither Storm resources");
    }

    private static void addDefaultResourcePack(Minecraft minecraft) {
        List<IResourcePack> defaultPacks = minecraft.defaultResourcePacks;
        defaultPacks.removeIf(pack -> pack instanceof BundledResourcePack);

        // 默认包按倒序查找，让原有 assets/ 修正与玩家资源包优先。
        defaultPacks.add(0, new BundledResourcePack());
    }
}
