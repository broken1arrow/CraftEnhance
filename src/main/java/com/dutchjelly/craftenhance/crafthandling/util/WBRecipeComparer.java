package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class WBRecipeComparer {

	private static ItemStack[] mirror(final ItemStack[] content, final int size) {
		if (content == null) return null;
		if (content.length == 0) return content;
		final ItemStack[] mirrored = new ItemStack[content.length];


		for (int i = 0; i < size; i++) {

			//Walk through right and left elements of this row and swab them.
			for (int j = 0; j < size / 2; j++) {
				final int i1 = i * size + (size - j - 1);
				mirrored[i * size + j] = content[i1];
				mirrored[i1] = content[i * size + j];
			}

			//Copy middle item to mirrored.
			if (size % 2 != 0)
				mirrored[i * size + (size / 2)] = content[i * size + (size / 2)];
		}
		return mirrored;
	}

	//This compares shapes and doesn't take mirrored recipes into account.
	//public for testing purposes. Not very professional I know, but it gets the job done.
	public static boolean shapeIterationMatches(final ItemStack[] itemsOne, final ItemStack[] itemsTwo, final IMatcher<ItemStack> matcher, final int rowSize) {
		//Find the first element of r and content.
		int indexTwo = -1, indexOne = -1;
		while (++indexTwo < itemsTwo.length && (itemsTwo[indexTwo] == null || itemsTwo[indexTwo].getType() == Material.AIR))
			;
		while (++indexOne < itemsOne.length && (itemsOne[indexOne] == null || itemsOne[indexOne].getType() == Material.AIR))
			;

		//Look if one or both recipes are empty. Return true if both are empty.
		if (indexTwo == itemsTwo.length || indexOne == itemsOne.length)
			return indexTwo == itemsTwo.length && indexOne == itemsOne.length;

		if (!matcher.match(itemsTwo[indexTwo], itemsOne[indexOne])) {
			return false;
		}

		//Offsets relative to the first item of the recipe.
		int iIndex, twoRowOffset, jIndex, oneRowOffset;
		for (; ; ) {
			iIndex = twoRowOffset = 0;
			jIndex = oneRowOffset = 0;
			while (++indexTwo < itemsTwo.length) {
				iIndex++;
				if (indexTwo % rowSize == 0) twoRowOffset++;

				if (itemsTwo[indexTwo] != null && itemsTwo[indexTwo].getType() != Material.AIR) break;

			}

			while (++indexOne < itemsOne.length) {
				jIndex++;
				if (indexOne % rowSize == 0) oneRowOffset++;

				if (itemsOne[indexOne] != null && itemsOne[indexOne].getType() != Material.AIR) break;
			}

			if (indexTwo == itemsTwo.length || indexOne == itemsOne.length) {
				return indexTwo == itemsTwo.length && indexOne == itemsOne.length;
			}
			if (!matcher.match(itemsTwo[indexTwo], itemsOne[indexOne]))
				return false;

			//The offsets have to be the same, otherwise the shape isn't equal.
			if (iIndex != jIndex || twoRowOffset != oneRowOffset) return false;
		}
	}

	public static boolean shapeMatches(final ItemStack[] content, final ItemStack[] stacks, final IMatcher<ItemStack> matcher) {
		final int rowSize = content == null ? 0 : (int) Math.sqrt(content.length);

		return shapeIterationMatches(content, stacks, matcher, rowSize) || shapeIterationMatches(mirror(content, rowSize), stacks, matcher, rowSize);
	}

	private static ItemStack[] ensureNoGaps(final ItemStack[] items) {
		return Arrays.asList(items).stream().filter(x -> x != null && x.getType() != Material.AIR).toArray(ItemStack[]::new);
	}

	public static boolean ingredientsMatch(ItemStack[] a, ItemStack[] b, final IMatcher<ItemStack> matcher) {
		//array with all values to false.
		a = ensureNoGaps(a);
		b = ensureNoGaps(b);

		if (a.length == 0 || b.length == 0) return false;
		if (a.length != b.length) return false;

		//use no primitive type to allow Boolean stream of objects instead of arrays.
		final Boolean[] used = new Boolean[a.length];
		Arrays.fill(used, false);

		for (final ItemStack inRecipe : a) {
			if (inRecipe == null) continue;
			//Look if inRecipe matches with an ingredient.
			for (int i = 0; i < used.length; i++) {
				if (used[i]) continue;
				if (b[i] == null) {
					Bukkit.getLogger().log(Level.SEVERE, "Error, found null ingredient.");
					return false;
				}
				if (matcher.match(b[i], inRecipe)) {
					used[i] = true;
					break;
				}
			}
		}
		return !Arrays.stream(used).anyMatch(x -> x == false);
	}

	public static boolean ingredientsMatchBrewing(ItemStack[] a, ItemStack[] b, final IMatcher<ItemStack> matcher) {

		if (a.length == 0 || b.length == 0) return false;
		if (a.length != b.length) return false;

		//use no primitive type to allow Boolean stream of objects instead of arrays.
		final Boolean[] used = new Boolean[a.length];
		Arrays.fill(used, false);

		for (final ItemStack inRecipe : a) {
			if (inRecipe == null) continue;
			//Look if inRecipe matches with an ingredient.
			for (int i = 0; i < used.length; i++) {
				if (used[i]) continue;
				if (matcher.match(b[i], inRecipe)) {
					used[i] = true;
					break;
				}
			}
		}
		return Arrays.stream(used).anyMatch(x -> x);
	}


	public static boolean shapedMatch(@NonNull final ShapedRecipe shapedRecipe, @NonNull final ItemStack[] matrix) {
		final String[] shape = shapedRecipe.getShape();
		final int gridSize = matrix.length == 4 ? 2 : 3;
		final int recipeHeight = shape.length;
		final int recipeWidth = shape[0].length();
		boolean isModern = self().getVersionChecker().newerThan(ServerVersion.v1_13);

		if (recipeWidth > gridSize || recipeHeight > gridSize) {
			return false;
		}
		if (isModern) {
			return checkChoiceMap(shapedRecipe, matrix, shape);
		} else {
			return checkIngredientMap(shapedRecipe, matrix, shape);
		}
	}


	public static boolean shapelessMatch(@NonNull final ShapelessRecipe shapeless, @NonNull final ItemStack[] matrix) {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			final List<RecipeChoice> choices = getRecipeChoices(matrix, shapeless);
			if (choices == null) return false;
			return choices.isEmpty();
		}
		final List<ItemStack> choices = getIngredientList(matrix, shapeless);
		if (choices == null) return false;
		return choices.isEmpty();
	}

	private static boolean checkIngredientMap(@Nonnull final ShapedRecipe shapedRecipe, @Nonnull final ItemStack[] matrix, final String[] shape) {
		final int gridSize = matrix.length == 4 ? 2 : 3;
		final int recipeHeight = shape.length;
		final int recipeWidth = shape[0].length();
		Map<Character, ItemStack> ingredientMap = shapedRecipe.getIngredientMap();
		for (int startY = 0; startY <= gridSize - recipeHeight; startY++) {
			for (int startX = 0; startX <= gridSize - recipeWidth; startX++) {
				if (checkMatchLegacy(matrix, ingredientMap, shape,
						startX, startY, gridSize, false) ||
						checkMatchLegacy(matrix, ingredientMap, shape,
								startX, startY, gridSize, true)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean checkChoiceMap(@Nonnull final ShapedRecipe shapedRecipe, @Nonnull final ItemStack[] matrix, final String[] shape) {
		final int gridSize = matrix.length == 4 ? 2 : 3;
		final int recipeHeight = shape.length;
		final int recipeWidth = shape[0].length();
		final Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
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

	private static boolean checkMatch(final ItemStack[] matrix, final Map<Character, RecipeChoice> choiceMap, final String[] shape, final int startX, final int startY, final int gridSize, final boolean mirrored) {
		final int rowLength = shape[0].length();

		for (int y = 0; y < gridSize; y++) {
			for (int x = 0; x < gridSize; x++) {
				final int matrixIndex = y * gridSize + x;
				final ItemStack item = matrix[matrixIndex];
				final int recipeY = y - startY;
				final int recipeX = x - startX;

				if (recipeY >= 0 && recipeY < shape.length && recipeX >= 0 && recipeX < rowLength) {

					int targetX = mirrored ? (rowLength - 1 - recipeX) : recipeX;
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

	private static boolean checkMatchLegacy(final ItemStack[] matrix, final Map<Character, ItemStack> ingredientMap, final String[] shape,
	                                        final int startX, final int startY, final int gridSize, final boolean mirrored) {
		final int rowLength = shape[0].length();

		for (int y = 0; y < gridSize; y++) {
			for (int x = 0; x < gridSize; x++) {

				int matrixIndex = y * gridSize + x;
				ItemStack item = matrix[matrixIndex];
				int recipeY = y - startY;
				int recipeX = x - startX;

				if (recipeY >= 0 && recipeY < shape.length &&
						recipeX >= 0 && recipeX < rowLength) {

					int targetX = mirrored ?
							(rowLength - 1 - recipeX) : recipeX;
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
							//ItemMatchers.matchTypeNoMeta();
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

	private static @Nullable List<ItemStack> getIngredientList(final ItemStack[] matrix, final ShapelessRecipe shapeless) {
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
