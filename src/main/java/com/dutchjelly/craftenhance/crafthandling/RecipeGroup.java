package com.dutchjelly.craftenhance.crafthandling;


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
	private List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();

	public RecipeGroup() {
	}

	public RecipeGroup(List<EnhancedRecipe> enhanced, List<Recipe> server) {
		this.enhancedRecipes.addAll(enhanced);
		this.serverRecipes.addAll(server);
	}

	public RecipeGroup addIfNotExist(EnhancedRecipe enhancedRecipe) {
		if (!enhancedRecipes.contains(enhancedRecipe))
			enhancedRecipes.add(enhancedRecipe);
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
		List<EnhancedRecipe> mergedEnhancedRecipes = new ArrayList<>();
		mergedEnhancedRecipes.addAll(enhancedRecipes);
		mergedEnhancedRecipes.addAll(othergroup.enhancedRecipes);
		enhancedRecipes = mergedEnhancedRecipes.stream().distinct().collect(Collectors.toList());
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
		this.enhancedRecipes.forEach(enhancedRecipe -> {
			final StringBuilder enhancedRecipes = new StringBuilder();
			enhancedRecipes.append(" key='");
			enhancedRecipes.append(enhancedRecipe.getKey()).append("'| type:");
			enhancedRecipes.append(enhancedRecipe.getType()).append("| worlds='");
			enhancedRecipes.append(enhancedRecipe.getAllowedWorldsFormatted()).append("'| perm='");
			enhancedRecipes.append(enhancedRecipe.getPermission()).append("'");
			if (!(enhancedRecipe instanceof com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe))
				enhancedRecipes.append("| command='").append(enhancedRecipe.getOnCraftCommand()).append("'");

			enhancedJoiner.add(enhancedRecipes);
		});
		recipes.append(enhancedJoiner);
		recipes.append("}");
		return recipes + "";
	}

}
