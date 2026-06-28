package com.dutchjelly.craftenhance.crafthandling.livedata.brewing;

import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class BrewingDragContext {
	InventoryDragEvent event;
	Location location;
	ItemStack itemStackCursor;
	Inventory brewingInv;

	public static BrewingDragContext ofClick(final Consumer<BrewingDragContext> callback) {
		final BrewingDragContext BrewingDragContext = new BrewingDragContext();
		callback.accept(BrewingDragContext);
		return BrewingDragContext;
	}

	public InventoryDragEvent getEvent() {
		return event;
	}

	public BrewingDragContext setEvent(final InventoryDragEvent event) {
		this.event = event;
		return this;
	}

	public Location getLocation() {
		return location;
	}

	public BrewingDragContext setLocation(final Location location) {
		this.location = location;
		return this;
	}

	public ItemStack getItemStackCursor() {
		return itemStackCursor;
	}

	public BrewingDragContext setItemStackCursor(final ItemStack itemStackCursor) {
		this.itemStackCursor = itemStackCursor;
		return this;
	}

	public Inventory getBrewingInv() {
		return brewingInv;
	}

	public BrewingDragContext setBrewingInv(final Inventory brewingInv) {
		this.brewingInv = brewingInv;
		return this;
	}

	
}
