package com.dutchjelly.craftenhance.crafthandling.livedata.event;


import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class PrepareItemCraftContext implements PrepareRecipeContext {
	private Inventory inventory;
	private Location location;
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
	public void setRecipeMatrix(final ItemStack[] recipeMatrix) {
		this.recipeMatrix = recipeMatrix;
	}

	public void setResult(final ResultContext  resultType) {
		this.result = resultType;
	}

	public void setViewers(final List<HumanEntity> viewers) {
		this.viewers = viewers;
	}

	public void setLocation(@Nonnull final Location location) {
		this.location = location;
	}

	@Override
	public ItemStack[] getRecipeMatrix() {
		if(recipeMatrix == null)
			return new ItemStack[0];
		return recipeMatrix;
	}

	public ResultContext getResult() {
		return this.result;
	}

	@Override
	public List<HumanEntity> getViewers() {
		return viewers;
	}

	public Location getLocation() {
		return location;
	}
}
