package com.dutchjelly.craftenhance.crafthandling.recipes.furnace;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlastRecipe extends FurnaceRecipe {

	@Getter
	private final RecipeType type = RecipeType.BLAST;

	private BlastRecipe(final Map<String, Object> args) {
		super(args);
	}

	public BlastRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
		super(perm, result, content);
	}

	public BlastRecipe(final EnhancedRecipe enhancedRecipe) {
		super(enhancedRecipe);
	}

	@Override
	public Recipe getServerRecipe() {
		return Adapter.getBlastRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(getKey()), getResult(), getContent()[0], 100, getExp());
	}

	public static BlastRecipe deserialize(final Map<String, Object> args) {
		final BlastRecipe recipe = new BlastRecipe(args);
		recipe.setDuration((int) args.get("duration"));
		recipe.setExp((float) (double) args.get("exp"));
		return recipe;
	}

	@Override
	public boolean matchesBlockType(final Material blockSmelting) {
		return blockSmelting == Material.BLAST_FURNACE;
	}

	@Nonnull
	@Override
	public Map<String, Object> serialize() {
		return new LinkedHashMap<String, Object>() {{
			putAll(BlastRecipe.super.serialize());
		}};
	}
}
