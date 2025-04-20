package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.api.event.crafting.BeforeCraftOutputEvent;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class WorkBenchRecipeInjector {
	private final RecipeInjector recipeInjector;
	private final CraftEnhance plugin = self();

	private final Map<Location, EnhancedRecipe> finishRecipe = new HashMap<>();

	public WorkBenchRecipeInjector(final RecipeInjector recipeInjector) {
		this.recipeInjector = recipeInjector;
	}

	public void craftItem(final Recipe serverRecipe, final ItemStack[] matrix, final Inventory inventory, final List<HumanEntity> viewers, Consumer<ItemStack> result) {
		this.finishRecipe.remove(inventory.getLocation());

		Debug.Send(Type.Crafting, () -> "The server wants to inject " + serverRecipe.getResult() + " ceh will check or modify this.");
		RecipeInjector recipeManger = this.recipeInjector;

		final List<RecipeGroup> possibleRecipeGroups = recipeManger.getLoader().findGroupsByResult(serverRecipe.getResult(), serverRecipe, RecipeType.WORKBENCH);
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

		if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
			if (recipeManger.isDisableDefaultModeldataCrafts() && Adapter.canUseModeldata() && recipeManger.containsModelData(matrix)) {
				result.accept(null);
			}
			if (recipeManger.checkForDisabledRecipe(disabledServerRecipes, serverRecipe.getResult())) {
				result.accept(null);
			}
			Debug.Send(Type.Crafting, () -> "No matching groups");
			return;
		}
		for (final RecipeGroup group : possibleRecipeGroups) {
			boolean notAllowedToCraft = false;

			//Check if any grouped enhanced recipe is a match.
			for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
				if (!(eRecipe instanceof WBRecipe)) continue;
				final WBRecipe wbRecipe = (WBRecipe) eRecipe;

				notAllowedToCraft = recipeManger.isCraftingAllowedInWorld(inventory.getLocation(), eRecipe);
				if (notAllowedToCraft) {
					Debug.Send(wbRecipe,  () -> "You are not allowed to craft.");
					continue;
				}

				if (recipeManger.checkForDisabledRecipe(disabledServerRecipes, wbRecipe, serverRecipe.getResult())) {
					result.accept(null);
					Debug.Send(wbRecipe, () -> "This recipe is disabled. ");
					continue;
				}


				Debug.Send(wbRecipe,  () -> "Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches.");
				final Player player = !viewers.isEmpty() ? (Player) viewers.get(0) : null;

				if (wbRecipe.matches(matrix)
						&& recipeManger.isViewersAllowedCraft(viewers, wbRecipe)
						&& !CraftEnhanceAPI.fireEvent(wbRecipe, player, inventory, group)) {
					Debug.Send(wbRecipe,  () -> "Recipe matches, injecting " + wbRecipe.getResult().toString());
					if (recipeManger.isMakeItemsadderCompatible() && recipeManger.containsModelData(matrix)) {
						Debug.Send(wbRecipe,  () -> "This recipe contains Modeldata and will be crafted if the recipe is not cancelled.");
						Bukkit.getScheduler().runTask(self(), () -> {
							if (wbRecipe.matches(matrix)) {
								final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(eRecipe, wbRecipe, wbRecipe.getResult().clone());
								if (beforeCraftOutputEvent.isCancelled()) {
									Debug.Send(wbRecipe,  () -> "This recipe is now cancelled and will not produce output item.");
									return;
								}
								Debug.Send(wbRecipe,  () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
								Debug.Send(wbRecipe,  () -> "The crafted recipe matrix: " + recipeManger.convertItemStackArrayToString(matrix));
								result.accept(beforeCraftOutputEvent.getResultItem());
							}
						});
					} else {
						Debug.Send(wbRecipe,  () -> "This recipe doesn't contains Modeldata and will be crafted if the recipe is not cancelled.");

						final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(eRecipe, wbRecipe, wbRecipe.getResult().clone());
						if (beforeCraftOutputEvent.isCancelled()) {
							Debug.Send(wbRecipe,  () -> "This recipe is now cancelled and will not produce output item.");
							return;
						}
						Debug.Send(wbRecipe,  () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
						Debug.Send(wbRecipe,  () -> "The crafted recipe matrix: " + recipeManger.convertItemStackArrayToString(matrix));
						result.accept(beforeCraftOutputEvent.getResultItem());
					}
					if (inventory.getType() != InventoryType.WORKBENCH && inventory.getType() != InventoryType.CRAFTING && plugin.getConfig().getBoolean("turn_of_crafter", true)) {
						Debug.Send(wbRecipe,  () -> "The crafting of this custom recipe is stopped.");
						result.accept(null);
					} else {
						this.finishRecipe.put(inventory.getLocation(), wbRecipe);
					}
					return;
				}
				Debug.Send(wbRecipe,  () -> "Recipe matrix doesn't match.");
				Debug.Send(wbRecipe,  () -> recipeManger.recipeIngredientsDebug(wbRecipe, matrix));

				if (wbRecipe.isCheckPartialMatch() && wbRecipe.matches(matrix, MatchType.MATCH_TYPE.getMatcher())) {
					Debug.Send(wbRecipe,  () -> "Partial matched recipe fond and will prevent craft this recipe.");
					result.accept(null);
					return;
				}
			}
			if (notAllowedToCraft)
				continue;

			Debug.Send(Type.Crafting, () -> "Check for similar server recipes if no enhanced ones match.");
			//Check for similar server recipes if no enhanced ones match.
			for (final Recipe sRecipe : group.getServerRecipes()) {
				if (sRecipe instanceof ShapedRecipe) {
					final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) sRecipe);
					if (WBRecipeComparer.shapeMatches(content, matrix, recipeManger.getTypeMatcher())) {
						Debug.Send(Type.Crafting, () -> "Match a ShapedRecipe.");
						Debug.Send(Type.Crafting, () -> "The crafted recipe matrix: " + recipeManger.convertItemStackArrayToString(matrix));
						result.accept(sRecipe.getResult());
						return;
					}
				} else if (sRecipe instanceof ShapelessRecipe) {
					final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) sRecipe);
					if (WBRecipeComparer.ingredientsMatch(ingredients, matrix, recipeManger.getTypeMatcher())) {
						Debug.Send(Type.Crafting, () -> "Match a ShapelessRecipe.");
						Debug.Send(Type.Crafting, () -> "The crafted recipe matrix: " + recipeManger.convertItemStackArrayToString(matrix));
						result.accept(sRecipe.getResult());
						return;
					}
				}
			}
		}
		Debug.Send(Type.Crafting, () -> "No recipe match found and the result is set to air.");
		result.accept(null); //We found similar custom recipes, but none matched exactly. So set result to null.
	}

	public void craftingClick(final InventoryClickEvent craftingClick) {

		if (craftingClick.getSlot() != 0) return;
		final Inventory clickedInventory = craftingClick.getClickedInventory();
		if (clickedInventory == null) return;

		this.finishRecipe.computeIfPresent(clickedInventory.getLocation(), (location, recipe) -> {
			if (recipe.getOnCraftCommand() == null || recipe.getOnCraftCommand().trim().isEmpty()) return null;
			Bukkit.getScheduler().runTaskLater(plugin, () ->
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), recipe.getOnCraftCommand().replace("%playername%", craftingClick.getWhoClicked().getName())),
					2L);
			return null;
		});

	}


}
