package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.permissions.Permissible;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeInjector extends RecipeDebug implements Listener {

	@Getter
	private final CraftEnhance plugin;
	private final boolean disableDefaultModeldataCrafts;
	private final boolean makeItemsadderCompatible;
	private final BrewingRecipeInjector brewingRecipeInjector;
	private final WorkBenchRecipeInjector workBenchRecipeInjector;
	@Getter
	private final FurnaceRecipeInjector furnaceRecipeInjector;
	private RecipeLoader loader;

	public RecipeInjector(final CraftEnhance plugin) {
		this.plugin = plugin;
		disableDefaultModeldataCrafts = plugin.getConfig().getBoolean("disable-default-custom-model-data-crafts");
		makeItemsadderCompatible = plugin.getConfig().getBoolean("make-itemsadder-compatible");
		this.brewingRecipeInjector = new BrewingRecipeInjector(this);
		this.workBenchRecipeInjector = new WorkBenchRecipeInjector(this);
		this.furnaceRecipeInjector = new FurnaceRecipeInjector(this);
		try {
			Bukkit.getPluginManager().registerEvents(new SmeltListener(), plugin);
			Bukkit.getPluginManager().registerEvents(new CrafterListener(), plugin);
		} catch (Throwable throwable) {
			Debug.Send("Some functions did not work on your serverversion. will be turned off.");
		}
	}

	public void reload() {
		this.brewingRecipeInjector.reloadSettings();
		//this.loader = RecipeLoader.getInstance();
	}



	public boolean containsModelData(final ItemStack[] matrix) {
		return Arrays.stream(matrix).anyMatch(x -> x != null && x.hasItemMeta() && x.getItemMeta().hasCustomModelData());
	}

	public IMatcher<ItemStack> getTypeMatcher() {
		return Adapter.canUseModeldata() && disableDefaultModeldataCrafts ?
				ItemMatchers.constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData)
				: ItemMatchers::matchType;
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (self().getConfig().getBoolean("learn-recipes")) {
			try {
				for (final NamespacedKey namespacedKey : e.getPlayer().getDiscoveredRecipes()) {
					if (namespacedKey.getNamespace().contains("craftenhance")) {
						e.getPlayer().undiscoverRecipe(namespacedKey);
					}
				}
			} catch (final Exception ignored) {
			}
			Adapter.DiscoverRecipes(e.getPlayer(), RecipeLoader.getInstance().getLoadedServerRecipes());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void handleCrafting(final PrepareItemCraftEvent craftEvent) {
		if (craftEvent.getRecipe() == null || craftEvent.getRecipe().getResult() == null || !plugin.getConfig().getBoolean("enable-recipes"))
			return;
		if (!(craftEvent.getInventory() instanceof CraftingInventory)) return;

		final CraftingInventory craftingInventory = craftEvent.getInventory();
		final Recipe serverRecipe = craftEvent.getRecipe();
		final List<HumanEntity> viewers = craftEvent.getViewers();
		this.workBenchRecipeInjector.craftItem(serverRecipe, craftingInventory.getMatrix(), craftingInventory, viewers, craftingInventory::setResult);
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
		this.furnaceRecipeInjector.smelt(e);
/*		Debug.Send(Type.Smelting, () -> "Furnace has smelt item");
		final RecipeGroup group = getMatchingRecipeGroup(e.getBlock(), e.getSource());
		final Furnace furnace = (Furnace) e.getBlock().getState();
		final Optional<ItemStack> result = getFurnaceResult(group, e.getSource(), furnace);
		Debug.Send(Type.Smelting, () -> "Custom result " + result);
		if (result != null && result.isPresent()) {
			ItemStack itemInResulSlot = furnace.getInventory().getResult();
		*/
		/*	if(!result.get().isSimilar(itemInResulSlot)) {
				System.out.println("result is not same");
				e.setCancelled(true);
				return;
			}*/
		/*
			e.setResult(result.get());
		} else {
			final ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(e.getSource().getType()));
			if (itemStack != null) {
				Debug.Send(Type.Smelting, () -> "Found similar vanilla recipe " + itemStack);
				if (group == null || group.getEnhancedRecipes() == null || group.getEnhancedRecipes().isEmpty()) {
					e.setResult(itemStack);
					return;
				}
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
					final boolean isVanillaRecipe = fRecipe.matchesType(new ItemStack[]{e.getSource()}) && !fRecipe.getResult().isSimilar(itemStack);
					if (eRecipe.isCheckPartialMatch() && isVanillaRecipe) {
						e.setCancelled(true);
						break;
					}
					if (isVanillaRecipe) {
						e.setResult(itemStack);
						notCustomItem.add(e.getBlock().getLocation());
						break;
					}
				}
			} else {
				Debug.Send(Type.Smelting, () -> "No similar matching to the vanilla recipe, will not changing the outcome.");
			}
			// else
			//e.setCancelled(true);
		}*/
	}

	@EventHandler(ignoreCancelled = false)
	public void burn(final FurnaceBurnEvent e) {
		if (e.isCancelled()) {
			Debug.Send(Type.Smelting, () -> "Furnace could not start start to burn the item. As the event is canceled.");
			return;
		}
		Debug.Send(Type.Smelting, () -> "Furnace start to burn the item");
		this.furnaceRecipeInjector.burn(e);
/*		final Furnace furnace = (Furnace) e.getBlock().getState();
		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		if (pausedFurnaces.getOrDefault(furnace, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			e.setCancelled(true);
			return;
		}
		final RecipeGroup recipe = getMatchingRecipeGroup(e.getBlock(), furnace.getInventory().getSmelting());
		final Optional<ItemStack> result = getFurnaceResult(recipe, furnace.getInventory().getSmelting(), furnace);
		ItemStack itemInResulSlot = furnace.getInventory().getResult();
		System.out.println("itemInResulSlot " + itemInResulSlot);
		if (result != null && result.isPresent() && itemInResulSlot != null && itemInResulSlot.getType() != Material.AIR && !result.get().isSimilar(itemInResulSlot)) {
			System.out.println("result is not same");
			e.setCancelled(true);
			return;
		}

		if (result != null && !result.isPresent()) {
			if (furnace.getInventory().getSmelting() != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(furnace.getInventory().getSmelting().getType())) != null)
				return;
			e.setCancelled(true);
			pausedFurnaces.put(furnace, LocalDateTime.now().plusSeconds(10L));
		}*/
	}

	@EventHandler
	public void furnaceClick(final InventoryClickEvent e) {
		if (e.isCancelled() || e.getClickedInventory() == null) return;
		if (e.getClickedInventory().getType() == InventoryType.FURNACE) {
			final Furnace f = (Furnace) e.getClickedInventory().getHolder();
			this.furnaceRecipeInjector.furnaceClick(e);
		}
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
	public void furnaceBreak(final BlockBreakEvent e) {
		if (e.isCancelled()) return;
		if (e.getBlock().getType().equals(Material.FURNACE)) {
			this.furnaceRecipeInjector.furnaceBreak(e);
		}
	}

	@EventHandler
	public void onBrewClick(InventoryClickEvent event) {
		if (event.getInventory().getType() != InventoryType.BREWING) return;
		brewingRecipeInjector.onBrewClick(event);
	}

	@EventHandler
	public void onBrewDrag(InventoryDragEvent event) {
		if (event.getInventory().getType() != InventoryType.BREWING) return;
		brewingRecipeInjector.onBrewDrag(event);
	}

	@EventHandler
	public void onBrew(BrewEvent event) {
	}

	public boolean isViewersAllowedCraft(final List<HumanEntity> viewers, final WBRecipe wbRecipe) {
		if (viewers.isEmpty())
			return true;
		return viewers.stream().allMatch(x -> entityCanCraft(x, wbRecipe));
	}

	public boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result)) {
					return true;
				}
			}
		return false;
	}

	public boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull WBRecipe wbRecipe, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result) && wbRecipe.isSimilar(disabledRecipe)) {
					return true;
				}
			}
		return false;
	}

	@Nullable
	public RecipeGroup getMatchingRecipeGroup(final Block typeOfFurnace, final ItemStack source) {
		final ItemStack[] srcMatrix = new ItemStack[]{source};
		RecipeType recipeType = RecipeType.getType(typeOfFurnace);
		if (recipeType == null) return null;

		FurnaceRecipe recipe = null;
		switch (recipeType) {
			case WORKBENCH:
				//recipe = new WBRecipe(null, null, srcMatrix);
				break;
			case FURNACE:
				recipe = new FurnaceRecipe(null, null, srcMatrix);
				break;
			case BLAST:
				recipe = new BlastRecipe(null, null, srcMatrix);
				break;
			case SMOKER:
				recipe = new SmokerRecipe(null, null, srcMatrix);
				break;
		}
		if (recipe == null) return null;

		return RecipeLoader.getInstance().findSimilarGroup(recipe);
	}


	public boolean isCraftingAllowedInWorld(final Location location, final EnhancedRecipe eRecipe) {
		final Set<String> allowedWorlds = eRecipe.getAllowedWorlds();
		//todo Similar recipes could prevent world blocking from working.
		if (allowedWorlds == null || allowedWorlds.isEmpty()) return false;
		if (location != null) {
			if (location.getWorld() == null) return true;
			for (final String world : allowedWorlds) {
				if (location.getWorld().getName().equals(world)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean entityCanCraft(final Permissible entity, final EnhancedRecipe group) {
		return group.getPermission() == null || group.getPermission().isEmpty()
				|| (entity != null && entity.hasPermission(group.getPermission()));
	}

	/**
	 * Gets the top inventory from the InventoryView of an InventoryEvent,
	 * using reflection to stay compatible with both old and new Spigot versions.
	 *
	 * @param event The InventoryEvent
	 * @return The top inventory, or null if unavailable
	 */
	public Inventory getTopInventory(InventoryEvent event) {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_19)) {
			try {
				// Use reflection to avoid linking to InventoryView directly
				Object view = InventoryEvent.class.getMethod("getView").invoke(event);
				return (Inventory) view.getClass().getMethod("getTopInventory").invoke(view);
			} catch (Exception e) {
				e.printStackTrace(); // Optionally log better
				return null;
			}
		} else {
			return event.getView().getTopInventory();
		}
	}

	public boolean isDisableDefaultModeldataCrafts() {
		return disableDefaultModeldataCrafts;
	}

	public boolean isMakeItemsadderCompatible() {
		return makeItemsadderCompatible;
	}

	public RecipeLoader getLoader() {
		return loader;
	}

	public void setLoader(final RecipeLoader loader) {
		this.loader = loader;
	}


	private class SmeltListener implements Listener {

		@EventHandler
		public void startSmelt(FurnaceStartSmeltEvent event) {
			final RecipeGroup group = getMatchingRecipeGroup(event.getBlock(), event.getSource());
			FurnaceRecipe furnaceRecipe = getFurnaceRecipeInjector().getFurnaceRecipe(event.getBlock().getType(), group, event.getSource(), null);
			if (furnaceRecipe == null) {
				/*todo need to fix so you can stop it from progress if not allow to burn the item  */
				return;
			}
			event.setTotalCookTime(furnaceRecipe.getDuration());
		}
	}

	private class CrafterListener implements Listener {

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
		public void CrafterCraft(final CrafterCraftEvent craftEvent) {
			Crafter crafterInventory = ((Crafter) craftEvent.getBlock().getState());
			workBenchRecipeInjector.craftItem(craftEvent.getRecipe(), crafterInventory.getInventory().getContents(), crafterInventory.getInventory(), new ArrayList<>(), (itemstack) -> {
				if (itemstack != null)
					craftEvent.setResult(itemstack);
				else
					craftEvent.setResult(new ItemStack(Material.AIR));
			});
		}
	}
}
