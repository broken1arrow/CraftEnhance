package com.dutchjelly.craftenhance.cache;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.SaveScheduler;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.database.RecipeDatabase;
import com.dutchjelly.craftenhance.messaging.Debug;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CacheRecipes {

	private final List<EnhancedRecipe> recipes = new ArrayList<>();
	private final RecipeDatabase database;
	private final SaveScheduler saveSchedule;

	public CacheRecipes(final CraftEnhance craftEnhance) {
		this.database = craftEnhance.getDatabase();
		this.saveSchedule = craftEnhance.getSaveScheduler();
	}

	public List<EnhancedRecipe> getRecipes() {
		return Collections.unmodifiableList(recipes);
	}

	@Nullable
	public EnhancedRecipe getRecipe(final String recipeName) {
		for (final EnhancedRecipe recipe : recipes) {
			if (recipe.getKey().equals(recipeName))
				return recipe;
		}
		return null;
	}

	public void add(EnhancedRecipe enhancedRecipe) {
		if (enhancedRecipe == null) return;
		recipes.add(enhancedRecipe);
	}

	public void addAll(List<EnhancedRecipe> enhancedRecipes) {
		recipes.addAll(enhancedRecipes);
	}

	public void remove(EnhancedRecipe enhancedRecipe) {
		EnhancedRecipe recipe = enhancedRecipe;
		saveSchedule.addTask(() -> {
			this.database.deleteRecipe(recipe);
		});
		recipes.remove(enhancedRecipe);
	}

	public boolean isUniqueRecipeKey(final String key) {
		return this.getRecipe(key) == null;
	}
	public void clear() {
		recipes.clear();
	}

	public void save() {
		saveSchedule.addTask(this.database::saveRecipes);
	}

	public void save(EnhancedRecipe enhancedRecipe) {
		saveSchedule.addTask(() -> this.database.saveRecipe(enhancedRecipe));
	}


}
