package com.dutchjelly.craftenhance;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import lombok.NonNull;
import org.bukkit.Location;
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

}
