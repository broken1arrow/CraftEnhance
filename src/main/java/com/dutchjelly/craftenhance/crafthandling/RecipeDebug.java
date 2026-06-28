package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class RecipeDebug {

	public static String recipeIngredientsDebug(final WBRecipe wbRecipe, final ItemStack[] matrix) {
		StringBuilder stringBuilder = new StringBuilder();
		if (!wbRecipe.matches(matrix)) {
			for (int i = 0; i < wbRecipe.getContent().length; i++) {
				ItemStack itemStack = wbRecipe.getContent()[i];
				ItemMeta recipeMeta = null;
				if (itemStack != null) recipeMeta = itemStack.getItemMeta();

				List<ItemStack> matchingInvItems = getItemStack(matrix, itemStack);

				if (itemStack != null && matchingInvItems != null) {
					stringBuilder.append("\n<--------Similar ingredient match-------->\n");
					stringBuilder.append("Ingredient  type= ").append(itemStack.getType()).append("\n");

					if (recipeMeta != null) {
						stringBuilder.append("The recipe display name= '").append(recipeMeta.getDisplayName()).append("'");
						if (recipeMeta.getLore() != null)
							stringBuilder.append("The recipe lore= ").append(recipeMeta.getLore());
					}

					stringBuilder.append("\nMatched ingredients in the crafting grid: \n");
					setIngredients(matchingInvItems, stringBuilder, recipeMeta);
					stringBuilder.append("\n<--------Similar ingredient match end-------->\n\n");
				}
			}

		}
		return stringBuilder + "";
	}

	public static String convertItemStackArrayToString(final Collection<ItemStack> collection) {
		final StringBuilder stringBuilder = new StringBuilder();
		final Map<ItemStack, Integer> map = new LinkedHashMap<>();
		final ItemStack[] matrix = collection.toArray(new ItemStack[0]);
		final Boolean[] checkMetadata = new Boolean[matrix.length];
		Arrays.fill(checkMetadata, false);
		for (int i = 0; i < matrix.length; i++) {
			ItemStack invItemStack = matrix[i];
			checkMetadata[i] = isNoMetadata(invItemStack);
			map.merge(invItemStack, 1, Integer::sum);
		}
		final boolean noItemWithMeta = Arrays.stream(checkMetadata).anyMatch(x -> x);
		stringBuilder.append("\n____________ingredient matrix_____________");

		if (!noItemWithMeta && !map.isEmpty()) {
			stringBuilder.append("\nIngredient type= ");
		}
		map.forEach((itemStack, integer) -> {
			formatStack(itemStack, stringBuilder, noItemWithMeta);
			if (integer > 1) {
				if (!noItemWithMeta) {
					stringBuilder.append("x").append(integer).append(", ");
				} else {
					stringBuilder.append("amount: ").append(integer);
				}
			}
		});
		stringBuilder.append("\n____________ingredient matrix_____________\n");
		return stringBuilder + "";
	}

	public static String convertItemStackArrayToString(final ItemStack[] matrix) {
		final StringBuilder stringBuilder = new StringBuilder();
		final Map<ItemStack, Integer> map = new LinkedHashMap<>();
		final Boolean[] checkMetadata = new Boolean[matrix.length];
		Arrays.fill(checkMetadata, false);

		for (int i = 0; i < matrix.length; i++) {
			ItemStack invItemStack = matrix[i];
			checkMetadata[i] = isNoMetadata(invItemStack);
			map.merge(invItemStack, 1, Integer::sum);
		}
		final boolean noItemWithMeta = Arrays.stream(checkMetadata).anyMatch(x -> x);

		stringBuilder.append("\n____________ingredient matrix_____________");
		if (!noItemWithMeta && !map.isEmpty()) {
			stringBuilder.append("\nIngredient type= ");
		}
		map.forEach((itemStack, integer) -> {
			formatStack(itemStack, stringBuilder, noItemWithMeta);
			if (!noItemWithMeta) {
				stringBuilder.append("x").append(integer).append(", ");
			} else {
				stringBuilder.append("amount: ").append(integer);
			}
		});
		stringBuilder.append("\n____________ingredient matrix_____________\n");
		return stringBuilder + "";
	}

	public static String formatOneStack(final ItemStack stack) {
		if (stack == null) return "empty";
		StringBuilder stringBuilder = new StringBuilder();
		boolean checkMetadata = isNoMetadata(stack);
		if (!checkMetadata)
			stringBuilder.append("\nIngredient type= ");
		formatStack(stack, stringBuilder, checkMetadata);
		stringBuilder.append("________________________________");
		return stringBuilder + "";
	}

	public static void formatStack(final ItemStack stack, final StringBuilder stringBuilder) {
		formatStack(stack, stringBuilder, true);
	}

	public static void formatStack(final ItemStack stack, final StringBuilder stringBuilder, boolean hasMetadata) {
		if (!hasMetadata) {
			if (stack != null) {
				stringBuilder.append(stack.getType());
			} else {
				stringBuilder.append("AIR");
			}
			return;
		}

		if (stack != null) {
			final ItemMeta itemMeta = stack.getItemMeta();
			stringBuilder.append("\nIngredient type= ").append(stack.getType());
			if (itemMeta != null) {
				final String displayName = itemMeta.getDisplayName();
				final boolean haveDisplayName = displayName == null || displayName.isEmpty() || displayName.equals("null");
				if (haveDisplayName && itemMeta.getLore() == null) {
					stringBuilder.append("\nMetadata= {no name and lore set}");
				} else {
					final String name = haveDisplayName ? "none" : "'" + displayName + "'";
					stringBuilder.append("\nMetadata= ").append("{name=").append(name);
					if (itemMeta.getLore() != null)
						stringBuilder.append(", lore=").append(itemMeta.getLore()).append("}");
					else stringBuilder.append(", lore=none set}");
				}
			} else {
				stringBuilder.append("\nMetadata= {not set}");
			}
			stringBuilder.append("\n");
		} else {
			stringBuilder.append("\nIngredient type= AIR\n");
		}
	}


	public static int findMismatchIndex(String str1, String str2) {
		if (str1 == null || str2 == null) {
			if (str1 == null && str2 == null)
				return -1;
			if (str1 != null) {
				return 0;
			}
			return 0;
		}
		int minLength = Math.min(str1.length(), str2.length());

		for (int i = 0; i < minLength; i++) {
			if (str1.charAt(i) != str2.charAt(i)) {
				return i;
			}
		}
		// If no mismatch found, check if strings are of different lengths
		if (str1.length() != str2.length()) {
			return -2;
		}
		return -1; // Strings are identical
	}

	private static void setIngredients(final List<ItemStack> matchingInvItems, final StringBuilder stringBuilder, final ItemMeta recipeMeta) {
		for (int index = 0; index < matchingInvItems.size(); index++) {
			ItemStack invItem = matchingInvItems.get(index);

			final ItemMeta invItemMeta = invItem == null ? null : invItem.getItemMeta();
			stringBuilder.append("____________ingredient match").append("_____________\n");
			stringBuilder.append("slot index=").append(index);
			stringBuilder.append("\nIngredient crafting with type= ").append(invItem == null ? "AIR" : invItem.getType());
			final String displayName = invItemMeta != null ? invItemMeta.getDisplayName() : "";

			final int mismatchIndex = findMismatchIndex(recipeMeta != null ? recipeMeta.getDisplayName() : "", displayName);
			String match = "matching exactly";
			if (mismatchIndex == -2) match = "different length of the names";
			if (mismatchIndex > 0) match = "match to pos " + mismatchIndex;
			stringBuilder.append("\nDisplay name match= ").append(match);

			if (invItemMeta != null) {
				final String name = displayName == null || displayName.isEmpty() || displayName.equals("null") ? "non" : "'" + displayName + "'";
				stringBuilder.append("\nplayer added item display name= ").append(name);
				if (invItemMeta.getLore() != null)
					stringBuilder.append("\nThe added item lore= ").append(invItemMeta.getLore());
				else stringBuilder.append("\nThe added item lore= non");
			} else {
				stringBuilder.append("\nplayer added item display name= non");
				stringBuilder.append("\nThe added item lore= non");
			}
			stringBuilder.append("\n____________ingredient match end_____________\n");
		}
	}

	private static List<ItemStack> getItemStack(final ItemStack[] matrix, final ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) return null;
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack invItemStack : matrix) {
			if (invItemStack != null && invItemStack.getType() == itemStack.getType()) {
				items.add(invItemStack);
			}
		}
		return items.isEmpty() ? null : items;
	}

	private static boolean isNoMetadata(final ItemStack stack) {
		if (stack == null) return false;
		if (!stack.hasItemMeta()) return false;
		final ItemMeta itemMeta = stack.getItemMeta();
		final String displayName = itemMeta.getDisplayName();
		final boolean haveDisplayName = displayName == null || displayName.isEmpty() || displayName.equals("null");
		return !haveDisplayName || itemMeta.getLore() != null;
	}


}
