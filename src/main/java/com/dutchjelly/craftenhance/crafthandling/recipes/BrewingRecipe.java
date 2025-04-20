package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

public class BrewingRecipe extends EnhancedRecipe{

	@Getter
	@Setter
	private int duration = 20;
	private ItemStack[] outPutResult = new ItemStack[3];
	public BrewingRecipe(final EnhancedRecipe enhancedRecipe) {
		super(enhancedRecipe);
	}

	public BrewingRecipe(final String permission, final ItemStack result, final ItemStack[] itemStacks) {
		super(permission, result,itemStacks);
	}
	private BrewingRecipe(final Map<String, Object> args) {
		super(args);
	}

	@Override
	public boolean matches(final ItemStack[] content) {
		return matches(content, getMatchType().getMatcher());
	}

	@Override
	public boolean matches(final ItemStack[] content, final IMatcher<ItemStack> matcher) {
		return WBRecipeComparer.ingredientsMatchBrewing(content, getContent(),  matcher);
	}

	public ItemStack[] getOutPutResult() {
		return outPutResult;
	}

	@Nullable
	public ItemStack getResultItem(int index) {
		if (outPutResult.length < index)
			return null;
		return outPutResult[index];
	}

	@Override
	public ItemStack[] getContent() {
		return super.getContent();
	}

	public ItemStack[] getCombinedContent() {
		ItemStack[] ingredients = super.getContent();
		ItemStack[] combined = new ItemStack[ingredients.length + outPutResult.length];
		System.arraycopy(ingredients, 0, combined, 0, ingredients.length);
		System.arraycopy(outPutResult, 0, combined, ingredients.length, outPutResult.length);
		return combined;
	}

	@Override
	public void setContent(final ItemStack[] content) {
		if (content == null) return;
		// Ingredients it will match to the items player add in the brewing stand.
		if (content.length >= 3) {
			super.setContent(Arrays.copyOfRange(content, 0, 3));
		}
		//the items that will replace the items added in the brewing stand.
		if (content.length >= 6) {
			outPutResult = Arrays.copyOfRange(content, 3, 6);
		} else {
			outPutResult = new ItemStack[3];
		}
	}

	@Override
	public boolean matchesBlockType(final Material blockSmelting) {
		return false;
	}

	@Override
	public Recipe getServerRecipe() {
		return this.getServerRecipe(this.getGroup());
	}

	@Override
	public Recipe getServerRecipe(final String groupName) {
		this.setGroup(groupName);
		return null;
	}

	@Override
	public boolean isSimilar(final Recipe r) {
		return false;
	}

	@Override
	public boolean isSimilar(final EnhancedRecipe r) {
		return false;
	}

	@Override
	public boolean isAlwaysSimilar(final Recipe r) {
		return false;
	}

	@Override
	public RecipeType getType() {
		return RecipeType.BREWING;
	}

	public static BrewingRecipe deserialize(final Map<String, Object> args) {
		BrewingRecipe brewingRecipe = new BrewingRecipe(args);
		brewingRecipe.duration = (int) args.get("duration");
		if (brewingRecipe.duration == 0) {
			brewingRecipe.duration = 20;
		}
		return brewingRecipe;
	}
}
