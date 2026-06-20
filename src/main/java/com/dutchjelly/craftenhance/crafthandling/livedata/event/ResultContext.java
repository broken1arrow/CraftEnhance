package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import org.bukkit.inventory.ItemStack;

public class ResultContext {
	private final ResultType craftResultType;
	private final ItemStack itemStack;

	public ResultContext(final ItemStack itemStack, final ResultType resultType) {
		this.itemStack = itemStack;
		this.craftResultType = resultType;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public ResultType getResultType() {
		return craftResultType;
	}

}


