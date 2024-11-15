package com.dutchjelly.craftenhance.api.event.crafting;

import com.dutchjelly.craftenhance.api.event.EventUtility;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BeforeCraftOutputEvent extends EventUtility {

	private static final HandlerList handlers = new HandlerList();
	private final EnhancedRecipe eRecipe;
	private final WBRecipe wbRecipe;
	private ItemStack resultItem;
	private boolean cancel;
	public BeforeCraftOutputEvent(final EnhancedRecipe eRecipe, final WBRecipe wbRecipe, final ItemStack resultItem) {
		super( handlers);
		this.eRecipe = eRecipe;
		this.wbRecipe = wbRecipe;
		this.resultItem = resultItem;
		registerEvent();
	}

	public EnhancedRecipe geteRecipe() {
		return eRecipe;
	}

	public WBRecipe getWbRecipe() {
		return wbRecipe;
	}

	public ItemStack getResultItem() {
		return resultItem;
	}

	public void setResultItem(final ItemStack resultItem) {
		this.resultItem = resultItem;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(final boolean cancel) {
		this.cancel = cancel;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
