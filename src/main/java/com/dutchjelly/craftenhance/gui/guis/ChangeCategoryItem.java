package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipesViewerCategorysSettings;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.button.logic.FillMenuButton;
import org.broken.arrow.menu.library.holder.MenuHolderPage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class ChangeCategoryItem extends MenuHolderPage<ItemStack> {


	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final String category;

	public ChangeCategoryItem(String category) {
		super(new ArrayList<>());
		this.menuTemplate = menuSettingsCache.getTemplates().get("ChangeCategoryItem");
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitel());
			setMenuSize(GuiUtil.invSize("ChangeCategoryItem", this.menuTemplate.getAmountOfButtons()));
			setMenuOpenSound(this.menuTemplate.getSound());
		}
		this.category = category;
		this.setUseColorConversion(true);
		this.setSlotsYouCanAddItems(true);
	}

	@Override
	public FillMenuButton<ItemStack> createFillMenuButton() {
		return null;
	}

	@Nullable
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
			public void onClickInsideMenu(@Nonnull final Player player, @Nonnull final Inventory menu, @Nonnull final ClickType click, @Nonnull final ItemStack clickedItem) {
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

		if (value.getButtonType() == ButtonType.Save) {
			for (int fillSlot : this.getFillSpace()) {
				ItemStack itemStack = menu.getItem(fillSlot);
				if (itemStack != null) {
					final CategoryData categoryData = self().getCategoryDataCache().get(this.category);
					if (categoryData == null) {
						Messenger.Message("Your category name not exist", player);
						return false;
					}
					final CategoryData newCategoryData = self().getCategoryDataCache().of(this.category, new ItemStack(itemStack), categoryData.getDisplayName());
					newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
					self().getCategoryDataCache().put(this.category, newCategoryData);
					Bukkit.getScheduler().runTaskLaterAsynchronously( self(),()-> self().getCategoryDataCache().save(),1L);
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					break;
				}
			}
		}

		if (value.getButtonType() == ButtonType.Back) {
			new RecipesViewerCategorysSettings(this.category).menuOpen(player);
		}
		return false;
	}


}
