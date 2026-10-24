package com.wdcftgg.witherstormmod.common.init;

import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.RecipeResourceConverter;
import com.wdcftgg.witherstormmod.common.resource.BundledResources;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import thedarkcolour.futuremc.recipe.stonecutter.StonecutterRecipes;

import java.io.InputStream;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModRecipes {

    private static final int EXPECTED_STONECUTTING_RECIPE_COUNT = 13;

    private ModRecipes() {
    }

    public static void registerSmelting() {
        GameRegistry.addSmelting(ModBlocks.get("tainted_cobblestone"), new ItemStack(ModBlocks.get("tainted_stone")), 0.1F);
        GameRegistry.addSmelting(ModBlocks.get("tainted_sandstone"), new ItemStack(ModBlocks.get("tainted_smooth_sandstone")), 0.1F);
        GameRegistry.addSmelting(ModBlocks.get("tainted_sand"), new ItemStack(ModBlocks.get("tainted_glass")), 0.1F);
    }

    public static void registerStonecutting() {
        int registered = 0;
        try {
            List<String> entries = BundledResources.listEntries(
                    RecipeResourceConverter.RECIPE_PREFIX, ".json");
            JsonContext context = new JsonContext(Tags.MOD_ID);
            for (String entryName : entries) {
                JsonObject converted;
                try (InputStream stream = BundledResources.open(entryName)) {
                    converted = RecipeResourceConverter.convertStonecutting(entryName, stream);
                }
                if (converted == null) continue;

                Ingredient ingredient = CraftingHelper.getIngredient(converted.get("ingredient"), context);
                ItemStack result = CraftingHelper.getItemStack(converted.getAsJsonObject("result"), context);
                StonecutterRecipes.INSTANCE.addRecipe(ingredient, result);
                registered++;
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to register stonecutting recipes from the bundled Wither Storm resources", exception);
        }
        if (registered != EXPECTED_STONECUTTING_RECIPE_COUNT) {
            throw new IllegalStateException("Expected " + EXPECTED_STONECUTTING_RECIPE_COUNT
                    + " bundled stonecutting recipes but registered " + registered);
        }
        WitherStormMod.LOGGER.info(
                "Registered {} stonecutting recipes from the bundled Wither Storm resources through Future MC",
                registered);
    }

    @SubscribeEvent
    public static void registerCraftingRecipes(RegistryEvent.Register<IRecipe> event) {
        int registered = 0;
        try {
            List<String> entries = BundledResources.listEntries(
                    RecipeResourceConverter.RECIPE_PREFIX, ".json");
            JsonContext context = new JsonContext(Tags.MOD_ID);
            for (String entryName : entries) {
                JsonObject converted;
                try (InputStream stream = BundledResources.open(entryName)) {
                    converted = RecipeResourceConverter.convert(entryName, stream);
                }
                if (converted == null) continue;

                String recipePath = entryName.substring(RecipeResourceConverter.RECIPE_PREFIX.length(),
                        entryName.length() - ".json".length());
                ResourceLocation registryName = new ResourceLocation(Tags.MOD_ID, recipePath);
                IRecipe recipe = CraftingHelper.getRecipe(converted, context).setRegistryName(registryName);
                event.getRegistry().register(recipe);
                registered++;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to register recipes from the bundled Wither Storm resources", exception);
        }
        if (registered <= 0) {
            throw new IllegalStateException("No crafting recipes were registered from the bundled Wither Storm resources");
        }
        WitherStormMod.LOGGER.info("Registered {} crafting recipes from the bundled Wither Storm resources", registered);
    }
}
