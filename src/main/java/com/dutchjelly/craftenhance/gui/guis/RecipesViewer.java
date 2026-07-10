package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedFurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBlast;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBrewing;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorFurnace;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorSmoker;
import com.dutchjelly.craftenhance.gui.guis.viewers.RecipeViewRecipe;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import com.dutchjelly.craftenhance.util.PaginatedItems;
import com.dutchjelly.craftenhance.util.PermissionTypes;
import com.dutchjelly.craftenhance.util.SortOrder;
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
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.GuiUtil.setcolorLore;
import static com.dutchjelly.craftenhance.util.StringUtil.capitalizeFully;

public class RecipesViewer extends MenuHolderPage<EnhancedRecipe> {
	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final CategoryData categoryData;
	private final String recipeSearchFor;
	private SortOrder sort;

	public RecipesViewer(final CategoryData categoryData, final String recipeSearchFor, final Player player) {
		this(categoryData, recipeSearchFor, null, player);
	}

	public RecipesViewer(final CategoryData categoryData, final String recipeSearchFor, final SortOrder sort, final Player player) {
		//super(canSeeRecipes(categoryData.getEnhancedRecipes(recipeSearchFor), player));
		super(new PaginatedItems(categoryData, self().getMenuSettingsCache().getTemplate("RecipesViewer")).retrieveList(player, sort, recipeSearchFor));
		this.recipeSearchFor = recipeSearchFor;
		this.menuTemplate = menuSettingsCache.getTemplate("RecipesViewer");
		this.categoryData = categoryData;
		this.sort = sort;
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(() -> this.menuTemplate.getMenuTitle() +
				(categoryData.getDisplayName() == null || categoryData.getDisplayName().isEmpty() ?
						categoryData.getRecipeCategory() : categoryData.getDisplayName()));
		setMenuSize(GuiUtil.invSize("RecipesViewer", this.menuTemplate.getAmountOfButtons()));
		setMenuOpenSound(this.menuTemplate.getSound());
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
			public void onClickInsideMenu(@Nonnull final Player player, @Nonnull final Inventory menu, @Nonnull final ClickType click, @Nonnull final ItemStack clickedItem) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				org.broken.arrow.library.menu.button.manager.utility.MenuButton button = null;
				if (getViewer().hasPermission(PermissionTypes.Edit.getPerm()))
					button = value.getActiveButton();
				if (button == null)
					button = value.getPassiveButton();
				final ItemStack itemStack = Adapter.getItemStack(button.getMaterial(), button.getDisplayName(), button.getLore(), button.getExtra(), button.isGlow());
				if (itemStack == null)
					return null;

				final Map<String, Object> placeHolders = new HashMap<>();
				placeHolders.put(InfoItemPlaceHolders.Sort.getPlaceHolder(), capitalizeFully(sort == null ? "NON" : sort.name()));

				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
		};
	}

	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {

		if (value.isActionTypeEqual(ButtonType.PrvPage.name())) {
			previousPage();
			return true;
		}

		if (value.isActionTypeEqual(ButtonType.NxtPage.name())) {
			nextPage();
			return true;
		}

		if (value.isActionTypeEqual(ButtonType.Sort.name())) {
			this.sort = this.sort == null ? SortOrder.NAME : this.sort.nextValue();
			new RecipesViewer(this.categoryData, this.recipeSearchFor, this.sort, this.getViewer()).menuOpen(player);
			return false;
		}

		if (value.isActionTypeEqual(ButtonType.Search.name())) {
			if (click == ClickType.RIGHT)
				new HandleChatInput(this, msg -> {
					if (GuiUtil.seachCategory(msg)) {
						new RecipesViewer(categoryData, msg, player).menuOpen(getViewer());
						return false;
					}
					return true;
				});
			/*	self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
					if (GuiUtil.seachCategory(msg)) {
						new RecipesViewer(categoryData,msg,player).menuOpen(getViewer());
						return false;
					}
					return true;
				});*/
			else new RecipesViewer(categoryData, "", player).menuOpen(player);
		}
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			new RecipesViewerCategories("").menuOpen(player);
		}
		return false;
	}

	@Override
	public FillMenuButton<EnhancedRecipe> createFillMenuButton() {
		MenuButtonData menuButton = this.menuTemplate.getMenuButton(-1);

		final boolean viewAll = player.hasPermission(PermissionTypes.View_ALL.getPerm()) || player.hasPermission(PermissionTypes.Edit.getPerm());
		return new FillMenuButton<>((player, itemStacks, clickType, itemStack, enhancedRecipe) -> {
			boolean allowClick = (clickType == ClickType.MIDDLE || clickType == ClickType.RIGHT) && getViewer().hasPermission(PermissionTypes.Edit.getPerm());

			if (enhancedRecipe instanceof WBRecipe) {
				if (allowClick)
					new RecipeEditor<>((WBRecipe) enhancedRecipe, this.getPageNumber(), categoryData, null, ButtonType.ChooseWorkbenchType).menuOpen(player);
				else
					new RecipeViewRecipe<>(categoryData, this.getPageNumber(), (WBRecipe) enhancedRecipe, "WBRecipeViewer").menuOpen(player);
			}
			if (enhancedRecipe instanceof EnhancedFurnaceRecipe) {
				if (allowClick)
					new RecipeEditorFurnace((EnhancedFurnaceRecipe) enhancedRecipe, this.getPageNumber(), categoryData, null, ButtonType.ChooseFurnaceType, true).menuOpen(player);
				else
					new RecipeViewRecipe<>(categoryData, this.getPageNumber(), (EnhancedFurnaceRecipe) enhancedRecipe, "FurnaceRecipeViewer").menuOpen(player);
			}
			if (enhancedRecipe instanceof BlastRecipe) {
				if (allowClick)
					new RecipeEditorBlast((BlastRecipe) enhancedRecipe, this.getPageNumber(), categoryData, null, ButtonType.ChooseFurnaceType, true).menuOpen(player);
				else
					new RecipeViewRecipe<>(categoryData, this.getPageNumber(), (BlastRecipe) enhancedRecipe, "FurnaceRecipeViewer").menuOpen(player);
			}
			if (enhancedRecipe instanceof SmokerRecipe) {
				if (allowClick)
					new RecipeEditorSmoker((SmokerRecipe) enhancedRecipe, this.getPageNumber(), categoryData, null, ButtonType.ChooseFurnaceType, true).menuOpen(player);
				else
					new RecipeViewRecipe<>(categoryData, this.getPageNumber(), (SmokerRecipe) enhancedRecipe, "FurnaceRecipeViewer").menuOpen(player);
			}
			if (enhancedRecipe instanceof BrewingRecipe) {
				if (allowClick)
					new RecipeEditorBrewing((BrewingRecipe) enhancedRecipe, this.getPageNumber(), categoryData, null, ButtonType.ChooseBrewingType, true).menuOpen(player);
				else
					new RecipeViewRecipe<>(categoryData, this.getPageNumber(), (BrewingRecipe) enhancedRecipe, "BrewerRecipeViewer").menuOpen(player);
			}
			return ButtonUpdateAction.NONE;
		}, (slot, enhancedRecipe) -> {
			if (enhancedRecipe != null) {
				org.broken.arrow.library.menu.button.manager.utility.MenuButton button = menuButton.getActiveButton();
				if (!viewAll || button == null)
					button = menuButton.getPassiveButton();
				ItemStack displayItem = enhancedRecipe.getDisplayItem().clone();
				List<String> lore = setcolorLore(button.getLore());

				ItemMeta meta = displayItem.getItemMeta();
				if (meta != null) {
					List<String> itemLore = meta.getLore();
					if (itemLore == null)
						itemLore = new ArrayList<>();
					itemLore.addAll(lore);
					meta.setLore(itemLore);
				}
				displayItem.setItemMeta(meta);
				final ItemStack itemStack = GuiUtil.ReplaceAllPlaceHolders(displayItem, getPlaceholders(enhancedRecipe));
				return itemStack;
			}
			return null;
		});
	}

	private Map<String, Object> getPlaceholders(final EnhancedRecipe enhancedRecipe) {
		final Player player = getViewer();
		return enhancedRecipe.getPlaceholders(player);
	}

}
