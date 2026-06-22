package com.dutchjelly.craftenhance.crafthandling.livedata.event;


import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PrepareItemCraftContext implements PrepareRecipeContext {
	private Inventory inventory;
	private ItemStack[] recipeMatrix;
	private  ResultContext  result;
	private List<HumanEntity> viewers;

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(final Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public ItemStack[] getRecipeMatrix() {
		if(recipeMatrix == null)
			return new ItemStack[0];
		return recipeMatrix;
	}

	@Override
	public void setRecipeMatrix(final ItemStack[] recipeMatrix) {
		this.recipeMatrix = recipeMatrix;
	}


	public void setResult(final ResultContext  resultType) {
		this.result = resultType;
	}

	public ResultContext getResult() {
		return this.result;
	}

	@Override
	public void setViewers(final List<HumanEntity> viewers) {
		this.viewers = viewers;
	}

	@Override
	public List<HumanEntity> getViewers() {
		return viewers;
	}



}
