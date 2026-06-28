package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.api.event.crafting.BeforeCraftOutputEvent;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
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
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EnchantedCraftWrapper implements RecipeWrapper {
	private final WBRecipe enhancedRecipe;
	private final EnumMap<Material, Integer> ingredients = new EnumMap<>(Material.class);
	private int totalSlotCount;
	private final String key;

	public EnchantedCraftWrapper(@Nonnull final WBRecipe enhancedRecipe) {
		this.enhancedRecipe = enhancedRecipe;
		final ItemStack[] content = enhancedRecipe.getContent();

		for (ItemStack stack : content) {
			if (stack == null) continue;
			Material type = stack.getType();
			if (type == Material.AIR) continue;
			ingredients.merge(type, 1, Integer::sum);
			totalSlotCount++;
		}

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
	public String getRecipeKey() {
		return enhancedRecipe.getKey();
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
		return true;
	}

	@Override
	public EnumMap<Material, Integer> getIngredients() {
		return ingredients;
	}

	@Override
	public int getTotalSlotCount() {
		return totalSlotCount;
	}

	@Override
	public int getAmountOfIngredient(final Material material) {
		Integer amount = this.ingredients.get(material);
		if (amount != null)
			return amount;
		return 0;
	}


	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareItemCraftContext craftContext = new PrepareItemCraftContext();
		contextConsumer.accept(craftContext);
		final ItemStack[] matrix = craftContext.getRecipeMatrix();
		final Inventory inventory = craftContext.getInventory();
		final Location location = craftContext.getLocation();
		final WBRecipe wbRecipe = enhancedRecipe;

		if (inventory == null) {
			Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> "You have not set the inventory, it will deny all crafting.");
			return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
		}

		final List<HumanEntity> viewers = craftContext.getViewers();
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();
		final boolean notAllowedToCraft = RecipeAdapter.isCraftingAllowedInWorld(location, wbRecipe);

		Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> {
			String serverRecipeInfo = "";
			if (serverRecipe != null && wbRecipe.getResult().getType() != serverRecipe.getResult().getType()) {
				//serverRecipeInfo = RecipeDebug.formatOneStack(serverRecipe.getResult());
				serverRecipeInfo = ". Server recipe differ to enhanced ";
			}
			return "It will check if recipe allowed for this world, not disabled" + serverRecipeInfo + "and this is a enchanted recipe:" + RecipeDebug.formatOneStack(wbRecipe.getResult());
		});
		if (notAllowedToCraft) {
			Debug.send(Type.Crafting, "world_check | recipe=" + wbRecipe.getKey(), () -> "You are not allowed to craft this recipe: " + wbRecipe.getKey());
			craftContext.setResult(null);
			return new ResultContext(wbRecipe, null, ResultType.NO_PERMISSION);
		}

		if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, wbRecipe, serverRecipe.getResult())) {
			Debug.send(Type.Crafting, "disabled | recipe=" + wbRecipe.getKey(), () -> "This recipe is disabled: " + wbRecipe.getKey());
			craftContext.setResult(null);
			return new ResultContext(wbRecipe, null, ResultType.DISABLED);
		}


		Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> "The recipe allowed for this world and recipe are not blocked on the server, will check the ingredient matrix match.");
		final Player player = !viewers.isEmpty() ? (Player) viewers.get(0) : null;

		if (wbRecipe.matches(matrix)
				&& RecipeAdapter.isViewersAllowedCraft(viewers, wbRecipe)
				&& !CraftEnhanceAPI.fireEvent(wbRecipe, player, inventory, null)) {
			if (inventory.getType() != InventoryType.WORKBENCH && inventory.getType() != InventoryType.CRAFTING && self().getConfig().getBoolean("turn_of_crafter", true)) {
				Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> "The crafting of this custom recipe is stopped as crafter is turned of in the config.");
				return new ResultContext(wbRecipe, null, ResultType.NO_MATCH);
			}

			Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> "Recipe matches the ingredient matrix, will now check Itemsadder is turned on and the item contains model data.");

			if (self().isMakeItemsadderCompatible() && Adapter.containsModelData(matrix)) {
				Debug.send(Type.Crafting, "model_check | recipe=" + wbRecipe.getKey(), () -> "This recipe contains Modeldata and will be crafted if the recipe is not cancelled.");
				if (wbRecipe.matches(matrix)) {
					final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
					if (beforeCraftOutputEvent.isCancelled()) {
						Debug.send(Type.Crafting, "cancelled | recipe= " + wbRecipe.getKey(), () -> "This recipe is now cancelled and will not produce output item.");
						return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
					}
					Debug.send(Type.Crafting, "result | recipe=" + wbRecipe.getKey(), () -> "The recipe is now crafted and output item is:\n " + RecipeDebug.formatOneStack(beforeCraftOutputEvent.getResultItem()));
					Debug.send(Type.Crafting, "result | recipe=" + wbRecipe.getKey(), () -> "The inputted  matrix to remove:\n " + RecipeDebug.convertItemStackArrayToString(matrix));
					return new ResultContext(wbRecipe, beforeCraftOutputEvent.getResultItem(), ResultType.ENHANCED);
				}
			} else {
				Debug.send(Type.Crafting, "recipe=" + wbRecipe.getKey(), () -> "This recipe doesn't contains Modeldata and will be crafted if the recipe is not cancelled.");

				final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(enhancedRecipe, wbRecipe, wbRecipe.getResult().clone());
				if (beforeCraftOutputEvent.isCancelled()) {
					Debug.send(Type.Crafting, "cancelled | recipe=" + wbRecipe.getKey(), () -> "This recipe is now cancelled and will not produce output item.");
					return new ResultContext(wbRecipe, null, ResultType.CANCELLED);
				}
				Debug.send(Type.Crafting, "result | recipe=" + wbRecipe.getKey(), () -> "The recipe is now crafted and output item is\n " + RecipeDebug.formatOneStack(beforeCraftOutputEvent.getResultItem()));
				Debug.send(Type.Crafting, "result | recipe=" + wbRecipe.getKey(), () -> "The inputted  matrix to remove:\n " + RecipeDebug.convertItemStackArrayToString(matrix));
				return new ResultContext(wbRecipe, beforeCraftOutputEvent.getResultItem(), ResultType.ENHANCED);
			}
		}
		Debug.send(Type.Crafting, "matrix_non | recipe=" + wbRecipe.getKey(), () -> "The crafting recipe matrix doesn't match.");
		Debug.send(Type.Crafting, "matching_matrix | recipe=" + wbRecipe.getKey(), () -> RecipeDebug.recipeIngredientsDebug(wbRecipe, matrix));

		if (wbRecipe.isCheckPartialMatch()) {
			if (wbRecipe.matches(matrix, MatchType.MATCH_TYPE.getMatcher())) {
				Debug.send(Type.Crafting, "partial | recipe=" + wbRecipe.getKey(), () -> "Partial matched recipe fond and will prevent craft this recipe.");
				return new ResultContext(wbRecipe, null, ResultType.PARTIAL_MATCH);
			} else {
				Debug.send(Type.Crafting, "partial | recipe=" + wbRecipe.getKey(), () -> "Partial matched recipe not fund, ingredients not match the type of a vanilla recipe.");
			}
		}
		Debug.send(Type.Crafting, "no_match | recipe=" + wbRecipe.getKey(), () -> "The recipe matrix in the crafting table did not match the pattern.");
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
		if (!(o instanceof EnchantedCraftWrapper)) return false;

		final EnchantedCraftWrapper that = (EnchantedCraftWrapper) o;
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
		final ItemStack[] content = this.enhancedRecipe.getContent();
		builder.append("___________< Enhanced recipe >___________").append("\n")
				.append("Key: ").append(this.enhancedRecipe.getKey()).append("\n")
				.append("Result: ").append(RecipeDebug.formatOneStack(this.enhancedRecipe.getResult())).append("\n")
				.append("Ingredients:")
				.append(RecipeDebug.convertItemStackArrayToString(content))
				.append("\n")
				.append("___________< Enhanced recipe end >___________\n");
		return builder.toString();
	}
}
