package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettings;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import lombok.NonNull;
import org.broken.arrow.menu.button.manager.library.utility.MenuButtonData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.button.logic.ButtonUpdateAction;
import org.broken.arrow.menu.library.button.logic.FillMenuButton;
import org.broken.arrow.menu.library.holder.MenuHolderPage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.GuiUtil.setTextItem;

public class CategoryList<RecipeT extends EnhancedRecipe> extends MenuHolderPage<CategoryData> {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final RecipeT recipe;
	private final CategoryData categoryData;
	private final String permission;
	private final ButtonType editorType;

	public CategoryList(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType, final String grupSeachFor) {
		super(FormatListContents.getCategorys(self().getCategoryDataCache().values(), grupSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplate("CategoryList");
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.permission = permission;
		this.editorType = editorType;
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitle());
			setMenuSize(GuiUtil.invSize("CategoryList", menuTemplate.getAmountOfButtons()));
			setMenuOpenSound(this.menuTemplate.getSound());
		}
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);
	}

	@Override
	public MenuButton getButtonAt(final int slot) {
		if (this.menuTemplate == null) return null;
		for (final Entry<List<Integer>, MenuButtonData> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()) {
			if (menuTemplate.getKey().contains(slot)) {
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(final MenuButtonData value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final @NonNull Player player, final @NonNull Inventory menu, final @NonNull ClickType click, final @NonNull ItemStack clickedItem) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				org.broken.arrow.menu.button.manager.library.utility.MenuButton button = value.getPassiveButton();
				return Adapter.getItemStack(button.getMaterial(),button.getDisplayName(),button.getLore(),button.getExtra(),button.isGlow());
			}
		};
	}

	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {
		if (value.isActionTypeEqual(ButtonType.PrvPage.name())) {
			previousPage();
			return true;
		}
		if (value.isActionTypeEqual( ButtonType.NxtPage.name())) {
			nextPage();
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			new RecipeSettings<>(this.recipe,0 ,this.categoryData, null, editorType)
					.menuOpen(player);
			//new RecipeEditor<>(this.recipe, this.categoryData, null,  editorType).menuOpen(player);
		}

		if (value.isActionTypeEqual(ButtonType.Search.name())) {
			if (click == ClickType.RIGHT) {
				new HandleChatInput(this, msg -> {
					if (GuiUtil.seachCategory(msg)) {
						new CategoryList<>(recipe, categoryData, permission, editorType, msg).menuOpen(getViewer());
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
			} else new CategoryList<>(recipe, categoryData, permission, editorType, "").menuOpen(player);
		}
		return false;
	}

	@Override
	public FillMenuButton<CategoryData> createFillMenuButton() {
		return new FillMenuButton<>((player1, itemStacks, clickType, itemStack, containerData) -> {
			if (containerData != null) {
				final String category = containerData.getRecipeCategory();
				final CategoryData changedCategory = containerData;
				recipe.setRecipeCategory(category);
				//recipe.save();
				CategoryData moveCategoryData = null;

					/*if (categoryData == null) {
						 self().getCategoryDataCache().of(category, changedCategory.getRecipeCategoryItem(), changedCategory.getDisplayName());
					} */
				if (categoryData != null) {
					moveCategoryData = self().getCategoryDataCache().move(categoryData.getRecipeCategory(), category, recipe);
					if (moveCategoryData == null) {
						Messenger.Message("Could not add recipe to this " + containerData + " category.");
						return ButtonUpdateAction.NONE;
					}
				}
				new RecipeSettings<>(recipe, 0,categoryData, null, editorType).menuOpen(player);
			}
			return ButtonUpdateAction.NONE;
		}, (slot, containerData) -> {
			if (containerData != null) {
				String displayName = " ";
				List<String> lore = new ArrayList<>();
				final Map<String, String> placeHolders = new HashMap<>();
				if (menuTemplate != null) {
					final MenuButtonData menuButton = menuTemplate.getMenuButton(-1);
					if (menuButton != null) {
						final org.broken.arrow.menu.button.manager.library.utility.MenuButton passiveButton = menuButton.getPassiveButton();
						displayName = passiveButton.getDisplayName();
						lore = passiveButton.getLore();
					}
				}
				final ItemStack itemStack = containerData.getRecipeCategoryItem();
				setTextItem(itemStack, displayName, lore);
				String categoryName = containerData.getDisplayName();
				if (categoryName == null || categoryName.equals(""))
					categoryName = containerData.getRecipeCategory();
				placeHolders.put(InfoItemPlaceHolders.DisplayName.getPlaceHolder(), categoryName);
				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
			return null;
		});
	}
}
