package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public interface RecipeWrapper {
	boolean isCustom();
	void matches(@Nonnull final RecipeInjector recipeInjector,@Nonnull final PrepareItemCraftEvent craftEvent,@Nonnull final Consumer<ItemStack> result);
	ItemStack getResult();


}
