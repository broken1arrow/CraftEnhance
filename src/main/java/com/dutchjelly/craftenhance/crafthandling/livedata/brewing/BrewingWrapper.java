package com.dutchjelly.craftenhance.crafthandling.livedata.brewing;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.recipes.FurnaceBurnWrapper;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.util.BooleanConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class BrewingWrapper implements RecipeWrapper {
	private final BrewingRecipe brewingRecipe;
	private final String key;

	public BrewingWrapper(@Nonnull final BrewingRecipe brewingRecipe) {
		this.brewingRecipe = brewingRecipe;

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
		return false;
	}

	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		return null;
	}

	public void brewingCheck(@Nonnull final BrewingClickContext wrapBrewingClick,@Nonnull final BooleanConsumer match) {
		final InventoryClickEvent event = wrapBrewingClick.getEvent();
		final Location location = wrapBrewingClick.getLocation();
		final ItemStack itemStackCursor = wrapBrewingClick.getItemStackCursor();
		final BrewerInventory brewingInv = wrapBrewingClick.getBrewingInv();
		final int slot = wrapBrewingClick.getSlot();
		final BrewingRecipe brewingRecipe = this.brewingRecipe;

		boolean notAllowedToBrew = RecipeAdapter.isCraftingAllowedInWorld(location, brewingRecipe);
		if (notAllowedToBrew) {
			Debug.Send(Type.Brewing, () -> "You are not allowed to brew potions in this world: " + location.getWorld() + " with this recipe key: " + brewingRecipe.getKey());
			return;
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
					self().getBrewingTask().addTask(location, brewingRecipe);
					match.accept(true);
				} else {
					Debug.Send(Type.Brewing, () -> "Failed to find a matching brewing ingredients recipe for recipe: " + brewingRecipe.getKey());
					Debug.Send(Type.Brewing, () -> "Result item: " + brewingRecipe.getResult());
					Debug.Send(Type.Brewing, () -> "The items to match: " + Arrays.toString(brewingRecipe.getContent()));
					Debug.Send(Type.Brewing, () -> "The items inside inventory: " + Arrays.toString(outputItems));
					match.accept(false);
				}
			});
		} else {
			Debug.Send(Type.Brewing, () -> "No match for this recipe: " + brewingRecipe.getKey());
		}
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

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof RecipeWrapper)) return false;
		final FurnaceBurnWrapper that = (FurnaceBurnWrapper) o;
		return that.key.equals(key);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 41 * hash + key.hashCode();
		return hash;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("########## Enhanced recipe ################").append("\n");
		builder.append("Key: ").append(this.brewingRecipe.getKey()).append("\n");
		builder.append("Result: ").append(this.brewingRecipe.getResult()).append("\n");
		builder.append("Ingredients: ").append(Arrays.toString(this.brewingRecipe.getContent())).append("\n");
		builder.append("########## Enhanced recipe ################\n");
		return builder.toString();
	}
}
