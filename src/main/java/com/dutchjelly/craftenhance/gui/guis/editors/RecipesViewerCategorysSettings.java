package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.ChangeCategoryItem;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewerCategorys;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
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

public class RecipesViewerCategorysSettings extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final String category;

	public RecipesViewerCategorysSettings(final String category) {
		this.menuTemplate = menuSettingsCache.getTemplate("CategorysSettings");
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitle());
			setMenuSize(GuiUtil.invSize("CategorysSettings",this.menuTemplate.getAmountOfButtons()));
			setMenuOpenSound(this.menuTemplate.getSound());
		}
		this.category = category;
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);
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
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				org.broken.arrow.library.menu.button.manager.utility.MenuButton button = value.getPassiveButton();
				return Adapter.getItemStack(button.getMaterial(),button.getDisplayName(),button.getLore(),button.getExtra(),button.isGlow());
			}
		};
	}

	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {

		if (value.isActionTypeEqual(  ButtonType.RemoveCategory.name())){
			final CategoryData categoryData = self().getCategoryDataCache().get(this.category);
			if (categoryData != null) {
				final List<EnhancedRecipe> enhancedRecipes = categoryData.getEnhancedRecipes();
				final String defaultCategory = "default";
				if (enhancedRecipes != null && !enhancedRecipes.isEmpty()) {
					CategoryData oldCategory = self().getCategoryDataCache().get(defaultCategory);
					if (oldCategory == null)
						oldCategory = self().getCategoryDataCache().of(defaultCategory, new ItemStack(Adapter.getMaterial("CRAFTING_TABLE")), null);
					for (final EnhancedRecipe recipe : enhancedRecipes) {
						recipe.setRecipeCategory(defaultCategory);
						oldCategory.addEnhancedRecipes(recipe);
					}
					self().getCategoryDataCache().put(defaultCategory, oldCategory);
				}
				self().getCategoryDataCache().remove((this.category));
				CraftEnhance.runTaskLaterAsync(1, () -> self().getCategoryDataCache().save());
				new RecipesViewerCategorys("").menuOpen(player);
			}
		}
		if (value.isActionTypeEqual( ButtonType.ChangeCategoryName.name())){
			new HandleChatInput(this, msg-> {
				if(!GuiUtil.changeCategoryName(this.category,msg,player)){
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Please input new display name. Like this 'name' without '.Type cancel, quit, exit to close this without change.")
					.start(player);
		}
		if (value.isActionTypeEqual( ButtonType.ChangeCategoryItem.name())){
			if (click.isLeftClick()) {
				new ChangeCategoryItem(this.category).menuOpen(player);
			} else
				new HandleChatInput(this, msg -> {
					if (!GuiUtil.changeCategoryItem(this.category, msg, player)) {
						new RecipesViewerCategorysSettings(this.category).menuOpen(player);
						return false;
					}
					return true;
				}).setMessages("Change category item. Like this 'stone' without '.Type cancel, quit, exit or q to close this without change.")
						.start(player);
		}
		if (value.isActionTypeEqual( ButtonType.ChangeCategory.name())){
			new HandleChatInput(this, msg -> {
				if (!GuiUtil.changeCategory(this.category, msg, player)) {
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Change category name. Like this 'new_category_name' without '.Type cancel, quit, exit or q to close this without change.")
					.start(player);
		}
		if (value.isActionTypeEqual( ButtonType.Back.name())){
			new RecipesViewerCategorys("").menuOpen(player);
		}
		return false;
	}
}