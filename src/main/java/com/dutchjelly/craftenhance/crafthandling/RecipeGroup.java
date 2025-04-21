package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.craftenhance.cache.RecipeCoreData;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeGroup {

	@Getter
	@Setter
	private List<Recipe> serverRecipes = new ArrayList<>();
	@Getter
	@Setter
	private List<RecipeCoreData> recipeCoreList = new ArrayList<>();

	public RecipeGroup() {
	}

	public RecipeGroup(List<RecipeCoreData> enhanced, List<Recipe> server) {
		this.recipeCoreList.addAll(enhanced);
		this.serverRecipes.addAll(server);
	}

	public RecipeGroup addIfNotExist(RecipeCoreData enhancedRecipe) {
		if (!recipeCoreList.contains(enhancedRecipe))
			recipeCoreList.add(enhancedRecipe);
		return this;
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

	//Returns this for chaining purposes.
	public RecipeGroup mergeWith(@NonNull RecipeGroup othergroup) {
		List<Recipe> mergedServerRecipes = new ArrayList<>();
		mergedServerRecipes.addAll(serverRecipes);
		mergedServerRecipes.addAll(othergroup.serverRecipes);
		serverRecipes = mergedServerRecipes.stream().distinct().collect(Collectors.toList());
		List<RecipeCoreData> mergedEnhancedRecipes = new ArrayList<>();
		mergedEnhancedRecipes.addAll(recipeCoreList);
		mergedEnhancedRecipes.addAll(othergroup.recipeCoreList);
		recipeCoreList = mergedEnhancedRecipes.stream().distinct().collect(Collectors.toList());
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
		this.recipeCoreList.forEach(recipeCoreData -> {
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
