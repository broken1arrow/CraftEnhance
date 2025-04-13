package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.EditorTypeSelector;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewer;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettings;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Messenger;
import lombok.Getter;
import lombok.NonNull;
import org.broken.arrow.menu.button.manager.library.utility.MenuButtonData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.broken.arrow.menu.library.CheckItemsInsideMenu;
import org.broken.arrow.menu.library.MenuUtility;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.button.logic.ButtonUpdateAction;
import org.broken.arrow.menu.library.button.logic.FillMenuButton;
import org.broken.arrow.menu.library.holder.MenuHolderPage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.formatRecipes;
import static com.dutchjelly.craftenhance.util.StringUtil.capitalizeFully;

public class RecipeEditor<RecipeT extends EnhancedRecipe> extends MenuHolderPage<ItemStack> {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final IngredientsCache ingredientsCache;
	private final boolean checkPartialMatch;
	private final int page;
	private String permission;
	@Getter
	private final RecipeT brewRecipe;
	private final MenuTemplate menuTemplate;
	@Getter
	ItemStack result;

	boolean hidden;
	ItemMatchers.MatchType matchType;
	private boolean shapeless;
	private final ButtonType editorType;
	private final CategoryData categoryData;

	public RecipeEditor(final RecipeT recipe, final int page, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		this(recipe, page, categoryData, permission, editorType, true);
	}

	public RecipeEditor(final RecipeT recipe, final int page, final CategoryData categoryData, final String permission, final ButtonType editorType, final boolean clearItems) {
		super(formatRecipes(recipe, self().getIngredientsCache(), !clearItems));
		this.page = page;
		if (permission == null || permission.equals(""))
			this.permission = recipe.getPermission();
		else this.permission = permission;
		ingredientsCache = self().getIngredientsCache();
		if (clearItems)
			ingredientsCache.clear();
		this.editorType = editorType;
		this.brewRecipe = recipe;
		this.categoryData = categoryData;
		if (recipe instanceof WBRecipe)
			shapeless = ((WBRecipe) this.brewRecipe).isShapeless();
		matchType = recipe.getMatchType();
		this.hidden = recipe.isHidden();
		this.checkPartialMatch = recipe.isCheckPartialMatch();
		menuTemplate = menuSettingsCache.getTemplate(editorType.getType());
		setMenuSize(27);
		setSlotsYouCanAddItems(true);
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);

