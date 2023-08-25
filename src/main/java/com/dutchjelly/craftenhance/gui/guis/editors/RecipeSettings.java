package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.CategoryList;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.brokenarrow.menu.library.MenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeSettings<RecipeT extends EnhancedRecipe> extends MenuHolder {

	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private String permission;
	private final MenuTemplate menuTemplate;
	private final RecipeT recipe;
	private final ItemMatchers.MatchType recipeMatchType;
	private final ButtonType editorType;
	private final CategoryData categoryData;

	public RecipeSettings(final RecipeT recipe, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		if (permission == null || permission.equals("")) this.permission = recipe.getPermissions();
		else this.permission = permission;
		this.editorType = editorType;
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.recipeMatchType = recipe.getMatchType();
		String menu = "RecipeSettingsCrafting";
		if (recipe instanceof FurnaceRecipe) {
			menu = "RecipeSettingsFurnace";
		}

		menuTemplate = menuSettingsCache.getTemplates().get(menu);
		setSlotsYouCanAddItems(true);
		if (menuTemplate != null) {
			setMenuSize(GuiUtil.invSize("RecipeSettings", this.menuTemplate.getAmountOfButtons()));
			setTitle(menuTemplate.getMenuTitel());
			//setFillSpace(menuTemplate.getFillSlots());
			setMenuOpenSound(this.menuTemplate.getSound());
		}
	}

	@Override
	public MenuButton getFillButtonAt(final @NotNull Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(final @NotNull Player player, final @NotNull Inventory inventory, final @NotNull ClickType clickType, final @NotNull ItemStack itemStack, final Object o) {

			}

			@Override
			public ItemStack getItem(final @NotNull Object object) {
				if (object instanceof ItemStack) return (ItemStack) object;
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
			public void onClickInsideMenu(final @NotNull Player player, final @NotNull Inventory menu, final @NotNull ClickType click, final @NotNull ItemStack clickedItem, final Object object) {
				if (run(value, menu, player, click)) {
					updateButton(this);
					//updateButtons();
				}
			}

			@Override
			public ItemStack getItem() {
				final Map<String, String> placeHolders = setPlaceholders();
				if (value.getItemStack() == null) return null;
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}


	public boolean run(final com.dutchjelly.craftenhance.gui.templates.MenuButton value, final Inventory menu, final Player player, final ClickType click) {
		if (value.getButtonType() == ButtonType.SetPosition) {
			new HandleChatInput(this, this::handlePositionChange);
			return true;
		}
		if (value.getButtonType() == ButtonType.AllowedWorldsCraft) {
			new HandleChatInput(this, message -> {
				if (!this.handleSetWorld(message, click)) this.menuOpen(player);
				return true;
			}).setMessages("Type a valid world:")
					.start(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.SetCookTime) {
			new HandleChatInput(this, (msg) -> {
				short parsed;
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit") || msg.equals("q")) {
					this.menuOpen(player);
					return false;
				}
				try {
					parsed = Short.parseShort(msg);
				} catch (final NumberFormatException e) {
					Messenger.Message("Error, you didn't input a number. your input " + msg, getViewer());
					return true;
				}
				if (parsed < 0) parsed = 0;
				Messenger.Message("Successfully set duration to " + parsed, getViewer());
				if (recipe instanceof FurnaceRecipe) {
					((FurnaceRecipe) recipe).setDuration(parsed);
				}
				this.menuOpen(player);
				//new RecipeEditor<>(this.recipe, this.categoryData,this.permission,this.editorType).menuOpen(player);
				return false;
			}).setMessages("Please input a cook duration.Type q, exit, cancel to turn it off.").start(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.SetExp) {
			new HandleChatInput(this, msg -> {
				int parsed;
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit")) {
					this.menuOpen(player);
					return false;
				}
				try {
					parsed = Integer.parseInt(msg);
				} catch (final NumberFormatException e) {
					Messenger.Message("Error, you didn't input a number. your input " + msg, getViewer());
					return true;
				}
				if (parsed < 0) parsed = 0;
				Messenger.Message("Successfully set exp to " + parsed, getViewer());
				if (recipe instanceof FurnaceRecipe) {
					((FurnaceRecipe) recipe).setExp(parsed);
				}
				this.menuOpen(player);
				//new RecipeEditor<>(this.recipe, this.categoryData,this.permission,this.editorType).menuOpen(player);
				return false;
			}).setMessages("Please input an exp amount.Type q, exit, cancel to turn it off.").start(getViewer());
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchShaped) {
			if (recipe instanceof WBRecipe) {
				final WBRecipe wbRecipe = ((WBRecipe) this.recipe);
				final boolean shapeless = !wbRecipe.isShapeless();
				wbRecipe.setShapeless(shapeless);
			}
			//this.menuOpen(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchHidden) {
			final boolean hidden = !this.recipe.isHidden();
			this.recipe.setHidden(hidden);
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchMatchMeta) {
			switchMatchMeta();
			return true;
		}
		if (value.getButtonType() == ButtonType.SetPermission) {
			new HandleChatInput(this, msg -> {
				if (!handlePermissionSetCB(msg)) {
					this.menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Set your own permission on a recipe. Only players some has this permission can craft the item.", " Type q,exit,cancel to turn it off").start(getViewer());
			return true;
		}
		if (value.getButtonType() == ButtonType.ChangeCategoryList) {
			new CategoryList<>(this.recipe, this.categoryData, this.permission, this.editorType, "").menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.ChangeCategory) {
			new HandleChatInput(this, msg -> {
				if (!GuiUtil.changeOrCreateCategory(msg, player)) {
					new RecipeSettings<>(this.recipe, this.categoryData, this.permission, this.editorType).menuOpen(player);
					return false;
				}
				return true;
			}).setMessages("Change category name and you can also change item (if not set it will use the old one). Like this 'category new_category_name crafting_table' without '. If you want create new category recomend use this format 'category crafting_table' without '", "Type q,exit,cancel to turn it off.").start(getViewer());
		}

		if (value.getButtonType() == ButtonType.Back) {
			new RecipeEditor<>(this.recipe, this.categoryData, this.permission, this.editorType, false).menuOpen(player);
		}
		return false;
	}

	@Override
	public void menuClose(final InventoryCloseEvent event, final MenuUtility menu) {
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
		final String permission = message;
		this.recipe.setPermissions(permission);
		//updatePlaceHolders();
		return false;
	}

	private void switchMatchMeta() {
		final ItemMatchers.MatchType[] matchTypes = ItemMatchers.MatchType.values();
		int i;
		for (i = 0; i < matchTypes.length; i++) {
			if (matchTypes[i] == this.recipeMatchType) break;
		}
		if (i == matchTypes.length) {
			Debug.Send("couldn't find match type that's currently selected in the editor");
			return;
		}
		final ItemMatchers.MatchType matchType = matchTypes[(i + 1) % matchTypes.length];
		this.recipe.setMatchType(matchType);
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

	public boolean handleSetWorld(final String message, final ClickType click) {
		if (message == null || message.trim().equals("")) return false;

		if (message.equals("") || message.equalsIgnoreCase("q") || message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit"))
			return false;
		final World allowedWorlds = Bukkit.getWorld(message);

		if (allowedWorlds == null) {
			Messenger.Message("Please specify a world that exist. This world either don't exist or not loaded " + message, getViewer());
			return true;
		}
		Set<World> worlds = recipe.getAllowedWorlds();
		if (worlds == null) worlds = new HashSet<>();
		if (worlds.stream().anyMatch(world -> world.getName().equals(allowedWorlds.getName()))) {
			Messenger.Message("This world is alredy set." + message, getViewer());
			return true;
		}
		if (click.isLeftClick()) worlds.add(allowedWorlds);
		else worlds.remove(allowedWorlds);

		recipe.setAllowedWorlds(worlds);

		Messenger.Message("You have now set this world " + message + ", and players can only make this recipe in this world.", getViewer());
		self().getFm().saveRecipe(recipe);

		//updatePlaceHolders();
		return false;
	}

	public Map<String, String> setPlaceholders() {
		return new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Key.getPlaceHolder(), recipe.getKey() == null ? "null" : recipe.getKey());
			if (recipe instanceof WBRecipe)
				put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), ((WBRecipe) recipe).isShapeless() ? "shapeless" : "shaped");
			if (recipe instanceof FurnaceRecipe) {
				put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(((FurnaceRecipe) recipe).getExp()));
				put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(((FurnaceRecipe) recipe).getDuration()));
			}
			final String permission = recipe.getPermissions();
			put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), recipe.getMatchType().getDescription());
			put(InfoItemPlaceHolders.MatchType.getPlaceHolder(), recipe.getMatchType().getDescription());
			put(InfoItemPlaceHolders.Hidden.getPlaceHolder(), recipe.isHidden() ? "hide recipe in menu" : "show recipe in menu");
			put(InfoItemPlaceHolders.Permission.getPlaceHolder(), permission == null || permission.trim().equals("") ? "none" : permission);
			put(InfoItemPlaceHolders.Slot.getPlaceHolder(), String.valueOf(recipe.getSlot()));
			put(InfoItemPlaceHolders.Page.getPlaceHolder(), String.valueOf(recipe.getPage()));
			put(InfoItemPlaceHolders.Worlds.getPlaceHolder(), String.valueOf(recipe.getAllowedWorlds() != null && !recipe.getAllowedWorlds().isEmpty()  ?
					recipe.getAllowedWorlds().stream().map(world-> world.getName()).collect(Collectors.toList()) : "non set"));
			if (categoryData != null)
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), categoryData.getRecipeCategory());
			else
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), recipe.getRecipeCategory() != null ? recipe.getRecipeCategory() : "default");
		}};
	}
}
