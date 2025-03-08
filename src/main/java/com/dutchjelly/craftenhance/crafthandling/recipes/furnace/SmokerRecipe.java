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

public class SmokerRecipe extends FurnaceRecipe {

	@Getter
	private final RecipeType type = RecipeType.SMOKER;
	private SmokerRecipe(final Map<String, Object> args) {
		super(args);
	}

	public SmokerRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
		super(perm, result, content);
	}

	public SmokerRecipe(final EnhancedRecipe enhancedRecipe) {
		super(enhancedRecipe);
	}

	@Override
	public Recipe getServerRecipe() {
		return Adapter.getSmokingRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(getKey()), getResult(), getContent()[0], 100, getExp());
	}
	@Override
	public boolean matchesBlockType(final Material blockSmelting) {
		return blockSmelting == Material.SMOKER;
	}
	public static SmokerRecipe deserialize(final Map<String, Object> args) {
		final SmokerRecipe recipe = new SmokerRecipe(args);
		recipe.setDuration((int) args.get("duration"));
		recipe.setExp((float) (double) args.get("exp"));
		return recipe;
	}

	@Nonnull
	@Override
	public Map<String, Object> serialize() {
		return new LinkedHashMap<String, Object>() {{
			putAll(SmokerRecipe.super.serialize());
		}};
	}
}
