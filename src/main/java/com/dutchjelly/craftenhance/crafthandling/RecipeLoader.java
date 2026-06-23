package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeRegistry;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.EnchantedCraftWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.FurnaceBurnWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.VanillaCookingWrapper;
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
import com.dutchjelly.craftenhance.messaging.Debug.DebugContext;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

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
	private static final DebugContext loading_recipe = DebugContext.of(Type.Other, "Loading recipe");
	private static final DebugContext unloading_recipe = DebugContext.of(Type.Other, "Unloading recipe");
	private final int recipeSize = 20;
	private final CategoryDataCache categoryDataCache;
	private final Server server;
	@Getter
	private Set<Recipe> serverRecipes = new HashSet<>();
	@Getter
	private List<Recipe> disabledServerRecipes = new ArrayList<>();
	@Getter
	private Map<String, RecipeGroup> mappedGroupedRecipes = new HashMap<>();
	private final Map<RecipeType, RecipeRegistry> mappedRecipes = new HashMap<>();

	private RecipeLoader(final Server server, final CategoryDataCache categoryDataCache) {
		this.server = server;
		updateServerRecipes();
		this.categoryDataCache = categoryDataCache;
	}

	public static RecipeLoader getInstance() {
		return instance == null ? instance = new RecipeLoader(Bukkit.getServer(), self().getCategoryDataCache()) : instance;
	}

	public static void clearInstance() {
		instance.unloadAllCehRecipes();
		instance = null;
	}

	@Nonnull
	public List<RecipeWrapper> findMatchingRecipe(@Nonnull final RecipeType recipeType, final ItemStack[] matrix) {
		final RecipeRegistry recipeCached = this.mappedRecipes.get(recipeType);
		if (recipeType == RecipeType.WORKBENCH)
			Debug.Send(Type.Crafting, () -> "Found the group for this type: " + recipeType);

		if (recipeCached == null)
			return Collections.emptyList();
		return recipeCached.findMatchingRecipes(recipeType, matrix);
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
				alwaysSimilar = r;
				break;
			}
		}

		String categoryName = loadCategories(recipe);
		String groupName = addToGroup(categoryName);
		Debug.Send(loading_recipe, () -> "Added to recipe group with the name: '" + groupName + "'");
		//Only load the recipe if there is not a server recipe that's always similar.
		final Recipe serverRecipe = recipe.getServerRecipe(groupName);
		if (alwaysSimilar == null) {
			if (!(recipe instanceof BrewingRecipe)) {
				if (serverRecipe == null) {
					Debug.Send(loading_recipe, () -> "Added server recipe is null for " + recipe.getKey());
					self().getLogger().log(Level.WARNING, "Recipe " + recipe.getKey() + " will not be cached because the result is null or invalid material type.");
					return;
				}
				final boolean alreadyRegistered = isAlreadyRegistered(recipe, containsRecipe);
				if (!alreadyRegistered && !isReloading) {
					server.addRecipe(serverRecipe);
				}
				Debug.Send(loading_recipe, () -> "Added server recipe for " + serverRecipe.getResult().getType());
			}
		} else {
			Debug.Send(loading_recipe, () -> "Didn't add server recipe for " + recipe.getKey() + " because a similar one was already loaded: " + recipe.getKey() + " with the result " + recipe.getResult().getType());
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
		Debug.Send(loading_recipe, () -> "Added server recipe to fast lookup cache: " + recipe.getRecipeType());
		Debug.Send(loading_recipe, () -> "Fast lookup cache recipe key: " + recipe.getRecipeKey());
		Debug.Send(loading_recipe, () -> "Fast lookup cache contents: " + RecipeDebug.convertItemStackArrayToString(content));
		this.mappedRecipes.computeIfAbsent(recipe.getRecipeType(), material -> new RecipeRegistry()).addRecipe(recipe, content);
	}

	public void unloadAll() {
		disabledServerRecipes.forEach(x ->
				enableServerRecipe(x)
		);
		mappedGroupedRecipes.clear();
		mappedRecipes.clear();
		unloadAllCehRecipes();
	}

	public void unloadRecipe(final EnhancedRecipe recipe) {
		final CategoryData categoryData = categoryDataCache.get(recipe.getRecipeCategory());
		if (categoryData != null && categoryData.getEnhancedRecipes() != null)
			categoryData.getEnhancedRecipes().remove(recipe);

		final RecipeRegistry recipeRegistry = mappedRecipes.get(recipe.getType());
		if (recipeRegistry != null) {
			recipeRegistry.removeRecipe(recipe, recipe.getContent());
		}
		unloadRecipe(recipe.getServerRecipe());

		Debug.Send(unloading_recipe, () -> "Unloaded the recipe: " + recipe.getKey());
		printGroupsDebugInfo();
	}

	public void addServerRecipes(@Nonnull final Recipe serverRecipe) {
		serverRecipes.add(serverRecipe);
	}

	public Set<Recipe> getLoadedServerRecipes() {
		return serverRecipes;
	}

	public void printGroupsDebugInfo() {
		if (!Debug.isGeneralDebugEnable()) return;
		final DebugContext debugContext = DebugContext.of(Type.Deep_lookup, "Check live cached recipes");
		Debug.Send(debugContext, () -> {
			for (final Entry<RecipeType, RecipeRegistry> recipeGrouping : mappedRecipes.entrySet()) {
				Debug.Send(debugContext, () -> "Groups for recipes of type: " + recipeGrouping.getKey() + "\n");
				final RecipeRegistry group = recipeGrouping.getValue();

				Debug.Send(debugContext, () -> "Recipes cached:\n " + group.getMappedRecipes().values().stream()
						.filter(Objects::nonNull).map(x ->
								x.stream().map(Object::toString)
										.collect(Collectors.joining("\nRecipe:\n")))
						.collect(Collectors.joining(",\n")));
			}
			return "";
		});
	}

	public boolean disableServerRecipe(final Recipe r) {
		if (serverRecipes.remove(r)) {
			Debug.Send(unloading_recipe, () -> "Disabling server recipe for " + r.getResult().getType().name());
			disabledServerRecipes.add(r);
			unloadRecipe(r);

			final ItemStack[] ingredients = Adapter.getIngredients(r);
			if (Adapter.isCookingRecipe(r))
				liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) r), ingredients);
			else
				liveCacheRecipe(new VanillaCraftWrapper(r), ingredients);
		}
		return false;
	}

	public boolean enableServerRecipe(final Recipe r) {
		if (serverRecipes.add(r)) {
			Debug.Send(loading_recipe, () -> "Enabling server recipe for " + r.getResult().getType().name());
			disabledServerRecipes.remove(r);
			final NamespacedKey namespacedKey = Adapter.getNamespacedKey(r);
			if (namespacedKey != null && server.getRecipe(namespacedKey) == null)
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
		this.serverRecipes = new HashSet<>();
		this.disabledServerRecipes = new ArrayList<>();
		this.mappedGroupedRecipes = new HashMap<>();
		this.mappedRecipes.clear();
	}

	public void updateServerRecipes() {
		try {
			server.recipeIterator().forEachRemaining(serverRecipe -> {
				if (!Adapter.recipeContainsNamespace(serverRecipe)) return;
				if (self().getCacheRecipes().isCustomRecipe(serverRecipe)) return;

				this.serverRecipes.add(serverRecipe);
				final ItemStack[] ingredients = Adapter.getIngredients(serverRecipe);
				if (Adapter.isCookingRecipe(serverRecipe)) {
					if (self().getVersionChecker().newerThan(ServerVersion.v1_13) && serverRecipe instanceof CookingRecipe) {
						liveCacheRecipe(new VanillaCookingWrapper((CookingRecipe<?>) serverRecipe), ingredients);
					} else {
						liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) serverRecipe), ingredients);
					}
				} else
					liveCacheRecipe(new VanillaCraftWrapper(serverRecipe), ingredients);
			});
		} catch (IllegalArgumentException e) {
			self().getLogger().log(Level.SEVERE, "This server recipe contains air, will not be loaded.", e);
		}
	}

	private boolean isAlreadyRegistered(@Nonnull final EnhancedRecipe recipe, @Nullable final RecipeRegistry containsRecipe) {
		Map<Material, Set<RecipeWrapper>> recipes = containsRecipe != null ? containsRecipe.getMappedRecipes() : new HashMap<>();
		boolean alreadyRegistered = false;
		for (ItemStack stack : recipe.getContent()) {
			if (stack == null) continue;
			Set<RecipeWrapper> recipeWrappers = recipes.get(stack.getType());
			if (recipeWrappers == null) continue;

			if (!recipeWrappers.isEmpty() && recipeWrappers.stream().anyMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey()))) {
				alreadyRegistered = true;
			}
		}
		return alreadyRegistered;
	}

	@Nullable
	private String addToGroup(final String categoryName) {
		final Pair<String, RecipeGroup> recipeGroupPair = getRecipeGroup(categoryName);
		final String first = recipeGroupPair.getFirst();
		RecipeGroup recipeGroup = recipeGroupPair.getSecond();
		if (recipeGroup == null) {
			recipeGroup = new RecipeGroup(first);
		}
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
			final String finalGroupName = groupName;
			Debug.Send(loading_recipe, () -> "Current group '" + finalGroupName + "' have more than " + recipeGroupSize + ", creating new group to add recipes inside.");
			int index = 0;
			while (checkGroupWithSpace(categoryName, index)) {
				index++;
			}
			groupName += index;
			return groupName;
		}
		final String finalName = groupName;
		Debug.Send(loading_recipe, () -> "Current group '" + finalName + "' have " + recipeGroupSize + ", it will add the recipe to the current group.");
		return groupName;
	}

	private boolean checkGroupWithSpace(final String categoryName, final int index) {
		final RecipeGroup recipeGroup = mappedGroupedRecipes.get(categoryName + index);
		if (recipeGroup != null && recipeGroup.getRecipeGroupSize() < recipeSize + 1) {
			return false;
		}
		return recipeGroup != null;
	}

	private void unloadAllCehRecipes() {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_12)) {
			Iterator<Recipe> iterator = Bukkit.recipeIterator();
			while (iterator.hasNext()) {
				Recipe recipe = iterator.next();
				final NamespacedKey namespacedKey = Adapter.getNamespacedKey(recipe);
				if (namespacedKey == null) {
					continue;
				}
				if (namespacedKey.getNamespace().equalsIgnoreCase("craftenhance")) {
					iterator.remove();
				}
			}
		} else {
			final Iterator<Recipe> it = server.recipeIterator();
			while (it.hasNext()) {
				final Recipe r = it.next();
				if (Adapter.ContainsSubKey(r, ServerRecipeTranslator.KeyPrefix)) {
					it.remove();
				}
			}
		}
	}

	private void unloadRecipe(final Recipe r) {
		serverRecipes.remove(r);
		if (self().getVersionChecker().newerThan(ServerVersion.v1_12)) {
			Iterator<Recipe> iterator = Bukkit.recipeIterator();
			while (iterator.hasNext()) {
				Recipe recipe = iterator.next();
				final NamespacedKey namespacedKey = Adapter.getNamespacedKey(recipe);
				if (namespacedKey == null) {
					continue;
				}
				if (namespacedKey.getNamespace().equalsIgnoreCase("craftenhance") && recipe.equals(r)) {
					iterator.remove();
				}
			}
		} else {
			final Iterator<Recipe> it = server.recipeIterator();
			while (it.hasNext()) {
				final Recipe currentRecipe = it.next();
				if (currentRecipe.equals(r)) {
					it.remove();
					return;
				}
			}
		}
	}
}
