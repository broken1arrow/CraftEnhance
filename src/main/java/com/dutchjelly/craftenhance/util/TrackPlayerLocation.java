package com.dutchjelly.craftenhance.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrackPlayerLocation {
	private final Map<UUID, Location> activeCraftingTables = new HashMap<>();

	public Location onPrepareCraft(Player player) {
		Location craftLocation = activeCraftingTables.get(player.getUniqueId());
		if (craftLocation == null) {
			craftLocation = player.getLocation();
		}
		return craftLocation;
	}

	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getInventory().getType() == InventoryType.WORKBENCH) {
			activeCraftingTables.remove(event.getPlayer().getUniqueId());
		}
	}

	public void onPlayerQuit(PlayerQuitEvent event) {
		activeCraftingTables.remove(event.getPlayer().getUniqueId());
	}

	public void onInventoryInteract(final PlayerInteractEvent event) {
		Player player = event.getPlayer();
		try {
			final Block clickedBlock = event.getClickedBlock();
			final BlockState state = clickedBlock.getState();
			System.out.println("state  " + state );
			Inventory inventory = (Inventory) state;
			final Location location = clickedBlock.getLocation();
			activeCraftingTables.put(player.getUniqueId(),  location);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("activeCraftingTables " + activeCraftingTables);
	}
}
