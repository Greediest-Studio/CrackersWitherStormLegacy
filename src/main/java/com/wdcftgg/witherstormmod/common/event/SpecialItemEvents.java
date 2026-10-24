package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class SpecialItemEvents {
    private SpecialItemEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attackCommandBlock(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof SupplementalEntities.CommandBlockEntity)
                || event.getEntityPlayer().world.isRemote) return;
        SupplementalEntities.CommandBlockEntity core =
                (SupplementalEntities.CommandBlockEntity) event.getTarget();
        if (core.attackPlayingDeadCore(event.getEntityPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void potTaintedMushroom(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().getBlockState(event.getPos()).getBlock() != Blocks.FLOWER_POT
                || event.getItemStack().isEmpty()
                || event.getItemStack().getItem() != ModBlocks.getItem("tainted_mushroom")) return;
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!(tile instanceof TileEntityFlowerPot)
                || !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) return;

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getWorld().isRemote) return;
        event.getWorld().setBlockState(event.getPos(),
                ModBlocks.get("potted_tainted_mushroom").getDefaultState(), 3);
        event.getEntityPlayer().addStat(StatList.FLOWER_POTTED);
        if (!event.getEntityPlayer().capabilities.isCreativeMode) {
            event.getItemStack().shrink(1);
        }
    }

    @SubscribeEvent
    public static void applySickenedStewEffect(LivingEntityUseItemEvent.Finish event) {
        ItemStack consumed = event.getItem();
        if (consumed.getItem() != Items.MUSHROOM_STEW || event.getEntityLiving().world.isRemote) return;
        NBTTagCompound tag = consumed.getSubCompound("WitherStormMod");
        if (tag == null || !tag.hasKey(SickenedEntities.SickenedMushroomCowEntity.STEW_DURATION_TAG, 99)) return;
        int duration = tag.getInteger(SickenedEntities.SickenedMushroomCowEntity.STEW_DURATION_TAG);
        if (duration > 0) event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.WITHER, duration));
    }

    @SubscribeEvent
    public static void boostSickenedPig(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.getWorld().isRemote
                || !(player.getRidingEntity() instanceof SickenedEntities.SickenedPigEntity)
                || event.getItemStack().getItem() != Items.CARROT_ON_A_STICK) return;
        ItemStack stack = event.getItemStack();
        if (stack.getMaxDamage() - stack.getItemDamage() < 7) return;
        SickenedEntities.SickenedPigEntity pig =
                (SickenedEntities.SickenedPigEntity) player.getRidingEntity();
        if (!pig.boost()) return;

        NBTTagCompound itemTag = stack.getTagCompound();
        stack.damageItem(7, player);
        if (stack.isEmpty()) {
            ItemStack fishingRod = new ItemStack(Items.FISHING_ROD);
            fishingRod.setTagCompound(itemTag);
            player.setHeldItem(event.getHand(), fishingRod);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
