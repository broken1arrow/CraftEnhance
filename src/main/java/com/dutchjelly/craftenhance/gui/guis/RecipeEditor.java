package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import lombok.Getter;
import org.brokenarrow.menu.library.CheckItemsInsideInventory;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
	private CategoryData categoryData;

	public RecipeEditor(RecipeT recipe,CategoryData categoryData, String permission, ButtonType editorType) {
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
			setTitle(menuTemplate.getMenuTitel());
			setFillSpace(menuTemplate.getFillSlots());
		}
	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {

			}

			@Override
			public ItemStack getItem(Object object) {
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
	public MenuButton getButtonAt(int slot) {
		if (this.menuTemplate == null) return null;
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				Map<String, String> placeHolders = new HashMap<String, String>() {{
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
						put(InfoItemPlaceHolders.Category.getPlaceHolder(), recipe.getRecipeCategory());
				}};
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}
	public boolean run(com.dutchjelly.craftenhance.gui.templates.MenuButton value, Inventory menu, Player player, ClickType click) {
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
				}catch(NumberFormatException e){
					Messenger.Message("Error, you didn't input a number. your input " + msg, getViewer());
					return true;
				}
				if(parsed < 0) parsed = 0;
				Messenger.Message("Successfully set duration to " + parsed, getViewer());
				this.duration = parsed;
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
				}catch(NumberFormatException e){
					Messenger.Message("Error, you didn't input a number. your input " +msg, getViewer());
					return true;
				}
				if(parsed < 0) parsed = 0;
				Messenger.Message("Successfully set exp to " + parsed, getViewer());
				exp = parsed;
				return false;
			});
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchShaped){
			this.shapeless = !this.shapeless;
			return true;
		}
		if (value.getButtonType() == ButtonType.DeleteRecipe){
			self().getFm().removeRecipe(recipe);
			RecipeLoader.getInstance().unloadRecipe(recipe);
			new EditorTypeSelector( null, permission).menuOpen(player);
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
			self().getGuiManager().waitForChatInput(this, player, this::handlePermissionSetCB);
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
				new RecipesViewerCopy(this.categoryData, "", player).menuOpen(player);
			else
				new EditorTypeSelector(null, permission).menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.changeCategoryList) {
			new CategoryList<>( this.recipe, this.categoryData, this.permission,  this.editorType, "").menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.changeCategory) {
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
	private void updateRecipeDisplay(Inventory menu) {
		List<Integer> fillSpace = this.menuTemplate.getFillSlots();
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

	private void save(final Map<Integer, ItemStack> map,Player player, boolean loadRecipe) {
		ItemStack[] newContents = getIngredients(map,player);
		if (newContents == null) {
			Messenger.Message("The recipe is empty.", player);
			return;
		}
		ItemStack newResult = getResult();
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
		}else
			Messenger.Message("Has not reload this recipe, click on save to reload the recipe or /ceh reload", player);
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
	private ItemStack[] getIngredients(Map<Integer, ItemStack> map,Player player) {

		int resultSlot = this.menuTemplate.getFillSlots().get(recipe.getContent().length);
		List<ItemStack> arrays = new ArrayList<>(recipe.getContent().length);
		int index = 0;
		for (Integer slot : this.menuTemplate.getFillSlots()) {
			ItemStack itemStack =  map.get(slot );
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
		return arrays.toArray(new ItemStack[9]);
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
		ItemMatchers.MatchType[] matchTypes = ItemMatchers.MatchType.values();
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
	public boolean handlePositionChange(String message) {
		if (message == null || message.trim() == "") return false;

		if (message.equals("") || message.equalsIgnoreCase("q")|| message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit")) return false;

		String[] args = message.split(" ");

		if (args.length != 2) {
			Messenger.Message("Please specify a page and slot number separated by a space.", getViewer());
			return true;
		}
		int page = 0, slot = 0;
		try {
			page = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			Messenger.Message("Could not parse the page number.", getViewer());
			return true;
		}

		try {
			slot = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
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
