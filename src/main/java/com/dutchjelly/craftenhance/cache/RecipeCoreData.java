package com.dutchjelly.craftenhance.cache;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeCoreData {
	private final String key;
	private final String category;

	private final ItemStack result;
	private final RecipeType recipeType;

	public RecipeCoreData(EnhancedRecipe enhancedRecipe) {
		this.key = enhancedRecipe.getKey();
		this.result = enhancedRecipe.getResult();
		this.recipeType = enhancedRecipe.getType();
		this.category = enhancedRecipe.getRecipeCategory();

	}

	public String getKey() {
		return key;
	}

	public String getCategory() {
		return category;
	}

	@Nullable
	public ItemStack getResult() {
		if (this.result == null)
			return null;
		return new ItemStack(result);
	}

	@Nullable
	public EnhancedRecipe getEnhancedRecipe() {
		return self().getCacheRecipes().getRecipe(this.getKey());
	}

	public RecipeType getRecipeType() {
		return recipeType;
	}

	@Override
	public String toString() {
		return "RecipeCoreData{" +
				"key='" + key + '\'' +
				", category='" + category + '\'' +
				", result=" + result +
				", recipeType=" + recipeType +
				'}';
	}
}
