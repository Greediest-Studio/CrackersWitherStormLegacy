package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.tile.FireworkBundleTileEntity;
import com.wdcftgg.witherstormmod.common.tile.TaintedSignTileEntity;
import com.wdcftgg.witherstormmod.common.tile.FormidibombTileEntity;
import com.wdcftgg.witherstormmod.common.tile.WitheredPhlegmTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModTileEntities {

    private ModTileEntities() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(FireworkBundleTileEntity.class, new ResourceLocation(Tags.MOD_ID, "firework_bundle"));
        GameRegistry.registerTileEntity(TaintedSignTileEntity.class, new ResourceLocation(Tags.MOD_ID, "tainted_sign"));
        GameRegistry.registerTileEntity(FormidibombTileEntity.class, new ResourceLocation(Tags.MOD_ID, "formidibomb"));
        GameRegistry.registerTileEntity(WitheredPhlegmTileEntity.class,
                new ResourceLocation(Tags.MOD_ID, "withered_phlegm"));
    }
}
