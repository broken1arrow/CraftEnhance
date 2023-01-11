package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.CategoryList;
import com.dutchjelly.craftenhance.gui.guis.EditorTypeSelector;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewer;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import lombok.Getter;
import org.brokenarrow.menu.library.CheckItemsInsideInventory;
import org.brokenarrow.menu.library.CreateMenus;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
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
		super( formatRecipes(recipe));
		if (permission == null || permission.equals(""))
			this.permission = recipe.getPermissions();
		else this.permission = permission;
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
		if (menuTemplate != null) {
			setMenuSize(GuiUtil.invSize("RecipeEditor",this.menuTemplate.getAmountOfButtons()));
			setTitle(menuTemplate.getMenuTitel());
			setFillSpace(menuTemplate.getFillSlots());
			setMenuOpenSound(this.menuTemplate.getSound());
		}
	}

	@Override
	public MenuButton getFillButtonAt(final Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final Player player, final Inventory inventory, final ClickType clickType, final ItemStack itemStack, final Object o) {

			}

			@Override
			public ItemStack getItem(final Object object) {
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
			public void onClickInsideMenu(final Player player, final Inventory menu, final ClickType click, final ItemStack clickedItem, final Object object) {
				if (run(value, menu, player, click)) {
					RecipeEditor.super.updateButton(this);
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
					if (categoryData != null)
						put(InfoItemPlaceHolders.Category.getPlaceHolder(), categoryData.getRecipeCategory());
					else
						put(InfoItemPlaceHolders.Category.getPlaceHolder(), recipe.getRecipeCategory() != null? recipe.getRecipeCategory(): "default");
				}};
				if (value.getItemStack() == null) return null;
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}
	public boolean run(final com.dutchjelly.craftenhance.gui.templates.MenuButton value, final Inventory menu, final Player player, final ClickType click) {
		if (value.getButtonType() == ButtonType.SetPosition){
			self().getGuiManager().waitForChatInput(this, player, this::handlePositionChange);
			return true;
		}
		if (value.getButtonType() == ButtonType.SetCookTime){
			Messenger.Message("Please input a cook duration.Type q, exit, cancel to turn it off.", getViewer());
			self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
				short parsed;
				if (msg.equals("cancel")||msg.equals("quit") ||msg.equals("exit"))
					return false;
				try{
					parsed = Short.parseShort(msg);
				}catch(final NumberFormatException e){
					Messenger.Message("Error, you didn't input a number. your input " + msg, getViewer());
					return true;
				}
				if(parsed < 0) parsed = 0;
				Messenger.Message("Successfully set duration to " + parsed, getViewer());
				this.duration = parsed;
				final CheckItemsInsideInventory checkItemsInsideInventory = new CheckItemsInsideInventory();
				checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
				final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsOnSpecifiedSlots( menu, player,false);
				getIngredients(map, player);
				this.menuOpen(player);
				//new RecipeEditor<>(this.recipe, this.categoryData,this.permission,this.editorType).menuOpen(player);
				return false;
			});
			return true;
		}
		if (value.getButtonType() == ButtonType.SetExp){
			Messenger.Message("Please input an exp amount.Type q, exit, cancel to turn it off.", getViewer());
			self().getGuiManager().waitForChatInput(this, getViewer(), (msg) -> {
				int parsed;
				if (msg.equals("cancel")||msg.equals("quit") ||msg.equals("exit"))
					return false;
				try{
					parsed = Integer.parseInt(msg);
				}catch(final NumberFormatException e){
					Messenger.Message("Error, you didn't input a number. your input " +msg, getViewer());
					return true;
				}
				if(parsed < 0) parsed = 0;
				Messenger.Message("Successfully set exp to " + parsed, getViewer());
				exp = parsed;
				this.menuOpen(player);
				//new RecipeEditor<>(this.recipe, this.categoryData,this.permission,this.editorType).menuOpen(player);
				return false;
			});
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchShaped){
			this.shapeless = !this.shapeless;

			//this.menuOpen(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.DeleteRecipe){
			self().getFm().removeRecipe(recipe);
			RecipeLoader.getInstance().unloadRecipe(recipe);
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchHidden){
			this.hidden = !this.hidden;
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchMatchMeta){
			switchMatchMeta();
			return true;
		}
		if (value.getButtonType() == ButtonType.ResetRecipe){
			updateRecipeDisplay(menu);
			return true;

		}
		if (value.getButtonType() == ButtonType.SetPermission){
			Messenger.Message("Set your own permission on a recipe.Only players some has this permission can craft the item.Type q,exit,cancel to turn it off", getViewer());
			self().getGuiManager().waitForChatInput(this, player, (message) ->{
				if (!handlePermissionSetCB(message)) {
					this.menuOpen(player);
					return false;
				}
				return true;
			});
			return true;
		}
		if (value.getButtonType() == ButtonType.SaveRecipe){
			final CheckItemsInsideInventory checkItemsInsideInventory = new CheckItemsInsideInventory();
			checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
			final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsOnSpecifiedSlots( menu, player,false);
			save( map, player,true);
			new RecipeEditor<>(this.recipe, this.categoryData,this.permission,this.editorType).menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.Back){
			if (this.categoryData != null)
				new RecipesViewer(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.ChangeCategoryList) {
			new CategoryList<>( this.recipe, this.categoryData, this.permission,  this.editorType, "").menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.ChangeCategory) {
			Messenger.Message("Change category name and you can also change item (if not set it will use the old one). Like this 'category new_category_name crafting_table' without '. If you want create new category recomend use this format 'category crafting_table' without '", getViewer());
			Messenger.Message("Type q,exit,cancel to turn it off.", getViewer());
			self().getGuiManager().waitForChatInput(this, getViewer(), msg->{
				if (!GuiUtil.changeOrCreateCategory(msg, player)){
					new RecipeEditor<>(this.recipe,this.categoryData,this.permission,this.editorType);
					return false;
				}
				return true;
			});
		}
		return false;
	}

	@Override
	public void menuClose(final InventoryCloseEvent event, final CreateMenus menu) {
		if (self().getConfig().getBoolean("save_on_close")){
			final CheckItemsInsideInventory checkItemsInsideInventory = new CheckItemsInsideInventory();
			checkItemsInsideInventory.setSlotsToCheck(menuTemplate.getFillSlots());
			final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsOnSpecifiedSlots( event.getInventory(),getViewer(),false);
			save( map, getViewer(),true);
		}
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
		final ItemStack[] newContents = getIngredients(map,player);
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
		if (recipe instanceof WBRecipe){
			((WBRecipe)recipe).setShapeless(shapeless);
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
			final ItemStack itemStack =  map.get(slot );
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

		if (message.equalsIgnoreCase("q")|| message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit")) return false;

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

		if (message.equals("") || message.equalsIgnoreCase("q")|| message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit")) return false;

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
