package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public class VanillaCraftWrapper implements RecipeWrapper {
	private final Recipe recipe;

	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return RecipeType.WORKBENCH;
	}

	@Override
	public int priority() {
		return 100;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@Override
	public void matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer) {
		final PrepareItemCraftContext prepareItemCraftContext = new PrepareItemCraftContext();
		contextConsumer.accept(prepareItemCraftContext);
		final ItemStack[] matrix = prepareItemCraftContext.getRecipeMatrix();

		if (recipe instanceof ShapedRecipe) {
			final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);

			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(content));
			if (WBRecipeComparer.shapeMatches(content, matrix, RecipeAdapter.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapedRecipe and will allow this server recipe.");
				prepareItemCraftContext.acceptResult(recipe.getResult());
			}
		} else if (recipe instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(ingredients));
			if (WBRecipeComparer.ingredientsMatch(ingredients, matrix, RecipeAdapter.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapelessRecipe and will allow this server recipe.");
				prepareItemCraftContext.acceptResult(recipe.getResult());
			}
		}
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.recipe))
			return Optional.of(type.cast(this.recipe));
		return Optional.empty();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		final VanillaCraftWrapper that = (VanillaCraftWrapper) o;
		if (that.recipe.getResult().isSimilar(recipe.getResult())) {
			org.bukkit.inventory.Recipe thatRecipe = that.getRecipe(Recipe.class).orElse(null);
			if (thatRecipe instanceof ShapelessRecipe && recipe instanceof ShapelessRecipe) {
				return ((ShapelessRecipe) thatRecipe).getChoiceList().equals(((ShapelessRecipe) recipe).getChoiceList());
			}
			if (thatRecipe instanceof ShapedRecipe && recipe instanceof ShapedRecipe) {
				return ((ShapedRecipe) thatRecipe).getChoiceMap().equals(((ShapedRecipe) recipe).getChoiceMap());
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 41 * hash + recipe.getResult().hashCode();
		if (recipe instanceof ShapelessRecipe)
			hash = 41 * hash + ((ShapelessRecipe) recipe).getChoiceList().hashCode();
		if (recipe instanceof ShapedRecipe)
			hash = 41 * hash + ((ShapedRecipe) recipe).getChoiceMap().hashCode();
		return hash;
	}

}
