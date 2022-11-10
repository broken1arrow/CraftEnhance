package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.templates.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipesViewerCategorys extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;

	public RecipesViewerCategorys() {
		super(Collections.singletonList(RecipeLoader.getInstance().getCategoryDataCache().keySet()));
		this.menuTemplate = menuSettingsCache.getTemplates().get("EditorTypeSelector");
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitel());

	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {

			}

			@Override
			public ItemStack getItem(Object object) {
				if (object instanceof String){
					CategoryData enhancedRecipes = RecipeLoader.getInstance().getCategoryDataCache().get(object);
					return enhancedRecipes.getRecipeCategoryItem();
				}
				return null;
			}

			@Override
			public ItemStack getItem() {
				return null;
			}
		};
	}
}
