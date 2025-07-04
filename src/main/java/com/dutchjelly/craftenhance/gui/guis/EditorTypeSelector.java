package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBlast;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBrewing;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorFurnace;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorSmoker;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import org.broken.arrow.library.menu.button.MenuButton;
import org.broken.arrow.library.menu.button.manager.utility.MenuButtonData;
import org.broken.arrow.library.menu.button.manager.utility.MenuTemplate;
import org.broken.arrow.library.menu.holder.MenuHolder;
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
		final CacheRecipes cacheRecipes = self().getCacheRecipes();
		if (keySeed == null || !cacheRecipes.isUniqueRecipeKey(keySeed)) {
			int uniqueKeyIndex = 1;
			keySeed = "recipe";

			while (!cacheRecipes.isUniqueRecipeKey(keySeed + uniqueKeyIndex)) uniqueKeyIndex++;
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
				org.broken.arrow.library.menu.button.manager.utility.MenuButton button = value.getPassiveButton();
				return Adapter.getItemStack(button.getMaterial(),button.getDisplayName(),button.getLore(),button.getExtra(),button.isGlow());
			}
		};
	}

	public void run(final MenuButtonData value, final Player player) {
		if (value.isActionTypeEqual( ButtonType.ChooseWorkbenchType.name())){
			EnhancedRecipe	newRecipe = new WBRecipe(permission, null, new ItemStack[9]);
			newRecipe.setKey(getFreshKey(recipeKey));
			final RecipeEditor<EnhancedRecipe> recipeEditor = new RecipeEditor<>(newRecipe,0, null,permission, ButtonType.ChooseWorkbenchType);
			recipeEditor.menuOpen(player);
			return;
		}
		if (value.isActionTypeEqual( ButtonType.ChooseFurnaceType.name())){
			FurnaceRecipe furnaceRecipe = new FurnaceRecipe(permission, null, new ItemStack[1]);
			furnaceRecipe.setKey(getFreshKey(recipeKey));
			new RecipeEditorFurnace(furnaceRecipe,0,null,permission,ButtonType.ChooseFurnaceType,true).menuOpen(player);
			return;
		}
		if (value.isActionTypeEqual( ButtonType.ChooseBlastType.name())){
			BlastRecipe blastRecipe = new BlastRecipe(permission, null, new ItemStack[1]);
			blastRecipe.setKey(getFreshKey(recipeKey));
			new RecipeEditorBlast(blastRecipe,0,null,permission,ButtonType.ChooseBlastType,true).menuOpen(player);
			return;
		}
		if (value.isActionTypeEqual( ButtonType.ChooseSmokerType.name())){
			SmokerRecipe blastRecipe = new SmokerRecipe(permission, null, new ItemStack[1]);
			blastRecipe.setKey(getFreshKey(recipeKey));
			new RecipeEditorSmoker(blastRecipe,0,null,permission,ButtonType.ChooseSmokerType,true).menuOpen(player);
		}
		if (value.isActionTypeEqual( ButtonType.ChooseBrewingType.name())){
			BrewingRecipe brewingRecipe = new BrewingRecipe(permission, null, new ItemStack[3]);
			brewingRecipe.setKey(getFreshKey(recipeKey));
			new RecipeEditorBrewing(brewingRecipe,0,null,permission,ButtonType.ChooseBrewingType, true).menuOpen(player);
		}
	}
}
