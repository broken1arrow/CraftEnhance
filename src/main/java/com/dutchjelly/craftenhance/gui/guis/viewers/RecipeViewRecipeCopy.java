package com.dutchjelly.craftenhance.gui.guis.viewers;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewerCopy;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.util.FormatRecipeContents.formatRecipes;

public class RecipeViewRecipeCopy<RecipeT extends EnhancedRecipe> extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final CategoryData categoryData;

	public RecipeViewRecipeCopy(CategoryData categoryData, RecipeT recipe, String menuType) {
		super( formatRecipes(recipe));
		this.categoryData = categoryData;
		this.menuTemplate = menuSettingsCache.getTemplates().get( menuType);
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitel());
		setMenuSize(27);
	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {

			}

			@Override
			public ItemStack getItem(Object object) {
				if (object instanceof ItemStack)
					return (ItemStack) object;
				return null;
			}

			@Override
			public ItemStack getItem() {
				return null;
			}
		};
	}

	@Override
	public MenuButton getButtonAt(int slot) {
		if (this.menuTemplate == null) return null;
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				return value.getItemStack();
			}
		};
	}
	public boolean run(com.dutchjelly.craftenhance.gui.templates.MenuButton value, Inventory menu, Player player, ClickType click) {
		if (value.getButtonType() == ButtonType.Back) {
			new RecipesViewerCopy(categoryData, "",player).menuOpen(player);
		}
		return false;
	}
}
