package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.wdcftgg.witherstormmod.common.init.ModRegistryNames;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdvancementResourceConverter {

    public static final String ADVANCEMENT_PREFIX = "data/witherstormmod/advancements/";
    public static final String SOURCE_MARKER = "_legacy_arr_source";

    public static final Set<String> ALL_MAIN_ADVANCEMENTS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "main/root.json",
                    "main/summon_wither_storm.json",
                    "main/amulet.json",
                    "main/fully_link_amulet.json",
                    "main/cured_sickened_mob.json",
                    "main/escape_wither_storm.json",
                    "main/harbinger_of_cataclysmic_fates.json",
                    "main/infinite_potential.json",
                    "main/fbomb.json",
                    "main/strong_grow_weak.json",
                    "main/weakened_arise_stronger.json",
                    "main/silver_lining.json",
                    "main/belly_of_the_beast.json",
                    "main/wither_storm_defeated.json",
                    "main/activate_super_beacon.json",
                    "main/resummon_wither_storm.json",
                    "main/resummon_withered_symbiont.json",
                    "main/summon_mob_withered_beacon.json",
                    "main/ring_bell_near_storm.json",
                    "main/spyglass_at_wither_storm.json",
                    "main/nearly_kill_wither_storm.json",
                    "main/insane_dedication.json",
                    "main/overly_dedicated.json")));

    public static final Set<String> REMOVED_MAIN_ADVANCEMENTS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "main/amulet.json",
                    "main/fully_link_amulet.json",
                    "main/activate_super_beacon.json",
                    "main/resummon_wither_storm.json",
                    "main/resummon_withered_symbiont.json",
                    "main/summon_mob_withered_beacon.json",
                    "main/spyglass_at_wither_storm.json",
                    "main/insane_dedication.json",
                    "main/overly_dedicated.json")));

    public static final Set<String> MAIN_CHAIN;

    private static final Set<String> REMOVED_TRIGGERS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "witherstormmod:link_amulet",
                    "witherstormmod:activate_super_beacon",
                    "witherstormmod:summon_mob_withered_beacon",
                    "witherstormmod:observe_wither_storm",
                    "minecraft:using_item")));

    private static final String BOWELS_DIMENSION = "witherstormmod:bowels";
    private static final String LEGACY_BOWELS_DIMENSION = "wither_storm_bowels";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        LinkedHashSet<String> retained = new LinkedHashSet<String>(ALL_MAIN_ADVANCEMENTS);
        retained.removeAll(REMOVED_MAIN_ADVANCEMENTS);
        MAIN_CHAIN = Collections.unmodifiableSet(retained);
    }

    private AdvancementResourceConverter() {
    }

    public static JsonObject convert(String entryName, InputStream source) throws IOException {
        String relative = relativeName(entryName);
        if (!ALL_MAIN_ADVANCEMENTS.contains(relative)) {
            throw new IOException("Unsupported upstream advancement: " + entryName);
        }
        if (REMOVED_MAIN_ADVANCEMENTS.contains(relative)) {
            return null;
        }
        return convertParsed(entryName, relative, readAdvancement(entryName, source));
    }

    public static String readParent(String entryName, InputStream source) throws IOException {
        JsonObject advancement = readAdvancement(entryName, source);
        if (advancement.has("parent") && advancement.get("parent").isJsonPrimitive()) {
            return advancement.get("parent").getAsString();
        }
        return null;
    }

    public static String advancementId(String relativeName) {
        if (relativeName.endsWith(".json")) {
            return "witherstormmod:" + relativeName.substring(0, relativeName.length() - ".json".length());
        }
        return "witherstormmod:" + relativeName;
    }

    public static void rewriteParent(JsonObject advancement,
                                     Set<String> keptMainIds,
                                     Map<String, String> originalParents) {
        if (!advancement.has("parent") || !advancement.get("parent").isJsonPrimitive()) {
            return;
        }
        String parent = advancement.get("parent").getAsString();
        Set<String> visited = new HashSet<String>();
        while (parent != null && parent.startsWith("witherstormmod:main/")
                && !keptMainIds.contains(parent) && visited.add(parent)) {
            parent = originalParents.get(parent);
        }
        if (parent == null || (parent.startsWith("witherstormmod:main/") && !keptMainIds.contains(parent))) {
            advancement.addProperty("parent", "witherstormmod:main/root");
        } else {
            advancement.addProperty("parent", parent);
        }
    }

    public static JsonObject convertRecipeAdvancement(String entryName, InputStream source)
            throws IOException {
        String relative = relativeName(entryName);
        if (!relative.startsWith("recipes/")) {
            throw new IOException("Not an upstream recipe advancement: " + entryName);
        }
        JsonObject advancement = readAdvancement(entryName, source);
        if (!hasLegacyRecipeReward(advancement)) return null;
        return convertParsed(entryName, relative, advancement);
    }

    private static JsonObject readAdvancement(String entryName, InputStream source) throws IOException {
        try (Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid upstream advancement: " + entryName, exception);
        }
    }

    private static JsonObject convertParsed(String entryName, String relative,
                                            JsonObject advancement) throws IOException {
        convertDisplay(advancement);
        if (!convertCriteria(advancement, relative)) {
            return null;
        }
        convertRewards(advancement);
        advancement.remove("sends_telemetry_event");
        advancement.addProperty(SOURCE_MARKER, entryName);
        return advancement;
    }

    public static String serialize(JsonObject advancement) {
        return GSON.toJson(advancement) + "\n";
    }

    public static String sourceEntry(String relativeName) {
        if (!ALL_MAIN_ADVANCEMENTS.contains(relativeName)) {
            throw new IllegalArgumentException("Unsupported upstream advancement: " + relativeName);
        }
        return ADVANCEMENT_PREFIX + relativeName;
    }

    public static boolean isGeneratedFile(JsonObject json, String entryName) {
        return json.has(SOURCE_MARKER)
                && entryName.equals(getString(json, SOURCE_MARKER, ""));
    }

    public static boolean isAnyGeneratedFile(JsonObject json) {
        return json.has(SOURCE_MARKER)
                && getString(json, SOURCE_MARKER, "").startsWith(ADVANCEMENT_PREFIX);
    }

    private static String relativeName(String entryName) throws IOException {
        if (entryName == null || !entryName.startsWith(ADVANCEMENT_PREFIX)
                || !entryName.endsWith(".json")) {
            throw new IOException("Invalid upstream advancement path: " + entryName);
        }
        return entryName.substring(ADVANCEMENT_PREFIX.length());
    }

    private static void convertDisplay(JsonObject advancement) {
        if (!advancement.has("display")) return;
        JsonObject display = getJsonObject(advancement, "display");
        JsonObject icon = getJsonObject(display, "icon", new JsonObject());
        if (icon.has("item") && icon.get("item").isJsonPrimitive()) {
            String itemId = icon.get("item").getAsString();
            if ("witherstormmod:withered_symbiont_spawn_egg".equals(itemId)) {
                icon.addProperty("item", "minecraft:spawn_egg");
                icon.addProperty("nbt",
                        "{EntityTag:{id:\"witherstormmod:withered_symbiont\"}}");
            } else if ("witherstormmod:golden_apple_stew".equals(itemId)) {
                icon.addProperty("item", "minecraft:golden_apple");
                icon.addProperty("data", 0);
            } else {
                String convertedId = convertItemId(itemId);
                if (!ModRegistryNames.isRegisteredItem(convertedId)) {
                    icon.addProperty("item", "witherstormmod:command_block_book");
                } else {
                    icon.addProperty("item", convertedId);
                    applyLegacyItemMetadata(icon, itemId);
                }
            }
        }
        if (display.has("background")) {
            String background = getString(display, "background");
            display.addProperty("background", background.replace(":textures/block/", ":textures/blocks/"));
        }
    }

    private static boolean convertCriteria(JsonObject advancement, String relativeName) throws IOException {
        JsonObject originalCriteria = getJsonObject(advancement, "criteria");
        JsonObject convertedCriteria = new JsonObject();
        Map<String, List<String>> replacements = new LinkedHashMap<String, List<String>>();
        boolean hadDroppedCriterion = false;

        for (Map.Entry<String, JsonElement> entry : originalCriteria.entrySet()) {
            String criterionName = entry.getKey();
            JsonObject criterion = getJsonObject(entry.getValue(), "criterion").deepCopy();
            String trigger = getString(criterion, "trigger");
            List<JsonObject> alternatives;

            if (REMOVED_TRIGGERS.contains(trigger)) {
                alternatives = Collections.emptyList();
            } else if ("minecraft:inventory_changed".equals(trigger)) {
                alternatives = convertInventoryCriterion(criterion, relativeName);
            } else {
                convertCriterion(criterion, trigger, relativeName);
                alternatives = Collections.singletonList(criterion);
            }

            if (alternatives.isEmpty()) {
                hadDroppedCriterion = true;
                replacements.put(criterionName, Collections.<String>emptyList());
                continue;
            }

            List<String> generatedNames = new ArrayList<String>();
            for (int index = 0; index < alternatives.size(); index++) {
                String generatedName = alternatives.size() == 1
                        ? criterionName : criterionName + "_alternative_" + (index + 1);
                if (originalCriteria.has(generatedName) && !generatedName.equals(criterionName)
                        || convertedCriteria.has(generatedName)) {
                    throw new JsonSyntaxException("Duplicate generated criterion '" + generatedName
                            + "' in " + relativeName);
                }
                convertedCriteria.add(generatedName, alternatives.get(index));
                generatedNames.add(generatedName);
            }
            replacements.put(criterionName, generatedNames);
        }

        if (convertedCriteria.size() == 0) {
            return false;
        }
        if (!advancement.has("requirements") && hadDroppedCriterion) {
            return false;
        }
        advancement.add("criteria", convertedCriteria);
        return rewriteRequirements(advancement, replacements);
    }

    private static void convertCriterion(JsonObject criterion, String trigger,
                                         String relativeName) {
        JsonObject conditions = getJsonObject(criterion, "conditions", new JsonObject());
        if ("minecraft:summoned_entity".equals(trigger)) {
            convertEntityCondition(conditions, "entity", relativeName);
        } else if ("minecraft:changed_dimension".equals(trigger)) {
            convertDimensionCondition(conditions, "from");
            convertDimensionCondition(conditions, "to");
        } else if ("witherstormmod:cured_sickened_mob".equals(trigger)) {
            convertEntityCondition(conditions, "sickened", relativeName);
            convertEntityCondition(conditions, "converison", relativeName);
        } else if (trigger.startsWith("witherstormmod:")) {
            convertEntityCondition(conditions, "entity", relativeName);
        }
        criterion.add("conditions", conditions);
    }

    private static List<JsonObject> convertInventoryCriterion(JsonObject criterion,
                                                               String relativeName)
            throws IOException {
        JsonObject conditions = getJsonObject(criterion, "conditions", new JsonObject());
        JsonArray itemPredicates = getJsonArray(conditions, "items", new JsonArray());
        List<JsonArray> combinations = new ArrayList<JsonArray>();
        combinations.add(new JsonArray());

        for (JsonElement element : itemPredicates) {
            JsonObject predicate = getJsonObject(element, "item predicate");
            List<JsonObject> choices = expandItemPredicate(predicate, relativeName);
            if (choices.isEmpty()) {
                return Collections.emptyList();
            }
            List<JsonArray> expanded = new ArrayList<JsonArray>();
            for (JsonArray combination : combinations) {
                for (JsonObject choice : choices) {
                    JsonArray next = combination.deepCopy();
                    next.add(choice);
                    expanded.add(next);
                }
            }
            combinations = expanded;
        }

        List<JsonObject> alternatives = new ArrayList<JsonObject>();
        for (JsonArray combination : combinations) {
            JsonObject convertedCriterion = criterion.deepCopy();
            JsonObject convertedConditions = conditions.deepCopy();
            convertedConditions.add("items", combination);
            convertedCriterion.add("conditions", convertedConditions);
            alternatives.add(convertedCriterion);
        }
        return alternatives;
    }

    private static List<JsonObject> expandItemPredicate(JsonObject source,
                                                        String relativeName) throws IOException {
        if (source.has("items") && source.has("tag")) {
            throw new JsonSyntaxException("Item predicate cannot contain both items and tag in "
                    + relativeName);
        }

        List<String> itemIds = new ArrayList<String>();
        if (source.has("items")) {
            JsonArray items = getJsonArray(source, "items");
            for (JsonElement item : items) {
                itemIds.add(getString(item, "item id"));
            }
        } else if (source.has("tag")) {
            itemIds.addAll(resolveItemTag(getString(source, "tag"),
                    new LinkedHashSet<String>()));
        } else if (source.has("item")) {
            itemIds.add(getString(source, "item"));
        } else {
            return Collections.singletonList(source.deepCopy());
        }

        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<JsonObject> alternatives = new ArrayList<JsonObject>();
        for (String itemId : new LinkedHashSet<String>(itemIds)) {
            String convertedItemId = convertItemId(itemId);
            if (!ModRegistryNames.isRegisteredItem(convertedItemId)) {
                continue;
            }
            JsonObject predicate = source.deepCopy();
            predicate.remove("items");
            predicate.remove("tag");
            predicate.addProperty("item", convertedItemId);
            applyLegacyItemMetadata(predicate, itemId);
            alternatives.add(predicate);
        }
        return alternatives;
    }

    private static List<String> resolveItemTag(String tagId, Set<String> resolving)
            throws IOException {
        if (!resolving.add(tagId)) {
            throw new IOException("Circular upstream item tag: " + tagId);
        }

        String namespace = "minecraft";
        String path = tagId;
        int separator = tagId.indexOf(':');
        if (separator >= 0) {
            namespace = tagId.substring(0, separator);
            path = tagId.substring(separator + 1);
        }
        if (namespace.isEmpty() || path.isEmpty() || namespace.indexOf('\\') >= 0
                || path.indexOf('\\') >= 0 || path.startsWith("/") || path.contains("..")) {
            throw new IOException("Invalid upstream item tag: " + tagId);
        }

        String entryName = "data/" + namespace + "/tags/items/" + path + ".json";
        JsonObject tag;
        try (InputStream source = UpstreamResourceArchive.open(entryName);
             Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            tag = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid upstream item tag: " + entryName, exception);
        }

        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (JsonElement element : getJsonArray(tag, "values")) {
            String value;
            boolean required = true;
            if (element.isJsonPrimitive()) {
                value = element.getAsString();
            } else {
                JsonObject object = getJsonObject(element, "tag value");
                value = getString(object, "id");
                required = getBoolean(object, "required", true);
            }

            if (value.startsWith("#")) {
                try {
                    values.addAll(resolveItemTag(value.substring(1), resolving));
                } catch (IOException exception) {
                    if (required) throw exception;
                }
            } else {
                values.add(value);
            }
        }
        resolving.remove(tagId);
        return new ArrayList<String>(values);
    }

    private static boolean rewriteRequirements(JsonObject advancement,
                                               Map<String, List<String>> replacements) {
        if (!advancement.has("requirements")) return true;
        JsonArray originalRequirements = getJsonArray(advancement, "requirements");
        JsonArray convertedRequirements = new JsonArray();
        for (JsonElement groupElement : originalRequirements) {
            JsonArray originalGroup = getJsonArray(groupElement, "requirement group");
            JsonArray convertedGroup = new JsonArray();
            for (JsonElement criterionElement : originalGroup) {
                String criterionName = getString(criterionElement, "criterion name");
                List<String> generatedNames = replacements.get(criterionName);
                if (generatedNames == null) {
                    convertedGroup.add(criterionName);
                } else {
                    for (String generatedName : generatedNames) convertedGroup.add(generatedName);
                }
            }
            if (convertedGroup.size() == 0) {
                return false;
            }
            convertedRequirements.add(convertedGroup);
        }
        advancement.add("requirements", convertedRequirements);
        return true;
    }

    private static void convertEntityCondition(JsonObject conditions, String key,
                                               String relativeName) {
        JsonElement entityElement = conditions.get(key);
        if (entityElement == null || entityElement.isJsonNull() || entityElement.isJsonObject()) return;
        if (!entityElement.isJsonArray()) {
            throw new JsonSyntaxException("Invalid entity predicate '" + key + "' in " + relativeName);
        }
        JsonArray predicates = entityElement.getAsJsonArray();
        if (predicates.size() != 1) {
            throw new JsonSyntaxException("Expected one entity predicate '" + key + "' in "
                    + relativeName);
        }
        JsonObject wrapper = getJsonObject(predicates.get(0), "entity predicate");
        JsonObject predicate = getJsonObject(wrapper, "predicate");
        JsonObject legacy = new JsonObject();
        copyIfPresent(predicate, legacy, "type");
        copyIfPresent(predicate, legacy, "distance");
        copyIfPresent(predicate, legacy, "location");
        copyIfPresent(predicate, legacy, "effects");
        if (predicate.has("nbt")) {
            legacy.addProperty("nbt", getString(predicate, "nbt"));
        }
        conditions.add(key, legacy);
    }

    private static void copyIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) target.add(key, source.get(key).deepCopy());
    }

    private static void convertDimensionCondition(JsonObject conditions, String key) {
        if (BOWELS_DIMENSION.equals(getString(conditions, key, ""))) {
            conditions.addProperty(key, LEGACY_BOWELS_DIMENSION);
        }
    }

    private static void convertRewards(JsonObject advancement) throws IOException {
        if (!advancement.has("rewards")) return;
        JsonObject rewards = getJsonObject(advancement, "rewards");
        if (!rewards.has("recipes")) return;

        JsonArray converted = new JsonArray();
        for (JsonElement element : getJsonArray(rewards, "recipes")) {
            String recipeId = getString(element, "recipe id");
            if (isLegacyUnlockableRecipe(recipeId)) converted.add(recipeId);
        }
        if (converted.size() == 0) rewards.remove("recipes");
        else rewards.add("recipes", converted);
    }

    private static boolean hasLegacyRecipeReward(JsonObject advancement) throws IOException {
        if (!advancement.has("rewards")) return false;
        JsonObject rewards = getJsonObject(advancement, "rewards");
        if (!rewards.has("recipes")) return false;
        JsonArray recipes = getJsonArray(rewards, "recipes");
        if (recipes.size() == 0) return false;
        for (JsonElement element : recipes) {
            if (!isLegacyUnlockableRecipe(getString(element, "recipe id"))) return false;
        }
        return true;
    }

    private static boolean isLegacyUnlockableRecipe(String recipeId) throws IOException {
        String namespace = "minecraft";
        String path = recipeId;
        int separator = recipeId.indexOf(':');
        if (separator >= 0) {
            namespace = recipeId.substring(0, separator);
            path = recipeId.substring(separator + 1);
        }
        String entryName = "data/" + namespace + "/recipes/" + path + ".json";
        for (String candidate : UpstreamResourceArchive.listEntries(
                "data/" + namespace + "/recipes/", ".json")) {
            if (!candidate.equals(entryName)) continue;
            try (InputStream source = UpstreamResourceArchive.open(candidate)) {
                return RecipeResourceConverter.convert(candidate, source) != null;
            }
        }
        return false;
    }

    private static void applyLegacyItemMetadata(JsonObject item, String originalItemId) {
        if ("minecraft:wither_skeleton_skull".equals(originalItemId)) {
            item.addProperty("data", 1);
        } else if ("minecraft:lapis_lazuli".equals(originalItemId)) {
            item.addProperty("data", 4);
        }
    }

    private static String convertItemId(String itemId) {
        if ("minecraft:wither_skeleton_skull".equals(itemId)) {
            return "minecraft:skull";
        }
        if ("minecraft:bell".equals(itemId)) {
            return "futuremc:bell";
        }
        if ("minecraft:barrel".equals(itemId)) {
            return "futuremc:barrel";
        }
        if ("minecraft:firework_rocket".equals(itemId)) {
            return "minecraft:fireworks";
        }
        if ("minecraft:lapis_lazuli".equals(itemId)) {
            return "minecraft:dye";
        }
        if ("minecraft:suspicious_stew".equals(itemId)) {
            return "futuremc:suspicious_stew";
        }
        if ("minecraft:crossbow".equals(itemId)) {
            return "minecraft:bow";
        }
        if ("minecraft:honey_bottle".equals(itemId)
                || "minecraft:honeycomb".equals(itemId)
                || "minecraft:wither_rose".equals(itemId)) {
            return "futuremc:" + itemId.substring("minecraft:".length());
        }
        return itemId;
    }

    private static String getString(JsonElement element, String memberName) {
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be a string");
    }

    private static String getString(JsonObject object, String memberName) {
        if (object.has(memberName)) {
            return getString(object.get(memberName), memberName);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a string");
    }

    private static String getString(JsonObject object, String memberName, String fallback) {
        return object.has(memberName) ? getString(object.get(memberName), memberName) : fallback;
    }

    private static boolean getBoolean(JsonObject object, String memberName, boolean fallback) {
        if (!object.has(memberName)) {
            return fallback;
        }
        JsonElement element = object.get(memberName);
        if (element.isJsonPrimitive()) {
            return element.getAsBoolean();
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be a boolean");
    }

    private static JsonObject getJsonObject(JsonElement element, String memberName) {
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be a JsonObject");
    }

    private static JsonObject getJsonObject(JsonObject object, String memberName) {
        if (object.has(memberName)) {
            return getJsonObject(object.get(memberName), memberName);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonObject");
    }

    private static JsonObject getJsonObject(JsonObject object, String memberName, JsonObject fallback) {
        return object.has(memberName) ? getJsonObject(object.get(memberName), memberName) : fallback;
    }

    private static JsonArray getJsonArray(JsonElement element, String memberName) {
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be a JsonArray");
    }

    private static JsonArray getJsonArray(JsonObject object, String memberName) {
        if (object.has(memberName)) {
            return getJsonArray(object.get(memberName), memberName);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonArray");
    }

    private static JsonArray getJsonArray(JsonObject object, String memberName, JsonArray fallback) {
        return object.has(memberName) ? getJsonArray(object.get(memberName), memberName) : fallback;
    }
}
