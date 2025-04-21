package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.cache.RecipeCoreData;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.CategoryDataCache;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeLoader {

	//Ensure one instance
	private static RecipeLoader instance = null;
	private final int recipeSize = 20;
	private final CategoryDataCache categoryDataCache;
	private final Server server;
	@Getter
	private final Map<String, Integer> recipes = new HashMap<>();
	@Getter
	private List<Recipe> serverRecipes = new ArrayList<>();
	@Getter
	private List<Recipe> disabledServerRecipes = new ArrayList<>();
	@Getter
	private List<EnhancedRecipe> loadedRecipes = new ArrayList<>();
	@Getter
	private Map<ItemStack, ItemStack> similarVanillaRecipe = new HashMap<>();
	private Map<String, Recipe> loaded = new HashMap<>();
	@Getter
	private Map<String, RecipeGroup> mappedGroupedRecipes = new HashMap<>();


	private RecipeLoader(final Server server, final CategoryDataCache categoryDataCache) {
		this.server = server;
		try {
			server.recipeIterator().forEachRemaining(serverRecipes::add);
		} catch (IllegalArgumentException e) {
			self().getLogger().log(Level.SEVERE, "This server recipe contains air, will not be loaded.", e);
		}

/*		for (final Iterator<Recipe> it = server.recipeIterator(); it.hasNext(); ) {
			final Recipe data = it.next();

		}*/
	/*	for (final RecipeType type : RecipeType.values()) {
			mappedGroupedRecipes.put(type, new ArrayList<>());
		}*/
		this.categoryDataCache = categoryDataCache;
	}

	public static RecipeLoader getInstance() {
		return instance == null ? instance = new RecipeLoader(Bukkit.getServer(), self().getCategoryDataCache()) : instance;
	}

	public static void clearInstance() {
		for (final EnhancedRecipe loaded : RecipeLoader.getInstance().getLoadedRecipes()) {
			instance.unloadRecipe(loaded.getServerRecipe());
		}
		instance = null;
	}


	public Map<String, Integer> getRecipes() {
		server.recipeIterator().forEachRemaining(recipe1 -> {
			if (recipe1 instanceof CraftingRecipe && (((CraftingRecipe) recipe1).getKey().getNamespace().contains("craftenhance") || ((CraftingRecipe) recipe1).getKey().getNamespace().contains("cehrecipe"))) {
				Integer recipeCounter = recipes.getOrDefault(((CraftingRecipe) recipe1).getGroup(), 0);
				recipes.put(((CraftingRecipe) recipe1).getGroup(), recipeCounter + 1);
			}
			if (recipe1 instanceof CookingRecipe && (((CookingRecipe) recipe1).getKey().getNamespace().contains("craftenhance") || ((CookingRecipe) recipe1).getKey().getNamespace().contains("cehrecipe"))) {
				Integer recipeCounter = recipes.getOrDefault(((CookingRecipe) recipe1).getGroup(), 0);
				recipes.put(((CookingRecipe) recipe1).getGroup(), recipeCounter + 1);
			}
		});

		return recipes;
	}

	//Adds or merges group with existing group.
	@Nullable
	private String addToGroup(final List<Recipe> serverRecipes, final EnhancedRecipe enhancedRecipe, final String categoryName) {
		final Pair<String, RecipeGroup> recipeGroupPair = getRecipeGroup(categoryName);
		Debug.Send("Recipe group", "is does now add recipe to the group: " + recipeGroupPair.getFirst());

		RecipeGroup recipeGroup = recipeGroupPair.getSecond();
		if (recipeGroup == null) {
			recipeGroup = new RecipeGroup();
		}
		recipeGroup.addIfNotExist(RecipeCoreData.of(enhancedRecipe));
		recipeGroup.setServerRecipes(serverRecipes);

		mappedGroupedRecipes.put(recipeGroupPair.getFirst(), recipeGroup);

		return recipeGroupPair.getFirst();
	}


	public RecipeGroup findGroup(final EnhancedRecipe recipe) {
		return mappedGroupedRecipes.get(recipe.getGroup());
	}

	public List<RecipeGroup> findGroupsByResult(final ItemStack result, final Recipe recipe) {
		return this.findGroupsByResult(result, recipe, RecipeType.NON);
	}

	//Find groups that contain at least one recipe that maps to result.
	public List<RecipeGroup> findGroupsByResult(final ItemStack result, final Recipe recipe, final RecipeType recipeType) {
		final List<RecipeGroup> originGroups = new ArrayList<>();
		if (self().getVersionChecker().olderThan(ServerVersion.v1_13) || recipe == null) {
			final Set<String> seenGroups = new HashSet<>();
			CacheRecipes cacheRecipes = self().getCacheRecipes();
			for (String groupKey : cacheRecipes.getGroupsForType(recipeType)) {
				if (!seenGroups.add(groupKey)) continue;

				final RecipeGroup group = mappedGroupedRecipes.get(groupKey);
				if (group == null) continue;

				if (group.getRecipeCoreList().stream().anyMatch(x -> result.isSimilar(x.getResult()))) {
					originGroups.add(group);
				} else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult()))) {
					originGroups.add(group);
				}
			}
			if (!originGroups.isEmpty()) {
				return originGroups;
			}
			for (final RecipeGroup group : mappedGroupedRecipes.values()) {
				if (group.getRecipeCoreList().stream().anyMatch(x -> result.equals(x.getResult())))
					originGroups.add(group);
				else if (group.getServerRecipes().stream().anyMatch(x -> result.equals(x.getResult())))
					originGroups.add(group);
			}
			return originGroups;
		}
		RecipeGroup group = null;
		if (recipe instanceof CraftingRecipe)
			group = mappedGroupedRecipes.get(((CraftingRecipe) recipe).getGroup());
		if (recipe instanceof CookingRecipe)
			group = mappedGroupedRecipes.get(((CookingRecipe<?>) recipe).getGroup());


		if (group != null) {
			if (recipe instanceof CookingRecipe) {
				if (group.getRecipeCoreList().stream().anyMatch(x -> x.isSimilarContent(result)))
					originGroups.add(group);
				else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(((CookingRecipe<?>) x).getInput())))
					originGroups.add(group);
			} else {
				if (group.getRecipeCoreList().stream().anyMatch(x -> result.isSimilar(x.getResult())))
					originGroups.add(group);
				else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult())))
					originGroups.add(group);
			}
		}
		return originGroups;
	}

	public List<RecipeGroup> findGroupsBySimilarResultMatch(final ItemStack result, final RecipeType type) {
		final List<RecipeGroup> originGroups = new ArrayList<>();
		final Set<String> seenGroups = new HashSet<>();
		CacheRecipes cacheRecipes = self().getCacheRecipes();
		for (String groupKey : cacheRecipes.getGroupsForType(type)) {
			if (!seenGroups.add(groupKey)) continue;
			final RecipeGroup group = mappedGroupedRecipes.get(groupKey);
			if (group == null) continue;

			if (group.getRecipeCoreList().stream().anyMatch(x -> result.isSimilar(x.getResult()))) {
				originGroups.add(group);
			} else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult()))) {
				originGroups.add(group);
			}
		}

		if (!originGroups.isEmpty()) {
			return originGroups;
		}

		for (final RecipeGroup group : mappedGroupedRecipes.values()) {
			if (group.getRecipeCoreList().stream().anyMatch(x -> result.isSimilar(x.getResult())))
				originGroups.add(group);
			else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult())))
				originGroups.add(group);
		}
		return originGroups;
	}

	public RecipeGroup findSimilarGroup(final EnhancedRecipe recipe) {
		return mappedGroupedRecipes.get(recipe.getGroup());
	}

	//There can only be one group that matches a matrix, because that's how they're grouped
