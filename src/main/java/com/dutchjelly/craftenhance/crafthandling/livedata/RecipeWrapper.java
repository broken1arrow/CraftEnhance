package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public interface RecipeWrapper {

	boolean isCustom();

	void matches(@Nonnull final RecipeInjector recipeInjector, @Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer);

	<T> Optional<T> getRecipe(Class<T> type);


}
