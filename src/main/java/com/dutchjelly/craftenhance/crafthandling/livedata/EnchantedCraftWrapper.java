package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.api.event.crafting.BeforeCraftOutputEvent;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EnchantedCraftWrapper implements RecipeWrapper {
	private final WBRecipe enhancedRecipe;
	private final String key;

	public EnchantedCraftWrapper(@Nonnull final WBRecipe enhancedRecipe) {
		this.enhancedRecipe = enhancedRecipe;

		this.key = enhancedRecipe.getResult().getType().name() + "|" +
				Arrays.stream(enhancedRecipe.getContent())
						.filter(Objects::nonNull)
						.map(i -> i.getType().name())
						.sorted()
						.collect(Collectors.joining(",")) +
				"|" + (enhancedRecipe.isShapeless() ? "shapeless" : "shaped");
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return enhancedRecipe.getType();
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
		final PrepareItemCraftContext craftContext = new PrepareItemCraftContext();
		contextConsumer.accept(craftContext);
		final ItemStack[] matrix = craftContext.getRecipeMatrix();
		final CraftingInventory inventory = craftContext.getInventory();
		final WBRecipe wbRecipe = enhancedRecipe;

		if (inventory == null) {
			Debug.Send(Type.Crafting, () -> "You have not set the inventory, it will deny all crafting.");
			return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
		}

		final List<HumanEntity> viewers = craftContext.getViewers();
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();
		final Location location = inventory.getLocation();

		Debug.Send(Type.Crafting, () -> "The server wants to inject " + serverRecipe.getResult() + " ceh will check or modify this.");

		boolean notAllowedToCraft = RecipeAdapter.isCraftingAllowedInWorld(location, wbRecipe);
		if (notAllowedToCraft) {
			Debug.Send(wbRecipe, () -> "You are not allowed to craft this recipe: " + wbRecipe.getKey());
			craftContext.setResult(null);
			return new ResultContext(wbRecipe, null, ResultType.NO_PERMISSION);
		}

		if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, wbRecipe, serverRecipe.getResult())) {
			Debug.Send(wbRecipe, () -> "This recipe is disabled: " + wbRecipe.getKey());
			craftContext.setResult(null);
			return new ResultContext(wbRecipe, null, ResultType.DISABLED);
		}


		Debug.Send(wbRecipe, () -> "Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches.");
		final Player player = !viewers.isEmpty() ? (Player) viewers.get(0) : null;
		if (inventory.getType() != InventoryType.WORKBENCH && inventory.getType() != InventoryType.CRAFTING && self().getConfig().getBoolean("turn_of_crafter", true)) {
			Debug.Send(wbRecipe, () -> "The crafting of this custom recipe is stopped.");
			return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
		}

		if (wbRecipe.matches(matrix)
				&& RecipeAdapter.isViewersAllowedCraft(viewers, wbRecipe)
				&& !CraftEnhanceAPI.fireEvent(wbRecipe, player, inventory, null)) {
			Debug.Send(wbRecipe, () -> "Recipe matches, injecting " + wbRecipe.getResult().toString());

			if (self().isMakeItemsadderCompatible() && Adapter.containsModelData(matrix)) {
				Debug.Send(wbRecipe, () -> "This recipe contains Modeldata and will be crafted if the recipe is not cancelled.");
				if (wbRecipe.matches(matrix)) {
					final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
					if (beforeCraftOutputEvent.isCancelled()) {
						Debug.Send(wbRecipe, () -> "This recipe is now cancelled and will not produce output item.");
						return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
					}
					Debug.Send(wbRecipe, () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
					Debug.Send(wbRecipe, () -> "The crafted recipe matrix: " + RecipeDebug.convertItemStackArrayToString(matrix));
					return new ResultContext(wbRecipe,beforeCraftOutputEvent.getResultItem(), ResultType.ENHANCED);
				}
			} else {
				Debug.Send(wbRecipe, () -> "This recipe doesn't contains Modeldata and will be crafted if the recipe is not cancelled.");

				final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
				if (beforeCraftOutputEvent.isCancelled()) {
					Debug.Send(wbRecipe, () -> "This recipe is now cancelled and will not produce output item.");
					return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
				}
				Debug.Send(wbRecipe, () -> "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
				Debug.Send(wbRecipe, () -> "The crafted recipe matrix: " + RecipeDebug.convertItemStackArrayToString(matrix));
				return new ResultContext(wbRecipe, beforeCraftOutputEvent.getResultItem(), ResultType.ENHANCED);
			}
		}
		Debug.Send(wbRecipe, () -> "Recipe matrix doesn't match.");
		Debug.Send(wbRecipe, () -> RecipeDebug.recipeIngredientsDebug(wbRecipe, matrix));

		if (wbRecipe.isCheckPartialMatch() && wbRecipe.matches(matrix, MatchType.MATCH_TYPE.getMatcher())) {
			Debug.Send(wbRecipe, () -> "Partial matched recipe fond and will prevent craft this recipe.");
			return new ResultContext(wbRecipe, null, ResultType.PARTIAL_MATCH);
		} else if (wbRecipe.isCheckPartialMatch()) {
			Debug.Send(wbRecipe, () -> "Partial matched recipe not fund ingredients not match the type, check next recipe if it exists.");
		}
		Debug.Send(Type.Crafting, () -> "The recipe did not match the pattern, will continue with next recipe if it exists.");
		return new ResultContext(wbRecipe, null, ResultType.NO_MATCH);
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.enhancedRecipe))
			return Optional.of(type.cast(this.enhancedRecipe));
		return Optional.empty();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof RecipeWrapper)) return false;

		final EnchantedCraftWrapper that = (EnchantedCraftWrapper) o;
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
		builder.append("########## Enhanced recipe ################").append("\n");
		builder.append("Key: ").append(this.enhancedRecipe.getKey()).append("\n");
		builder.append("Result: ").append(this.enhancedRecipe.getResult()).append("\n");
		builder.append("Ingredients: ").append(Arrays.toString(this.enhancedRecipe.getContent())).append("\n");
		builder.append("########## Enhanced recipe ################\n");
		return builder.toString();
	}
}
