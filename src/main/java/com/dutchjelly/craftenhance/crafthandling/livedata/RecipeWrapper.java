package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public interface RecipeWrapper {

	@Nonnull
	RecipeType getRecipeType();

	int priority();

	boolean isCustom();

	void matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer);

	<T> Optional<T> getRecipe(Class<T> type);

}
