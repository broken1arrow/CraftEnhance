package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.canSeeRecipes;

public class PaginatedItems {

	private final CategoryData categoryData;
	private final int slotsPerPage;
	private final List<Item> itemList = new ArrayList<>();
	private final List<Item> needSortItems = new ArrayList<>();

	public PaginatedItems(final CategoryData categoryData, final MenuTemplate menuTemplate) {
		this.categoryData = categoryData;
		this.slotsPerPage = menuTemplate.getFillSlots() != null ? menuTemplate.getFillSlots().size() : 0;
	}

	public List<EnhancedRecipe> retrieveList(final Player player, final SortOrder sort, final String recipeSearchFor) {
		List<EnhancedRecipe> enhancedRecipes = canSeeRecipes(getEnhancedRecipes(recipeSearchFor), player);

		if (sort != null) {
			switch (sort) {
				case NAME:
					enhancedRecipes.sort(Comparator.comparing(EnhancedRecipe::getKey));
					break;
				case ID:
					enhancedRecipes.sort(Comparator.comparing(EnhancedRecipe::getId));
					break;
				case MATCH_TYPE:
					enhancedRecipes.sort(Comparator.comparing(EnhancedRecipe::getMatchType));
					break;
				case RECIPE_TYPE:
					enhancedRecipes.sort(Comparator.comparing(EnhancedRecipe::getType));
					break;
				case GROUP:
					enhancedRecipes.sort(Comparator.comparing(enhancedRecipe -> enhancedRecipe.getGroup() != null ? enhancedRecipe.getGroup() : ""));
					break;
			}
		}

		for (EnhancedRecipe recipe : enhancedRecipes) {
			addItem(new Item(recipe));
		}
		for (Item item : needSortItems) {
			addDuplicates(item);
		}
		return itemList.stream()
				.map(item -> (item != null) ? item.getEnhancedRecipe() : null)
				.collect(Collectors.toList());
	}

	public List<EnhancedRecipe> getEnhancedRecipes(final String recipeSearchFor) {
		CacheRecipes cacheRecipes = self().getCacheRecipes();
		if (recipeSearchFor == null || recipeSearchFor.equals(""))
			return cacheRecipes.getRecipesFiltered(enhancedRecipe -> enhancedRecipe.getRecipeCategory().equals(this.categoryData.getRecipeCategory()));
		return cacheRecipes.getRecipesFiltered(enhancedRecipe ->
				enhancedRecipe.getRecipeCategory().equals(this.categoryData.getRecipeCategory()) &&
						enhancedRecipe.getKey().contains(recipeSearchFor));
	}

	private void addDuplicates(final Item item) {
		int index = (item.page != -1) ? findNextFreeSlotInPage(item.page) : findNextFreeSlot();
		if (index == -1) {
			// No space left, move to a new page
			item.page = (itemList.size() / slotsPerPage) + 1;
			item.slot = 0;
			index = getIndex(item.page, item.slot);
		}
		ensureCapacity(index);
		itemList.set(index, item);
	}

	private void addItem(Item item) {
		if (item.page != -1 && item.slot != -1) {
			// Attempt to place fixed slot item
			int index = getIndex(item.page, item.slot);
			ensureCapacity(index);
			if (itemList.get(index) == null) {
				itemList.set(index, item);
				return;
			}
			item.slot = -1;
		}
		needSortItems.add(item);
	}


	private int getIndex(int page, int slot) {
		return (page - 1) * slotsPerPage + slot;
	}

	private void ensureCapacity(int index) {
		while (itemList.size() <= index) {
			itemList.add(null);
		}
	}

	private int findNextFreeSlotInPage(int page) {
		int start = getIndex(page, 0);
		int end = start + slotsPerPage;
		for (int i = start; i < end; i++) {
			if (i >= itemList.size() || itemList.get(i) == null) {
				return i;
			}
		}
		return -1;
	}

	private int findNextFreeSlot() {
		int index = 0;
		for (int i = 0; i < itemList.size(); i++) {
			index++;
			if (itemList.get(i) == null) {
				return i;
			}
		}
		return index >= itemList.size() - 1 ? itemList.size() : index;
	}

	public static class Item {
		private final EnhancedRecipe enhancedRecipe;
		int page;
		int slot;

		Item(final EnhancedRecipe recipe) {
			this.enhancedRecipe = recipe;
			this.page = recipe.getPage();
			this.slot = recipe.getSlot() > 0 ? recipe.getSlot() - 1 : recipe.getSlot();
			//this.slot = recipe.getSlot();
		}

		public EnhancedRecipe getEnhancedRecipe() {
			return enhancedRecipe;
		}

		@Override
		public String toString() {
			return "Item{" +
					"enhancedRecipe=" + enhancedRecipe +
					", page=" + page +
					", slot=" + slot +
					'}';
		}
	}
}