//    public List<RecipeGroup> findMatchingGroups(ItemStack[] matrix, RecipeType type){
//        List<RecipeGroup> matchingGroups = new ArrayList<>();
//        for(RecipeGroup group : mappedGroupedRecipes.get(type)){
//            try{
//                if(group.getEnhancedRecipes().stream().anyMatch(x -> x.matches(matrix)))
//                    matchingGroups.add(group);
//            } catch(ClassCastException e){}
//        }
//        return matchingGroups;
//    }

	public boolean isLoadedAsServerRecipe(final EnhancedRecipe recipe) {
		return loaded.containsKey(recipe.getKey());
	}

	private void unloadCehRecipes() {
		final Iterator<Recipe> it = server.recipeIterator();
		while (it.hasNext()) {
			final Recipe r = it.next();
			if (Adapter.ContainsSubKey(r, ServerRecipeTranslator.KeyPrefix)) {
				it.remove();
			}
		}
	}

	private void unloadRecipe(final Recipe r) {
		final Iterator<Recipe> it = server.recipeIterator();
		while (it.hasNext()) {
			final Recipe currentRecipe = it.next();
			if (currentRecipe.equals(r)) {
				it.remove();
				return;
			}
		}
	}

	public void unloadAll() {
		disabledServerRecipes.forEach(x ->
				enableServerRecipe(x)
		);
		mappedGroupedRecipes.clear();
		serverRecipes.clear();
		loaded.clear();
		unloadCehRecipes();
	}

	public void unloadRecipe(final EnhancedRecipe recipe) {
		final RecipeGroup group = findGroup(recipe);
		loadedRecipes.remove(recipe);

		final CategoryData categoryData = categoryDataCache.get(recipe.getRecipeCategory());
		if (categoryData != null && categoryData.getEnhancedRecipes() != null)
			categoryData.getEnhancedRecipes().remove(recipe);

		final Recipe serverRecipe = loaded.get(recipe.getKey());

		//Only unload from server if there are no similar server recipes.
		if (serverRecipe != null) {
			loaded.remove(recipe.getKey());
			unloadRecipe(serverRecipe);
		}

		if (group == null) {
			printGroupsDebugInfo();
			Messenger.Error("Could not unload recipe from group, because the recipe doesn't have a group.");
			return;
		}

        /* TODO (Optimization) When e.g. a shapeless recipe disappears from a recipe, not all recipes in that group are
            similar anymore. So detect this and make sure they get split up. */

		//Remove entire recipe group if it's the last enhanced recipe, or remove a single recipe from the group.
		if (group.getRecipeCoreList().size() == 1)
			mappedGroupedRecipes.remove(recipe.getGroup());
		else group.getRecipeCoreList().remove(recipe);
		Debug.Send("Unloaded a recipe");
		printGroupsDebugInfo();
	}

	public void loadRecipe(@NonNull final EnhancedRecipe recipe) {
		loadRecipe(recipe, false);
	}

	public void loadRecipe(@NonNull final EnhancedRecipe recipe, final boolean isReloading) {
		if (recipe.validate() != null) {
			Messenger.Error("(loadRecipe) There's an issue with recipe " + recipe.getKey() + ": " + recipe.validate());
			return;
		}

		final boolean containsRecipe = loaded.containsKey(recipe.getKey());
		if (containsRecipe)
			unloadRecipe(recipe);

		final List<Recipe> similarServerRecipes = new ArrayList<>();
		for (final Recipe r : serverRecipes) {
			if (recipe.isSimilar(r)) {
				similarServerRecipes.add(r);
			}
		}

		Recipe alwaysSimilar = null;
		for (final Recipe r : similarServerRecipes) {
			if (recipe.isAlwaysSimilar(r)) {
				if (self().getVersionChecker().newerThan(ServerVersion.v1_12)) {
					if (r instanceof ShapedRecipe)
						if (((ShapedRecipe) r).getKey().getNamespace().contains("craftenhance"))
							continue;
					if (r instanceof ShapelessRecipe)
						if (((ShapelessRecipe) r).getKey().getNamespace().contains("craftenhance"))
							continue;
				}
				alwaysSimilar = r;
				break;
			}
		}
		//cache orginal recipe if user make furnace recipe and give right item as output.
		cacheSimilarVanillaRecipe(recipe);

		String categoryName = loadCategories(recipe);
		String groupName = addToGroup(similarServerRecipes, recipe, categoryName);
		loadedRecipes.add(recipe);
		//Only load the recipe if there is not a server recipe that's always similar.
		final Recipe serverRecipe = recipe.getServerRecipe(groupName);
		if (alwaysSimilar == null) {
			if (!(recipe instanceof BrewingRecipe)) {
				if (serverRecipe == null) {
					Debug.Send("Loading recipe", "Added server recipe is null for " + recipe.getKey());
					self().getLogger().log(Level.WARNING, "Recipe " + recipe.getKey() + " will not be cached because the result is null or invalid material type.");
					return;
				}
				if (!containsRecipe && !isReloading) {
					server.addRecipe(serverRecipe);
				}
				Debug.Send("Loading recipe", "Added server recipe for " + serverRecipe.getResult());
				loaded.put(recipe.getKey(), serverRecipe);
			}
		} else {
			Debug.Send("Loading recipe", "Didn't add server recipe for " + recipe.getKey() + " because a similar one was already loaded: " + alwaysSimilar.toString() + " with the result " + alwaysSimilar.getResult().toString());
		}
	}

	private String loadCategories(@NonNull final EnhancedRecipe recipe) {
		String categoryName = recipe.getRecipeCategory();
		if (recipe instanceof FurnaceRecipe)
			categoryName = categoryName == null || categoryName.isEmpty() ? "furnace" : categoryName;
		else if (recipe instanceof BrewingRecipe)
			categoryName = categoryName == null || categoryName.isEmpty() ? "brewing" : categoryName;
		else
			categoryName = categoryName == null || categoryName.isEmpty() ? "default" : categoryName;

		if (recipe.getRecipeCategory() == null)
			recipe.setRecipeCategory(categoryName);
		CategoryData recipeCategory = this.categoryDataCache.get(categoryName);

		final List<RecipeCoreData> recipeCoreDataList;
		if (recipeCategory != null) {
			recipeCoreDataList = recipeCategory.getRecipeCoreData();
			if (recipeCoreDataList.stream().noneMatch(cachedRecipe -> cachedRecipe.getKey().equals(recipe.getKey())))
				//if (!enhancedRecipeList.contains(recipe))
				recipeCoreDataList.add(new RecipeCoreData(recipe));
		} else {
			final ItemStack itemStack;
			if (recipe instanceof FurnaceRecipe)
				itemStack = new ItemStack(Adapter.getMaterial("FURNACE"));
			else if (recipe instanceof BrewingRecipe)
				itemStack = new ItemStack(Adapter.getMaterial("BREWING_STAND"));
			else
				itemStack = new ItemStack(Adapter.getMaterial("CRAFTING_TABLE"));
			recipeCategory = this.categoryDataCache.of(categoryName, itemStack, null);
			recipeCategory.addEnhancedRecipes(recipe);
		}
		this.categoryDataCache.put(categoryName, recipeCategory);

		return categoryName;
	}

	public void cacheSimilarVanillaRecipe(final EnhancedRecipe recipe) {
		if (!(recipe instanceof FurnaceRecipe)) return;
		Debug.Send("Start to add Furnace recipe");
		for (final Recipe r : serverRecipes) {
			if (!(r instanceof org.bukkit.inventory.FurnaceRecipe)) continue;
			if (recipe.getContent().length <= 0 || recipe.getContent()[0] == null) continue;

			final org.bukkit.inventory.FurnaceRecipe serverRecipe = (org.bukkit.inventory.FurnaceRecipe) r;
			final ItemStack itemStack = serverRecipe.getInput();
			Debug.Send("Added Furnace recipe for " + serverRecipe.getResult());

			if (recipe.getContent()[0].getType() == itemStack.getType()) {
				this.similarVanillaRecipe.put(serverRecipe.getInput(), serverRecipe.getResult());
				//Adapter.GetFurnaceRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(itemStack.getType().name().toLowerCase()), itemStack, serverRecipe.getResult().getType(), serverRecipe.getCookingTime(), getExp(itemStack.getType()));
			}
		}
	}

	public List<Recipe> getLoadedServerRecipes() {
		return new ArrayList<>(loaded.values());
	}

	public void printGroupsDebugInfo() {
		if (!Debug.isGeneralDebugEnable()) return;

		for (final Entry<String, RecipeGroup> recipeGrouping : mappedGroupedRecipes.entrySet()) {
			Debug.Send("Groups for recipes of type: " + recipeGrouping.getKey());
			final RecipeGroup group = recipeGrouping.getValue();
			Debug.Send("<group>");
			Debug.Send("Enhanced recipes: " + group.getRecipeCoreList().stream().filter(Objects::nonNull).map(x -> x.getResult().toString()).collect(Collectors.joining("\nEnhanced recipes: ")));
			Debug.Send("Server recipes: " + group.getServerRecipes().stream().filter(Objects::nonNull).map(x -> x.getResult().toString()).collect(Collectors.joining("\nEnhanced recipes: ")));
		}
	}

	public boolean disableServerRecipe(final Recipe r) {
		if (serverRecipes.contains(r)) {
			Debug.Send("[Recipe Loader] disabling server recipe for " + r.getResult().getType().name());

			serverRecipes.remove(r);
			disabledServerRecipes.add(r);
			unloadRecipe(r);

			//final RecipeType type = RecipeType.getType(r);
			for (final RecipeGroup recipeGroup : mappedGroupedRecipes.values()) {
				if (recipeGroup.getServerRecipes().contains(r)) {
					recipeGroup.getServerRecipes().remove(r);
					final RecipeCoreData recipeCoreData = recipeGroup.getRecipeCoreList().stream().filter(x -> x.isAlwaysSimilar(r)).findFirst().orElse(null);
					//If there's a recipe that's always similar, we have to load it again.
					if (recipeCoreData != null) {
						final EnhancedRecipe enhancedRecipe = recipeCoreData.getEnhancedRecipe();
						if (enhancedRecipe != null) {
							loaded.put(recipeCoreData.getKey(), enhancedRecipe.getServerRecipe());
							server.addRecipe(enhancedRecipe.getServerRecipe());
						}
					}
				}
			}
		}
		return false;
	}

	public boolean enableServerRecipe(final Recipe r) {
		if (!serverRecipes.contains(r)) {
			Debug.Send("Recipe Loader", " enabling server recipe for " + r.getResult().getType().name());
			serverRecipes.add(r);
			disabledServerRecipes.remove(r);
			if (server.getRecipe(r.getResult().getType().getKey()) == null)
				server.addRecipe(r);

			final RecipeType type = RecipeType.getType(r);
			if (type != null) {
				for (final RecipeGroup recipeGroup : mappedGroupedRecipes.values()) {
					if (recipeGroup.getRecipeCoreList().stream().anyMatch(x -> x.isSimilar(r))) {
						recipeGroup.getServerRecipes().add(r);
						final RecipeCoreData recipeCoreData = recipeGroup.getRecipeCoreList().stream().filter(x -> x.isAlwaysSimilar(r)).findFirst().orElse(null);
						//If there's a recipe that's always similar, we have to unload it again.
						if (recipeCoreData != null)
							loaded.remove(recipeCoreData.getKey());
					}
				}
			}
			return true;
		}
		return false;
	}


	@Nonnull
	private Pair<String, RecipeGroup> getRecipeGroup(final String categoryName) {
		RecipeGroup recipeGroup = mappedGroupedRecipes.get(categoryName);
		if (recipeGroup == null)
			recipeGroup = new RecipeGroup();
		String groupName = getGroupName(categoryName, recipeGroup);
		if (groupName != null) {
			recipeGroup = mappedGroupedRecipes.get(groupName);
			if (recipeGroup == null)
				recipeGroup = new RecipeGroup();
		}
		return new Pair<>(groupName, recipeGroup);
	}

	@Nullable
	private String getGroupName(final String categoryName, final RecipeGroup groupedRecipes) {
		String groupName = categoryName;

		if (groupedRecipes.getRecipeCoreList().size() > recipeSize) {
			Debug.Send("Recipe group", "Current group '" + groupName + "' have more than " + groupedRecipes.getRecipeCoreList().size() + ", creating new group to add recipes inside.");
			int index = 0;
			while (checkGroupWithSpace(categoryName, index)) {
				index++;
			}
			groupName += index;
			return groupName;
		}
		Debug.Send("Recipe group", "Current group '" + groupName + "' have " + groupedRecipes.getRecipeCoreList().size() + ", it will add the recipe to the current group.");
		return groupName;
	}

	private boolean checkGroupWithSpace(final String categoryName, final int index) {
		final RecipeGroup recipeGroup = mappedGroupedRecipes.get(categoryName + index);
		if (recipeGroup != null && recipeGroup.getRecipeCoreList().size() < recipeSize + 1) {
			return false;
		}
		return recipeGroup != null;
	}

	public EnhancedRecipe getLoadedRecipes(final Predicate<? super EnhancedRecipe> predicate) {
		return this.getLoadedRecipes().stream().filter(predicate).findFirst().orElse(null);
	}

	public void disableServerRecipes(final List<Recipe> disabledServerRecipes) {
		//No need to be efficient here, this'll only run once.
		disabledServerRecipes.forEach(this::disableServerRecipe);
	}

	public void clearCache() {
		this.serverRecipes = new ArrayList<>();
		this.disabledServerRecipes = new ArrayList<>();
		this.similarVanillaRecipe = new HashMap<>();
		this.loaded = new HashMap<>();
		this.loadedRecipes = new ArrayList<>();
		this.mappedGroupedRecipes = new HashMap<>();
	}
}
