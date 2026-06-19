package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.api.event.crafting.BeforeCraftOutputEvent;
import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.permissions.Permissible;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EnchantedCraftWrapper implements RecipeWrapper<EnhancedRecipe> {
	private final EnhancedRecipe enhancedRecipe;
	private final Map<Location, EnhancedRecipe> finishRecipe = new HashMap<>();

	public EnchantedCraftWrapper(@Nonnull final EnhancedRecipe enhancedRecipe) {
		this.enhancedRecipe = enhancedRecipe;
	}

	@Override
	public boolean isCustom() {
		return false;
	}

	@Override
	public void matches(@Nonnull final RecipeInjector recipeInjector, @Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareItemCraftContext> contextConsumer) {
		final PrepareItemCraftContext craftContext = new PrepareItemCraftContext();
		contextConsumer.accept(craftContext);
		final ItemStack[] matrix = craftContext.getRecipeMatrix();
		final CraftingInventory inventory = craftContext.getInventory();

		final List<HumanEntity> viewers = craftContext.getViewers();
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();
		final Location location = inventory.getLocation();
		this.finishRecipe.remove(location);

		Debug.Send(Type.Crafting, () -> "The server wants to inject " + serverRecipe.getResult() + " ceh will check or modify this.");

		if (!(enhancedRecipe instanceof WBRecipe)) return;
		final WBRecipe wbRecipe = (WBRecipe) enhancedRecipe;

		boolean notAllowedToCraft = this.isCraftingAllowedInWorld(location, enhancedRecipe);
		if (notAllowedToCraft) {
			Debug.Send(wbRecipe, () -> "You are not allowed to craft this recipe: " + this.enhancedRecipe.getKey());
			craftContext.acceptResult(null);
			return;
		}

		if (this.checkForDisabledRecipe(disabledServerRecipes, wbRecipe, serverRecipe.getResult())) {
			Debug.Send(wbRecipe, () -> "This recipe is disabled: " + this.enhancedRecipe.getKey());
			craftContext.acceptResult(null);
			return;
		}


		Debug.Send(wbRecipe, () -> "Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches.");
		final Player player = !viewers.isEmpty() ? (Player) viewers.get(0) : null;

		if (wbRecipe.matches(matrix)
				&& this.isViewersAllowedCraft(viewers, wbRecipe)
				&& !CraftEnhanceAPI.fireEvent(wbRecipe, player, inventory, null)) {
			Debug.Send(wbRecipe, () -> "Recipe matches, injecting " + wbRecipe.getResult().toString());
			if (recipeInjector.isMakeItemsadderCompatible() && recipeInjector.containsModelData(matrix)) {
				Debug.Send(wbRecipe, () -> "This recipe contains Modeldata and will be crafted if the recipe is not cancelled.");
				CraftEnhance.runTask(() -> {
					if (wbRecipe.matches(matrix)) {
						final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
						if (beforeCraftOutputEvent.isCancelled()) {
							Debug.Send(wbRecipe, () -> "This recipe is now cancelled and will not produce output item.");
							return;
						}
						Debug.Send(wbRecipe, () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
						Debug.Send(wbRecipe, () -> "The crafted recipe matrix: " + recipeInjector.convertItemStackArrayToString(matrix));

						craftContext.acceptResult(beforeCraftOutputEvent.getResultItem());
					}
				});
				return;
			} else {
				Debug.Send(wbRecipe, () -> "This recipe doesn't contains Modeldata and will be crafted if the recipe is not cancelled.");

				final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
				if (beforeCraftOutputEvent.isCancelled()) {
					Debug.Send(wbRecipe, () -> "This recipe is now cancelled and will not produce output item.");
					return;
				}
				Debug.Send(wbRecipe, () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
				Debug.Send(wbRecipe, () -> "The crafted recipe matrix: " + recipeInjector.convertItemStackArrayToString(matrix));
				craftContext.acceptResult(beforeCraftOutputEvent.getResultItem());
			}
			if (inventory.getType() != InventoryType.WORKBENCH && inventory.getType() != InventoryType.CRAFTING && self().getConfig().getBoolean("turn_of_crafter", true)) {
				Debug.Send(wbRecipe, () -> "The crafting of this custom recipe is stopped.");
				craftContext.acceptResult(null);
			} else {
				this.finishRecipe.put(location, wbRecipe);
				return;
			}
		}
		Debug.Send(wbRecipe, () -> "Recipe matrix doesn't match.");
		Debug.Send(wbRecipe, () -> recipeInjector.recipeIngredientsDebug(wbRecipe, matrix));

		if (wbRecipe.isCheckPartialMatch() && wbRecipe.matches(matrix, MatchType.MATCH_TYPE.getMatcher())) {
			Debug.Send(wbRecipe, () -> "Partial matched recipe fond and will prevent craft this recipe.");
			craftContext.acceptResult(null);
		} else if (wbRecipe.isCheckPartialMatch()) {
			Debug.Send(wbRecipe, () -> "Partial matched recipe not fund ingredients not match the type, check next recipe if it exists.");
		}
		Debug.Send(Type.Crafting, () -> "The recipe did not match the pattern, will continue with next recipe if it exists.");
		craftContext.acceptResult(null);
	}


	public void craftingClick(@Nonnull final InventoryClickEvent craftingClick) {

		if (craftingClick.getSlot() != 0) return;
		final Inventory clickedInventory = craftingClick.getClickedInventory();
		if (clickedInventory == null) return;

		this.finishRecipe.computeIfPresent(clickedInventory.getLocation(), (location, recipe) -> {
			if (recipe.getOnCraftCommand() == null || recipe.getOnCraftCommand().trim().isEmpty()) return null;
			CraftEnhance.runTaskLater(2, () ->
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), recipe.getOnCraftCommand().replace("%playername%", craftingClick.getWhoClicked().getName()))
			);
			return null;
		});
	}

	@Override
	public EnhancedRecipe getRecipe() {
		return this.enhancedRecipe;
	}

	private boolean isCraftingAllowedInWorld(final Location location, final EnhancedRecipe eRecipe) {
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

	private boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull WBRecipe wbRecipe, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result) && wbRecipe.isSimilar(disabledRecipe)) {
					return true;
				}
			}
		return false;
	}

	private boolean isViewersAllowedCraft(final List<HumanEntity> viewers, final WBRecipe wbRecipe) {
		if (viewers.isEmpty())
			return true;
		return viewers.stream().allMatch(x -> entityCanCraft(x, wbRecipe));
	}

	private boolean entityCanCraft(final Permissible entity, final EnhancedRecipe group) {
		return group.getPermission() == null || group.getPermission().isEmpty()
				|| (entity != null && entity.hasPermission(group.getPermission()));
	}

}
