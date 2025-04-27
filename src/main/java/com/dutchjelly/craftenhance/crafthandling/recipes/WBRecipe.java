package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public class WBRecipe extends EnhancedRecipe {

	@Getter
	private final RecipeType type = RecipeType.WORKBENCH;
	@Getter
	@Setter
	private boolean shapeless = false; //false by default
	private Recipe recipe;

	public WBRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
		super(perm, result, content);
	}

	private WBRecipe(final Map<String, Object> args) {
		super(args);
		if (args.containsKey("shapeless"))
			shapeless = (Boolean) args.get("shapeless");
	}

	protected WBRecipe(EnhancedRecipe enhancedRecipe) {
		super(enhancedRecipe);
	}

	public WBRecipe() {
		super("", null, null);
	}


	public static WBRecipe deserialize(final Map<String, Object> args) {

		final WBRecipe recipe = new WBRecipe(args);
		if (args.containsKey("shapeless"))
			recipe.shapeless = (Boolean) args.get("shapeless");

		return recipe;
	}

	@Override
	@Nonnull
	public Map<String, Object> serialize() {
		return new LinkedHashMap<String, Object>() {{
			putAll(WBRecipe.super.serialize());
			put("shapeless", shapeless);
		}};
	}

	@Override
	public Recipe getServerRecipe() {
		return this.getServerRecipe(this.getGroup());
	}

	@Override
	public Recipe getServerRecipe(final String groupName) {
		if (recipe == null) {
			if (shapeless)
				recipe = ServerRecipeTranslator.translateShapelessEnhancedRecipe(this);
			else
				recipe = ServerRecipeTranslator.translateShapedEnhancedRecipe(this);
		}
		if (recipe != null && groupName != null) {
			Adapter.setGroup(recipe, groupName);
		}
		this.setGroup(groupName);
		return recipe;
	}


	//The recipe is similar to a server recipe if theyre both shaped and their shapes match, if at least one is shaped and the ingredients match
	//Note that similar doesn't mean that the recipes are always equal. Shaped is always similar to shapeless, but not the other way around.
	@Override
	public boolean isSimilar(final Recipe r) {
		if (r instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) r);
			final boolean result = WBRecipeComparer.ingredientsMatch(getContent(), ingredients, ItemMatchers::matchType);
			return result;
		}

		if (r instanceof ShapedRecipe) {
			final ItemStack[] shapedContent = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) r);
			if (shapeless) {
				return WBRecipeComparer.ingredientsMatch(shapedContent, getContent(), ItemMatchers::matchType);
			}
			return WBRecipeComparer.shapeMatches(getContent(), shapedContent, ItemMatchers::matchType);
		}
		return false;
	}

	//Looks if r is always similar to this (so we know it doesn't have to be loaded in again)
	@Override
	public boolean isAlwaysSimilar(final Recipe r) {
		if (!ItemMatchers.matchItems(r.getResult(), getResult())) //different result means it needs to be loaded in
			return false;

		if (r instanceof ShapelessRecipe) { //shapeless to shaped or shapeless is always similar
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) r);
			return WBRecipeComparer.ingredientsMatch(getContent(), ingredients, ItemMatchers::matchTypeData);
		}

		if (r instanceof ShapedRecipe && !shapeless) { //shaped to shaped (not shapeless) is similar
			final ItemStack[] shapedContent = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) r);
			return WBRecipeComparer.shapeMatches(getContent(), shapedContent, ItemMatchers::matchTypeData);
		}
		return false;
	}


	public boolean isEqual(final Recipe r) {
		if (r instanceof ShapelessRecipe) {
			if (!shapeless) return false;
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) r);
			final boolean result = WBRecipeComparer.ingredientsMatch(getContent(), ingredients, ItemMatchers::matchType);
			return result;
		}

		if (r instanceof ShapedRecipe) {
			final ItemStack[] shapedContent = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) r);
			if (shapeless) {
				return WBRecipeComparer.ingredientsMatch(shapedContent, getContent(), ItemMatchers::matchType);
			}
			return WBRecipeComparer.shapeMatches(getContent(), shapedContent, ItemMatchers::matchType);
		}
		return false;
	}

	@Override
	public boolean isSimilar(final EnhancedRecipe r) {
		if (r == null) return false;
		if (!(r instanceof WBRecipe)) return false;

		final WBRecipe wbr = (WBRecipe) r;
		if (wbr.isShapeless() || shapeless) {
			return WBRecipeComparer.ingredientsMatch(getContent(), wbr.getContent(), ItemMatchers::matchType);
		}
		return WBRecipeComparer.shapeMatches(getContent(), wbr.getContent(), ItemMatchers::matchType);
	}


	@Override
	public boolean matches(final ItemStack[] content) {
		return matches(content, getMatchType().getMatcher());
	}

	@Override
	public boolean matches(final ItemStack[] content, final IMatcher<ItemStack> matcher) {
		if (isShapeless() && WBRecipeComparer.ingredientsMatch(content, getContent(), matcher)) {
			return true;
		}

		if (!isShapeless() && WBRecipeComparer.shapeMatches(content, getContent(), matcher)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean matchesBlockType(final Material blockSmelting) {
		return true;
	}
}
