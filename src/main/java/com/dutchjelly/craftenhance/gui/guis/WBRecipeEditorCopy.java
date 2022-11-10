package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.templates.MenuSettingsCache;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class WBRecipeEditorCopy<RecipeT extends EnhancedRecipe> extends MenuHolder {

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

	public  WBRecipeEditorCopy(RecipeT recipe,String permission) {
		super(Arrays.asList(recipe.getContent()));
		if (permission == null || permission.equals(""))
			this.permission = recipe.getPermissions();
		else this.permission = permission;
		this.recipe = recipe;
		if (recipe instanceof WBRecipe)
			shapeless = ((WBRecipe) this.recipe).isShapeless();
		menuTemplate = menuSettingsCache.getTemplates().get("WBRecipeEditor");
		setMenuSize(36);
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
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButton().entrySet()){
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
					put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), matchType.getDescription());
					put(InfoItemPlaceHolders.MatchType.getPlaceHolder(), matchType.getDescription());
					put(InfoItemPlaceHolders.Hidden.getPlaceHolder(), hidden ? "hide recipe in menu" : "show recipe in menu");
					put(InfoItemPlaceHolders.Permission.getPlaceHolder(), permission == null || permission.trim().equals("") ? "none" : permission);
					put(InfoItemPlaceHolders.Slot.getPlaceHolder(), String.valueOf(recipe.getSlot()));
					put(InfoItemPlaceHolders.Page.getPlaceHolder(), String.valueOf(recipe.getPage()));
				}};
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}
	public boolean run(com.dutchjelly.craftenhance.gui.templates.MenuButton value, Inventory menu, Player player, ClickType click) {
		if (value.getButtonType() == ButtonType.SetPosition){
			self().getGuiManager().waitForChatInput(null, player, this::handlePositionChange);
	/*				WBRecipeEditor gui = new WBRecipeEditor(self().getGuiManager(), self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), null, player, newRecipe);
					manager.openGUI(player, gui);*/
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchShaped){
			this.shapeless = !this.shapeless;
			return true;
		}
		if (value.getButtonType() == ButtonType.DeleteRecipe){
			self().getFm().removeRecipe(recipe);
			RecipeLoader.getInstance().unloadRecipe(recipe);
			new EditorTypeSelectorCopy( null, permission).menuOpen(player);
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
			self().getGuiManager().waitForChatInput(null, player, this::handlePermissionSetCB);
			return true;
		}
		if (value.getButtonType() == ButtonType.SaveRecipe){
			final CheckItemsInsideInventory checkItemsInsideInventory = new CheckItemsInsideInventory();
			final Map<Integer, ItemStack> map = checkItemsInsideInventory.getItemsExceptBottomBar( menu, player,false);
			save( map, player,true);
			return true;
		}
		if (value.getButtonType() == ButtonType.Back){
			new EditorTypeSelectorCopy( null, permission).menuOpen(player);
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
	}
	@Nullable
	private ItemStack[] getIngredients(Map<Integer, ItemStack> map,Player player) {
		int result = -1;
		System.out.println("recipe.getContent().length " + recipe.getContent().length);
		int resultSlot = this.menuTemplate.getFillSlots().get(recipe.getContent().length);
		System.out.println("recipe.getContent().length " + resultSlot);
		for (Entry<Integer, ItemStack> itemStackEntry : map.entrySet()){
				result = itemStackEntry.getKey();
				if (itemStackEntry.getValue().getAmount() > 1 && result != resultSlot) {
					Messenger.Message("Recipes only support amounts of 1 in the content.", player);
					itemStackEntry.getValue().setAmount(1);
				}
		}

		if (result >= 0 && result == resultSlot)
			 this.result = map.remove(result);

		if (!map.values().stream().anyMatch(x -> x != null)) {
			return null;
		}
		return map.values().toArray(new ItemStack[map.values().size() + 1]);
	}

	private boolean handlePermissionSetCB(String message) {
		if (message == null || message.trim().equals("")) return false;

		message = message.trim();

		if (message.toLowerCase().equals("q")) return false;

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

		if (message.toLowerCase().equals("q")) return false;

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
