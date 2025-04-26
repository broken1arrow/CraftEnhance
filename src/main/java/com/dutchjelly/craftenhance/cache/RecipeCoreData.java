package com.dutchjelly.craftenhance.cache;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nullable;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeCoreData {
	private final String key;
	private final String category;

	private final ItemStack result;
	private final RecipeType recipeType;

	public RecipeCoreData(final EnhancedRecipe enhancedRecipe) {
		this.key = enhancedRecipe.getKey();
		this.result = enhancedRecipe.getResult();
		this.recipeType = enhancedRecipe.getType();
		this.category = enhancedRecipe.getRecipeCategory();

	}

	public static RecipeCoreData of(final EnhancedRecipe enhancedRecipe) {
		return new RecipeCoreData(enhancedRecipe);
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

	public boolean isSimilarContent(final ItemStack... content) {
		final EnhancedRecipe recipe = this.getEnhancedRecipe();
		if (content == null || recipe == null)
			return false;
		final ItemStack[] recipeContent = recipe.getContent();
		if (content.length == 1 && recipeContent.length > 0 && content[0].isSimilar(recipeContent[0])) {
			return true;
		}
		return WBRecipeComparer.ingredientsMatch(content, recipe.getContent(), MatchType.MATCH_BASIC_META.getMatcher());
	}

	public boolean isSimilar(final Recipe recipe) {
		final EnhancedRecipe enhancedRecipe = this.getEnhancedRecipe();
		if (recipe == null || enhancedRecipe == null)
			return false;

		return enhancedRecipe.isSimilar(recipe);
	}

	public boolean isAlwaysSimilar(final Recipe recipe) {
		final EnhancedRecipe enhancedRecipe = this.getEnhancedRecipe();
		if (recipe == null || enhancedRecipe == null)
			return false;

		return enhancedRecipe.isAlwaysSimilar(recipe);
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
