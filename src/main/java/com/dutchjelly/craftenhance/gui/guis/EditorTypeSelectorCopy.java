package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.gui.GuiManager;
import com.dutchjelly.craftenhance.gui.templates.GuiTemplate;
import com.dutchjelly.craftenhance.gui.templates.MenuSettingsCache;
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

public class EditorTypeSelectorCopy extends MenuHolder {

	private final GuiTemplate template;
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final String permission;
	private final String recipeKey;
	private int slots;
	public EditorTypeSelectorCopy(GuiManager manager, GuiTemplate template, GUIElement previousGui, String recipeKey, String permission) {
		this.template = template;
		this.permission = permission;
		this.recipeKey = recipeKey;
		setMenuSize(template.getTemplate().length);
		setTitle(template.getInvTitle());
		menuTemplate = menuSettingsCache.getTemplates().get("EditorTypeSelector");

/*		chooseWorkbenchType = new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {
				WBRecipe newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
				newRecipe.setKey(getFreshKey(recipeKey));

				WBRecipeEditor gui = new WBRecipeEditor(self().getGuiManager(), self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), previousGui, player, newRecipe);
				manager.openGUI(player, gui);
			}

			@Override
			public ItemStack getItem() {
				ItemStack[] itemStack = template.getTemplate();
				if (itemStack.length > slots) return itemStack[slots];
				return null;
			}
		};
		chooseFurnaceType = new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {
				WBRecipe newRecipe = new WBRecipe(permission, null, new ItemStack[9]);

				newRecipe.setKey(getFreshKey(recipeKey));

				WBRecipeEditor gui = new WBRecipeEditor(self().getGuiManager(), self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), previousGui, player, newRecipe);
				manager.openGUI(player, gui);
			}

			@Override
			public ItemStack getItem() {
				ItemStack[] itemStack = template.getTemplate();
				if (itemStack.length > 	slots) return itemStack[slots];
				return null;
			}
		};
		placeholderItems = new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {

			}

			@Override
			public ItemStack getItem(int slot, @Nullable Object object) {
				ItemStack[] itemStack = template.getTemplate();
				if (itemStack.length > 	slot && !template.getButtonMapping().containsKey(slot)) return itemStack[slot];
				return null;
			}


			@Override
			public ItemStack getItem() {
				return null;
			}
		};*/

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
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButton().entrySet()){
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
				run( value);
			}

			@Override
			public ItemStack getItem() {
				return value.getItemStack();
			}
		};
	}

	public void run(com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		if (value.getButtonType() == ButtonType.ChooseWorkbenchType){
				/*		WBRecipe newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
					newRecipe.setKey(getFreshKey(recipeKey));

					WBRecipeEditor gui = new WBRecipeEditor(self().getGuiManager(), self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), previousGui, player, newRecipe);
					manager.openGUI(player, gui);*/
		}
		if (value.getButtonType() == ButtonType.ChooseFurnaceType){
				/*			WBRecipe newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
				newRecipe.setKey(getFreshKey(recipeKey));
				WBRecipeEditor gui = new WBRecipeEditor(self().getGuiManager(), self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), previousGui, player, newRecipe);
				manager.openGUI(player, gui);*/
		}
	}
}
