package com.dutchjelly.craftenhance;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.permissions.Permissible;

import java.util.List;
import java.util.Set;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeAdapter {

	public static boolean isViewersAllowedCraft(final List<HumanEntity> viewers, final WBRecipe wbRecipe) {
		if (viewers.isEmpty())
			return true;
		return viewers.stream().allMatch(x -> entityCanCraft(x, wbRecipe));
	}

	public static boolean entityCanCraft(final Permissible entity, final EnhancedRecipe group) {
		return group.getPermission() == null || group.getPermission().isEmpty()
				|| (entity != null && entity.hasPermission(group.getPermission()));
	}


	public static boolean isCraftingAllowedInWorld(final Location location, final EnhancedRecipe eRecipe) {
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

	public static boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result)) {
					return true;
				}
			}
		return false;
	}

	public static boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull WBRecipe wbRecipe, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result) && wbRecipe.isSimilar(disabledRecipe)) {
					return true;
				}
			}
		return false;
	}


	public static IMatcher<ItemStack> getTypeMatcher() {
		return Adapter.canUseModeldata() && self().isDisableDefaultModeldataCrafts() ?
				ItemMatchers.constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData)
				: ItemMatchers::matchType;
	}

	public static IMatcher<ItemStack> getTypeMatcherNoMetadata() {
		return Adapter.canUseModeldata() && self().isDisableDefaultModeldataCrafts() ?
				ItemMatchers.constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData)
				: ItemMatchers::matchTypeNoMeta;
	}

	public static ItemStack[] compressGrid(ItemStack[] originalGrid) {
		// Räkna föremål för att avgöra strategi
		int amount = 0;
		for (ItemStack is : originalGrid) {
			if (is != null && is.getType() != Material.AIR) amount++;
		}

		if (amount <= 2) {
			final ItemStack[] itemStacks = compressShapeTo2x2(originalGrid);
			return itemStacks != null ? itemStacks : new ItemStack[0];
		} else if (amount <= 4) {
			//Handle it as shapeless recipe when is more than 2 items to compress into a 2*2 grid.
			final boolean treatAsShapelessIn2x2 = true;
		}
		return new ItemStack[0];
	}

	public static ItemStack[] compressShapeTo2x2(ItemStack[] grid3x3) {
		int minRow = 3;
		int maxRow = -1;
		int minCol = 3;
		int maxCol = -1;
		int amount = 0;
		for (int i = 0; i < 9; i++) {
			if (grid3x3[i] != null && grid3x3[i].getType() != Material.AIR) {
				amount++;
				int row = i / 3;
				int col = i % 3;
				if (row < minRow) minRow = row;
				if (row > maxRow) maxRow = row;
				if (col < minCol) minCol = col;
				if (col > maxCol) maxCol = col;
			}
		}
		if (amount == 0 || amount > 2) return null;
		int height = (maxRow - minRow) + 1;
		int width = (maxCol - minCol) + 1;
		if (height > 2 || width > 2) return null;

		ItemStack[] grid2x2 = new ItemStack[4];
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int oldIndex = (minRow + r) * 3 + (minCol + c);
				int newIndex = r * 2 + c;
				grid2x2[newIndex] = grid3x3[oldIndex];
			}
		}
		return grid2x2;
	}
}
