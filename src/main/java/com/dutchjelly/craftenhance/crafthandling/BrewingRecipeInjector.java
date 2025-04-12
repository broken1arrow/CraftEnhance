package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
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

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class BrewingRecipeInjector {

	private final RecipeInjector recipeInjector;
	private final CraftEnhance plugin = self();
	private final boolean enableBrewing = plugin.getConfig().getBoolean("enable-brewing-recipes");
	private final boolean enableCraft = plugin.getConfig().getBoolean("enable-recipes");

	public BrewingRecipeInjector(final RecipeInjector recipeInjector) {
		this.recipeInjector = recipeInjector;
	}

	public void onBrewClick(InventoryClickEvent event) {
		if (!enableBrewing || !enableCraft) return;

		if (event.getInventory().getType() != InventoryType.BREWING) return;

		final Location location = event.getInventory().getLocation();
		if (location == null) return;
		if (!(location.getBlock().getState() instanceof BrewingStand)) return;

		BrewingStand stand = (BrewingStand) location.getBlock().getState();

		BrewerInventory inv = stand.getInventory();
		final Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null || clickedInventory.getType() != InventoryType.BREWING) return;

		Debug.Send(Type.Brewing, () -> "Player clicked inside Brewing stand, will start to check after matching recipe.");

		ItemStack itemStackCursor = event.getCursor();
		if (inv.getIngredient() != null)
			itemStackCursor = inv.getIngredient();

		final int slot = event.getSlot();
		if (slot == 4) {
			return;
		}

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

				Debug.Send(Type.Brewing, () -> "Couldn't find any matching brewing groups. Skipping the rest of the recipe checks.");
				return;
			}
			Debug.Send(Type.Brewing, () -> "Found a matching group of brewing recipes. Now checking for a matching recipe based on your provided items.");

			for (final RecipeGroup group : possibleRecipeGroups) {
				//Check if any grouped enhanced recipe is a match.
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					if (!(eRecipe instanceof BrewingRecipe)) continue;
					final BrewingRecipe wbRecipe = (BrewingRecipe) eRecipe;

					boolean notAllowedToBrew = recipeInjector.isCraftingAllowedInWorld(location, eRecipe);
					if (notAllowedToBrew) {
						Debug.Send(Type.Brewing, () -> "You are not allowed to brew potions in this world: " + location.getWorld() + " with this recipe: " + eRecipe.getKey());
						continue;
					}


					if (wbRecipe.getResult().isSimilar(itemStackCursor)) {

						if (checkBrewingClick(event))
							event.setCancelled(true);

						this.handleInventoryClick(event, clickedInventory, slot);

						Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
							ItemStack[] itemStacks = inv.getContents();
							ItemStack[] outputItems = Arrays.copyOfRange(itemStacks, 0, 3);
							if (wbRecipe.matches(outputItems)) {
								Debug.Send(Type.Brewing, () -> "Found matching brewing recipe and will start to make the recipe: " + eRecipe.getKey());
								this.plugin.getBrewingTask().addTask(location, wbRecipe);
							} else {
								Debug.Send(Type.Brewing, () -> "Failed to find a matching brewing recipe for recipe: " + eRecipe.getKey());
								Debug.Send(Type.Brewing, () -> "Result item: " + eRecipe.getResult());
								Debug.Send(Type.Brewing, () -> "The items to match: " + Arrays.toString(eRecipe.getContent()));
							}
						}, 1);
						break;
					}
				}
			}
		}
	}

	public void onBrewDrag(InventoryDragEvent event) {
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

				Debug.Send(Type.Brewing, () -> "No matching groups");
				return;
			}
			for (final RecipeGroup group : possibleRecipeGroups) {
				//Check if any grouped enhanced recipe is a match.
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					if (!(eRecipe instanceof BrewingRecipe)) continue;
					final BrewingRecipe wbRecipe = (BrewingRecipe) eRecipe;
					ItemStack[] itemStacks = inv.getContents();
					ItemStack firstItem = itemStacks[0];
					ItemStack secondItem = itemStacks[1];
					ItemStack thirdItem = itemStacks[2];
					ItemStack[] outputItems = new ItemStack[]{firstItem, secondItem, thirdItem};
					if (wbRecipe.getResult().isSimilar(itemStackCursor)) {
						ItemStack eventCursor = event.getCursor();
						if (eventCursor == null)
							eventCursor = event.getOldCursor();

						if (eventCursor.getType() != Material.AIR) {
							for (int slot : event.getRawSlots()) {
								if (slot < clickedInventory.getSize()) {
									// This is a top inventory slot
									// You can now check the type, cancel, or handle logic
									System.out.println("Dragged into top inventory at slot: " + slot);

									// Example: Cancel if it's a brewing stand
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


	public void handleInventoryClick(InventoryClickEvent event, Inventory clickedInventory, int slot) {
		ItemStack item = clickedInventory.getItem(slot);

		if (item == null) {
			handleNullItemInSlot(event, clickedInventory, slot);
		}

		if (event.getCursor().getAmount() < event.getCursor().getMaxStackSize() && event.getCursor().isSimilar(item)) {
			handleCursorStacking(event, clickedInventory, slot, item);
		}
	}

	private void handleNullItemInSlot(InventoryClickEvent event, Inventory clickedInventory, int slot) {
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

	private void handleCursorStacking(InventoryClickEvent event, Inventory clickedInventory, int slot, ItemStack item) {
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

	private ItemStack copyItemStack(ItemStack itemStack, int amount) {
		ItemStack copy = itemStack.clone();
		if (amount >= 0)
			copy.setAmount(amount);
		return copy;
	}

	public boolean checkBrewingClick(InventoryClickEvent event) {
		return event.getSlot() != 4 && event.getCursor().getType() != Material.AIR;
	}


}
