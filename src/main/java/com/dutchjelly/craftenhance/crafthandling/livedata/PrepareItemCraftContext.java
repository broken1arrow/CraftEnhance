package com.dutchjelly.craftenhance.crafthandling.livedata;


import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class PrepareItemCraftContext {
	private CraftingInventory inventory;
	private ItemStack[] recipeMatrix;
	private  Consumer<ItemStack> result;
	private List<HumanEntity> viewers;

	public CraftingInventory getInventory() {
		return inventory;
	}

	public void setInventory(final CraftingInventory inventory) {
		this.inventory = inventory;
	}

	public ItemStack[] getRecipeMatrix() {
		if(recipeMatrix == null)
			return new ItemStack[0];
		return recipeMatrix;
	}

	public void setRecipeMatrix(final ItemStack[] recipeMatrix) {
		this.recipeMatrix = recipeMatrix;
	}

	public void acceptResult(final ItemStack itemStack) {
		if(result == null) return;
		result.accept(itemStack);
	}

	public void setResult(final Consumer<ItemStack> result) {
		this.result = result;
	}

	public List<HumanEntity> getViewers() {
		return viewers;
	}

	public void setViewers(final List<HumanEntity> viewers) {
		this.viewers = viewers;
	}
}
