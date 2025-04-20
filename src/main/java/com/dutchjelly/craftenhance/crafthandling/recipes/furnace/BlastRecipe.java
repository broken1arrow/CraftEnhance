package com.dutchjelly.craftenhance.crafthandling.recipes.furnace;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

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

	public static BlastRecipe deserialize(final Map<String, Object> args) {
		final BlastRecipe recipe = new BlastRecipe(args);
		recipe.setDuration((int) args.get("duration"));
		recipe.setExp((float) (double) args.get("exp"));
		return recipe;
	}

	@Override
	public Recipe getServerRecipe() {
		return this.getServerRecipe(this.getGroup());
	}
	@Override
	public Recipe getServerRecipe(final String groupName) {
		int duration = self().getVersionChecker().olderThan(ServerVersion.v1_17) ? this.getDuration() : 100;
		final BlastingRecipe blastRecipe = Adapter.getBlastRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(getKey()), getResult(), getContent()[0], duration, getExp());
		if (groupName != null && blastRecipe != null) {
			Adapter.setGroup(blastRecipe,groupName);
		}
		this.setGroup(groupName);
		return blastRecipe;
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
