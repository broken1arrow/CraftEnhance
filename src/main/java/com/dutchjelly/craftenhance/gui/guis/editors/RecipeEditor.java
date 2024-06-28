package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
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

public class RecipeEditor<RecipeT extends EnhancedRecipe> extends MenuHolderPage<ItemStack> {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final IngredientsCache ingredientsCache;
	private final boolean checkPartialMatch;
	private String permission;
	@Getter
	private final RecipeT recipe;
	private final MenuTemplate menuTemplate;
	@Getter
	private ItemStack result;

	private boolean hidden;
	private ItemMatchers.MatchType matchType;
	private boolean shapeless;
	private final ButtonType editorType;
	private final CategoryData categoryData;

	public RecipeEditor(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		this(recipe, categoryData, permission, editorType, true);
	}

	public RecipeEditor(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType, final boolean clearItems) {
		super(formatRecipes(recipe, self().getIngredientsCache(), !clearItems));
		if (permission == null || permission.equals(""))
			this.permission = recipe.getPermission();
		else this.permission = permission;
		ingredientsCache = self().getIngredientsCache();
		if (clearItems)
			ingredientsCache.clear();
		this.editorType = editorType;
		this.recipe = recipe;
		this.categoryData = categoryData;
		if (recipe instanceof WBRecipe)
			shapeless = ((WBRecipe) this.recipe).isShapeless();
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
			setTitle(menuTemplate.getMenuTitle() == null? "editor" :menuTemplate.getMenuTitle().replace(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), recipe.getType().name().toLowerCase()));
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
				final Map<String, String> placeHolders = getPalceholders();

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
			self().getFm().removeRecipe(recipe);
			RecipeLoader.getInstance().unloadRecipe(recipe);
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.RecipeSettings.name())) {
			if (recipe instanceof WBRecipe)
				new RecipeSettings<>(this.recipe, this.categoryData, this.permission, this.editorType)
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
			if (this.recipe instanceof WBRecipe) {
				new RecipeEditor<>((WBRecipe) this.recipe, categoryData, null, ButtonType.ChooseWorkbenchType).menuOpen(player);
			}
			if (this.recipe instanceof FurnaceRecipe) {
				new RecipeEditorFurnace((FurnaceRecipe) this.recipe, categoryData, null, ButtonType.ChooseFurnaceType,false).menuOpen(player);
			}
			if (this.recipe instanceof BlastRecipe) {
				new RecipeEditorBlast((BlastRecipe) this.recipe, categoryData, null, ButtonType.ChooseBlastType,false).menuOpen(player);
			}
			if (this.recipe instanceof SmokerRecipe) {
				new RecipeEditorSmoker((SmokerRecipe) this.recipe, categoryData, null, ButtonType.ChooseSmokerType,false).menuOpen(player);
			}
		}
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
		}
		return onPlayerClick(this.recipe, this.categoryData, this.permission, value.getActionType(), player);
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

	private void updateRecipeDisplay(final Inventory menu) {
		final List<Integer> fillSpace = this.menuTemplate.getFillSlots();
		if (fillSpace.size() != recipe.getContent().length + 1)
			throw new ConfigError("fill space of Recipe editor must be " + (recipe.getContent().length + 1));
		for (int i = 0; i < recipe.getContent().length; i++) {
			if (fillSpace.get(i) >= menu.getSize())
				throw new ConfigError("fill space spot " + fillSpace.get(i) + " is outside of inventory");
			menu.setItem(fillSpace.get(i), recipe.getContent()[i]);
		}
		if (fillSpace.get(recipe.getContent().length) >= menu.getSize())
			throw new ConfigError("fill space spot " + fillSpace.get(recipe.getContent().length) + " is outside of inventory");
		menu.setItem(fillSpace.get(recipe.getContent().length), recipe.getResult());
		matchType = recipe.getMatchType();
		hidden = recipe.isHidden();
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
		recipe.setContent(newContents);
		recipe.setResult(newResult);

		recipe.setMatchType(matchType);
		recipe.setHidden(hidden);
		recipe.setCheckPartialMatch(checkPartialMatch);
		this.beforeSave(recipe);
		recipe.setPermission(permission);
		recipe.save();
		if (loadRecipe) {
			recipe.load();
		} else
			Messenger.Message("Has not reload this recipe, click on save to reload the recipe or /ceh reload", player);
		Messenger.Message("Successfully saved the recipe.", player);
	}

	protected void beforeSave(final RecipeT recipe) {
		if (this.recipe instanceof WBRecipe) {
			((WBRecipe) this.recipe).setShapeless(shapeless);
		}
	}

	protected Map<String, String> recipePlaceholders(final RecipeT recipe) {
		return null;
	}

	protected boolean onPlayerClick(final RecipeT recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		return false;
	}

	@Nullable
	private ItemStack[] getIngredients(final Map<Integer, ItemStack> map, final Player player) {

		final int resultSlot = this.menuTemplate.getFillSlots().get(recipe.getContent().length);
		final List<ItemStack> arrays = new ArrayList<>(recipe.getContent().length);
		int index = 0;
		for (final Integer slot : this.menuTemplate.getFillSlots()) {
			final ItemStack itemStack = map.get(slot);
			if (itemStack != null && itemStack.getAmount() > 1 && slot != resultSlot) {
				Messenger.Message("Recipes only support amounts of 1 in the content.", player);
				itemStack.setAmount(1);
			}
			if (slot != resultSlot)
				arrays.add(index, itemStack);
			if (slot == resultSlot)
				this.recipe.setResultSlot(index);
			index++;

		}

		this.result = map.remove(resultSlot);
		if (!arrays.stream().anyMatch(x -> x != null)) {
			return null;
		}
		if (recipe instanceof FurnaceRecipe)
			return arrays.toArray(new ItemStack[1]);
		final ItemStack[] itemstacks = arrays.toArray(new ItemStack[0]);
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

	public boolean handlePositionChange(final String message) {
		if (message == null || message.trim() == "") return false;

		if (message.equals("") || message.equalsIgnoreCase("q") || message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit"))
			return false;

		final String[] args = message.split(" ");

		if (args.length != 2) {
			Messenger.Message("Please specify a page and slot number separated by a space.", getViewer());
			return true;
		}
		int page = 0, slot = 0;
		try {
			page = Integer.parseInt(args[0]);
		} catch (final NumberFormatException e) {
			Messenger.Message("Could not parse the page number.", getViewer());
			return true;
		}

		try {
			slot = Integer.parseInt(args[1]);
		} catch (final NumberFormatException e) {
			Messenger.Message("Could not parse the slot number.", getViewer());
			return true;
		}
		recipe.setPage(page);
		recipe.setSlot(slot);

		Messenger.Message("Set the page to " + page + ", and the slot to " + slot + ". This will get auto-filled if it's not available.", getViewer());
		self().getFm().saveRecipe(recipe);

		//updatePlaceHolders();
		return false;
	}

	private Map<String, String> getPalceholders() {
		final Map<String, String> placeHolders = new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Key.getPlaceHolder(), recipe.getKey() == null ? "null" : recipe.getKey());
			if (recipe instanceof WBRecipe)
				put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), shapeless ? "shapeless" : "shaped");

			put(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), recipe.getType().name().toLowerCase());
			put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), matchType.getDescription());
			put(InfoItemPlaceHolders.MatchType.getPlaceHolder(), matchType.getDescription());
			put(InfoItemPlaceHolders.Hidden.getPlaceHolder(), hidden ? "hide recipe in menu" : "show recipe in menu");
			put(InfoItemPlaceHolders.Permission.getPlaceHolder(), permission == null || permission.trim().equals("") ? "none" : permission);
			put(InfoItemPlaceHolders.Slot.getPlaceHolder(), String.valueOf(recipe.getSlot()));
			put(InfoItemPlaceHolders.Page.getPlaceHolder(), String.valueOf(recipe.getPage()));
			put(InfoItemPlaceHolders.Worlds.getPlaceHolder(), recipe.getAllowedWorlds() != null && !recipe.getAllowedWorlds().isEmpty() ?
					recipe.getAllowedWorldsFormatted() : "non set");
			if (categoryData != null)
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), categoryData.getRecipeCategory());
			else
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), recipe.getRecipeCategory() != null ? recipe.getRecipeCategory() : "default");
		}};

		Map<String, String> extraPlaceholders = recipePlaceholders(recipe);
		if (extraPlaceholders != null)
			placeHolders.putAll(extraPlaceholders);

		return placeHolders;
	}
}
