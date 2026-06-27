package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RecipeRegistry {
	private final Map<Material, Set<RecipeWrapper>> mappedRecipes = new HashMap<>();
	private final Set<RecipeWrapper> allRecipes = new HashSet<>();
	private final EnumSet<Material> matrixMaterials = EnumSet.noneOf(Material.class);
	private final List<RecipeWrapper> filteredResult = new ArrayList<>();

	public void addRecipe(@Nonnull final RecipeWrapper recipe, @Nonnull final ItemStack[] content) {
		this.allRecipes.add(recipe);

		final Set<Material> uniqueMaterials = getUniqueMaterials(content);
		for (Material type : uniqueMaterials) {
			if (type == null) continue;
			this.mappedRecipes.computeIfAbsent(type, material -> new HashSet<>()).add(recipe);
		}
	}

	public List<RecipeWrapper> findMatchingRecipes(@Nonnull final ItemStack[] matrix) {
		return findMatchingRecipes(null, matrix);
	}

	public List<RecipeWrapper> findMatchingRecipes(@Nullable final RecipeType recipeType,
	                                               @Nonnull final ItemStack[] matrix) {
		final EnumMap<Material, Integer> matrixMaterials = new EnumMap<>(Material.class);
		Debug.send(recipeType, "Find_matching_recipes", () -> "The recipe matrix to find a match: [" +
				Arrays.stream(matrix)
						.map(stack -> stack != null ? stack.getType().name() : null)
						.collect(Collectors.joining(",")) + "]"
		);
		int matrixSize = 0;
		for (ItemStack itemStack : matrix) {
			if (itemStack == null) continue;

			Material type = itemStack.getType();
			if (type != null && type != Material.AIR) {
				matrixMaterials.merge(type, 1, Integer::sum);
				matrixSize++;
			}
		}

		if (matrixMaterials.isEmpty()) {
			Debug.send(recipeType, "Find_matching_recipes",
					() -> "Was no recipe that matched the crafting matrix in the cache");
			return Collections.emptyList();
		}

		Material bestTrigger = null;
		int smallestCacheSize = Integer.MAX_VALUE;

		for (Material type : matrixMaterials.keySet()) {
			Set<RecipeWrapper> set = this.mappedRecipes.get(type);
			if (set == null || set.isEmpty()) continue;

			int size = set.size();
			if (size < smallestCacheSize) {
				smallestCacheSize = size;
				bestTrigger = type;
			}
		}

		if (bestTrigger == null) {
			Debug.send(recipeType, "Find_matching_recipes",
					() -> "Was no recipe that matched the best matched item type in the cache");
			return Collections.emptyList();
		}
		final Set<RecipeWrapper> smallestRecipeSet = this.mappedRecipes.get(bestTrigger);
		final List<RecipeWrapper> filteredResult = new ArrayList<>();
		System.out.println("matrixSize " + matrixSize);
		System.out.println("matrixSize " + matrixMaterials);
		System.out.println("matrixSize " + smallestRecipeSet);
		for (RecipeWrapper recipe : smallestRecipeSet) {
			if (canPossiblyMatch(matrixMaterials, matrixSize, recipe)) {
				filteredResult.add(recipe);
			}
		}
		Debug.send(recipeType, "Find_matching_recipes",
				() -> "The filtered matched recipes amount: " + filteredResult.size());

		filteredResult.sort(Comparator.comparingInt(RecipeWrapper::priority));

		//Debug.send(recipeType, "Find_matching_recipes", () -> "\n___________________Final recipes___________________" + (filteredResult.isEmpty() ? "'no match found" : "\n>" + filteredResult + "<\n") + "\n___________________Final recipes end________________");
		return filteredResult;
	}

	public void removeRecipe(@Nonnull final EnhancedRecipe enhancedRecipe, @Nonnull final ItemStack[] content) {
		this.allRecipes.removeIf(recipeWrapper -> recipeWrapper.getRecipeKey().equals(enhancedRecipe.getKey()));

		final Set<Material> uniqueMaterials = getUniqueMaterials(content);
		for (Material material : uniqueMaterials) {
			Set<RecipeWrapper> recipeWrappers = this.mappedRecipes.get(material);
			if (recipeWrappers != null) {
				recipeWrappers.removeIf(wrapper -> wrapper.getRecipeKey().equals(enhancedRecipe.getKey()));
				if (recipeWrappers.isEmpty()) {
					this.mappedRecipes.remove(material);
				}
			}
		}
	}

	public void removeRecipe(@Nonnull final RecipeWrapper recipeWrapper, @Nonnull final ItemStack[] content) {
		this.allRecipes.remove(recipeWrapper);
		final Set<Material> uniqueMaterials = getUniqueMaterials(content);

		for (Material material : uniqueMaterials) {
			Set<RecipeWrapper> recipeWrappers = this.mappedRecipes.get(material);
			if (recipeWrappers != null) {
				recipeWrappers.remove(recipeWrapper);
				if (recipeWrappers.isEmpty()) {
					this.mappedRecipes.remove(material);
				}
			}
		}
	}

	public void clearAllRecipes() {
		this.mappedRecipes.clear();
		this.allRecipes.clear();
	}

	public void forEachRecipe(@Nonnull final Consumer<RecipeWrapper> callback) {
		this.allRecipes.forEach(callback);
	}

	public void forEach(@Nonnull final Consumer<Entry<Material, Set<RecipeWrapper>>> callback) {
		mappedRecipes.entrySet().forEach(callback);
	}

	@Nonnull
	public Map<Material, Set<RecipeWrapper>> getMappedRecipes() {
		return Collections.unmodifiableMap(mappedRecipes);
	}

	@Nonnull
	private Set<Material> getUniqueMaterials(@Nullable final ItemStack[] content) {
		if (content == null)
			return Collections.emptySet();

		return Arrays.stream(content)
				.filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
				.map(ItemStack::getType)
				.collect(Collectors.toSet());
	}


	private boolean canPossiblyMatch(final EnumMap<Material, Integer> matrixMaterials, final int matrixSize, RecipeWrapper recipe) {
		if (recipe.getTotalSlotCount() != matrixSize) {
			return false;
		}
		final EnumMap<Material, Integer> ingredients = recipe.getIngredients();
		for (Entry<Material, Integer> gridEntry : matrixMaterials.entrySet()) {
			Material gridMat = gridEntry.getKey();
			int gridAmount = gridEntry.getValue();
			Integer recipeMaxAmount = ingredients.get(gridMat);

			if (recipeMaxAmount == null) {
				return false;
			}
			if (gridAmount > recipeMaxAmount) {
				return false;
			}
		}
		return true;
	}

}
