package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static menulibrary.dependencies.rbglib.TextTranslator.toSpigotFormat;

public class CategoryList<RecipeT extends EnhancedRecipe> extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final RecipeT recipe;
	private final CategoryData categoryData;
	private final String permission;
	private final ButtonType editorType;

	public CategoryList(RecipeT recipe, CategoryData categoryData, String permission, ButtonType editorType, String grupSeachFor) {
		super(FormatListContents.getCategorys(self().getCategoryDataCache().getRecipeCategorys().values(), grupSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplates().get("CategoryList");
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.permission = permission;
		this.editorType = editorType;
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitel());
		}
		setMenuSize(54);
	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {
				if (o instanceof CategoryData){
					String category = ((CategoryData) object).getRecipeCategory();
					recipe.setRecipeCategory(category);
					recipe.save();
					CategoryData newCategoryData = self().getCategoryDataCache().of(  category,categoryData.getRecipeCategoryItem());
					if (!self().getCategoryDataCache().move(categoryData.getRecipeCategory(),  recipe, category, newCategoryData))
						Messenger.Message("Could not find category, so it create new one insted");
					new RecipeEditor<>(recipe, newCategoryData, null,  editorType).menuOpen(player);
				}
			}

			@Override
			public ItemStack getItem(Object object) {
				if (object instanceof CategoryData) {
					ItemStack itemStack = ((CategoryData) object).getRecipeCategoryItem();
					ItemMeta meta = itemStack.getItemMeta();
					if (meta != null) {
						String displayName = ((CategoryData) object).getDisplayName();
						if (displayName == null || displayName.equals(""))
							displayName = ((CategoryData) object).getRecipeCategory();
						meta.setDisplayName(toSpigotFormat(displayName));
					}
					itemStack.setItemMeta(meta);
					return itemStack;
				}
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
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()) {
			if (menuTemplate.getKey().contains(slot)) {
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
		if (value.getButtonType() == ButtonType.PrvPage) {
			previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage) {
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.Back){
				new RecipeEditor<>(this.recipe, this.categoryData, null,  editorType).menuOpen(player);
		}

		if (value.getButtonType() == ButtonType.Search) {
			if (click == ClickType.RIGHT) {
				Messenger.Message("Search for categorys.", getViewer());
				self().getGuiManager().waitForChatInput(this, getViewer(), msg -> {
					if (GuiUtil.seachCategory(msg)) {
						new CategoryList<>( recipe, categoryData, permission,  editorType,msg).menuOpen(getViewer());
						return false;
					}
					return true;
				});
			} else new CategoryList<>(recipe, categoryData, permission,  editorType,"").menuOpen(player);
		}
		return false;
	}
}
