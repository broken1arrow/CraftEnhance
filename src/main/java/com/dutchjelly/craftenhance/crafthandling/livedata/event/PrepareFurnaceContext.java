package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PrepareFurnaceContext implements PrepareRecipeContext {
	private Furnace furnace;
	private ItemStack[] recipeMatrix;

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
