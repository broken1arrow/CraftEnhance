package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
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

public class EditorTypeSelector extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final String permission;
	private final String recipeKey;
	private int slots;
	public EditorTypeSelector(String recipeKey, String permission) {
		this.permission = permission;
		this.recipeKey = recipeKey;
		menuTemplate = menuSettingsCache.getTemplates().get("EditorTypeSelector");
		setMenuSize(9);
		setTitle(menuTemplate.getMenuTitel());

	}

	private String getFreshKey(String keySeed) {
		if (keySeed == null || !self().getFm().isUniqueRecipeKey(keySeed)) {
			int uniqueKeyIndex = 1;
			keySeed = "recipe";

			while (!self().getFm().isUniqueRecipeKey(keySeed + uniqueKeyIndex)) uniqueKeyIndex++;
			keySeed += uniqueKeyIndex;
		}
		return keySeed;
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
				run( value,player);
			}

			@Override
			public ItemStack getItem() {
				return value.getItemStack();
			}
		};
	}

	public void run(com.dutchjelly.craftenhance.gui.templates.MenuButton value,Player player) {
		EnhancedRecipe newRecipe = null;
		if (value.getButtonType() == ButtonType.ChooseWorkbenchType){
			newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
		}
		if (value.getButtonType() == ButtonType.ChooseFurnaceType){
			newRecipe = new FurnaceRecipe(permission, null, new ItemStack[1]);
		}
		if (newRecipe != null) {
			newRecipe.setKey(getFreshKey(recipeKey));
			RecipeEditor<EnhancedRecipe> recipeEditor = new RecipeEditor<>(newRecipe, null,permission, value.getButtonType());
			recipeEditor.menuOpen(player);
		}
	}
}
