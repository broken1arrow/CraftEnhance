package com.dutchjelly.craftenhance.crafthandling.recipes.utility;

import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public enum RecipeType {
	WORKBENCH, FURNACE, BLAST, SMOKER, BREWING;

	public static RecipeType getType(Recipe r) {
		if (r instanceof ShapedRecipe) return WORKBENCH;
		if (r instanceof ShapelessRecipe) return WORKBENCH;
		if (r instanceof FurnaceRecipe) return FURNACE;
		if (r instanceof BlastRecipe) return BLAST;
		if (r instanceof SmokerRecipe) return SMOKER;
		if (r instanceof BrewingRecipe) return BREWING;
		return null;
	}

	public static RecipeType getType(Block block) {
		Material material = block.getType();
		if (self().getVersionChecker().olderThan(ServerVersion.v1_13)) {
			if (material.name().equals("WORKBENCH"))
				return WORKBENCH;
			if (material.name().equals("BURNING_FURNACE"))
				return FURNACE;
			return WORKBENCH;
		}
		switch (block.getType()) {
			case CRAFTING_TABLE:
				return WORKBENCH;
			case FURNACE:
				return FURNACE;
			case BLAST_FURNACE:
				return BLAST;
			case SMOKER:
				return SMOKER;
			case BREWING_STAND:
				return BREWING;
			default:
				return null;
		}
	}
	public static RecipeType getType(String type) {
		if (type == null) return WORKBENCH;

		RecipeType [] recipeTypes = values();
		String typeUp = type.toUpperCase();

		for (RecipeType recipeType : recipeTypes) {
			if (recipeType.name().equals(typeUp)) {
				return recipeType;
			}
		}

		return WORKBENCH;
	}

	public String capitalize(){
		return StringUtil.capitalizeFully(this.name().toLowerCase());
	}
}