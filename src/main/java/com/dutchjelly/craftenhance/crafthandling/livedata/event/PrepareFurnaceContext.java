package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import com.dutchjelly.craftenhance.util.RecipeResult;
import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrepareFurnaceContext implements PrepareRecipeContext {
	private Furnace furnace;
	private ItemStack[] recipeMatrix;
	private RecipeResult recipeResult;

	public void setFurnaceResult(RecipeResult result) {
		this.recipeResult = result;
	}

	public Optional<RecipeResult> getFurnaceResult() {
		return Optional.ofNullable(recipeResult);
	}

	public Furnace getFurnace() {
		return furnace;
	}

	public void setFurnace(final Furnace furnace) {
		this.furnace = furnace;
	}

	@Override
	public ItemStack[] getRecipeMatrix() {
		return recipeMatrix;
	}

	@Override
	public void setRecipeMatrix(final ItemStack[] recipeMatrix) {
		this.recipeMatrix = recipeMatrix;
	}

	@Override
	public void setViewers(final List<HumanEntity> viewers) {

	}

	@Override
	public List<HumanEntity> getViewers() {
		return new ArrayList<>();
	}
}
