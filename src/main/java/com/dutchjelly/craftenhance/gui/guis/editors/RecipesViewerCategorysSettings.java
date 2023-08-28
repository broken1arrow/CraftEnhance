package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewerCategorys;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.holder.MenuHolder;
import org.bukkit.Bukkit;
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
		this.menuTemplate = menuSettingsCache.getTemplates().get("CategorysSettings");
		if (this.menuTemplate != null) {
			setFillSpace(this.menuTemplate.getFillSlots());
			setTitle(this.menuTemplate.getMenuTitel());
			setMenuSize(GuiUtil.invSize("CategorysSettings",this.menuTemplate.getAmountOfButtons()));
			setMenuOpenSound(this.menuTemplate.getSound());
		}
		this.category = category;
		this.setUseColorConversion(true);
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
			public void onClickInsideMenu(@Nonnull final Player player, @Nonnull final Inventory menu, @Nonnull final ClickType click, @Nonnull final ItemStack clickedItem, final Object object) {
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

		if (value.getButtonType() == ButtonType.RemoveCategory){
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
				Bukkit.getScheduler().runTaskLaterAsynchronously(self(), () -> self().getCategoryDataCache().save(), 1);
				new RecipesViewerCategorys("").menuOpen(player);
			}
		}
		if (value.getButtonType() == ButtonType.ChangeCategoryName){
			new HandleChatInput(this, msg-> {
				if(!GuiUtil.changeCategoryName(this.category,msg,player)){
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Please input new display name. Like this 'name' without '.Type cancel, quit, exit to close this without change.")
					.start(player);
		}
		if (value.getButtonType() == ButtonType.ChangeCategoryItem){
			new HandleChatInput(this, msg-> {
				if (!GuiUtil.changeCategoryItem(this.category,msg, player)) {
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Change category item. Like this 'stone' without '.Type cancel, quit, exit or q to close this without change.")
					.start(player);
		}
		if (value.getButtonType() == ButtonType.ChangeCategory){
			new HandleChatInput(this, msg-> {
				if (!GuiUtil.changeCategory(this.category,msg, player)) {
					new RecipesViewerCategorysSettings(this.category).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Change category name. Like this 'new_category_name' without '.Type cancel, quit, exit or q to close this without change.")
					.start(player);
		}
		if (value.getButtonType() == ButtonType.Back) {
			new RecipesViewerCategorys("").menuOpen(player);
		}
		return false;
	}
}