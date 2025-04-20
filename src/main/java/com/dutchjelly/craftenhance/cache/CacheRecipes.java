package com.dutchjelly.craftenhance.cache;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.SaveScheduler;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.database.RecipeDatabase;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CacheRecipes extends CacheRecipesGroup {

	private final Map<String,EnhancedRecipe> recipes = new HashMap<>();
	private final RecipeDatabase database;
	private final SaveScheduler saveSchedule;

	public CacheRecipes(final CraftEnhance craftEnhance) {
		this.database = craftEnhance.getDatabase();
		this.saveSchedule = craftEnhance.getSaveScheduler();
	}


	public List<EnhancedRecipe> getRecipes() {
		return new ArrayList<>(recipes.values());
	}

	@Override
	public Map<String, EnhancedRecipe> getRecipesMap() {
		return Collections.unmodifiableMap(recipes);
	}

	public List<EnhancedRecipe> getRecipesFiltered(Predicate<? super EnhancedRecipe> predicate) {
		return recipes.values().stream().filter(predicate).collect(Collectors.toList());
	}

	@Nullable
	public EnhancedRecipe getRecipe(final String recipeName) {
		return this.recipes.get(recipeName);
	}

	public void add(final EnhancedRecipe enhancedRecipe) {
		if (enhancedRecipe == null ||  getRecipe( enhancedRecipe.getKey()) != null) return;
		recipes.put(enhancedRecipe.getKey(),enhancedRecipe);
		groupCacheDirty = true;
	}

	public void addAll(final List<EnhancedRecipe> enhancedRecipes) {
		enhancedRecipes.forEach(this::add);
		rebuildGroupCache();
	}

	public void remove(EnhancedRecipe enhancedRecipe) {
		saveSchedule.addTask(() -> {
			this.database.deleteRecipe(enhancedRecipe);
			recipes.remove(enhancedRecipe.getKey());
		});
		enhancedRecipe.setRemove(true);
		groupCacheDirty = true;
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

	public void save(final EnhancedRecipe enhancedRecipe) {
		if(enhancedRecipe.isRemove()) {
			saveSchedule.addTask(() -> {
				this.database.deleteRecipe(enhancedRecipe);
				recipes.remove(enhancedRecipe.getKey());
			});
			return;
		}
		this.add(enhancedRecipe);
		saveSchedule.addTask(() -> this.database.saveRecipe(enhancedRecipe));
	}


}