		if (menuTemplate != null) {
			setMenuSize(GuiUtil.invSize("RecipeEditor", this.menuTemplate.getAmountOfButtons()));
			final String title = menuTemplate.getMenuTitle() == null ? "editor" : menuTemplate.getMenuTitle().replace(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), recipe.getType().name().toLowerCase());
			setTitle(() -> title);
			setFillSpace(menuTemplate.getFillSlots());
			setMenuOpenSound(this.menuTemplate.getSound());
		}
	}

	@Override
	public FillMenuButton<ItemStack> createFillMenuButton() {
		return new FillMenuButton<>((player1, itemStacks, clickType, itemStack, recipeT) -> ButtonUpdateAction.NONE,
				(slot, itemStack) -> itemStack);
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
				if (run(value, menu, player, click)) {
					updateButton(this);
					//updateButtons();
				}
			}

			@Override
			public ItemStack getItem() {
				final Map<String, Object> placeHolders = getPlaceholders();

				org.broken.arrow.menu.button.manager.library.utility.MenuButton button = value.getPassiveButton();
				ItemStack itemStack = Adapter.getItemStack(button.getMaterial(), button.getDisplayName(), button.getLore(), button.getExtra(), button.isGlow());
				if (itemStack == null)
					return null;

				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
		};
	}

	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {

		if (value.isActionTypeEqual(ButtonType.DeleteRecipe.name())) {
			self().getCacheRecipes().remove(brewRecipe);
			RecipeLoader.getInstance().unloadRecipe(brewRecipe);
			if (this.categoryData != null) {
				final RecipesViewer recipesViewer = new RecipesViewer(this.categoryData, "", player);
				recipesViewer.menuOpen(player);
				if ( this.page > 0)
					recipesViewer.setPage( this.page);
			}
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.RecipeSettings.name())) {
			if (brewRecipe instanceof WBRecipe)
				new RecipeSettings<>(this.brewRecipe, this.page, this.categoryData, this.permission, this.editorType)
						.menuOpen(player);
		}


		if (value.isActionTypeEqual(ButtonType.ResetRecipe.name())) {
			updateRecipeDisplay(menu);
			return true;

		}
		if (value.isActionTypeEqual(ButtonType.SaveRecipe.name())) {
			final CheckItemsInsideMenu checkItemsInsideInventory = getCheckItemsInsideMenu();
			checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
			final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsFromSetSlots(menu, player, false);
			save(map, player, true);
			if (this.brewRecipe instanceof WBRecipe) {
				new RecipeEditor<>((WBRecipe) this.brewRecipe, this.page, categoryData, null, ButtonType.ChooseWorkbenchType).menuOpen(player);
			}
			if (this.brewRecipe instanceof FurnaceRecipe) {
				new RecipeEditorFurnace((FurnaceRecipe) this.brewRecipe, this.page,categoryData, null, ButtonType.ChooseFurnaceType, false).menuOpen(player);
			}
			if (this.brewRecipe instanceof BlastRecipe) {
				new RecipeEditorBlast((BlastRecipe) this.brewRecipe, this.page,categoryData, null, ButtonType.ChooseBlastType, false).menuOpen(player);
			}
			if (this.brewRecipe instanceof SmokerRecipe) {
				new RecipeEditorSmoker((SmokerRecipe) this.brewRecipe, this.page,categoryData, null, ButtonType.ChooseSmokerType, false).menuOpen(player);
			}
			if (this.brewRecipe instanceof BrewingRecipe) {
				new RecipeEditorBrewing((BrewingRecipe) this.brewRecipe, this.page, categoryData, null, ButtonType.ChooseBrewingType,false).menuOpen(player);
			}
		}
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			if (this.categoryData != null) {
				final RecipesViewer recipesViewer = new RecipesViewer(this.categoryData, "", player);
				recipesViewer.menuOpen(player);
				if ( this.page > 0)
					recipesViewer.setPage( this.page);
			} else
				new EditorTypeSelector(null, permission).menuOpen(player);
		}
		return onPlayerClick(this.brewRecipe, this.categoryData, this.permission, value.getActionType(), player);
	}

	@Override
	public void menuClose(final InventoryCloseEvent event, final MenuUtility menu) {
		final CheckItemsInsideMenu checkItemsInsideInventory = getCheckItemsInsideMenu();
		checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
		final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsFromSetSlots(event.getInventory(), getViewer(), false);
		if (self().getConfig().getBoolean("save_on_close")) {
			save(map, getViewer(), true);
		}
		ingredientsCache.setItemStacks(getIngredients(map, player));
		ingredientsCache.setItemStackResult(result);
	}

	protected void updateRecipeDisplay(final Inventory menu) {
		final List<Integer> fillSpace = this.menuTemplate.getFillSlots();
		if (fillSpace.size() != brewRecipe.getContent().length + 1)
			throw new ConfigError("fill space of Recipe editor must be " + (brewRecipe.getContent().length + 1));
		for (int i = 0; i < brewRecipe.getContent().length; i++) {
			if (fillSpace.get(i) >= menu.getSize())
				throw new ConfigError("fill space spot " + fillSpace.get(i) + " is outside of inventory");
			menu.setItem(fillSpace.get(i), brewRecipe.getContent()[i]);
		}
		if (fillSpace.get(brewRecipe.getContent().length) >= menu.getSize())
			throw new ConfigError("fill space spot " + fillSpace.get(brewRecipe.getContent().length) + " is outside of inventory");
		menu.setItem(fillSpace.get(brewRecipe.getContent().length), brewRecipe.getResult());
		matchType = brewRecipe.getMatchType();
		hidden = brewRecipe.isHidden();
		//onRecipeDisplayUpdate();
	}

	private void save(final Map<Integer, ItemStack> map, final Player player, final boolean loadRecipe) {
		final ItemStack[] newContents = getIngredients(map, player);
		if (newContents == null) {
			Messenger.Message("The recipe is empty.", player);
			return;
		}
		final ItemStack newResult = getResult();
		if (newResult == null) {
			Messenger.Message("The result slot is empty.", player);
			return;
		}
		brewRecipe.setContent(newContents);
		brewRecipe.setResult(newResult.clone());

		brewRecipe.setMatchType(matchType);
		brewRecipe.setHidden(hidden);
		brewRecipe.setCheckPartialMatch(checkPartialMatch);
		this.beforeSave(brewRecipe);
		brewRecipe.setPermission(permission);
		brewRecipe.save();
		if (loadRecipe) {
			brewRecipe.load();
		} else
			Messenger.Message("Has not reload this recipe, click on save to reload the recipe or /ceh reload", player);
		Messenger.Message("Successfully saved the recipe.", player);
	}

	protected void beforeSave(final RecipeT recipe) {
		if (this.brewRecipe instanceof WBRecipe) {
			((WBRecipe) this.brewRecipe).setShapeless(shapeless);
		}
	}

	protected Map<String, String> recipePlaceholders(final RecipeT recipe) {
		return null;
	}

	protected boolean onPlayerClick(final RecipeT recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		return false;
	}

	@Nullable
	protected ItemStack[] getIngredients(final Map<Integer, ItemStack> map, final Player player) {
		List<Integer> fillSlots = this.menuTemplate.getFillSlots();
		final int resultSlot = fillSlots != null && fillSlots.size() > brewRecipe.getContent().length ? this.menuTemplate.getFillSlots().get(brewRecipe.getContent().length) : fillSlots.size();
		final List<ItemStack> stackList = new ArrayList<>(brewRecipe.getContent().length);
		int index = 0;
		for (final Integer slot : this.menuTemplate.getFillSlots()) {
			final ItemStack itemStack = map.get(slot);
			if (itemStack != null && itemStack.getAmount() > 1 && slot != resultSlot) {
				Messenger.Message("Recipes only support amounts of 1 in the content.", player);
				itemStack.setAmount(1);
			}
			if(index > stackList.size())
				break;
			if (slot != resultSlot)
				stackList.add(index, itemStack);
			if (slot == resultSlot)
				this.brewRecipe.setResultSlot(index);
			index++;

		}
		this.result = map.remove(resultSlot);
		if (!stackList.stream().anyMatch(x -> x != null)) {
			return null;
		}
		if (brewRecipe instanceof FurnaceRecipe)
			return stackList.toArray(new ItemStack[1]);
		final ItemStack[] itemstacks = stackList.toArray(new ItemStack[0]);
/*		for (final ItemStack lastItem : itemstacks){
			if (lastItem != null)
				lastItemIndex++;
		}
		final ItemStack[] copy;
		if (lastItemIndex < 5) {
			copy = new ItemStack[lastItemIndex];
			if (lastItemIndex > 0) {
				System.arraycopy(itemstacks, 0, copy, 0, lastItemIndex);
			}
		} else {
			copy = itemstacks;
		}*/
		return itemstacks;
	}

	private Map<String, Object> getPlaceholders() {
		final Map<String, Object> placeHolders = new HashMap<String, Object>() {{
			put(InfoItemPlaceHolders.Key.getPlaceHolder(), brewRecipe.getKey() == null ? "null" : brewRecipe.getKey());
			if (brewRecipe instanceof WBRecipe)
				put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), shapeless ? "shapeless" : "shaped");

			put(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), brewRecipe.getType().name().toLowerCase());
			put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), capitalizeFully(matchType.name()));
			put(InfoItemPlaceHolders.MatchDescription.getPlaceHolder(), matchType.getDescription());
			put(InfoItemPlaceHolders.Hidden.getPlaceHolder(), hidden ? "hide recipe in menu" : "show recipe in menu");
			put(InfoItemPlaceHolders.Permission.getPlaceHolder(), permission == null || permission.trim().equals("") ? "none" : permission);
			put(InfoItemPlaceHolders.Slot.getPlaceHolder(), String.valueOf(brewRecipe.getSlot()));
			put(InfoItemPlaceHolders.Page.getPlaceHolder(), String.valueOf(brewRecipe.getPage()));
			put(InfoItemPlaceHolders.Worlds.getPlaceHolder(), brewRecipe.getAllowedWorlds() != null && !brewRecipe.getAllowedWorlds().isEmpty() ?
					brewRecipe.getAllowedWorldsFormatted() : "non set");
			if (categoryData != null)
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), categoryData.getRecipeCategory());
			else
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), brewRecipe.getRecipeCategory() != null ? brewRecipe.getRecipeCategory() : "default");
		}};

		Map<String, String> extraPlaceholders = recipePlaceholders(brewRecipe);
		if (extraPlaceholders != null)
			placeHolders.putAll(extraPlaceholders);

		return placeHolders;
	}
}
