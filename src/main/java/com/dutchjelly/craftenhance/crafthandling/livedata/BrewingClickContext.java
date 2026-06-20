package com.dutchjelly.craftenhance.crafthandling.livedata;

import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class BrewingClickContext {
		InventoryClickEvent event;
		Location location;
		ItemStack itemStackCursor;
		BrewerInventory brewingInv;
		int slot;

		public static BrewingClickContext ofClick(final Consumer<BrewingClickContext> callback) {
			final BrewingClickContext brewingClickContext = new BrewingClickContext();
			callback.accept(brewingClickContext);
			return brewingClickContext;
		}

		public InventoryClickEvent getEvent() {
			return event;
		}

		public BrewingClickContext setEvent(final InventoryClickEvent event) {
			this.event = event;
			return this;
		}
		
		public Location getLocation() {
			return location;
		}

		public BrewingClickContext setLocation(final Location location) {
			this.location = location;
			return this;
		}

		public ItemStack getItemStackCursor() {
			return itemStackCursor;
		}

		public BrewingClickContext setItemStackCursor(final ItemStack itemStackCursor) {
			this.itemStackCursor = itemStackCursor;
			return this;
		}

		public BrewerInventory getBrewingInv() {
			return brewingInv;
		}

		public BrewingClickContext setBrewingInv(final BrewerInventory brewingInv) {
			this.brewingInv = brewingInv;
			return this;
		}

		public int getSlot() {
			return slot;
		}

		public BrewingClickContext setSlot(final int slot) {
			this.slot = slot;
			return this;
		}
	}