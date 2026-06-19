package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
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
import java.util.function.Consumer;

public class VanillaCraftWrapper implements RecipeWrapper<Recipe> {
	private final Recipe recipe;

	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public boolean isCustom() {
		return false;
	}

	@Override
	public void matches(@Nonnull final RecipeInjector recipeInjector, @Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer) {
		final PrepareItemCraftContext prepareItemCraftContext = new PrepareItemCraftContext();
		contextConsumer.accept(prepareItemCraftContext);
		final ItemStack[] matrix = prepareItemCraftContext.getRecipeMatrix();

		if (recipe instanceof ShapedRecipe) {
			final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);

			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] matrix to match: " + recipeInjector.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] ingredients" + recipeInjector.convertItemStackArrayToString(content));
			if (WBRecipeComparer.shapeMatches(content, matrix, recipeInjector.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapedRecipe and will allow this server recipe.");
				prepareItemCraftContext.acceptResult(recipe.getResult());
			}
		} else if (recipe instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] matrix to match: " + recipeInjector.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] ingredients" + recipeInjector.convertItemStackArrayToString(ingredients));
			if (WBRecipeComparer.ingredientsMatch(ingredients, matrix, recipeInjector.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapelessRecipe and will allow this server recipe.");
				prepareItemCraftContext.acceptResult(recipe.getResult());
			}
		}
	}


	@Override
	public Recipe getRecipe() {
		return this.recipe;
	}
}
