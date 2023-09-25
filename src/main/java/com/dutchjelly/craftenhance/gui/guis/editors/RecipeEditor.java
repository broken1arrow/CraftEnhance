package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedItem;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.EditorTypeSelector;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewer;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import lombok.Getter;
import lombok.NonNull;
import org.broken.arrow.menu.library.CheckItemsInsideMenu;
import org.broken.arrow.menu.library.MenuUtility;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.holder.MenuHolder;
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

public class RecipeEditor<RecipeT extends EnhancedRecipe> extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final IngredientsCache ingredientsCache;
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
	private short duration;
	private float exp;
	private final CategoryData categoryData;

	public RecipeEditor(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		this(recipe, categoryData, permission, editorType, true);
	}

	public RecipeEditor(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType, final boolean clearItems) {
		super(formatRecipes(recipe, self().getIngredientsCache(),!clearItems));
		if (permission == null || permission.equals(""))
			this.permission = recipe.getPermissions();
		else this.permission = permission;
		ingredientsCache = self().getIngredientsCache();
		if (clearItems)
			ingredientsCache.clear();
		this.editorType = editorType;
		this.recipe = recipe;
		this.categoryData = categoryData;
		if (recipe instanceof FurnaceRecipe) {
			this.duration = (short) ((FurnaceRecipe) recipe).getDuration();
			this.exp = ((FurnaceRecipe) recipe).getExp();
		}
		if (recipe instanceof WBRecipe)
			shapeless = ((WBRecipe) this.recipe).isShapeless();
		matchType = recipe.getMatchType();
		menuTemplate = menuSettingsCache.getTemplates().get(editorType.getType());
		setMenuSize(27);
		setSlotsYouCanAddItems(true);
		this.setUseColorConversion(true);
		if (menuTemplate != null) {
			setMenuSize(GuiUtil.invSize("RecipeEditor", this.menuTemplate.getAmountOfButtons()));
			setTitle(menuTemplate.getMenuTitel());
			setFillSpace(menuTemplate.getFillSlots());
			setMenuOpenSound(this.menuTemplate.getSound());
		}
	}

	@Override
	public MenuButton getFillButtonAt(final @NonNull Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final @NonNull Player player, final @NonNull Inventory inventory, final @NonNull ClickType clickType, final @NonNull ItemStack itemStack, final Object o) {

			}

			@Override
			public ItemStack getItem(final @NonNull Object object) {
				if (object instanceof ItemStack)
					return (ItemStack) object;
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
			public void onClickInsideMenu(final @NonNull Player player, final @NonNull Inventory menu, final @NonNull ClickType click, final @NonNull ItemStack clickedItem, final Object object) {
				if (run(value, menu, player, click)) {
					updateButton(this);
					//updateButtons();
				}
			}

			@Override
			public ItemStack getItem() {
				final Map<String, String> placeHolders = new HashMap<String, String>() {{
					put(InfoItemPlaceHolders.Key.getPlaceHolder(), recipe.getKey() == null ? "null" : recipe.getKey());
					if (recipe instanceof WBRecipe)
						put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), shapeless ? "shapeless" : "shaped");
					if (recipe instanceof FurnaceRecipe) {
						put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(exp));
						put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(duration));
					}
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
				if (value.getItemStack() == null) return null;
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}

	public boolean run(final com.dutchjelly.craftenhance.gui.templates.MenuButton value, final Inventory menu, final Player player, final ClickType click) {

		if (value.getButtonType() == ButtonType.DeleteRecipe) {
			self().getFm().removeRecipe(recipe);
			RecipeLoader.getInstance().unloadRecipe(recipe);
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.RecipeSettings)
			new RecipeSettings<>(this.recipe, this.categoryData, this.permission, this.editorType)
					.menuOpen(player);
		if (value.getButtonType() == ButtonType.ResetRecipe) {
			updateRecipeDisplay(menu);
			return true;

		}
		if (value.getButtonType() == ButtonType.SaveRecipe) {
			final CheckItemsInsideMenu checkItemsInsideInventory = getCheckItemsInsideMenu();
			checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
			final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsFromSetSlots(menu, player, false);
			save(map, player, true);
			new RecipeEditor<>(this.recipe, this.categoryData, this.permission, this.editorType).menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.Back) {
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
		}
		return false;
	}

	@Override
	public void menuClose(final InventoryCloseEvent event, final MenuUtility menu) {
		final CheckItemsInsideMenu checkItemsInsideInventory =  getCheckItemsInsideMenu();
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
			menu.setItem(fillSpace.get(i), recipe.getContent()[i].getItem());
		}
		if (fillSpace.get(recipe.getContent().length) >= menu.getSize())
			throw new ConfigError("fill space spot " + fillSpace.get(recipe.getContent().length) + " is outside of inventory");
		menu.setItem(fillSpace.get(recipe.getContent().length), recipe.getResult().getItem());
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
		recipe.setContent(EnhancedItem.of(newContents));
		recipe.setResult(new EnhancedItem(newResult));

		recipe.setMatchType(matchType);
		recipe.setHidden(hidden);
		beforeSave();
		recipe.setPermissions(permission);
		recipe.save();
		if (loadRecipe) {
			recipe.load();
		} else
			Messenger.Message("Has not reload this recipe, click on save to reload the recipe or /ceh reload", player);
		Messenger.Message("Successfully saved the recipe.", player);
	}

	private void beforeSave() {
		if (recipe instanceof WBRecipe) {
			((WBRecipe) recipe).setShapeless(shapeless);
		}
		if (recipe instanceof FurnaceRecipe) {
			((FurnaceRecipe) recipe).setDuration(duration);
			((FurnaceRecipe) recipe).setExp(exp);
		}
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

	private boolean handlePermissionSetCB(String message) {
		if (message == null || message.trim().equals("")) return false;

		message = message.trim();

		if (message.equalsIgnoreCase("q") || message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit"))
			return false;

		if (message.equals("-")) {
			permission = "";
			//updatePlaceHolders();
			return false;
		}

		if (message.contains(" ")) {
			Messenger.Message("A permission can't contain a space.", getViewer());
			return true;
		}
		permission = message;
		//updatePlaceHolders();
		return false;
	}

	private void switchMatchMeta() {
		final ItemMatchers.MatchType[] matchTypes = ItemMatchers.MatchType.values();
		int i;
		for (i = 0; i < matchTypes.length; i++) {
			if (matchTypes[i] == matchType) break;
		}
		if (i == matchTypes.length) {
			Debug.Send("couldn't find match type that's currently selected in the editor");
			return;
		}
		this.matchType = matchTypes[(i + 1) % matchTypes.length];
		//updatePlaceHolders();
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

}
