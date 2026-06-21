package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeRegistry;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.EnchantedCraftWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.FurnaceBurnWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.VanillaCraftWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.VanillaFurnaceWrapper;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.CategoryDataCache;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
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
	private List<Recipe> serverRecipes = new ArrayList<>();
	@Getter
	private List<Recipe> disabledServerRecipes = new ArrayList<>();
	@Getter
	private Map<ItemStack, ItemStack> similarVanillaRecipe = new HashMap<>();
	@Getter
	private Map<String, RecipeGroup> mappedGroupedRecipes = new HashMap<>();

	private final Map<RecipeType, RecipeRegistry> mappedRecipes = new HashMap<>();

	private RecipeLoader(final Server server, final CategoryDataCache categoryDataCache) {
		this.server = server;
		try {
			server.recipeIterator().forEachRemaining(serverRecipes -> {
				this.serverRecipes.add(serverRecipes);

				final ItemStack[] ingredients = Adapter.getIngredients(serverRecipes);
				if (serverRecipes instanceof org.bukkit.inventory.FurnaceRecipe) {
					liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) serverRecipes), ingredients);
				} else
					liveCacheRecipe(new VanillaCraftWrapper(serverRecipes), ingredients);
			});
		} catch (IllegalArgumentException e) {
			self().getLogger().log(Level.SEVERE, "This server recipe contains air, will not be loaded.", e);
		}
		this.categoryDataCache = categoryDataCache;
	}

	public static RecipeLoader getInstance() {
		return instance == null ? instance = new RecipeLoader(Bukkit.getServer(), self().getCategoryDataCache()) : instance;
	}

	public static void clearInstance() {
		for (final Entry<String, EnhancedRecipe> loaded : self().getCacheRecipes().getRecipesMap().entrySet()) {
			instance.unloadRecipe(loaded.getValue().getServerRecipe());
		}
		instance = null;
	}

	@Nonnull
	public List<RecipeWrapper> findMatchingRecipe(@Nonnull final RecipeType recipeType, final ItemStack[] matrix) {
		final RecipeRegistry recipeCached = this.mappedRecipes.get(recipeType);
		if(recipeCached == null)
			return Collections.emptyList();
		return recipeCached.findMatchingRecipes(matrix);
	}

	public void loadRecipe(@NonNull final EnhancedRecipe recipe) {
		loadRecipe(recipe, false);
	}

	public void loadRecipe(@NonNull final EnhancedRecipe recipe, final boolean isReloading) {
		if (recipe.validate() != null) {
			Messenger.Error("(loadRecipe) There's an issue with recipe " + recipe.getKey() + ": " + recipe.validate());
			return;
		}

		//final boolean containsRecipe = loaded.containsKey(recipe.getKey());
		final RecipeRegistry containsRecipe = this.mappedRecipes.get(recipe.getType());
		//if (containsRecipe)
		//	unloadRecipe(recipe);

		final List<Recipe> similarServerRecipes = new ArrayList<>();
		for (final Recipe r : serverRecipes) {
			if (!Adapter.recipeContainsNamespace(r) && recipe.sharesIngredientWith(r)) {
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
		//Only load the recipe if there is not a server recipe that's always similar.
		final Recipe serverRecipe = recipe.getServerRecipe(groupName);
		if (alwaysSimilar == null) {
			if (!(recipe instanceof BrewingRecipe)) {
				if (serverRecipe == null) {
					Debug.Send("Loading recipe", "Added server recipe is null for " + recipe.getKey());
					self().getLogger().log(Level.WARNING, "Recipe " + recipe.getKey() + " will not be cached because the result is null or invalid material type.");
					return;
				}
				final boolean alreadyRegistered = isAlreadyRegistered(recipe, containsRecipe);
				if (!alreadyRegistered && !isReloading) {
					server.addRecipe(serverRecipe);
				}
				Debug.Send("Loading recipe", "Added server recipe for " + serverRecipe.getResult());
			}
		} else {
			Debug.Send("Loading recipe", "Didn't add server recipe for " + recipe.getKey() + " because a similar one was already loaded: " + alwaysSimilar.toString() + " with the result " + alwaysSimilar.getResult().toString());
		}
		ItemStack[] content = recipe.getContent();
		if (recipe instanceof WBRecipe)
			liveCacheRecipe(new EnchantedCraftWrapper((WBRecipe) recipe), content);
		else if (recipe instanceof FurnaceRecipe)
			liveCacheRecipe(new FurnaceBurnWrapper((FurnaceRecipe) recipe), content);
		else if (recipe instanceof BrewingRecipe) {
			liveCacheRecipe(new BrewingWrapper((BrewingRecipe) recipe), new ItemStack[]{recipe.getResult()});
		}
	}


	public void liveCacheRecipe(@Nonnull final RecipeWrapper recipe, @Nonnull final ItemStack[] content) {
		this.mappedRecipes.computeIfAbsent(recipe.getRecipeType(), material -> new RecipeRegistry()).addRecipe(recipe, content);
	}

	public void unloadAll() {
		disabledServerRecipes.forEach(x ->
				enableServerRecipe(x)
		);
		mappedGroupedRecipes.clear();
		mappedRecipes.clear();
		serverRecipes.clear();
		unloadCehRecipes();
	}

	public void unloadRecipe(final EnhancedRecipe recipe) {
		final CategoryData categoryData = categoryDataCache.get(recipe.getRecipeCategory());
		if (categoryData != null && categoryData.getEnhancedRecipes() != null)
			categoryData.getEnhancedRecipes().remove(recipe);

		final RecipeRegistry recipeRegistry = mappedRecipes.get(recipe.getType());
		if (recipeRegistry != null) {
			recipeRegistry.removeRecipe(recipe, recipe.getContent());
		}

		final RecipeGroup group = findGroup(recipe);
		if (group == null) {
			//Messenger.Error("Could not unload recipe from group, because the recipe doesn't have a group.");
			printGroupsDebugInfo();
			return;
		}
		//Remove entire recipe group if it's the last enhanced recipe, or remove a single recipe from the group.
		if (group.getRecipeGroupSize() == 1)
			mappedGroupedRecipes.remove(recipe.getGroup());
		else {
			group.remove(recipe);
			unloadRecipe(recipe.getServerRecipe());
		}
		Debug.Send("Unloaded a recipe");
		printGroupsDebugInfo();
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
		List<Recipe> recipes = new ArrayList<>();
		this.server.recipeIterator().forEachRemaining(recipes::add);
		return recipes;
	}

	public void printGroupsDebugInfo() {
		if (!Debug.isGeneralDebugEnable()) return;

		for (final Entry<RecipeType, RecipeRegistry> recipeGrouping : mappedRecipes.entrySet()) {
			Debug.Send("Groups for recipes of type: " + recipeGrouping.getKey());
			final RecipeRegistry group = recipeGrouping.getValue();

			Debug.Send("Recipes cached:\n " + group.getMappedRecipes().values().stream()
					.filter(Objects::nonNull).map(x ->
							x.stream().map(Object::toString).toString())
					.collect(Collectors.joining("\nRecipes: ")));
		}
	}

	public boolean disableServerRecipe(final Recipe r) {
		if (serverRecipes.contains(r)) {
			Debug.Send("[Recipe Loader] disabling server recipe for " + r.getResult().getType().name());

			serverRecipes.remove(r);
			disabledServerRecipes.add(r);
			unloadRecipe(r);

			final ItemStack[] ingredients = Adapter.getIngredients(r);
			if (serverRecipes instanceof org.bukkit.inventory.FurnaceRecipe)
				liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) r), ingredients);
			else
				liveCacheRecipe(new VanillaCraftWrapper(r), ingredients);
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
				for (final RecipeRegistry recipeRegistry : mappedRecipes.values()) {
					final ItemStack[] ingredients = Adapter.getIngredients(r);
					List<RecipeWrapper> similarRecipes = recipeRegistry.findMatchingRecipes(ingredients);
					if (!similarRecipes.isEmpty()) {
						if (r instanceof org.bukkit.inventory.FurnaceRecipe) {
							recipeRegistry.addRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) r), ingredients);
						} else {
							recipeRegistry.addRecipe(new VanillaCraftWrapper(r), ingredients);
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	public void disableServerRecipes(final List<Recipe> disabledServerRecipes) {
		//No need to be efficient here, this'll only run once.
		disabledServerRecipes.forEach(this::disableServerRecipe);
	}

	public void clearCache() {
		this.serverRecipes = new ArrayList<>();
		this.disabledServerRecipes = new ArrayList<>();
		this.similarVanillaRecipe = new HashMap<>();
		this.mappedGroupedRecipes = new HashMap<>();
		this.mappedRecipes.clear();
	}

	private boolean isAlreadyRegistered(@Nonnull final EnhancedRecipe recipe, @Nullable final RecipeRegistry containsRecipe) {
		Map<Material, Set<RecipeWrapper>> recipes = containsRecipe != null ? containsRecipe.getMappedRecipes() : new HashMap<>();
		boolean alreadyRegistered = false;
		for (ItemStack stack : recipe.getContent()) {
			if (stack == null) continue;
			Set<RecipeWrapper> recipeWrappers = recipes.get(stack.getType());
			if (recipeWrappers == null) continue;

			if (!recipeWrappers.isEmpty() && recipeWrappers.stream().allMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey()))) {
				alreadyRegistered = true;
			}
		}
		if(recipe instanceof FurnaceRecipe){
			System.out.println("recipe alreadyRegistered " + recipe.getKey());
			System.out.println("recipe alreadyRegistered " + recipe.getResult());
			System.out.println("recipe alreadyRegistered " + alreadyRegistered);
		}
		return alreadyRegistered;
	}

	@Nullable
	private String addToGroup(final List<Recipe> serverRecipes, final EnhancedRecipe enhancedRecipe, final String categoryName) {
		final Pair<String, RecipeGroup> recipeGroupPair = getRecipeGroup(categoryName);
		final String first = recipeGroupPair.getFirst();
		Debug.Send("Recipe group", "is does now add recipe to the group: " + first);

		RecipeGroup recipeGroup = recipeGroupPair.getSecond();
		if (recipeGroup == null) {
			recipeGroup = new RecipeGroup(first);
		}
		//recipeGroup.putCustomRecipe(EnhancedRecipeWrapper.of(enhancedRecipe));
		//recipeGroup.addAllServerRecipes(serverRecipes);

		mappedGroupedRecipes.put(first, recipeGroup);

		return first;
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

		final List<EnhancedRecipeWrapper> enhancedRecipeWrapperList;
		if (recipeCategory != null) {
			enhancedRecipeWrapperList = recipeCategory.getRecipeCoreData();
			if (enhancedRecipeWrapperList.stream().noneMatch(cachedRecipe -> cachedRecipe.getKey().equals(recipe.getKey())))
				//if (!enhancedRecipeList.contains(recipe))
				enhancedRecipeWrapperList.add(new EnhancedRecipeWrapper(recipe));
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

	private RecipeGroup getCraftingRecipeGroup(final Recipe recipe) {
		String recipeGroup = Adapter.getGroup(recipe);
		Debug.Send(Type.Crafting, () -> "Attempt to find the group for recipe. Group name: '" + (recipeGroup.isEmpty() ? "No group set" : recipeGroup) + "'");
		return mappedGroupedRecipes.get(recipeGroup);
	}

	@Nonnull
	private Pair<String, RecipeGroup> getRecipeGroup(final String categoryName) {
		RecipeGroup recipeGroup = mappedGroupedRecipes.get(categoryName);
		if (recipeGroup == null)
			recipeGroup = new RecipeGroup(categoryName);
		String groupName = getGroupName(categoryName, recipeGroup);
		if (groupName != null) {
			recipeGroup = mappedGroupedRecipes.get(groupName);
			if (recipeGroup == null)
				recipeGroup = new RecipeGroup(groupName);
		}
		return new Pair<>(groupName, recipeGroup);
	}

	@Nullable
	private String getGroupName(final String categoryName, final RecipeGroup groupedRecipes) {
		String groupName = categoryName;

		final int recipeGroupSize = groupedRecipes.getRecipeGroupSize();
		if (recipeGroupSize > recipeSize) {
			Debug.Send("Recipe group", "Current group '" + groupName + "' have more than " + recipeGroupSize + ", creating new group to add recipes inside.");
			int index = 0;
			while (checkGroupWithSpace(categoryName, index)) {
				index++;
			}
			groupName += index;
			return groupName;
		}
		Debug.Send("Recipe group", "Current group '" + groupName + "' have " + recipeGroupSize + ", it will add the recipe to the current group.");
		return groupName;
	}

	private boolean checkGroupWithSpace(final String categoryName, final int index) {
		final RecipeGroup recipeGroup = mappedGroupedRecipes.get(categoryName + index);
		if (recipeGroup != null && recipeGroup.getRecipeGroupSize() < recipeSize + 1) {
			return false;
		}
		return recipeGroup != null;
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

	public boolean isLoadedAsServerRecipe(final EnhancedRecipe recipe) {
		return mappedGroupedRecipes.containsKey(recipe.getGroup());
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

				if (group.isSimilarResult(result, recipeType)) {
					originGroups.add(group);
				} else if (group.isServerRecipe(result)) {
					originGroups.add(group);
				}
				if (recipeType == RecipeType.FURNACE && group.isSimilarResultType(result.getType(), recipeType)) {
					originGroups.add(group);
				}
			}
			if (!originGroups.isEmpty()) {
				return originGroups;
			}
			for (final RecipeGroup group : mappedGroupedRecipes.values()) {
				if (group.isSimilarResult(result, recipeType))
					originGroups.add(group);
				else if (group.isServerRecipe(result))
					originGroups.add(group);
			}
			return originGroups;
		}
		RecipeGroup group = getCraftingRecipeGroup(recipe);
		if (group != null) {
			if (recipe instanceof CookingRecipe) {
				if (group.isSimilarContent(result))
					originGroups.add(group);
				else if (group.isServerRecipe(result))
					originGroups.add(group);
				else if (group.isServerRecipe(x -> {
					if (x instanceof CookingRecipe<?>)
						return result.isSimilar(((CookingRecipe<?>) x).getInput());
					return false;
				})) {
					originGroups.add(group);
				}
			} else {
				if (group.isSimilarResult(result, recipeType))
					originGroups.add(group);
				else if (group.isServerRecipe(result))
					originGroups.add(group);
			}
		} else {
			if (recipe instanceof CookingRecipe) {
				Debug.Send(Type.Smelting, () -> "No group found, will attempt to find group by looking trough cached recipes with recipe type: " + recipeType);
				String cokingGroup = ((CookingRecipe<?>) recipe).getGroup();
				if (cokingGroup.isEmpty()) {
					final Set<String> seenGroups = new HashSet<>();
					CacheRecipes cacheRecipes = self().getCacheRecipes();
					for (String groupKey : cacheRecipes.getGroupsForType(recipeType)) {
						if (!seenGroups.add(groupKey)) continue;

						final RecipeGroup recipeGroup = mappedGroupedRecipes.get(groupKey);
						if (recipeGroup == null) continue;

						if (recipeGroup.isSimilarResult(result, recipeType)) {
							originGroups.add(recipeGroup);
						} else if (recipeGroup.isServerRecipe(result)) {
							originGroups.add(recipeGroup);
						}
						if (recipeType == RecipeType.FURNACE && recipeGroup.isSimilarResultType(result.getType(), recipeType)) {
							originGroups.add(recipeGroup);
						}
					}
				}
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

			if (group.isSimilarResult(result, type)) {
				originGroups.add(group);
			} else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult()))) {
				originGroups.add(group);
			}
		}

		if (!originGroups.isEmpty()) {
			return originGroups;
		}

		for (final RecipeGroup group : mappedGroupedRecipes.values()) {
			if (group.isSimilarResult(result, type))
				originGroups.add(group);
			else if (group.getServerRecipes().stream().anyMatch(x -> result.isSimilar(x.getResult())))
				originGroups.add(group);
		}
		return originGroups;
	}


}
