package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class BlockToolEfficiencyEvents {
    private static final Set<String> PICKAXE_BLOCKS = setOf(
            "tainted_glass", "tainted_glass_pane");
    private static final Set<String> AXE_BLOCKS = setOf(
            "tainted_pumpkin", "tainted_carved_pumpkin", "tainted_jack_o_lantern");
    private static final Set<String> HOE_BLOCKS = setOf(
            "tainted_leaves", "tainted_flesh_block", "infected_flesh_block", "firework_bundle",
            "tainted_flesh_veins", "tainted_zombie_sitting", "tainted_zombie_wall",
            "tainted_zombie_lying", "tainted_bone_pile", "tainted_skeleton_wall",
            "tainted_skull_ceiling");
    private static final Field HOE_MATERIAL = ReflectionHelper.findField(
            ItemHoe.class, "toolMaterial", "field_77843_a");

    private BlockToolEfficiencyEvents() {
    }

    @SubscribeEvent
    public static void applyUpstreamMineableTag(PlayerEvent.BreakSpeed event) {
        IBlockState state = event.getState();
        Block block = state.getBlock();
        if (block.getRegistryName() == null || !Tags.MOD_ID.equals(block.getRegistryName().getNamespace())) return;

        String name = block.getRegistryName().getPath();
        ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
        if (stack.isEmpty()) return;

        float desiredSpeed = 1.0F;
        if (PICKAXE_BLOCKS.contains(name) && isTool(stack, "pickaxe", ItemPickaxe.class)) {
            desiredSpeed = stack.getDestroySpeed(Blocks.STONE.getDefaultState());
        } else if (AXE_BLOCKS.contains(name) && isTool(stack, "axe", ItemAxe.class)) {
            desiredSpeed = stack.getDestroySpeed(Blocks.LOG.getDefaultState());
        } else if (HOE_BLOCKS.contains(name) && isTool(stack, "hoe", ItemHoe.class)) {
            desiredSpeed = getHoeEfficiency(stack);
        }
        if (desiredSpeed <= 1.0F) return;

        float currentSpeed = Math.max(1.0F, stack.getDestroySpeed(state));
        event.setNewSpeed(event.getNewSpeed() * desiredSpeed / currentSpeed);
    }

    private static boolean isTool(ItemStack stack, String toolClass, Class<? extends Item> vanillaType) {
        return vanillaType.isInstance(stack.getItem())
                || stack.getItem().getToolClasses(stack).contains(toolClass);
    }

    private static float getHoeEfficiency(ItemStack stack) {
        if (stack.getItem() instanceof ItemHoe) {
            try {
                return ((Item.ToolMaterial) HOE_MATERIAL.get(stack.getItem())).getEfficiency();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to read hoe tool material", exception);
            }
        }
        return Math.max(1.0F, stack.getDestroySpeed(Blocks.HAY_BLOCK.getDefaultState()));
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<String>(Arrays.asList(names));
    }
}
