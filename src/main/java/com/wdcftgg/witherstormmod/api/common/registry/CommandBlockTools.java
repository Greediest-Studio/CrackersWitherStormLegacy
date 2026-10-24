package com.wdcftgg.witherstormmod.api.common.registry;

import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 扩展命令工具资格；核心战的阶段和伤害仍由原控制器决定。 */
public final class CommandBlockTools {
    private static final List<Predicate<ItemStack>> EXTENSIONS = new ArrayList<>();

    private CommandBlockTools() {}

    public static void register(Predicate<ItemStack> predicate) {
        EXTENSIONS.add(predicate);
    }

    public static boolean contains(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (UpstreamItemTags.contains(UpstreamItemTags.COMMAND_BLOCK_TOOLS, stack)) return true;
        for (Predicate<ItemStack> predicate : EXTENSIONS) {
            if (predicate.test(stack)) return true;
        }
        return false;
    }
}
