package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import org.bukkit.Material;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
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

	boolean containsIngredient(final Material material);

	ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer);

	<T> Optional<T> getRecipe(Class<T> type);


}
