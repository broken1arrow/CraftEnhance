package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.Location;

import javax.annotation.Nonnull;

public class FinishCraft {
	private final Location location;
	private final EnhancedRecipe enhancedRecipe;

	private FinishCraft(@Nonnull final Location location, @Nonnull final EnhancedRecipe enhancedRecipe) {
		this.location = location;
		this.enhancedRecipe = enhancedRecipe;
	}

	public static FinishCraft of(@Nonnull final Location location, @Nonnull final EnhancedRecipe enhancedRecipe) {
		return new FinishCraft(location, enhancedRecipe);
	}

	@Nonnull
	public Location getLocation() {
		return location;
	}

	@Nonnull
	public EnhancedRecipe getEnhancedRecipe() {
		return enhancedRecipe;
	}
}
