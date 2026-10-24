package com.wdcftgg.witherstormmod.common.init;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ModRegistryNames {

    private static final String MOD_PREFIX = "witherstormmod:";

    private static final String[] ITEMS = {
            "withered_bone", "command_block_book", "withered_nether_star"
    };

    private static final String[] BLOCKS = {
            "super_tnt", "formidibomb", "firework_bundle",
            "tainted_zombie_sitting", "tainted_zombie_wall", "tainted_zombie_lying", "tainted_bone_pile",
            "tainted_skeleton_wall", "tainted_skull_ceiling", "tainted_flesh_veins", "tainted_flesh_block",
            "infected_flesh_block", "hardened_flesh_block", "withered_phlegm_block", "tainted_stone",
            "tainted_stone_stairs", "tainted_stone_slab", "tainted_stone_button", "tainted_stone_pressure_plate",
            "tainted_cobblestone", "tainted_cobblestone_stairs", "tainted_cobblestone_slab",
            "tainted_cobblestone_wall", "tainted_sand", "tainted_dirt", "tainted_sandstone",
            "tainted_sandstone_slab", "tainted_sandstone_stairs", "tainted_sandstone_wall",
            "tainted_cut_sandstone", "tainted_cut_sandstone_slab", "tainted_chiseled_sandstone",
            "tainted_smooth_sandstone", "tainted_smooth_sandstone_slab", "tainted_smooth_sandstone_stairs",
            "tainted_smooth_sandstone_wall", "tainted_glass", "tainted_glass_pane", "tainted_planks",
            "tainted_torch", "tainted_wall_torch", "tainted_sign", "tainted_wall_sign", "tainted_hanging_sign",
            "tainted_wall_hanging_sign", "stripped_tainted_log",
            "stripped_tainted_wood", "tainted_log", "tainted_wood", "tainted_leaves", "tainted_door",
            "tainted_trapdoor", "tainted_button", "tainted_pressure_plate", "tainted_stairs", "tainted_slab",
            "tainted_fence", "tainted_fence_gate", "tainted_mushroom", "potted_tainted_mushroom",
            "tainted_pumpkin", "tainted_carved_pumpkin", "tainted_jack_o_lantern", "tainted_dust",
            "tainted_dust_block"
    };

    private static final String[] ITEMLESS_BLOCKS = {
            "potted_tainted_mushroom", "tainted_wall_sign", "tainted_wall_torch", "tainted_hanging_sign",
            "tainted_wall_hanging_sign"
    };

    private static final String[] SOUNDS = {
            "block_cluster_shake", "bowels_loop",
            "bowels_loud_hurt", "bowels_mood", "bowels_transport", "bowels_tremble", "cave_wind",
            "command_block_activates", "command_block_build", "command_block_cracks", "command_block_damage",
            "command_block_death", "command_block_destruct", "command_block_hit", "command_block_power_down",
            "command_block_pulse_loop", "command_block_summon", "earth_rumble", "flaming_skull_impact",
            "formidibomb_explosion", "formidibomb_explosion_quiet", "formidibomb_pulse_loop",
            "loud_tremble", "mob_cured", "mob_infected",
            "rib_bone_crack", "super_tnt_fuse", "tentacle_spike_stab", "tremble", "whoosh",
            "wither_storm_ambient", "wither_storm_bite", "wither_storm_close_loop", "wither_storm_death",
            "wither_storm_distant_loop", "wither_storm_evolves", "wither_storm_far_loop",
            "wither_storm_hurt", "wither_storm_loop", "wither_storm_reactivates", "wither_storm_roar",
            "wither_storm_shoot", "wither_storm_splits", "wither_storm_thump", "wither_storm_tractor_beam",
            "wither_storm_tractor_beam_activate", "wither_storm_tremble",
            "withered_phlegm_block_close", "withered_phlegm_block_open", "withered_symbiont_ambient",
            "withered_symbiont_cast_spell", "withered_symbiont_death", "withered_symbiont_heart_beat",
            "withered_symbiont_hurt", "withered_symbiont_launch_mob",
            "withered_symbiont_normal_death", "withered_symbiont_power_down", "withered_symbiont_prepare_spell",
            "withered_symbiont_pull", "withered_symbiont_spawn", "withered_symbiont_step",
            "withered_symbiont_summon"
    };

    private static final Set<String> ITEM_SET =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(ITEMS)));
    private static final Set<String> BLOCK_SET =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(BLOCKS)));
    private static final Set<String> ITEMLESS_BLOCK_SET =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(ITEMLESS_BLOCKS)));
    private static final Set<String> SOUND_SET =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(SOUNDS)));

    private ModRegistryNames() {
    }

    public static String[] itemNames() {
        return ITEMS.clone();
    }

    public static String[] blockNames() {
        return BLOCKS.clone();
    }

    public static String[] soundNames() {
        return SOUNDS.clone();
    }

    public static boolean isItemlessBlock(String name) {
        return ITEMLESS_BLOCK_SET.contains(name);
    }

    public static boolean isRegisteredBlockName(String name) {
        return BLOCK_SET.contains(name);
    }

    public static boolean isRegisteredItemName(String name) {
        return ITEM_SET.contains(name) || (BLOCK_SET.contains(name) && !ITEMLESS_BLOCK_SET.contains(name));
    }

    public static boolean isRegisteredSoundName(String name) {
        return SOUND_SET.contains(name);
    }

    public static boolean isRegisteredBlock(String id) {
        if (id == null) return false;
        if (!id.startsWith(MOD_PREFIX)) return true;
        return isRegisteredBlockName(id.substring(MOD_PREFIX.length()));
    }

    private static final String[] REMOVED_ITEM_ASSET_PREFIXES = {
            "amulet",
            "phasometer",
            "formidi_blade",
            "eye_of_the_storm",
            "golden_apple_stew",
            "withered_flesh",
            "withered_spider_eye",
            "super_beacon",
            "super_support_beacon",
            "command_block_sword",
            "command_block_pickaxe",
            "command_block_axe",
            "command_block_shovel",
            "command_block_hoe",
            "wooden_command_block_",
            "stone_command_block_",
            "iron_command_block_",
            "gold_command_block_",
            "crossbow"
    };

    public static boolean isRegisteredItem(String id) {
        if (id == null) return false;
        if (!id.startsWith(MOD_PREFIX)) return true;
        return isRegisteredItemName(id.substring(MOD_PREFIX.length()));
    }

    public static boolean isRemovedAssetResource(String rawName) {
        if (rawName == null) {
            return false;
        }
        String name = rawName.replace("/textures/blocks/", "/textures/block/")
                .replace("/textures/items/", "/textures/item/");
        if (name.startsWith("assets/witherstormmod/blockstates/") && name.endsWith(".json")) {
            String blockName = name.substring(
                    "assets/witherstormmod/blockstates/".length(),
                    name.length() - ".json".length());
            return !isRegisteredBlockName(blockName);
        }
        if (name.startsWith("assets/witherstormmod/models/block/")) {
            String file = name.substring("assets/witherstormmod/models/block/".length());
            return file.startsWith("super_beacon") || file.startsWith("super_support_beacon");
        }
        if (name.startsWith("assets/witherstormmod/textures/block/")) {
            String file = name.substring("assets/witherstormmod/textures/block/".length());
            return file.startsWith("super_beacon")
                    || file.startsWith("super_support_beacon")
                    || file.startsWith("beacon_beam");
        }
        if (name.startsWith("assets/witherstormmod/textures/gui/")) {
            String file = name.substring("assets/witherstormmod/textures/gui/".length());
            return file.startsWith("super_beacon")
                    || file.startsWith("title/background/panorama_");
        }
        if (name.startsWith("assets/witherstormmod/textures/misc/")) {
            String file = name.substring("assets/witherstormmod/textures/misc/".length());
            return file.startsWith("phasometer");
        }
        if (name.startsWith("assets/witherstormmod/textures/entity/")) {
            String file = name.substring("assets/witherstormmod/textures/entity/".length());
            return file.startsWith("super_beacon")
                    || file.startsWith("super_support_beacon")
                    || file.startsWith("beacon_beam");
        }
        if (name.startsWith("assets/witherstormmod/models/item/")) {
            String file = name.substring("assets/witherstormmod/models/item/".length());
            return hasRemovedItemAssetPrefix(file);
        }
        if (name.startsWith("assets/witherstormmod/textures/item/")) {
            String file = name.substring("assets/witherstormmod/textures/item/".length());
            return hasRemovedItemAssetPrefix(file);
        }
        if (name.startsWith("assets/witherstormmod/sounds/")) {
            int slash = name.lastIndexOf('/');
            String file = slash < 0 ? name : name.substring(slash + 1);
            return file.startsWith("amulet_")
                    || file.startsWith("formidi_blade_")
                    || file.startsWith("withered_beacon_");
        }
        return false;
    }

    private static boolean hasRemovedItemAssetPrefix(String file) {
        for (String prefix : REMOVED_ITEM_ASSET_PREFIXES) {
            if (file.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
