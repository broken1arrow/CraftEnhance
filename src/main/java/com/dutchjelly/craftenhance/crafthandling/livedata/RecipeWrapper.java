package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import org.bukkit.Material;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;

public interface RecipeWrapper {
	@Nonnull
	String getRecipeKey();

	@Nonnull
	RecipeType getRecipeType();

	int priority();

	boolean isCustom();

	EnumMap<Material, Integer> getIngredients();

	int getAmountOfIngredient(final Material material);

	int getTotalSlotCount();

	ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer);

	@Nullable
	Recipe getRecipe();
	/**
	 * Retrieve the recipe via a safe cast, include the custom recipe classes like
	 * {@link com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe} or
	 * {@link com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe}
	 *
	 * @param type the recipe class you want to access.
	 * @param <T>  The type of recipe class.
	 * @return returns an optional that is safe to cast.
	 */
	<T> Optional<T> getRecipe(Class<T> type);


}
