package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeRegistry;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.EnchantedCraftWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.EnchantedFurnaceRecipeWrapper;
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
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
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
	private List<Recipe> disabledServerRecipes = new ArrayList<>();
	private final Map<RecipeType, RecipeRegistry> mappedRecipes = new HashMap<>();

	private RecipeLoader(final Server server, final CategoryDataCache categoryDataCache) {
		this.server = server;
		updateServerRecipes();
		this.categoryDataCache = categoryDataCache;
	}

	public static RecipeLoader getInstance() {
		return instance == null ? instance = new RecipeLoader(Bukkit.getServer(), self().getCategoryDataCache()) : instance;
	}

	public static void refreshInstance() {
		instance.unloadAllCehRecipes();
		instance = null;
		RecipeLoader loader = getInstance();
		final List<Recipe> collect = self().getFm().readDisabledServerRecipes().stream().map(x ->
				Adapter.FilterRecipes(loader.getLoadedServerRecipes(), x)
		).collect(Collectors.toList());
		loader.disableServerRecipes(collect);
		self().getCacheRecipes().getRecipes().forEach((string, enhancedRecipe) -> loader.loadRecipe(enhancedRecipe));
		loader.printGroupsDebugInfo();
	}

	@Nonnull
	public List<RecipeWrapper> findMatchingRecipe(@Nonnull final RecipeType recipeType, final ItemStack[] matrix) {
		final RecipeRegistry recipeCached = this.mappedRecipes.get(recipeType);

		if (recipeCached != null) {
			Debug.send(recipeType, "Find_matching_recipes", () -> "Found the group for this type: " + recipeType);
		} else {
			Debug.send(recipeType, "Find_matching_recipes", () -> "Did not find this group in cache: " + recipeType);
		}

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
		for (final Recipe r : this.getLoadedServerRecipes()) {
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
		String groupName = recipe.getGroup() != null ? recipe.getGroup() : categoryName;
		Debug.send(Type.Other, "Loading recipe", () -> "Added to recipe group with the name: '" + groupName + "'");
		if (recipe.getGroup() == null)
			recipe.setGroup(groupName);
		final Recipe serverRecipe = recipe.getServerRecipe();

		//Only load the recipe if there is not a server recipe that's always similar.
		if (alwaysSimilar == null) {
			if (!(recipe instanceof BrewingRecipe)) {
				if (serverRecipe == null) {
					Debug.send(Type.Other, "Loading recipe", () -> "Added server recipe is null for " + recipe.getKey());
					self().getLogger().log(Level.WARNING, "Recipe " + recipe.getKey() + " will not be cached because the result is null or invalid material type.");
					return;
				}
				final boolean alreadyRegistered = isAlreadyRegistered(recipe, containsRecipe);
				if (!alreadyRegistered && !isReloading) {
					server.addRecipe(serverRecipe);
				}
				Debug.send(Type.Other, "Loading recipe", () -> "Added server recipe for " + serverRecipe.getResult().getType());
			}
		} else {
			Debug.send(Type.Other, "Loading recipe", () -> "Didn't add server recipe for " + recipe.getKey() + " because a similar one was already loaded: " + recipe.getKey() + " with the result " + recipe.getResult().getType());
		}
		ItemStack[] content = recipe.getContent();
		if (recipe instanceof WBRecipe)
			liveCacheRecipe(new EnchantedCraftWrapper((WBRecipe) recipe), content);
		else if (recipe instanceof FurnaceRecipe)
			liveCacheRecipe(new EnchantedFurnaceRecipeWrapper((FurnaceRecipe) recipe), content);
		else if (recipe instanceof BrewingRecipe) {
			liveCacheRecipe(new BrewingWrapper((BrewingRecipe) recipe), new ItemStack[]{recipe.getResult()});
		}
	}


	public void liveCacheRecipe(@Nonnull final RecipeWrapper recipe, @Nonnull final ItemStack[] content) {
		Debug.send(Type.Other, "Loading recipe", () -> "Added server recipe to fast lookup cache: " + recipe.getRecipeType());
		Debug.send(Type.Other, "Loading recipe", () -> "Fast lookup cache recipe key: " + recipe.getRecipeKey());
		Debug.send(Type.Other, "Loading recipe", () -> "Fast lookup cache contents: " + RecipeDebug.convertItemStackArrayToString(content));
		this.mappedRecipes.computeIfAbsent(recipe.getRecipeType(), material -> new RecipeRegistry()).addRecipe(recipe, content);
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
		Debug.send(Type.Other, "Unloading recipe", () -> "Unloaded the recipe: " + recipe.getKey());
		printGroupsDebugInfo();
	}

	public void addServerRecipes(@Nonnull final Recipe serverRecipe) {

	}

	public List<Recipe> getLoadedServerRecipes() {
		return mappedRecipes.values().stream()
				.flatMap(recipeRegistry -> recipeRegistry.getAllRecipes().stream())
				.filter(recipeWrapper -> recipeWrapper.getRecipe() != null && !recipeWrapper.isCustom())
				.map(RecipeWrapper::getRecipe)
				.collect(Collectors.toList());
	}

	public void printGroupsDebugInfo() {
		if (!Debug.isGeneralDebugEnable()) return;

		Debug.send(Type.Deep_lookup, "Check live cached recipes", () -> {
			for (final Entry<RecipeType, RecipeRegistry> recipeGrouping : mappedRecipes.entrySet()) {
				Debug.send(Type.Deep_lookup, "Check live cached recipes", () -> "Groups for recipes of type: " + recipeGrouping.getKey() + "\n");
				final RecipeRegistry group = recipeGrouping.getValue();

				Debug.send(Type.Deep_lookup, "Check live cached recipes", () -> "Recipes cached:\n " + group.getMappedRecipes().values().stream()
						.filter(Objects::nonNull).map(x ->
								x.stream().map(Object::toString)
										.collect(Collectors.joining("\nRecipe:\n")))
						.collect(Collectors.joining(",\n")));
			}
			return "";
		});
	}

	public boolean disableServerRecipe(final Recipe r) {
		final RecipeType type = RecipeType.getType(r);
		final RecipeRegistry recipeRegistry = this.mappedRecipes.get(type);

		if (recipeRegistry != null && recipeRegistry.removeRecipe(r)) {
			Debug.send(Type.Other, "Unloading recipe", () -> "Disabling server recipe for " + r.getResult().getType().name());
			disabledServerRecipes.add(r);
			unloadRecipe(r);
		}
		return false;
	}

	public boolean enableServerRecipe(final Recipe recipe) {
		final RecipeType type = RecipeType.getType(recipe);
		final RecipeRegistry recipeRegistry = this.mappedRecipes.get(type);

		if (recipeRegistry != null) {
			Debug.send(Type.Other, "Loading recipe", () -> "Enabling server recipe for " + recipe.getResult().getType().name());
			disabledServerRecipes.remove(recipe);
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
				final NamespacedKey namespacedKey = Adapter.getNamespacedKey(recipe);
				if (namespacedKey != null && server.getRecipe(namespacedKey) == null)
					server.addRecipe(recipe);
			} else {
				server.addRecipe(recipe);
			}
			final ItemStack[] ingredients = Adapter.getIngredients(recipe);
			if (Adapter.isCookingRecipe(recipe)) {
				if (self().getVersionChecker().newerThan(ServerVersion.v1_13) && recipe instanceof CookingRecipe) {
					liveCacheRecipe(new VanillaCookingWrapper((CookingRecipe<?>) recipe), ingredients);
				} else {
					liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) recipe), ingredients);
				}
			} else
				liveCacheRecipe(new VanillaCraftWrapper(recipe), ingredients);
		}
		return true;
	}

	public void disableServerRecipes(final List<Recipe> disabledServerRecipes) {
		//No need to be efficient here, this'll only run once.
		disabledServerRecipes.forEach(this::disableServerRecipe);
	}

	public void clearCache() {
		this.disabledServerRecipes = new ArrayList<>();
		this.mappedRecipes.clear();
	}

	public void updateServerRecipes() {
		server.recipeIterator().forEachRemaining(serverRecipe -> {
			try {
				if (Adapter.isRecipeCustom(serverRecipe) || self().getCacheRecipes().isCustomRecipe(serverRecipe)) {
					return;
				}

				final ItemStack[] ingredients = Adapter.getIngredients(serverRecipe);
				if (Adapter.isCookingRecipe(serverRecipe)) {
					if (self().getVersionChecker().newerThan(ServerVersion.v1_13) && serverRecipe instanceof CookingRecipe) {
						liveCacheRecipe(new VanillaCookingWrapper((CookingRecipe<?>) serverRecipe), ingredients);
					} else {
						liveCacheRecipe(new VanillaFurnaceWrapper((org.bukkit.inventory.FurnaceRecipe) serverRecipe), ingredients);
					}
				} else
					liveCacheRecipe(new VanillaCraftWrapper(serverRecipe), ingredients);
			} catch (IllegalArgumentException e) {
				self().getLogger().log(Level.SEVERE, "This server recipe contains air, will not be loaded.", e);
			}
		});
	}

	private boolean isAlreadyRegistered(@Nonnull final EnhancedRecipe recipe,
	                                    @Nullable final RecipeRegistry containsRecipe) {
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
		final Iterator<Recipe> it = server.recipeIterator();
		while (it.hasNext()) {
			final Recipe currentRecipe = it.next();
			if (self().getVersionChecker().newerThan(ServerVersion.v1_12)) {
				final NamespacedKey namespacedKey = Adapter.getNamespacedKey(r);
				final NamespacedKey currentRecipeKey = Adapter.getNamespacedKey(currentRecipe);
				if (currentRecipeKey == null || namespacedKey == null) {
					continue;
				}
				if (currentRecipeKey.equals(namespacedKey)) {
					it.remove();
					return;
				}
			} else {
				if (currentRecipe.equals(r)) {
					it.remove();
					return;
				}
			}
		}
	}
}
