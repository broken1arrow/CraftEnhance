package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.ServerLoadable;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.TrackPlayerLocation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeInjector implements Listener {
	@Getter
	private final CraftEnhance plugin;
	private final Map<UUID, FinishCraft> finishRecipe = new HashMap<>();

	private final BrewingRecipeInjector brewingRecipeInjector;
	private WorkBenchRecipeInjector workBenchRecipeInjector;
	@Getter
	private final FurnaceRecipeInjector furnaceRecipeInjector;
	private TrackPlayerLocation trackPlayerLocation;
	private RecipeLoader loader;

	public RecipeInjector(final CraftEnhance plugin) {
		this.plugin = plugin;

		this.brewingRecipeInjector = new BrewingRecipeInjector();
		//this.workBenchRecipeInjector = new WorkBenchRecipeInjector(this);
		this.furnaceRecipeInjector = new FurnaceRecipeInjector(this);
		if (plugin.getVersionChecker().olderThan(ServerVersion.v1_10))
			this.trackPlayerLocation = new TrackPlayerLocation();
		try {
			Bukkit.getPluginManager().registerEvents(new SmeltListener(), plugin);
			Bukkit.getPluginManager().registerEvents(new CrafterListener(), plugin);
		} catch (Throwable throwable) {
			Debug.error("Some functions did not work on your server version. will be turned off.", throwable);
		}
	}

	public void reload() {
		this.brewingRecipeInjector.reloadSettings();
		//this.loader = RecipeLoader.getInstance();
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (self().getConfig().getBoolean("learn-recipes")) {
			final Player player = e.getPlayer();
			try {
				for (final NamespacedKey namespacedKey : player.getDiscoveredRecipes()) {
					if (namespacedKey.getNamespace().contains("craftenhance")) {
						player.undiscoverRecipe(namespacedKey);
					}
				}
			} catch (final Exception ignored) {
			}
			Adapter.DiscoverRecipes(player, self().getCacheRecipes().getListOfRecipes().stream()
					.filter(enhancedRecipe -> FormatListContents.canViewRecipe(enhancedRecipe, player))
					.map(ServerLoadable::getServerRecipe)
					.collect(Collectors.toList()));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void handleCrafting(final PrepareItemCraftEvent craftEvent) {
		final Recipe serverRecipe = craftEvent.getRecipe();

		final boolean enabled = !plugin.getConfig().getBoolean("enable-recipes");
		if (serverRecipe == null || serverRecipe.getResult() == null || enabled) {
			String reason = "Unknown event";
			if (serverRecipe == null)
				reason = "The server recipe is null";
			if (enabled)
				reason = "The check for custom recipes is turned off in the config.yml, it will allowing the crafting and not doing the recipe check.";

			final String finalReason = reason;
			Debug.send(Type.Crafting, "craft event", () -> "Failed to search for a recipe because: " + finalReason);
			return;
		}
		if (!(craftEvent.getInventory() instanceof CraftingInventory)) return;

		final CraftingInventory craftingInventory = craftEvent.getInventory();
		final List<HumanEntity> viewers = craftEvent.getViewers();


		final List<RecipeWrapper> recipes = this.getLoader().findMatchingRecipe(RecipeType.WORKBENCH, craftingInventory.getMatrix());
		final TrackPlayerLocation trackPlayerCraft = this.trackPlayerLocation;
		if (trackPlayerCraft != null) {
			craftingInventory.setResult(null);
			Debug.send(Type.Crafting, "Legacy craft", () -> "Legacy crafting detected, amount recipes found for the items added in crafting grid: " + recipes.size());
			boolean foundMatch = trackPlayerCraft.onPrepareCrafting(this, craftEvent, recipes, viewers);
			if (!foundMatch) {
				Debug.send(Type.Crafting, "Legacy craft", () -> "Legacy crafting detected, could not found a valid recipe for result will deny the crafting: " + serverRecipe.getResult());
			}
			return;
		}

		final Location location = craftingInventory.getLocation();
		viewers.forEach(humanEntity -> removeFinishRecipe(humanEntity.getUniqueId()));

		for (RecipeWrapper recipe : recipes) {
			ResultContext contextResult = recipe.matches(craftEvent.getRecipe(), prepareRecipeContext -> {
				if (prepareRecipeContext instanceof PrepareItemCraftContext) {
					final PrepareItemCraftContext recipeContext = (PrepareItemCraftContext) prepareRecipeContext;
					recipeContext.setRecipeMatrix(craftingInventory.getMatrix());
					recipeContext.setViewers(viewers);
					recipeContext.setLocation(location);
					recipeContext.setInventory(craftingInventory);
				}
			});
			if (contextResult == null) continue;
			if (endCraftingCheck(contextResult, location, craftingInventory)) return;
		}
		craftingInventory.setResult(null);
	}


	@EventHandler
	public void exstract(final FurnaceExtractEvent e) {

	/*	if (!notCustomItem.isEmpty() && notCustomItem.contains(e.getBlock().getLocation())) {
			e.setExpToDrop(getExp(e.getItemType()));
			notCustomItem.remove(e.getBlock().getLocation());
		}*/
	}

	@EventHandler
	public void smelt(final FurnaceSmeltEvent e) {
		this.furnaceRecipeInjector.smeltTask(e);
	}

	@EventHandler(ignoreCancelled = false)
	public void burn(final FurnaceBurnEvent e) {
		if (e.isCancelled()) {
			Debug.Send(Type.Smelting, () -> "Furnace could not start start to burn the item. As the event is canceled.");
			return;
		}
		this.furnaceRecipeInjector.burnTask(e);
	}

	@EventHandler
	public void onPlayerBlockClick(PlayerInteractEvent event) {
		final Block clickedBlock = event.getClickedBlock();
		com.dutchjelly.craftenhance.util.TrackPlayerLocation trackPlayer = this.trackPlayerLocation;
		if (trackPlayer != null) {
			if (clickedBlock == null || clickedBlock.getType() != Material.getMaterial("WORKBENCH"))
				return;
			trackPlayer.onInventoryInteract(event);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getInventory().getType() == InventoryType.WORKBENCH) {
			com.dutchjelly.craftenhance.util.TrackPlayerLocation trackPlayer = this.trackPlayerLocation;
			if (trackPlayer != null)
				trackPlayer.onInventoryClose(event);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		com.dutchjelly.craftenhance.util.TrackPlayerLocation trackPlayer = this.trackPlayerLocation;
		if (trackPlayer != null)
			trackPlayer.onPlayerQuit(event);
	}

	@EventHandler
	public void onInventoryClick(final InventoryClickEvent event) {
		if (event.isCancelled() || event.getClickedInventory() == null) return;
		if (event.getClickedInventory().getType() == InventoryType.FURNACE) {
			final Furnace f = (Furnace) event.getClickedInventory().getHolder();
			this.furnaceRecipeInjector.furnaceClick(event);
		}
		if (event.getClickedInventory().getType() == InventoryType.WORKBENCH) {
			this.craftingClick(event);
		}
		if (event.getInventory().getType() == InventoryType.BREWING) {
			brewingRecipeInjector.onBrewClick(event);
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (event.getInventory().getType() != InventoryType.BREWING) return;
		brewingRecipeInjector.onBrewDrag(event);
	}

	@EventHandler
	public void furnacePlace(final BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		Material material = e.getBlock().getType();
		if (material == Material.FURNACE || this.plugin.getVersionChecker().newerThan(ServerVersion.v1_13) && (material == Material.BLAST_FURNACE || material == Material.SMOKER)) {
			this.furnaceRecipeInjector.furnacePlace(e);
		}
	}

	@EventHandler
	public void containerBreak(final BlockBreakEvent e) {
		if (e.isCancelled()) return;
		if (e.getBlock().getType().equals(Material.FURNACE)) {
			this.furnaceRecipeInjector.furnaceBreak(e);
		}
	}

	@EventHandler
	public void onBrew(BrewEvent event) {
	}

	public void removeFinishRecipe(final UUID location) {
		finishRecipe.remove(location);
	}


	public void craftingClick(final InventoryClickEvent craftingClick) {

		if (craftingClick.getSlot() != 0) return;
		final Inventory clickedInventory = craftingClick.getClickedInventory();
		if (clickedInventory == null) return;

		this.finishRecipe.computeIfPresent(craftingClick.getWhoClicked().getUniqueId(), (location, finishCraft) -> {
			final EnhancedRecipe enhancedRecipe = finishCraft.getEnhancedRecipe();
			final String onCraftCommand = enhancedRecipe.getOnCraftCommand();

			if (onCraftCommand == null || onCraftCommand.trim().isEmpty())
				return null;
			CraftEnhance.runTaskLater(2, () ->
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), onCraftCommand.replace("%playername%", craftingClick.getWhoClicked().getName()))
			);
			return null;
		});
	}

	public boolean endCraftingCheck(final ResultContext contextResult, final Location location, final CraftingInventory craftingInventory) {
		switch (contextResult.getResultType()) {
			case ENHANCED:
			case VANILLA:
				if (self().isMakeItemsadderCompatible() && Adapter.containsModelData(craftingInventory.getMatrix())) {
					CraftEnhance.runTask(() -> craftingInventory.setResult(contextResult.getItemStack()));
				} else {
					craftingInventory.setResult(contextResult.getItemStack());
				}
				EnhancedRecipe enhancedRecipe = contextResult.getEnhancedRecipe();
				if (enhancedRecipe != null) {
					final String onCraftCommand = enhancedRecipe.getOnCraftCommand();
					if (onCraftCommand != null && !onCraftCommand.isEmpty()) {
						craftingInventory.getViewers().forEach(humanEntity ->
								this.finishRecipe.put(humanEntity.getUniqueId(), FinishCraft.of(location, enhancedRecipe))
						);
					}
				}
				return true;
			case PARTIAL_MATCH:
			case DISABLED:
			case NO_PERMISSION:
			case CANCELLED:
			case BLOCKED:
				craftingInventory.setResult(null);
				return true;
			case NO_MATCH:
		}
		return false;
	}

	public RecipeLoader getLoader() {
		return loader;
	}

	public void setLoader(final RecipeLoader loader) {
		this.loader = loader;
	}


	private class SmeltListener implements Listener {

		public boolean isLeftClick(Action action) {
			return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
		}

		public boolean isRightClick(Action action) {
			return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
		}

		@EventHandler
		public void startSmelt(FurnaceStartSmeltEvent event) {
			RecipeType recipeType = RecipeType.FURNACE;
			final Furnace furnace = (Furnace) event.getBlock().getState();
			if (plugin.getVersionChecker().newerThan(ServerVersion.v1_13)) {
				if (furnace instanceof BlastFurnace)
					recipeType = RecipeType.BLAST;
				if (furnace instanceof Smoker)
					recipeType = RecipeType.SMOKER;
			}
			Debug.Send(Type.Smelting, () -> "Alter the time for the furnace recipe if could find it in cache.");
			final List<RecipeWrapper> matchingRecipe = loader.findMatchingRecipe(recipeType, new ItemStack[]{event.getSource()});
			ResultContext furnaceContext = getFurnaceRecipeInjector().getFurnaceContext(event.getRecipe(), matchingRecipe, new ItemStack[]{event.getSource()}, furnace);
			if (furnaceContext == null) {
				/*todo need to fix so you can stop it from progress if not allow to burn the item  */
				return;
			}
			if (furnaceContext.getEnhancedRecipe() instanceof FurnaceRecipe) {
				final int duration = ((FurnaceRecipe) furnaceContext.getEnhancedRecipe()).getDuration();
				Debug.Send(Type.Smelting, () -> "Alter the time for the furnace recipe to the correct time that is: " + duration + " ticks from the recipes set: " + event.getTotalCookTime());
				event.setTotalCookTime(duration);
			}
		}
	}

	private class CrafterListener implements Listener {

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
		public void CrafterCraft(final CrafterCraftEvent craftEvent) {
			Crafter crafter = ((Crafter) craftEvent.getBlock().getState());
			final Inventory inventory = crafter.getInventory();
			final ItemStack[] matrix = inventory.getContents();

			final List<RecipeWrapper> recipes = getLoader().findMatchingRecipe(RecipeType.WORKBENCH, matrix);

			for (RecipeWrapper recipe : recipes) {
				ResultContext contextResult = recipe.matches(craftEvent.getRecipe(), prepareRecipeContext -> {
					if (prepareRecipeContext instanceof PrepareItemCraftContext) {
						final PrepareItemCraftContext recipeContext = (PrepareItemCraftContext) prepareRecipeContext;
						recipeContext.setRecipeMatrix(matrix);
						recipeContext.setViewers(new ArrayList<>());
						recipeContext.setInventory(inventory);
						recipeContext.setLocation(inventory.getLocation());
					}
				});
				if (contextResult == null) continue;
				if (endCraftingCheck(contextResult, crafter, craftEvent)) return;
			}
		}

		private boolean endCraftingCheck(final ResultContext contextResult, final Crafter crafter, final CrafterCraftEvent craftEvent) {
			switch (contextResult.getResultType()) {
				case ENHANCED:
				case VANILLA:
					final ItemStack itemStack = contextResult.getItemStack() != null ? contextResult.getItemStack() : new ItemStack(Material.AIR);
					if (self().isMakeItemsadderCompatible() && Adapter.containsModelData(crafter.getInventory().getContents())) {
						CraftEnhance.runTask(() -> craftEvent.setResult(itemStack));
					} else {
						craftEvent.setResult(itemStack);
					}
					return true;
				case PARTIAL_MATCH:
				case DISABLED:
				case NO_PERMISSION:
				case CANCELLED:
				case BLOCKED:
					craftEvent.setResult(new ItemStack(Material.AIR));
					return true;
				case NO_MATCH:
			}
			return false;
		}
	}
}
