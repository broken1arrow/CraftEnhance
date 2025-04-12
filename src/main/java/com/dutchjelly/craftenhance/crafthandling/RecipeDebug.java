package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class RecipeDebug {


	public String recipeIngredientsDebug(final WBRecipe wbRecipe, final ItemStack[] matrix) {
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

					stringBuilder.append("\nMatched ingrediens in the crafting grid: \n\n");
					setIngredients(matchingInvItems, stringBuilder, recipeMeta);
					stringBuilder.append("\n<--------Ingredient-------->\n");
				}
			}

		}
		return stringBuilder + "";
	}

	private void setIngredients(final List<ItemStack> matchingInvItems, final StringBuilder stringBuilder, final ItemMeta recipeMeta) {
		for (int index = 0; index < matchingInvItems.size(); index++) {
			ItemStack invItem = matchingInvItems.get(index);

			final ItemMeta inveMeta = invItem == null ? null : invItem.getItemMeta();
			stringBuilder.append("____________ingredient match №=").append(index).append("_____________");
			stringBuilder.append("\nIngredient crafting with type= ").append(invItem == null ? "AIR" : invItem.getType());

			final int mismatchIndex = findMismatchIndex(recipeMeta != null ? recipeMeta.getDisplayName() : "", inveMeta != null ? inveMeta.getDisplayName() : "");
			String match = "matching exacly";
			if (mismatchIndex == -2) match = "different length of the names";
			if (mismatchIndex > 0) match = "match to pos " + mismatchIndex;
			stringBuilder.append("\nDisplay name match= ").append(match);

			if (inveMeta != null) {
				stringBuilder.append("\nplayer added item display name= ").append(inveMeta.getDisplayName().isEmpty() ? "non" : "'" + inveMeta.getDisplayName() + "'");
				if (inveMeta.getLore() != null)
					stringBuilder.append("\nThe added item lore= ").append(inveMeta.getLore());
				else stringBuilder.append("\nThe added item lore= non");
			} else {
				stringBuilder.append("\nplayer added item display name= non");
				stringBuilder.append("\nThe added item lore= non");
			}
			stringBuilder.append("\n____________ingredient match end_____________\n\n");
		}
	}

	private List<ItemStack> getItemStack(final ItemStack[] matrix, final ItemStack itemStack) {
		if (itemStack == null) return null;
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack invItemStack : matrix) {
			if (invItemStack != null && invItemStack.getType() == itemStack.getType()) {
				items.add(invItemStack);
			}
		}
		return items.isEmpty() ? null : items;
	}

	public String convertItemStackArrayToString(final ItemStack[] matrix) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n____________ingredient matrix_____________");
		for (ItemStack invItemStack : matrix) {
			if (invItemStack != null) {
				final ItemMeta itemMeta = invItemStack.getItemMeta();
				stringBuilder.append("\nIngredient  type= ").append(invItemStack.getType());
				if (itemMeta != null) {
					stringBuilder.append("\nItem display name= ").append(itemMeta.getDisplayName().isEmpty() ? "non" : "'" + itemMeta.getDisplayName() + "'");
					if (itemMeta.getLore() != null)
						stringBuilder.append("\nItem lore= ").append(itemMeta.getLore());
					else stringBuilder.append("\nItem lore= non");
				} else {
					stringBuilder.append("\nItem display name= non");
					stringBuilder.append("\nItem lore= non");
				}
				stringBuilder.append("\n");
			}
		}
		stringBuilder.append("\n____________ingredient matrix_____________\n");
		return stringBuilder + "";
	}

	public int findMismatchIndex(String str1, String str2) {
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


}
