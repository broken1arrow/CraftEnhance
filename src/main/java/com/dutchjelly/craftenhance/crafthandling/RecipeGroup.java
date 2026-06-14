package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeGroup {
	private final String group;
	@Getter
	@Setter
	private List<Recipe> serverRecipes = new ArrayList<>();
	@Getter
	@Setter
	private List<EnhancedRecipeWrapper> recipeCoreList = new ArrayList<>();
	private final Map<String, EnhancedRecipeWrapper> recipeGroupCache = new HashMap<>();

	public RecipeGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public void putCustomRecipe(@NonNull final EnhancedRecipeWrapper enhancedRecipeWrapper) {
		recipeGroupCache.put(enhancedRecipeWrapper.getKey(), enhancedRecipeWrapper);
	}

	@Nullable
	public EnhancedRecipeWrapper getCustomRecipe(@NonNull final EnhancedRecipeWrapper enhancedRecipeWrapper) {
		return recipeGroupCache.get(enhancedRecipeWrapper.getKey());
	}

	@Nullable
	public EnhancedRecipeWrapper getCustomRecipe(@NonNull final String enhancedRecipeKey) {
		return recipeGroupCache.get(enhancedRecipeKey);
	}

	public void remove(@NonNull final EnhancedRecipe recipe) {
		recipeGroupCache.get(recipe.getKey());
	}

	public int getRecipeGroupSize() {
		return recipeGroupCache.size();
	}

	public List<EnhancedRecipe> findSimilarRecipes(@NonNull final Recipe recipe) {
		final List<EnhancedRecipe> enhancedRecipe = new ArrayList<>();;
		recipeGroupCache.values().stream()
				.filter(recipeCoreData -> recipeCoreData.isAlwaysSimilar(recipe))
				.forEach((recipeCoreData) ->
						enhancedRecipe.add(recipeCoreData.getEnhancedRecipe())
				);
		return enhancedRecipe;
	}

	public boolean isSimilarContent(@NonNull final ItemStack... content) {
		return recipeGroupCache.values().stream()
				.anyMatch(recipeCoreData -> recipeCoreData.isSimilarContent(content));
	}

	public boolean isSimilarResult(@NonNull final ItemStack result) {
		return recipeGroupCache.values().stream().anyMatch(x -> result.isSimilar(x.getResult()));
	}

	public boolean isSimilarResultType(@NonNull final Material resultType) {
		return recipeGroupCache.values().stream().anyMatch(x -> x.getResult() != null && resultType == x.getResult().getType());
	}

	public Map<String, EnhancedRecipeWrapper> getRecipeGroupCache() {
		return recipeGroupCache;
	}

	public RecipeGroup addIfNotExist(Recipe recipe) {
		if (!serverRecipes.contains(recipe))
			serverRecipes.add(recipe);
		return this;
	}

	public RecipeGroup addAllNotExist(List<Recipe> recipes) {
		for (Recipe recipe : recipes)
			if (!serverRecipes.contains(recipe))
				serverRecipes.add(recipe);
		return this;
	}


	@Override
	public String toString() {
		return "RecipeGroup{" + formatGroup() + '}';
	}

	public String formatGroup() {
		final StringBuilder recipes = new StringBuilder();
		final StringJoiner joiner = new StringJoiner(", ");

		recipes.append("serverRecipes= {");
		this.serverRecipes.forEach(serverRecipe -> {
			final StringBuilder serverRecipesBuild = new StringBuilder();
			if (serverRecipe instanceof ShapedRecipe) {
				final ShapedRecipe shaped = (ShapedRecipe) serverRecipe;
				serverRecipesBuild.append(shaped.getKey());
				serverRecipesBuild.append(shaped.getResult());
				serverRecipesBuild.append(Arrays.toString(shaped.getShape()));
			}
			if (serverRecipe instanceof ShapelessRecipe) {
				final ShapelessRecipe shapedLess = (ShapelessRecipe) serverRecipe;
				serverRecipesBuild.append(shapedLess.getKey()).append(", ");
				serverRecipesBuild.append(shapedLess.getResult());
			}
			if (serverRecipe instanceof FurnaceRecipe) {
				final FurnaceRecipe furnaceRecipe = (FurnaceRecipe) serverRecipe;
				serverRecipesBuild.append(furnaceRecipe.getKey()).append(", ");
				serverRecipesBuild.append(furnaceRecipe.getResult()).append(", ");
				serverRecipesBuild.append(furnaceRecipe.getCookingTime());
			}
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
				if (serverRecipe instanceof BlastingRecipe) {
					final BlastingRecipe blastRecipe = (BlastingRecipe) serverRecipe;
					serverRecipesBuild.append(blastRecipe.getKey()).append(", ");
					serverRecipesBuild.append(blastRecipe.getResult()).append(", ");
					serverRecipesBuild.append(blastRecipe.getCookingTime());
				}
				if (serverRecipe instanceof SmokingRecipe) {
					final SmokingRecipe smokerRecipe = (SmokingRecipe) serverRecipe;
					serverRecipesBuild.append(smokerRecipe.getKey()).append(", ");
					serverRecipesBuild.append(smokerRecipe.getResult()).append(", ");
					serverRecipesBuild.append(smokerRecipe.getCookingTime());
				}
			}
			joiner.add(serverRecipesBuild);
		});
		recipes.append(joiner);
		recipes.append("}\n");
		recipes.append("enhancedRecipes= {");

		final StringJoiner enhancedJoiner = new StringJoiner(",");
		this.recipeGroupCache.values().forEach(recipeCoreData -> {
			final StringBuilder recipesBuilder = new StringBuilder();
			recipesBuilder.append(" key='");
			recipesBuilder.append(recipeCoreData.getKey()).append("'| category=");
			recipesBuilder.append(recipeCoreData.getCategory());

			final EnhancedRecipe enhancedRecipe = recipeCoreData.getEnhancedRecipe();
			if (enhancedRecipe != null) {
				recipesBuilder.append("'| type=").append(enhancedRecipe.getType()).append("| worlds='");
				recipesBuilder.append(enhancedRecipe.getAllowedWorldsFormatted()).append("'| perm='");
				recipesBuilder.append(enhancedRecipe.getPermission()).append("'");
				if (!(enhancedRecipe instanceof com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe))
					recipesBuilder.append("| command='").append(enhancedRecipe.getOnCraftCommand()).append("'");
			}
			enhancedJoiner.add(recipesBuilder);
		});
		recipes.append(enhancedJoiner);
		recipes.append("}");
		return recipes + "";
	}


}
