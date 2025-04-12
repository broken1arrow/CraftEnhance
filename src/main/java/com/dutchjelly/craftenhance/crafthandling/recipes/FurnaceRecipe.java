package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FurnaceRecipe extends EnhancedRecipe {

	@Getter
	@Setter
	private int duration = 160;

	@Getter
	@Setter
	private float exp = 0;

	@Getter
	private final RecipeType type = RecipeType.FURNACE;

	protected FurnaceRecipe(final Map<String, Object> args) {
		super(args);
	}

	public FurnaceRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
		super(perm, result, content);
	}

	protected FurnaceRecipe(EnhancedRecipe enhancedRecipe) {
		super(enhancedRecipe);
	}

	public static FurnaceRecipe deserialize(final Map<String, Object> args) {
		final FurnaceRecipe recipe = new FurnaceRecipe(args);
		recipe.duration = (int) args.get("duration");
		if (recipe.duration == 0) {
			recipe.duration = 160;
		}
		recipe.exp = (float) (double) args.get("exp"); //snake yaml saves floats as doubles so we need to first parse to double
		return recipe;
	}

	@Nonnull
	@Override
	public Map<String, Object> serialize() {
		return new LinkedHashMap<String, Object>() {{
			putAll(FurnaceRecipe.super.serialize());
			put("exp", exp);
			put("duration", duration);
		}};
	}

	@Override
	public boolean matches(final ItemStack[] content) {
		return content.length == 1 && getMatchType().getMatcher().match(content[0], getContent()[0]);
	}

	@Override
	public boolean matches(final ItemStack[] content, final IMatcher<ItemStack> matcher) {
		return content.length == 1 && getMatchType().getMatcher().match(content[0], getContent()[0]);
	}

	@Override
	public boolean matchesBlockType(final Material blockSmelting) {
		if (self().getVersionChecker().olderThan(ServerVersion.v1_13)) {
			return blockSmelting == Material.getMaterial("BURNING_FURNACE");
		}
		return blockSmelting == Material.FURNACE;
	}

	public boolean matchesType(final ItemStack[] content) {
		return content.length == 1 && ItemMatchers.matchType(content[0], getContent()[0]);
	}

	@Override
	public Recipe getServerRecipe() {
		int duration = self().getVersionChecker().olderThan(ServerVersion.v1_17) ? this.duration : 200;
		return Adapter.GetFurnaceRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(getKey()), getResult(), getContent()[0], duration, getExp());
	}

	@Override
	public boolean isSimilar(final Recipe r) {
		if (!(r instanceof org.bukkit.inventory.FurnaceRecipe)) return false;
		final org.bukkit.inventory.FurnaceRecipe serverRecipe = (org.bukkit.inventory.FurnaceRecipe) r;

		return ItemMatchers.matchType(serverRecipe.getInput(), getContent()[0])
				&& ItemMatchers.matchType(serverRecipe.getResult(), getResult());
	}

	@Override
	public boolean isSimilar(final EnhancedRecipe r) {
		return r instanceof FurnaceRecipe && ItemMatchers.matchTypeData(r.getContent()[0], getContent()[0]);
	}

	@Override
	public boolean isAlwaysSimilar(final Recipe r) {
		if (!ItemMatchers.matchItems(r.getResult(), getResult()))
			return false;
		if (!(r instanceof org.bukkit.inventory.FurnaceRecipe))
			return false;
		return isSimilar(r);
	}
}