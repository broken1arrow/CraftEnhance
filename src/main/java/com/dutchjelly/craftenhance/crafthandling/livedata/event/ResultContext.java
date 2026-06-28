package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class ResultContext {
	private final ResultType craftResultType;
	private final EnhancedRecipe enhancedRecipe;
	private final ItemStack itemStack;

	public ResultContext(final ItemStack itemStack, final ResultType resultType) {
		this(null, itemStack, resultType);
	}

	public ResultContext(@Nullable final EnhancedRecipe enhancedRecipe, final ItemStack itemStack, final ResultType resultType) {
		this.enhancedRecipe = enhancedRecipe;
		this.itemStack = itemStack;
		this.craftResultType = resultType;
	}

	@Nullable
	public ItemStack getItemStack() {
		return itemStack;
	}

	@Nullable
	public EnhancedRecipe getEnhancedRecipe() {
		return enhancedRecipe;
	}

	public ResultType getResultType() {
		return craftResultType;
	}

}


