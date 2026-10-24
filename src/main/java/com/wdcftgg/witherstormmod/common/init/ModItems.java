package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.item.FoiledItem;
import com.wdcftgg.witherstormmod.common.item.SimpleItem;
import com.wdcftgg.witherstormmod.common.item.WitheredNetherStarItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModItems {

    private static final Map<String, Item> ITEMS = new LinkedHashMap<String, Item>();
    private static final String[] ITEM_NAMES = ModRegistryNames.itemNames();

    static {
        for (String name : ITEM_NAMES) {
            if (!ModBlocksContains.blockExists(name)) {
                ITEMS.put(name, createItem(name));
            }
        }
    }

    private ModItems() {
    }

    public static void bootstrap() {
    }

    public static Item get(String name) {
        Item item = ITEMS.get(name);
        if (item != null) {
            return item;
        }
        return ModBlocks.getItem(name);
    }

    public static String[] getRegisteredNames() {
        return ITEM_NAMES.clone();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ITEMS.values().toArray(new Item[0]));
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        for (Item item : ITEMS.values()) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    private static final class ModBlocksContains {
        private static boolean blockExists(String name) {
            return ModBlocks.get(name) != null;
        }
    }

    private static Item createItem(String name) {
        if ("withered_bone".equals(name)) {
            return new SimpleItem(name, EnumRarity.UNCOMMON);
        }
        if ("command_block_book".equals(name)) {
            return new FoiledItem(name, EnumRarity.RARE);
        }
        if ("withered_nether_star".equals(name)) {
            return new WitheredNetherStarItem(name);
        }
        return new SimpleItem(name);
    }
}
