package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import org.broken.arrow.menu.button.manager.library.utility.MenuButtonData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.holder.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EditorTypeSelector extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final String permission;
	private final String recipeKey;
	private int slots;
	public EditorTypeSelector(final String recipeKey, final String permission) {
		this.permission = permission;
		this.recipeKey = recipeKey;
		menuTemplate = menuSettingsCache.getTemplate("EditorTypeSelector");
		setMenuSize(GuiUtil.invSize("EditorTypeSelector",menuTemplate.getAmountOfButtons()));
		setTitle(menuTemplate.getMenuTitle());
		setMenuOpenSound(this.menuTemplate.getSound());
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);
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
	public MenuButton getButtonAt(final int slot) {
		if (this.menuTemplate == null) return null;
		for (final Entry<List<Integer>, MenuButtonData> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}

	private MenuButton registerButtons(final MenuButtonData value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(@Nonnull final Player player, @Nonnull final Inventory menu, @Nonnull final ClickType click, @Nonnull final ItemStack clickedItem) {
				run( value,player);
			}

			@Override
			public ItemStack getItem() {
				org.broken.arrow.menu.button.manager.library.utility.MenuButton button = value.getPassiveButton();
				return Adapter.getItemStack(button.getMaterial(),button.getDisplayName(),button.getLore(),button.getExtra(),button.isGlow());
			}
		};
	}

	public void run(final MenuButtonData value, final Player player) {
		EnhancedRecipe newRecipe = null;
		if (value.isActionTypeEqual( ButtonType.ChooseWorkbenchType.name())){
			newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
		}
		if (value.isActionTypeEqual( ButtonType.ChooseFurnaceType.name())){
			newRecipe = new FurnaceRecipe(permission, null, new ItemStack[1]);
		}
		if (newRecipe != null) {
			newRecipe.setKey(getFreshKey(recipeKey));
			final RecipeEditor<EnhancedRecipe> recipeEditor = new RecipeEditor<>(newRecipe, null,permission,
					value.isActionTypeEqual(ButtonType.ChooseFurnaceType.name()) ? ButtonType.ChooseFurnaceType: ButtonType.ChooseWorkbenchType);
			recipeEditor.menuOpen(player);
		}
	}
}
