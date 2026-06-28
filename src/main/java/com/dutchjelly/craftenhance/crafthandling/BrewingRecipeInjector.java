package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingClickContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingDragContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.brewing.BrewingWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class BrewingRecipeInjector {
	private final CraftEnhance plugin = self();
	private boolean enableBrewing = plugin.getConfig().getBoolean("enable-brewing-recipes");
	private boolean enableCraft = plugin.getConfig().getBoolean("enable-recipes");


	public void reloadSettings() {
		enableBrewing = plugin.getConfig().getBoolean("enable-brewing-recipes");
		enableCraft = plugin.getConfig().getBoolean("enable-recipes");
	}

	public void onBrewClick(final InventoryClickEvent event) {
		if (!enableBrewing || !enableCraft) {
			Debug.send(Type.Brewing, "denied", () -> "Brewing is turned off in the config, set it to true to allowing custom brewing recipes.");
			return;
		}
		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final RecipeLoader loader = RecipeLoader.getInstance();
		final Location location = event.getInventory().getLocation();
		if (location == null) return;
		if (!(location.getBlock().getState() instanceof BrewingStand)) return;

		final BrewingStand stand = (BrewingStand) location.getBlock().getState();
		final BrewerInventory brewerInventory = stand.getInventory();
		final Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null || clickedInventory.getType() != InventoryType.BREWING) return;

		final int slot = event.getSlot();
		if (slot == 4) {
			return;
		}

		Debug.send(Type.Brewing, "start searching", () -> "Player clicked inside Brewing stand, will start to check after matching recipe.");
		ItemStack itemStackCursor = event.getCursor();
		if (brewerInventory.getIngredient() != null)
			itemStackCursor = brewerInventory.getIngredient();

/*
		if (itemStackCursor != null) {
			final ItemStack itemStackCheck = itemStackCursor.clone();
			final List<RecipeGroup> possibleRecipeGroups = loader.findGroupsBySimilarResultMatch(itemStackCheck, RecipeType.BREWING);
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
				if (self().isDisableDefaultModeldataCrafts() && Adapter.containsModelData(brewerInventory.getContents())) {
					return;
				}
				if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, itemStackCheck)) {
					return;
				}

				Debug.Send(Type.Brewing, () -> "Couldn't find any matching brewing groups. Skipping the rest of the recipe checks.");
				return;
			}
			Debug.Send(Type.Brewing, () -> "Found a matching group of brewing recipes. Now checking for a matching recipe based on your provided items.");
			WrapBrewingClick wrapBrewingClick = WrapBrewingClick.wrapBrewingClick(wrapBrewing -> wrapBrewing
					.setEvent(event)
					.setBrewingInv(brewerInventory)
					.setLocation(location)
					.setItemStackCursor(itemStackCheck)
					.setSlot(slot)
					.setPossibleRecipeGroups(possibleRecipeGroups)
			);
			this.brewingCheck(wrapBrewingClick);
		}*/
		if (itemStackCursor != null) {
			final ItemStack itemStackCheck = itemStackCursor.clone();
			final List<RecipeWrapper> possibleRecipeGroups = loader.findMatchingRecipe(RecipeType.BREWING, new ItemStack[]{itemStackCheck});
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups.isEmpty()) {
				if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, itemStackCheck)) {
					Debug.send(Type.Brewing, "disabled_recipe", () -> "This brewing recipe is turned off, will not allow put the item inside the brewing stand.");
					return;
				}
				Debug.send(Type.Brewing, "No_group", () -> "Couldn't find any matching brewing groups. Skipping the rest of the recipe checks.");
				return;
			}
			if (self().isDisableDefaultModeldataCrafts() && Adapter.containsModelData(brewerInventory.getContents())) {
				Debug.send(Type.Brewing, "model_match", () -> "Found a matching group of brewing recipes, but has turn off creating recipe with modeldata.");
				return;
			}
			Debug.send(Type.Brewing, "Find_matching_recipes",()-> "Found " + possibleRecipeGroups.size() + " groups that matching the brewing recipe. Now checking for a matching recipe based on your provided items.");
			final BrewingClickContext clickContext = BrewingClickContext.ofClick(wrapBrewing -> wrapBrewing
					.setEvent(event)
					.setBrewingInv(brewerInventory)
					.setLocation(location)
					.setItemStackCursor(itemStackCheck)
					.setSlot(slot)
			);
			for (RecipeWrapper recipeWrapper : possibleRecipeGroups) {
				if (recipeWrapper instanceof BrewingWrapper) {
					if(((BrewingWrapper) recipeWrapper).brewingCheck(clickContext))
						return;
				}
			}
		}
	}


	public void onBrewDrag(final InventoryDragEvent event) {
		if (!enableBrewing || !enableCraft) return;
		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final RecipeLoader loader = RecipeLoader.getInstance();
		final Location location = event.getInventory().getLocation();
		if (location == null) return;

		final Inventory clickedInventory = Adapter.getTopInventory(event);
		if (clickedInventory == null || clickedInventory.getType() != InventoryType.BREWING) return;

		final BrewingStand stand = (BrewingStand) location.getBlock().getState();
		final BrewerInventory inv = stand.getInventory();

		ItemStack itemStackCursor = event.getCursor();
		if (itemStackCursor == null)
			itemStackCursor = event.getOldCursor();
		if (inv.getIngredient() != null)
			itemStackCursor = inv.getIngredient();

		if (itemStackCursor != null) {
			final ItemStack itemStackCheck = itemStackCursor.clone();
			final List<RecipeWrapper> possibleRecipeGroups = loader.findMatchingRecipe(RecipeType.BREWING, new ItemStack[]{itemStackCheck});
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
				if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, itemStackCursor)) {
					return;
				}
				Debug.Send(Type.Brewing, () -> "No matching group or groups for the recipe.");
				return;
			}
			if (self().isDisableDefaultModeldataCrafts() && Adapter.containsModelData(inv.getContents())) {
				return;
			}
			final BrewingDragContext clickContext = BrewingDragContext.ofClick(wrapBrewing -> wrapBrewing
					.setEvent(event)
					.setBrewingInv(clickedInventory)
					.setLocation(location)
					.setItemStackCursor(itemStackCheck)
			);
			for (RecipeWrapper recipeWrapper : possibleRecipeGroups) {
				if (recipeWrapper instanceof BrewingWrapper) {
					if (((BrewingWrapper) recipeWrapper).brewingDragCheck(clickContext))
						return;
				}
			}
		}
	}

	/*	@EventHandler
	public void onBrewClose(InventoryCloseEvent event) {
		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final Location location = event.getInventory().getLocation();
		if (location == null) return;
		if (!(location.getBlock().getState() instanceof BrewingStand)) return;

		BrewingStand stand = (BrewingStand) location.getBlock().getState();

		BrewerInventory inv = stand.getInventory();
		Debug.Send(Type.Brewing, () -> "inv.getIngredient() " + inv.getIngredient());
		if (inv.getIngredient() != null) {
			final ItemStack ingredient = inv.getIngredient();
			final List<RecipeGroup> possibleRecipeGroups = loader.findGroupsByResult(ingredient, RecipeType.BREWING);
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
				if (disableDefaultModeldataCrafts && Adapter.canUseModeldata() && containsModelData(inv.getContents())) {
					return;
				}
				if (checkForDisabledRecipe(disabledServerRecipes, ingredient)) {
					return;
				}

				Debug.Send(Type.Brewing, () -> "No matching groups");
				return;
			}
			for (final RecipeGroup group : possibleRecipeGroups) {
				//Check if any grouped enhanced recipe is a match.
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					if (!(eRecipe instanceof BrewRecipe)) continue;
					final BrewRecipe wbRecipe = (BrewRecipe) eRecipe;
					ItemStack[] itemStacks = inv.getContents();
					ItemStack firstItem = itemStacks[0];
					ItemStack secondItem = itemStacks[1];
					ItemStack thirdItem = itemStacks[2];
					ItemStack[] outputItems = new ItemStack[]{firstItem, secondItem, thirdItem};
					Debug.Send(Type.Brewing, () -> "wbRecipe.getResult().isSimilar(ingredient)" + wbRecipe.getResult().isSimilar(ingredient));
					Debug.Send(Type.Brewing, () -> " wbRecipe.matches(outputItems)" + wbRecipe.matches(outputItems));
					if (wbRecipe.getResult().isSimilar(ingredient) && wbRecipe.matches(outputItems)) {
						Debug.Send(Type.Brewing, () -> "Found matching brewing recipe");
						break;
					}
				}
			}
		}
		if (inv.getIngredient() != null)
			self().getBrewingTask().getTask(location, stand);
	}*/
	private void brewingCheck(final WrapBrewingClick wrapBrewingClick) {
		final InventoryClickEvent event = wrapBrewingClick.getEvent();
		final List<RecipeGroup> possibleRecipeGroups = wrapBrewingClick.getPossibleRecipeGroups();
		final Location location = wrapBrewingClick.getLocation();
		final ItemStack itemStackCursor = wrapBrewingClick.getItemStackCursor();
		final BrewerInventory brewingInv = wrapBrewingClick.getBrewingInv();
		final int slot = wrapBrewingClick.getSlot();

		for (final RecipeGroup group : possibleRecipeGroups) {
			//Check if any grouped enhanced recipe is a match.
			for (final EnhancedRecipeWrapper eRecipe : group.getRecipeGroupCache().values()) {
				final EnhancedRecipe enhancedRecipe = eRecipe.getEnhancedRecipe();
				if (!(enhancedRecipe instanceof BrewingRecipe)) continue;

				final BrewingRecipe brewingRecipe = (BrewingRecipe) enhancedRecipe;

				boolean notAllowedToBrew = RecipeAdapter.isCraftingAllowedInWorld(location, brewingRecipe);
				if (notAllowedToBrew) {
					Debug.Send(Type.Brewing, () -> "You are not allowed to brew potions in this world: " + location.getWorld() + " with this recipe key: " + brewingRecipe.getKey());
					continue;
				}

				if (brewingRecipe.getResult().isSimilar(itemStackCursor)) {

					if (checkBrewingClick(event))
						event.setCancelled(true);

					this.handleInventoryClick(event, brewingInv, slot);

					CraftEnhance.runTaskLater(1, () -> {
						ItemStack[] itemStacks = brewingInv.getContents();
						ItemStack[] outputItems = Arrays.copyOfRange(itemStacks, 0, 3);
						if (brewingRecipe.matches(outputItems)) {
							Debug.Send(Type.Brewing, () -> "Found matching brewing recipe and will start to make the recipe: " + brewingRecipe.getKey());
							this.plugin.getBrewingTask().addTask(location, brewingRecipe);
						} else {
							Debug.Send(Type.Brewing, () -> "Failed to find a matching brewing ingredients recipe for recipe: " + brewingRecipe.getKey());
							Debug.Send(Type.Brewing, () -> "Result item: " + brewingRecipe.getResult());
							Debug.Send(Type.Brewing, () -> "The items to match: " + Arrays.toString(brewingRecipe.getContent()));
							Debug.Send(Type.Brewing, () -> "The items inside inventory: " + Arrays.toString(outputItems));
						}
					});
					break;
				} else {
					Debug.Send(Type.Brewing, () -> "No match for this recipe " + eRecipe + " with this recipe key: " + eRecipe.getKey() +
							" will continue with next recipe if either the group have more recipes or has not loop trough all recipes. ");
				}
			}
		}
	}

	public void handleInventoryClick(final InventoryClickEvent event, final Inventory clickedInventory, final int slot) {
		ItemStack item = clickedInventory.getItem(slot);

		if (item == null) {
			handleNullItemInSlot(event, clickedInventory, slot);
		}

		if (event.getCursor().getAmount() < event.getCursor().getMaxStackSize() && event.getCursor().isSimilar(item)) {
			handleCursorStacking(event, clickedInventory, slot, item);
		}
	}

	private void handleNullItemInSlot(final InventoryClickEvent event, final Inventory clickedInventory, final int slot) {
		ItemStack itemCursorType = event.getCursor().clone();
		ItemStack copy = copyItemStack(itemCursorType, slot != 3 || event.getClick() == ClickType.RIGHT ? 1 : -1);


		clickedInventory.setItem(slot, copy);
		if (slot == 3 && event.getClick() == ClickType.LEFT) {
			copy.setAmount(0);
		} else {
			copy.setAmount(itemCursorType.getAmount() - 1);
		}
		event.setCursor(new ItemStack(copy));
	}

	private void handleCursorStacking(final InventoryClickEvent event, final Inventory clickedInventory, final int slot, final ItemStack item) {
		ItemStack cursorCopy = copyItemStack(event.getCursor(), event.getCursor().getAmount());

		if (event.getClick() == ClickType.LEFT && item != null && item.getAmount() + cursorCopy.getAmount() <= cursorCopy.getMaxStackSize()) {
			cursorCopy.setAmount(cursorCopy.getAmount() + item.getAmount());
			clickedInventory.setItem(slot, null);
		} else {
			if (event.getClick() == ClickType.LEFT) {
				cursorCopy.setAmount(cursorCopy.getAmount() + 1);
			}
			if (item != null) {
				if (event.getClick() == ClickType.SHIFT_LEFT) {
					item.setAmount(cursorCopy.getAmount() - item.getAmount());
					event.getWhoClicked().getInventory().addItem(item);

				} else if (event.getClick() == ClickType.LEFT) {
					item.setAmount(cursorCopy.getAmount() - item.getAmount());
				} else if (slot == 3) {
					item.setAmount(item.getAmount() + 1);
					cursorCopy.setAmount(cursorCopy.getAmount() - 1);
				}
			}
			clickedInventory.setItem(slot, item);
		}

		event.setCursor(new ItemStack(cursorCopy));
	}

	private ItemStack copyItemStack(final ItemStack itemStack, final int amount) {
		ItemStack copy = itemStack.clone();
		if (amount >= 0)
			copy.setAmount(amount);
		return copy;
	}

	public boolean checkBrewingClick(final InventoryClickEvent event) {
		return event.getSlot() != 4 && event.getCursor().getType() != Material.AIR;
	}

	private static class WrapBrewingClick {
		InventoryClickEvent event;
		List<RecipeGroup> possibleRecipeGroups;
		Location location;
		ItemStack itemStackCursor;
		BrewerInventory brewingInv;
		int slot;

		public static WrapBrewingClick wrapBrewingClick(final Consumer<WrapBrewingClick> callback) {
			final WrapBrewingClick wrapBrewingClick = new WrapBrewingClick();
			callback.accept(wrapBrewingClick);
			return wrapBrewingClick;
		}

		public InventoryClickEvent getEvent() {
			return event;
		}

		public WrapBrewingClick setEvent(final InventoryClickEvent event) {
			this.event = event;
			return this;
		}

		public List<RecipeGroup> getPossibleRecipeGroups() {
			return possibleRecipeGroups;
		}

		public WrapBrewingClick setPossibleRecipeGroups(final List<RecipeGroup> possibleRecipeGroups) {
			this.possibleRecipeGroups = possibleRecipeGroups;
			return this;
		}

		public Location getLocation() {
			return location;
		}

		public WrapBrewingClick setLocation(final Location location) {
			this.location = location;
			return this;
		}

		public ItemStack getItemStackCursor() {
			return itemStackCursor;
		}

		public WrapBrewingClick setItemStackCursor(final ItemStack itemStackCursor) {
			this.itemStackCursor = itemStackCursor;
			return this;
		}

		public BrewerInventory getBrewingInv() {
			return brewingInv;
		}

		public WrapBrewingClick setBrewingInv(final BrewerInventory brewingInv) {
			this.brewingInv = brewingInv;
			return this;
		}

		public int getSlot() {
			return slot;
		}

		public WrapBrewingClick setSlot(final int slot) {
			this.slot = slot;
			return this;
		}
	}

}
