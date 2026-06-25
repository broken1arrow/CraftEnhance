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

	public List<RecipeWrapper> findMatchingRecipes(@Nullable final RecipeType recipeType, @Nonnull final ItemStack[] matrix) {
		Set<RecipeWrapper> wrappersMatch = null;

		Debug.send(recipeType, "Find_matching_recipes", () -> "The recipe matrics to find a match: [" + Arrays.stream(matrix).map(stack -> {
					if (stack != null)
						return stack.getType().name();
					return null;
				}).collect(Collectors.joining(",")) + "]"
		);

		for (ItemStack itemStack : matrix) {
			final Material type = itemStack == null ? null : itemStack.getType();
			if (type == null || type == Material.AIR) continue;
			final Set<RecipeWrapper> recipeCached = this.mappedRecipes.getOrDefault(type, Collections.emptySet());

			Debug.send(recipeType, "Find_matching_recipes", () -> "The item type to find a match: '" + type + "' the number of ingredients matching: " + recipeCached.size());

			if (recipeCached.isEmpty()) {
				return Collections.emptyList();
			}
			if (wrappersMatch == null) {
				wrappersMatch = new HashSet<>(recipeCached);
			} else {
				wrappersMatch.retainAll(recipeCached);
			}
			if (wrappersMatch.isEmpty())
				return Collections.emptyList();
		}

		final Set<RecipeWrapper> finalWrappersMatch = wrappersMatch;
		Debug.send(recipeType, "Find_matching_recipes", () -> "The final matched recipes:" + (finalWrappersMatch == null ? "'no match found" : "\n|" + finalWrappersMatch) + "|");
		if (wrappersMatch == null)
			return Collections.emptyList();

		final List<RecipeWrapper> sortedRecipes = new ArrayList<>(wrappersMatch);
		sortedRecipes.sort(Comparator.comparingInt(RecipeWrapper::priority));
		return sortedRecipes;
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
}
