package com.dutchjelly.craftenhance.gui.guis.editors;

import org.bukkit.inventory.ItemStack;

public class IngredientsCache {

	private  ItemStack[] itemStacks;
	private ItemStack itemStackResult;

	public void setItemStacks(final ItemStack[] itemStacks) {
		this.itemStacks = itemStacks;
	}

	public void setItemStackResult(final ItemStack itemStackResult) {
		this.itemStackResult = itemStackResult;
	}

	public void clear() {
		itemStacks = null;
		itemStackResult = null;
	}

	public ItemStack[] getItemStacks() {
		return itemStacks;
	}

	public ItemStack getItemStackResult() {
		return itemStackResult;
	}
}
