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

import java.util.ArrayList;
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
			final ItemStack[] itemStacks = compressShapeTo2x2(originalGrid);
			return itemStacks != null ? itemStacks : new ItemStack[0];

			//Handle it as shapeless recipe when is more than 2 items to compress into a 2*2 grid.
			// boolean treatAsShapelessIn2x2 = true;
	}

	public static ItemStack[] compressShapeTo2x2(ItemStack[] ingredients) {
		int amount = 0;
		int firstIndex = -1;
		int secondIndex = -1;
		final List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			final ItemStack ingredient = ingredients[i];
			if (ingredient != null && ingredient.getType() != Material.AIR) {
				stacks.add(ingredient);
				amount++;
				if (firstIndex == -1) {
					firstIndex = i;
				} else {
					secondIndex = i;
				}
			}
		}
		if (amount > 2) {
			if(stacks.size() < 5)
				return stacks.toArray(new ItemStack[0]);
			return null;
		}
		final ItemStack[] grid2x2 = new ItemStack[4];

		int row1 = firstIndex / 3;
		int col1 = firstIndex % 3;
		int row2 = secondIndex / 3;
		int col2 = secondIndex % 3;

		if (row1 == row2) {
			grid2x2[0] = ingredients[firstIndex];
			grid2x2[1] = ingredients[secondIndex];
		} else if (col1 == col2) {
			grid2x2[0] = ingredients[firstIndex];
			grid2x2[2] = ingredients[secondIndex];
		} else {
			grid2x2[0] = ingredients[firstIndex];
			grid2x2[3] = ingredients[secondIndex];
		}
		return grid2x2;
	}
}
