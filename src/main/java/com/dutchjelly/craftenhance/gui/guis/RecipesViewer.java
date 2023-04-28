package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.viewers.RecipeViewRecipe;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.util.PermissionTypes;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.canSeeRecipes;

public class RecipesViewer extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final CategoryData categoryData;
	public RecipesViewer(final CategoryData categoryData, final String recipeSeachFor, final Player player) {
		super(canSeeRecipes(categoryData.getEnhancedRecipes(recipeSeachFor),  player));
		this.menuTemplate = menuSettingsCache.getTemplates().get("RecipesViewer");
		this.categoryData = categoryData;
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(()->this.menuTemplate.getMenuTitel() +
				(categoryData.getDisplayName() == null || categoryData.getDisplayName().isEmpty() ?
						categoryData.getRecipeCategory(): categoryData.getDisplayName()));
		setMenuSize(GuiUtil.invSize("RecipesViewer",this.menuTemplate.getAmountOfButtons()));
		setMenuOpenSound(this.menuTemplate.getSound());
	}

	@Override
	public MenuButton getFillButtonAt(final Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final Player player, final Inventory inventory, final ClickType clickType, final ItemStack itemStack, final Object o) {
				if (o instanceof WBRecipe) {
					if ((clickType == ClickType.MIDDLE || clickType == ClickType.RIGHT) && getViewer().hasPermission(PermissionTypes.Edit.getPerm()))
						new RecipeEditor<>((WBRecipe) o, categoryData, null,ButtonType.ChooseWorkbenchType).menuOpen(player);
					else
						new RecipeViewRecipe<>(categoryData, (WBRecipe) o, "WBRecipeViewer").menuOpen(player);
				}
				if (o instanceof FurnaceRecipe) {
					if ((clickType == ClickType.MIDDLE || clickType == ClickType.RIGHT) && getViewer().hasPermission(PermissionTypes.Edit.getPerm()))
						new RecipeEditor<>((FurnaceRecipe) o, categoryData,null, ButtonType.ChooseFurnaceType).menuOpen(player);
					else
						new RecipeViewRecipe<>(categoryData, (FurnaceRecipe) o, "FurnaceRecipeViewer").menuOpen(player);
				}
			}

			@Override
			public ItemStack getItem(final Object object) {
				if (object instanceof EnhancedRecipe){
					return ((EnhancedRecipe)object).getDisplayItem();
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
	public MenuButton getButtonAt(final int slot) {
		if (this.menuTemplate == null) return null;
		for (final Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(final com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final Player player, final Inventory menu, final ClickType click, final ItemStack clickedItem, final Object object) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				return value.getItemStack();
			}
		};
	}
	public boolean run(final com.dutchjelly.craftenhance.gui.templates.MenuButton value, final Inventory menu, final Player player, final ClickType click) {
		if (value.getButtonType() == ButtonType.PrvPage){
            previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage){
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.Search) {
			if (click == ClickType.RIGHT)
				self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
					if (GuiUtil.seachCategory(msg)) {
						new RecipesViewer(categoryData,msg,player).menuOpen(getViewer());
						return false;
					}
					return true;
				});
			else new RecipesViewer(categoryData, "", player).menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.Back) {
			new RecipesViewerCategorys("").menuOpen(player);
		}
		return false;
	}

}
