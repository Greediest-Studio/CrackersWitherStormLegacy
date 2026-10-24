package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class ModCreativeTabs {

    public static final CreativeTabs MAIN = new CreativeTabs(Tags.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.get("withered_nether_star"));
        }
    };

    private ModCreativeTabs() {
    }
}
