package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaCraftWrapper implements RecipeWrapper {
	private final Recipe recipe;
	private final String key;

	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
		StringBuilder builder = new StringBuilder(recipe.getResult().getType().name());
		builder.append("|");

		String joined = Adapter.getIngredientsList(recipe).stream()
				.filter(Objects::nonNull)
				.map(stack -> stack.getType().name())
				.sorted()
				.collect(Collectors.joining(","));
		if (recipe instanceof ShapelessRecipe) {
			builder.append("S|").append(joined);
		}
		if (recipe instanceof ShapedRecipe) {
			builder.append("H|").append(joined);
		}
		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public String getRecipeKey() {
		return "vanilla_recipe:" + key;
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return RecipeType.WORKBENCH;
	}

	@Override
	public int priority() {
		return 100;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareItemCraftContext prepareItemCraftContext = new PrepareItemCraftContext();
		contextConsumer.accept(prepareItemCraftContext);
		final ItemStack[] matrix = prepareItemCraftContext.getRecipeMatrix();
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

		Debug.Send(Type.Crafting, () -> "Vanilla recipe check, the result to match: " + recipe.getResult());
		if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, recipe.getResult())) {
			Debug.Send(Type.Crafting, () -> "This recipe is disabled with result: " + recipe.getResult());
			return new ResultContext(null, ResultType.DISABLED);
		}

		if (recipe instanceof ShapedRecipe) {
			final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(content));
			if (shapedMatch(matrix)) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapedRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		} else if (recipe instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(ingredients));
			if (shapelessMatch(matrix)) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapelessRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		}
		Debug.Send(Type.Crafting, () -> "Did not match this recipe.");
		return new ResultContext(null, ResultType.NO_MATCH);
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.recipe))
			return Optional.of(type.cast(this.recipe));
		return Optional.empty();
	}

	private boolean shapedMatch(final ItemStack[] matrix) {
		final ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
		final String[] shape = shapedRecipe.getShape();
		final Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();

		final int gridSize = matrix.length == 4 ? 2 : 3;
		final int recipeHeight = shape.length;
		final int recipeWidth = shape[0].length();

		if (recipeWidth > gridSize || recipeHeight > gridSize) {
			return false;
		}

		for (int startY = 0; startY <= gridSize - recipeHeight; startY++) {
			for (int startX = 0; startX <= gridSize - recipeWidth; startX++) {

				if (checkMatch(matrix, choiceMap, shape, startX, startY, gridSize, false) ||
						checkMatch(matrix, choiceMap, shape, startX, startY, gridSize, true)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean shapelessMatch(final ItemStack[] matrix) {
		final ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			final List<RecipeChoice> choices = getRecipeChoices(matrix, shapeless);
			if (choices == null) return false;
			return choices.isEmpty();
		}
		final List<ItemStack> choices = getRecipeChoicesList(matrix, shapeless);
		if (choices == null) return false;
		return choices.isEmpty();
	}

	private boolean checkMatch(ItemStack[] matrix, Map<Character, RecipeChoice> choiceMap, String[] shape, int startX, int startY, int gridSize, boolean mirrored) {
		for (int y = 0; y < gridSize; y++) {
			for (int x = 0; x < gridSize; x++) {
				final int matrixIndex = y * gridSize + x;
				final ItemStack item = matrix[matrixIndex];
				final int recipeY = y - startY;
				final int recipeX = x - startX;

				if (recipeY >= 0 && recipeY < shape.length && recipeX >= 0 && recipeX < shape[0].length()) {

					int targetX = mirrored ? (shape[0].length() - 1 - recipeX) : recipeX;
					char key = shape[recipeY].charAt(targetX);

					RecipeChoice choice = choiceMap.get(key);

					if (choice == null) {
						if (item != null && item.getType() != Material.AIR) {
							return false;
						}
					} else {

						if (item == null || !choice.test(item)) {
							return false;
						}
					}
				} else {
					if (item != null && item.getType() != Material.AIR) {
						return false;
					}
				}
			}
		}
		return true;
	}


	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof VanillaCraftWrapper)) return false;
		final VanillaCraftWrapper that = (VanillaCraftWrapper) o;
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
		builder.append("___________<  Vanilla Recipe >___________").append("\n");
		builder.append("Result: ").append(this.recipe.getResult()).append("\n");

		if (recipe instanceof ShapelessRecipe) {
			final ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
				builder.append("Key: ").append(shapelessRecipe.getKey()).append("\n");
			builder.append("Ingredients:\n")
					.append(Arrays.stream(Adapter.getIngredients(shapelessRecipe)).map(stack -> {
						if (stack != null) return stack.getType().name();
						return "empty";
					}).collect(Collectors.joining(",")))
					.append("\n");
		}
		if (recipe instanceof ShapedRecipe) {
			final ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
				builder.append("Key: ").append(shapedRecipe.getKey()).append("\n");
			builder.append("Ingredients:\n")
					.append(Arrays.stream(Adapter.getIngredients(shapedRecipe)).map(stack -> {
						if (stack != null) return stack.getType().name();
						return "empty";
					}).collect(Collectors.joining(",")))
					.append("\n");
		}
		builder.append("___________<  Vanilla Recipe end >___________\n");
		return builder.toString();
	}

	private boolean checkMatchLegacy(
			ItemStack[] matrix, ShapedRecipe shaped, String[] shape,
			int startX, int startY, int gridSize, boolean mirrored) {

		Map<Character, ItemStack> ingredientMap = shaped.getIngredientMap();
		for (int y = 0; y < gridSize; y++) {
			for (int x = 0; x < gridSize; x++) {

				int matrixIndex = y * gridSize + x;
				ItemStack item = matrix[matrixIndex];
				int recipeY = y - startY;
				int recipeX = x - startX;

				if (recipeY >= 0 && recipeY < shape.length &&
						recipeX >= 0 && recipeX < shape[0].length()) {

					int targetX = mirrored ?
							(shape[0].length() - 1 - recipeX) : recipeX;
					char key = shape[recipeY].charAt(targetX);

					ItemStack ingredient = ingredientMap.get(key);

					if (ingredient == null) {
						if (item != null && item.getType() != Material.AIR) {
							return false;
						}
					} else {
						// Gamla versioner kollar bara rak material-typ
						// (Om du vill kolla metadata/durability lägger du till det här)
						if (item == null || item.getType() != ingredient.getType()) {
							return false;
						}
					}
				} else {
					if (item != null && item.getType() != Material.AIR) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static @Nullable List<RecipeChoice> getRecipeChoices(final ItemStack[] matrix, final ShapelessRecipe shapeless) {
		final List<RecipeChoice> choices = new ArrayList<>(shapeless.getChoiceList());
		for (ItemStack item : matrix) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			boolean matchFound = false;
			for (int i = 0; i < choices.size(); i++) {
				RecipeChoice choice = choices.get(i);
				if (choice != null && choice.test(item)) {
					choices.remove(i);
					matchFound = true;
					break;
				}
			}
			// Om spelaren lagt till ett extra föremål som inte krävs
			if (!matchFound) {
				return null;
			}
		}
		return choices;
	}

	private static @Nullable List<ItemStack> getRecipeChoicesList(final ItemStack[] matrix, final ShapelessRecipe shapeless) {
		final List<ItemStack> itemStacks = new ArrayList<>(shapeless.getIngredientList());
		for (ItemStack item : matrix) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			boolean matchFound = false;
			for (int i = 0; i < itemStacks.size(); i++) {
				ItemStack stack = itemStacks.get(i);
				if (stack != null && stack.getType() == item.getType()) {
					itemStacks.remove(i);
					matchFound = true;
					break;
				}
			}
			if (!matchFound) {
				return null;
			}
		}
		return itemStacks;
	}

}
