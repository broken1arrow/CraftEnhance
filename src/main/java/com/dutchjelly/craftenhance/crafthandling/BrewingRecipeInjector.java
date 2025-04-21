package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.cache.RecipeCoreData;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Bukkit;
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

	private final RecipeInjector recipeInjector;
	private final CraftEnhance plugin = self();
	private boolean enableBrewing = plugin.getConfig().getBoolean("enable-brewing-recipes");
	private boolean enableCraft = plugin.getConfig().getBoolean("enable-recipes");

	public BrewingRecipeInjector(final RecipeInjector recipeInjector) {
		this.recipeInjector = recipeInjector;
	}

	public void reloadSettings() {
		enableBrewing = plugin.getConfig().getBoolean("enable-brewing-recipes");
		enableCraft = plugin.getConfig().getBoolean("enable-recipes");
	}

	public void onBrewClick(final InventoryClickEvent event) {
		if (!enableBrewing || !enableCraft) return;

		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final Location location = event.getInventory().getLocation();
		if (location == null) return;
		if (!(location.getBlock().getState() instanceof BrewingStand)) return;

		BrewingStand stand = (BrewingStand) location.getBlock().getState();

		BrewerInventory brewerInventory = stand.getInventory();
		final Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null || clickedInventory.getType() != InventoryType.BREWING) return;

		Debug.Send(Type.Brewing, () -> "Player clicked inside Brewing stand, will start to check after matching recipe.");

		ItemStack itemStackCursor = event.getCursor();
		if (brewerInventory.getIngredient() != null)
			itemStackCursor = brewerInventory.getIngredient();

		final int slot = event.getSlot();
		if (slot == 4) {
			return;
		}

		if (itemStackCursor != null) {
			final ItemStack itemStackCheck = itemStackCursor.clone();
			final List<RecipeGroup> possibleRecipeGroups = this.recipeInjector.getLoader().findGroupsBySimilarResultMatch(itemStackCheck, RecipeType.BREWING);
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
				if (this.recipeInjector.isDisableDefaultModeldataCrafts() && Adapter.canUseModeldata() && this.recipeInjector.containsModelData(brewerInventory.getContents())) {
					return;
				}
				if (this.recipeInjector.checkForDisabledRecipe(disabledServerRecipes, itemStackCheck)) {
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
		}
	}


	public void onBrewDrag(final InventoryDragEvent event) {
		if (!enableBrewing || !enableCraft) return;

		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final Location location = event.getInventory().getLocation();
		if (location == null) return;
		if (!(location.getBlock().getState() instanceof BrewingStand)) return;

		BrewingStand stand = (BrewingStand) location.getBlock().getState();

		BrewerInventory inv = stand.getInventory();
		final Inventory clickedInventory = recipeInjector.getTopInventory(event);
		if (clickedInventory == null || clickedInventory.getType() != InventoryType.BREWING) return;

		ItemStack itemStackCursor = event.getCursor();
		if (itemStackCursor == null)
			itemStackCursor = event.getOldCursor();
		if (inv.getIngredient() != null)
			itemStackCursor = inv.getIngredient();

		if (itemStackCursor != null) {
			final ItemStack itemStackCheck = itemStackCursor.clone();
			final List<RecipeGroup> possibleRecipeGroups = this.recipeInjector.getLoader().findGroupsBySimilarResultMatch(itemStackCheck, RecipeType.BREWING);
			final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

			if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
				if (this.recipeInjector.isDisableDefaultModeldataCrafts() && Adapter.canUseModeldata() && this.recipeInjector.containsModelData(inv.getContents())) {
					return;
				}
				if (this.recipeInjector.checkForDisabledRecipe(disabledServerRecipes, itemStackCursor)) {
					return;
				}

				Debug.Send(Type.Brewing, () -> "No matching group or groups for the recipe.");
				return;
			}
			for (final RecipeGroup group : possibleRecipeGroups) {
				//Check if any grouped enhanced recipe is a match.
				for (final RecipeCoreData eRecipe : group.getRecipeCoreList()) {
					EnhancedRecipe enhancedRecipe = eRecipe.getEnhancedRecipe();
					if (!(enhancedRecipe  instanceof BrewingRecipe)) continue;
					final BrewingRecipe brewingRecipe = (BrewingRecipe) enhancedRecipe;

					ItemStack[] itemStacks = inv.getContents();
					ItemStack firstItem = itemStacks[0];
					ItemStack secondItem = itemStacks[1];
					ItemStack thirdItem = itemStacks[2];
					ItemStack[] outputItems = new ItemStack[]{firstItem, secondItem, thirdItem};
					if (brewingRecipe.getResult().isSimilar(itemStackCursor)) {
						ItemStack eventCursor = event.getCursor();
						if (eventCursor == null)
							eventCursor = event.getOldCursor();

						if (eventCursor.getType() != Material.AIR) {
							for (int slot : event.getRawSlots()) {
								if (slot < clickedInventory.getSize()) {

									Debug.Send(Type.Brewing, () -> "Dragged into top inventory at slot: " + slot);
									if (clickedInventory.getType() == InventoryType.BREWING) {
										event.setCancelled(true);
										break;
									}
								}
							}
						}
						Debug.Send(Type.Brewing, () -> "Found matching brewing recipe, will prevent inventory drag.");
						break;
					}
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
			for (final RecipeCoreData eRecipe : group.getRecipeCoreList()) {
				final EnhancedRecipe enhancedRecipe = eRecipe.getEnhancedRecipe();
				if (!(enhancedRecipe  instanceof BrewingRecipe)) continue;

				final BrewingRecipe brewingRecipe = (BrewingRecipe) enhancedRecipe;

				boolean notAllowedToBrew = recipeInjector.isCraftingAllowedInWorld(location, brewingRecipe);
				if (notAllowedToBrew) {
					Debug.Send(Type.Brewing, () -> "You are not allowed to brew potions in this world: " + location.getWorld() + " with this recipe key: " + brewingRecipe.getKey());
					continue;
				}

				if (brewingRecipe.getResult().isSimilar(itemStackCursor)) {

					if (checkBrewingClick(event))
						event.setCancelled(true);

					this.handleInventoryClick(event, brewingInv, slot);

					Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
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
					}, 1);
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
