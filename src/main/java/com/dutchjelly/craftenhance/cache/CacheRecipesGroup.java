package com.dutchjelly.craftenhance.cache;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CacheRecipesGroup {
	private final Map<RecipeType, Set<String>> groups = new HashMap<>();
	protected boolean groupCacheDirty;
	public Map<String, EnhancedRecipe> getRecipesMap()  {
		return new HashMap<>();
	}

	public Set<String> getGroupsForType(RecipeType type) {
		if (groupCacheDirty) {
			groups.clear();
			groupCacheDirty = false;
		}
		return groups.computeIfAbsent(type, t -> this.getRecipesMap().values().stream()
				.filter(enhancedRecipe -> enhancedRecipe.getType() == t)
				.map(EnhancedRecipe::getGroup)
				.distinct()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet()));
	}

	public void rebuildGroupCache() {
		groups.clear();
		for (RecipeType type : RecipeType.values()) {
			getGroupsForType(type);
		}
		groupCacheDirty = false;
	}

}
