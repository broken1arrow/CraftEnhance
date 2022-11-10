package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipesViewerCategorys extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;

	public RecipesViewerCategorys(String grupSeachFor) {
		super(new ArrayList<>(self().getCategoryDataCache().getRecipeCategorys().values()));
		this.menuTemplate = menuSettingsCache.getTemplates().get("RecipesCategorys");
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitel());
		setMenuSize(54);
	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {
				if (o instanceof CategoryData)
					new RecipesViewerCopy((CategoryData) o,"",player).menuOpen(player);
			}
			@Override
			public ItemStack getItem(Object object) {
				if (object instanceof CategoryData){
					return ((CategoryData) object).getRecipeCategoryItem();
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
		if (value.getButtonType() == ButtonType.PrvPage){
			previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage){
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.Search){
			new RecipesViewerCategorys( "");
		}
		if (value.getButtonType() == ButtonType.newCategory){
			Messenger.Message("Please input your category name and item type you want. Like this 'category crafting_table' without '.", getViewer());
			self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
					return false;
				if (!msg.isEmpty()) {
					String[] split = msg.split(" ");
					if (split.length > 1){
						Material material = Material.getMaterial(split[1]);
						if (material == null) {
							Messenger.Message("Please input valid item name. Your input " + split[1], getViewer());
							return true;
						}
						if (self().getCategoryDataCache().addCategory(split[0],new ItemStack(material))){
							Messenger.Message("Your category name alredy exist", getViewer());
							return true;
						} else return false;
					}
				}
				return true;
			});
			return true;
		}
		if (value.getButtonType() == ButtonType.changeCategory){
			Messenger.Message("Change category name and you can also change item (if not set it will use the old one). Like this 'category new_category_name crafting_table' without '.", getViewer());
			self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
					return false;
				if (!msg.isEmpty()) {
					String[] split = msg.split(" ");
					if (split.length > 1){
						CategoryData categoryData = self().getCategoryDataCache().getRecipeCategorys().get(split[0]);
						if (categoryData == null){
							Messenger.Message("Your category name not exist", getViewer());
							return true;
						} else {
							Material material = null;
							if (split.length >= 3)
								material = Material.getMaterial(split[2]);
							CategoryData newCategoryData = new CategoryData(material != null ? new ItemStack( material) :categoryData.getRecipeCategoryItem(),split[1]);
							self().getCategoryDataCache().getRecipeCategorys().remove(split[0]);
							newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
							self().getCategoryDataCache().getRecipeCategorys().put(split[1],newCategoryData);
							return false;
						}
					}
				}
				return true;
			});
			return true;
		}
		return false;
	}
}
