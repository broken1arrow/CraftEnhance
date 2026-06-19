package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public interface RecipeWrapper<T> {

	boolean isCustom();

	void matches(@Nonnull final RecipeInjector recipeInjector,@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer);

	T getRecipe();


}
