package com.dutchjelly.craftenhance.crafthandling.livedata.brewing;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class BrewingWrapper implements RecipeWrapper {
	private final BrewingRecipe brewingRecipe;
	private final EnumMap<Material, Integer> map = new EnumMap<>(Material.class);
	private final String key;

	public BrewingWrapper(@Nonnull final BrewingRecipe brewingRecipe) {
		this.brewingRecipe = brewingRecipe;
		map.put(brewingRecipe.getResult().getType(), 1);
		StringBuilder builder = new StringBuilder(brewingRecipe.getResult().getType().name());
		builder.append("|");
		String content = Arrays.stream(brewingRecipe.getContent())
				.filter(Objects::nonNull)
				.map(i -> i.getType().name())
				.sorted()
				.collect(Collectors.joining(","));
		builder.append(content);
		builder.append("|");
		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public String getRecipeKey() {
		return brewingRecipe.getKey();
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return RecipeType.BREWING;
	}

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@Override
	public EnumMap<Material, Integer> getIngredients() {
		return map;
	}

	@Override
	public int getAmountOfIngredient(final Material material) {
		return 1;
	}

	@Override
	public int getTotalSlotCount() {
		return 1;
	}

	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		return null;
	}

	public boolean brewingCheck(@Nonnull final BrewingClickContext wrapBrewingClick) {
		final InventoryClickEvent event = wrapBrewingClick.getEvent();
		final Location location = wrapBrewingClick.getLocation();
		final ItemStack itemStackCursor = wrapBrewingClick.getItemStackCursor();
		final BrewerInventory brewingInv = wrapBrewingClick.getBrewingInv();
		final int slot = wrapBrewingClick.getSlot();
		final BrewingRecipe brewingRecipe = this.brewingRecipe;

		boolean notAllowedToBrew = RecipeAdapter.isCraftingAllowedInWorld(location, brewingRecipe);
		if (notAllowedToBrew) {
			Debug.send(Type.Brewing, "deny_world | recipe=" + brewingRecipe.getKey(), () -> "You are not allowed to brew potions in this world: " + location.getWorld() + " with this recipe key: " + brewingRecipe.getKey());
			return false;
		}

		if (brewingRecipe.getResult().isSimilar(itemStackCursor)) {
			Debug.send(Type.Brewing, "result | recipe=" + brewingRecipe.getKey(), () -> "Found a brewing recipe with this top item: " + RecipeDebug.formatOneStack(brewingRecipe.getResult()));

			if (checkBrewingClick(event))
				event.setCancelled(true);

			if (!this.handleInventoryClick(brewingRecipe, event, brewingInv, slot)) {
				event.setCancelled(true);
				Debug.send(Type.Brewing, "not_valid | recipe=" + brewingRecipe.getKey(), () -> "The item you trying to add to this recipe not match.");
				return false;
			}

			CraftEnhance.runTaskLater(1, () -> {
				ItemStack[] itemStacks = brewingInv.getContents();
				ItemStack[] outputItems = Arrays.copyOfRange(itemStacks, 0, 3);
				if (brewingRecipe.matches(outputItems)) {
					Debug.send(Type.Brewing, "result | recipe=" + brewingRecipe.getKey(), () -> "Found matching brewing recipe and will start to make the recipe: " + brewingRecipe.getKey());
					self().getBrewingTask().addTask(location, brewingRecipe);
				} else {
					Debug.send(Type.Brewing, "result | recipe=" + brewingRecipe.getKey(), () -> "You have not yet added ingredients on the bottom rows to activate brewing");
					Debug.send(Type.Brewing, "result | recipe=" + brewingRecipe.getKey(), () -> "The items to match: " + RecipeDebug.convertItemStackArrayToString(brewingRecipe.getContent()));
					Debug.send(Type.Brewing, "result | recipe=" + brewingRecipe.getKey(), () -> "The items inside inventory: " + RecipeDebug.convertItemStackArrayToString((outputItems)));
				}
			});
			return true;
		} else {
			Debug.send(Type.Brewing, "non_match | recipe=" + brewingRecipe.getKey(), () -> "No match for this brewing recipe.");
		}
		return false;
	}

	public boolean brewingDragCheck(@Nonnull final BrewingDragContext brewingDragContext) {
		final EnhancedRecipe enhancedRecipe = brewingRecipe;
		final InventoryDragEvent event = brewingDragContext.getEvent();
		final ItemStack itemStackCursor = brewingDragContext.getItemStackCursor();
		final Inventory clickedInventory = event.getInventory();

		final ItemStack[] itemStacks = clickedInventory.getContents();
		final ItemStack firstItem = itemStacks[0];
		final ItemStack secondItem = itemStacks[1];
		final ItemStack thirdItem = itemStacks[2];
		final ItemStack[] outputItems = new ItemStack[]{firstItem, secondItem, thirdItem};

		if (enhancedRecipe.getResult().isSimilar(itemStackCursor)) {
			ItemStack eventCursor = event.getCursor();
			if (eventCursor == null)
				eventCursor = event.getOldCursor();

			if (eventCursor.getType() != Material.AIR) {
				for (int slot : event.getRawSlots()) {
					if (slot < clickedInventory.getSize()) {
						Debug.Send(Type.Brewing, () -> "Dragged into top inventory at slot: " + slot);
						break;
					}
				}
			}
			event.setCancelled(true);
			Debug.Send(Type.Brewing, () -> "Found matching brewing recipe, will prevent inventory drag.");
			return true;
		}
		return false;
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.brewingRecipe))
			return Optional.of(type.cast(this.brewingRecipe));
		return Optional.empty();
	}

	public boolean checkBrewingClick(final InventoryClickEvent event) {
		return event.getSlot() != 4 && event.getCursor().getType() != Material.AIR;
	}

	public boolean handleInventoryClick(final BrewingRecipe brewingRecipe, final InventoryClickEvent event, final Inventory clickedInventory, final int slot) {
		ItemStack item = clickedInventory.getItem(slot);
		final ItemStack cursor = event.getCursor();

		if (item == null) {
			final ItemStack contentStack = getContentStack(brewingRecipe, slot);
			if (contentStack == null && event.getSlot() != 3) return false;

			if (cursor.isSimilar(contentStack) || event.getSlot() == 3) {
				handleNullItemInSlot(event, clickedInventory, slot);
			} else {
				return false;
			}
			return true;
		}
		if (cursor.getAmount() < cursor.getMaxStackSize() && cursor.isSimilar(item)) {
			handleCursorStacking(event, clickedInventory, slot, item);
		}
		return true;
	}

	private static @Nullable ItemStack getContentStack(final BrewingRecipe brewingRecipe, final int slot) {
		final ItemStack[] content = brewingRecipe.getContent();
		ItemStack contentStack = null;
		if (content.length > slot)
			contentStack = content[slot];
		return contentStack;
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

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof BrewingWrapper)) return false;
		final BrewingWrapper that = (BrewingWrapper) o;
		return that.key.equals(key);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 41 * hash + key.hashCode();
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		final ItemStack[] content = this.brewingRecipe.getContent();
		builder.append("___________< Enhanced brewing recipe >___________").append("\n")
				.append("Key: ").append(this.brewingRecipe.getKey()).append("\n")
				.append("Result: ").append(this.brewingRecipe.getResult()).append("\n")
				.append("Ingredients:")
				.append(RecipeDebug.convertItemStackArrayToString(content))
				.append("\n")
				.append("___________< Enhanced brewing recipe end >___________\n");
		return builder.toString();
	}
}
