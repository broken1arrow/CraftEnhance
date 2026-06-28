package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackPlayerLocation {
	private final Map<UUID, Location> activeCraftingTables = new HashMap<>();

	public void onInventoryClose(InventoryCloseEvent event) {

		if (event.getInventory().getType() == InventoryType.WORKBENCH) {
			Location removed = activeCraftingTables.remove(event.getPlayer().getUniqueId());
			if (removed != null)
				Debug.send(Type.Crafting,"Legacy crafting close", () -> "Legacy crafting detected, removed player has he close the crafting inventory.");
		}
	}

	public void onPlayerQuit(PlayerQuitEvent event) {
		Location removed = activeCraftingTables.remove(event.getPlayer().getUniqueId());
		if (removed != null)
			Debug.send(Type.Crafting,"Legacy crafting quit", () -> "Legacy crafting detected, removed player has he left the server.");
	}

	public void onInventoryInteract(final PlayerInteractEvent event) {
		Player player = event.getPlayer();
		final Block clickedBlock = event.getClickedBlock();
		final Location location = clickedBlock.getLocation();
		activeCraftingTables.put(player.getUniqueId(), location);
		Debug.send(Type.Crafting,"Legacy crafting interact", () -> "Legacy crafting detected, adding player during crafting to the cache.");
	}

	public boolean onPrepareCrafting(@Nonnull final RecipeInjector recipeInjector, @Nonnull final PrepareItemCraftEvent craftEvent, @Nonnull final List<RecipeWrapper> recipes, @Nonnull final List<HumanEntity> viewers) {
		for (HumanEntity viewer : viewers) {
			final Location location = this.activeCraftingTables.get(viewer.getUniqueId());
			final CraftingInventory craftingInventory = craftEvent.getInventory();

			recipeInjector.removeFinishRecipe(viewer.getUniqueId());

			for (RecipeWrapper recipe : recipes) {
				ResultContext contextResult = recipe.matches(craftEvent.getRecipe(), prepareRecipeContext -> {
					if (prepareRecipeContext instanceof PrepareItemCraftContext) {
						final PrepareItemCraftContext recipeContext = (PrepareItemCraftContext) prepareRecipeContext;
						recipeContext.setRecipeMatrix(craftingInventory.getMatrix());
						recipeContext.setViewers(viewers);
						recipeContext.setInventory(craftingInventory);
						recipeContext.setLocation(location);
					}
				});
				if (contextResult == null) continue;
				if (recipeInjector.endCraftingCheck(contextResult, location, craftingInventory)) return true;
			}
		}
		return false;
	}

}
