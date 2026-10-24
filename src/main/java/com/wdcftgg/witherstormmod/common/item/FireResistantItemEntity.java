package com.wdcftgg.witherstormmod.common.item;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.api.common.registry.CommandBlockTools;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class FireResistantItemEntity extends EntityItem {

    public FireResistantItemEntity(World world, Entity location, ItemStack stack) {
        super(world, location.posX, location.posY, location.posZ, stack.copy());
        motionX = location.motionX;
        motionY = location.motionY;
        motionZ = location.motionZ;
        isImmuneToFire = true;
    }

    public static boolean isFireResistant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (CommandBlockTools.contains(stack)) {
            return true;
        }
        Item item = stack.getItem();
        if (!isLegacyItemImplementation(item)) {
            return false;
        }
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null && item instanceof ItemBlock) {
            registryName = ((ItemBlock) item).getBlock().getRegistryName();
        }
        if (registryName == null || !Tags.MOD_ID.equals(registryName.getNamespace())) return false;
        String name = registryName.getPath();
        return "command_block_book".equals(name)
                || "withered_nether_star".equals(name)
                || "formidibomb".equals(name);
    }

    private static boolean isLegacyItemImplementation(Item item) {
        return item instanceof SimpleItem
                || item instanceof RarityBlockItem
                || item instanceof FormidibombItem;
    }

    public static Entity create(World world, Entity location, ItemStack stack) {
        return new FireResistantItemEntity(world, location, stack);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        ItemStack stack = getItem();
        if (isInvulnerableExceptToBypassDamage(stack)) {
            return source.canHarmInCreative() && super.attackEntityFrom(source, amount);
        }
        return !source.isFireDamage() && super.attackEntityFrom(source, amount);
    }

    private static boolean isInvulnerableExceptToBypassDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (CommandBlockTools.contains(stack)) return true;
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && Tags.MOD_ID.equals(name.getNamespace())
                && "withered_nether_star".equals(name.getPath());
    }
}
