package com.dutchjelly.craftenhance.crafthandling.livedata;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
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
		Set<RecipeWrapper> wrappersMatch = null;

		for (ItemStack itemStack : matrix) {
			final Material type = itemStack == null ? null : itemStack.getType();
			if (type == null) continue;
			final Set<RecipeWrapper> recipeCached = this.mappedRecipes.getOrDefault(type, Collections.emptySet());
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
		if(wrappersMatch == null)
			return Collections.emptyList();

		final List<RecipeWrapper> sortedRecipes = new ArrayList<>(wrappersMatch);
		sortedRecipes.sort(Comparator.comparingInt(RecipeWrapper::priority));
		return sortedRecipes;
	}

	public void removeRecipe(@Nonnull final RecipeWrapper recipe, @Nonnull final ItemStack[] content) {
		this.allRecipes.remove(recipe);

		final Set<Material> uniqueMaterials = getUniqueMaterials(content);

		for (Material material : uniqueMaterials) {
			this.mappedRecipes.compute(material, (mat, recipeWrappers) -> {
				if (recipeWrappers != null) {
					recipeWrappers.remove(recipe);
					if (recipeWrappers.isEmpty()) {
						return null;
					}
				}
				return recipeWrappers;
			});
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
	private static Set<Material> getUniqueMaterials(@Nonnull final ItemStack[] content) {
		return Arrays.stream(content)
				.filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
				.map(ItemStack::getType)
				.collect(Collectors.toSet());
	}
}
