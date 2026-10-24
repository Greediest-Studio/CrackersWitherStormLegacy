package com.wdcftgg.witherstormmod.common.gui;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.inventory.WitheredPhlegmContainer;
import com.wdcftgg.witherstormmod.common.tile.WitheredPhlegmTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class ModGuiHandler implements IGuiHandler {
    public static final int WITHERED_PHLEGM = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == WITHERED_PHLEGM && tile instanceof WitheredPhlegmTileEntity) {
            return new WitheredPhlegmContainer(player.inventory, (WitheredPhlegmTileEntity) tile);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == WITHERED_PHLEGM && tile instanceof WitheredPhlegmTileEntity) {
            WitheredPhlegmTileEntity phlegm = (WitheredPhlegmTileEntity) tile;
            return WitherStormMod.proxy.createWitheredPhlegmGui(player, phlegm);
        }
        return null;
    }
}
