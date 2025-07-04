package com.dutchjelly.craftenhance.gui.guis.viewers;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewer;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBlast;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBrewing;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorFurnace;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorSmoker;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.util.PermissionTypes;
import org.broken.arrow.library.menu.button.MenuButton;
import org.broken.arrow.library.menu.button.logic.ButtonUpdateAction;
import org.broken.arrow.library.menu.button.logic.FillMenuButton;
import org.broken.arrow.library.menu.button.manager.utility.MenuButtonData;
import org.broken.arrow.library.menu.button.manager.utility.MenuTemplate;
import org.broken.arrow.library.menu.holder.MenuHolderPage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.formatRecipes;
import static com.dutchjelly.craftenhance.util.StringUtil.capitalizeFully;

public class RecipeViewRecipe<RecipeT extends EnhancedRecipe> extends MenuHolderPage<ItemStack> {
	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final CategoryData categoryData;
	private final RecipeT recipe;
	private final int page;

	public RecipeViewRecipe(final CategoryData categoryData, final int pageNumber, final RecipeT recipe, final String menuType) {
		super(formatRecipes(recipe, null, false));
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.menuTemplate = menuSettingsCache.getTemplate(menuType);
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitle() == null ? "viewer" : this.menuTemplate.getMenuTitle().replace("[Recipe_type]", recipe.getType().name().toLowerCase()));
		setMenuSize(27);
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);
		this.page = pageNumber;
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
			public void onClickInsideMenu(@Nonnull final Player player, @Nonnull final Inventory menu, @Nonnull final ClickType click, @Nonnull final ItemStack clickedItem) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				final Map<String, Object> placeHolders = new HashMap<String, Object>() {{
					final boolean viewAll = player.hasPermission(PermissionTypes.View_ALL.getPerm()) || player.hasPermission(PermissionTypes.Edit.getPerm());
					final CraftEnhance self = self();

					if (recipe instanceof WBRecipe)
						put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), ((WBRecipe) recipe).isShapeless() ? self.getText("shapeless_recipe") : self.getText("shaped_recipe"));
					if (recipe instanceof FurnaceRecipe) {
						put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(((FurnaceRecipe) recipe).getExp()));
						put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(((FurnaceRecipe) recipe).getDuration()));
					}
					if (recipe instanceof BrewingRecipe) {
						put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(0));
						put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(((BrewingRecipe) recipe).getDuration()));
					}
					String permission = recipe.getPermission();
					final boolean permissionSet = permission == null || permission.trim().equals("");
					final MatchType matchType = recipe.getMatchType();
					final Object description = matchType.getMatchDescription();


					put(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), capitalizeFully(recipe.getType().name()));
					put(InfoItemPlaceHolders.Config_permission.getPlaceHolder(), PermissionTypes.Edit.getPerm());
					put(InfoItemPlaceHolders.Key.getPlaceHolder(), recipe.getKey() == null ? "null" : recipe.getKey());
					put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), matchType.getMatchName());

					put(InfoItemPlaceHolders.MatchDescription.getPlaceHolder(), description);
					put(InfoItemPlaceHolders.Permission.getPlaceHolder(), getPermissionText(viewAll, permission ,permissionSet));
					put(InfoItemPlaceHolders.Worlds.getPlaceHolder(), recipe.getAllowedWorlds() == null || recipe.getAllowedWorlds().isEmpty() ? self.getText("allowed_worlds_not_set"): recipe.getAllowedWorldsFormatted());
				}};

				org.broken.arrow.library.menu.button.manager.utility.MenuButton button = null;
				if (getViewer().hasPermission(PermissionTypes.Edit.getPerm()))
					button = value.getActiveButton();
				if (button == null)
					button = value.getPassiveButton();

				ItemStack itemStack = Adapter.getItemStack(button.getMaterial(), button.getDisplayName(), button.getLore(), button.getExtra(), button.isGlow());
				if (itemStack == null)
					return null;
				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
		};
	}

	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			final RecipesViewer recipesViewer = new RecipesViewer(this.categoryData, "", player);
			recipesViewer.menuOpen(player);
			if ( this.page > 0)
				recipesViewer.setPage( this.page);
		}
		if (value.isActionTypeEqual(ButtonType.edit_recipe.name())) {
			if (player.hasPermission(PermissionTypes.Edit.getPerm())) {
				if (recipe instanceof WBRecipe) {
					new RecipeEditor<>((WBRecipe) recipe, this.page, categoryData, null, ButtonType.ChooseWorkbenchType).menuOpen(player);
				}
				if (recipe instanceof FurnaceRecipe) {
					new RecipeEditorFurnace((FurnaceRecipe) recipe, this.page,categoryData, null, ButtonType.ChooseFurnaceType, true).menuOpen(player);
				}
				if (recipe instanceof BlastRecipe) {
					new RecipeEditorBlast((BlastRecipe) recipe, this.page,categoryData, null, ButtonType.ChooseBlastType, true).menuOpen(player);
				}
				if (recipe instanceof SmokerRecipe) {
					new RecipeEditorSmoker((SmokerRecipe) recipe, this.page,categoryData, null, ButtonType.ChooseSmokerType, true).menuOpen(player);
				}
				if (recipe instanceof BrewingRecipe) {
					new RecipeEditorBrewing((BrewingRecipe) recipe, this.page,categoryData, null, ButtonType.ChooseBrewingType, true).menuOpen(player);
				}
			}
			return false;
		}
		return false;
	}

	@Override
	public FillMenuButton<ItemStack> createFillMenuButton() {
		return new FillMenuButton<>((player1, itemStacks, clickType, itemStack, recipeT) -> ButtonUpdateAction.NONE,
				(integer, itemStack) -> itemStack);
	}

	private Object getPermissionText(final boolean viewAll, final String permissionText, final boolean permissionSet) {
		final CraftEnhance self = self();
		return viewAll && !permissionSet ? permissionText : permissionSet ? self.getText("permission_non_set") : self.getText("permission_no_perm");
	}



}
