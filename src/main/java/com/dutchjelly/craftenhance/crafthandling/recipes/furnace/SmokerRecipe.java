package com.dutchjelly.craftenhance.crafthandling.recipes.furnace;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

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
		return this.getServerRecipe(this.getGroup());
	}

	@Override
	public Recipe getServerRecipe(final String groupName) {
		int duration = self().getVersionChecker().olderThan(ServerVersion.v1_17) ? this.getDuration() : 100;
		final SmokingRecipe smokingRecipe = Adapter.getSmokingRecipe(self(), ServerRecipeTranslator.GetFreeKey(getKey()), getResult(), getContent()[0], duration, getExp());
		if (groupName != null && smokingRecipe != null) {
			Adapter.setGroup(smokingRecipe,groupName);
		}
		this.setGroup(groupName);
		return smokingRecipe;
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
