package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PrepareFurnaceContext implements PrepareRecipeContext {
	private Furnace furnace;

	public Furnace getFurnace() {
		return furnace;
	}

	public void setFurnace(final Furnace furnace) {
		this.furnace = furnace;
	}

	@Override
	public ItemStack[] getRecipeMatrix() {
		return new ItemStack[0];
	}

	@Override
	public void setRecipeMatrix(final ItemStack[] recipeMatrix) {

	}

	@Override
	public void acceptResult(final ItemStack itemStack) {

	}

	@Override
	public void setResult(final Consumer<ItemStack> result) {

	}

	@Override
	public void setViewers(final List<HumanEntity> viewers) {

	}

	@Override
	public List<HumanEntity> getViewers() {
		return new ArrayList<>();
	}
}
