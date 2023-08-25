package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeSettings;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.GuiUtil.setTextItem;

public class CategoryList<RecipeT extends EnhancedRecipe> extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final RecipeT recipe;
	private final CategoryData categoryData;
	private final String permission;
	private final ButtonType editorType;

	public CategoryList(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType, final String grupSeachFor) {
		super(FormatListContents.getCategorys(self().getCategoryDataCache().values(), grupSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplates().get("CategoryList");
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.permission = permission;
		this.editorType = editorType;
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitel());
			setMenuSize(GuiUtil.invSize("CategoryList",menuTemplate.getAmountOfButtons()));
			setMenuOpenSound(this.menuTemplate.getSound());
		}
	}

	@Override
	public MenuButton getFillButtonAt(final @NotNull Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final @NotNull Player player, final @NotNull Inventory inventory, final @NotNull ClickType clickType, final @NotNull ItemStack itemStack, final Object o) {
				if (o instanceof CategoryData) {
					final String category = ((CategoryData) o).getRecipeCategory();
					final CategoryData changedCategory = ((CategoryData) o);
					recipe.setRecipeCategory(category);
					recipe.save();
					CategoryData moveCategoryData = null;

					/*if (categoryData == null) {
						 self().getCategoryDataCache().of(category, changedCategory.getRecipeCategoryItem(), changedCategory.getDisplayName());
					} */
					if (categoryData != null) {
						moveCategoryData = self().getCategoryDataCache().move(categoryData.getRecipeCategory(), category, recipe);
						if (moveCategoryData == null) {
							Messenger.Message("Could not add recipe to this " + o + " category.");
							return;
						}
					}
					new RecipeEditor<>(recipe, moveCategoryData, null, editorType).menuOpen(player);
				}
			}

			@Override
			public ItemStack getItem(final @NotNull Object object) {
				if (object instanceof CategoryData) {
					String displayName = " ";
					List<String> lore = new ArrayList<>();
					final Map<String, String> placeHolders = new HashMap<>();
					if (menuTemplate != null) {
						final com.dutchjelly.craftenhance.gui.templates.MenuButton menuButton = menuTemplate.getMenuButton(-1);
						if (menuButton != null) {
							displayName = menuButton.getDisplayName();
							lore = menuButton.getLore();
						}
					}
					final ItemStack itemStack = ((CategoryData) object).getRecipeCategoryItem();
					setTextItem(itemStack, displayName, lore);
					String categoryName = ((CategoryData) object).getDisplayName();
					if (categoryName == null || categoryName.equals(""))
						categoryName = ((CategoryData) object).getRecipeCategory();
					placeHolders.put(InfoItemPlaceHolders.DisplayName.getPlaceHolder(), categoryName);

					return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
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
		for (final Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()) {
			if (menuTemplate.getKey().contains(slot)) {
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(final com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final @NotNull Player player, final @NotNull Inventory menu, final @NotNull ClickType click, final @NotNull ItemStack clickedItem, final Object object) {
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
		if (value.getButtonType() == ButtonType.PrvPage) {
			previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage) {
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.Back){
			new RecipeSettings<>(this.recipe, this.categoryData, null,  editorType)
					.menuOpen(player);
				//new RecipeEditor<>(this.recipe, this.categoryData, null,  editorType).menuOpen(player);
		}

		if (value.getButtonType() == ButtonType.Search) {
			if (click == ClickType.RIGHT) {
				new HandleChatInput(this, msg-> {
					if (GuiUtil.seachCategory(msg)) {
						new CategoryList<>( recipe, categoryData, permission,  editorType,msg).menuOpen(getViewer());
						return false;
					}
					return true;
				}).setMessages("Search for categorys.")
						.start(getViewer());
			/*	Messenger.Message("Search for categorys.", getViewer());
				self().getGuiManager().waitForChatInput(this, getViewer(), msg -> {
					if (GuiUtil.seachCategory(msg)) {
						new CategoryList<>( recipe, categoryData, permission,  editorType,msg).menuOpen(getViewer());
						return false;
					}
					return true;
				});*/
			} else new CategoryList<>(recipe, categoryData, permission,  editorType,"").menuOpen(player);
		}
		return false;
	}
}
