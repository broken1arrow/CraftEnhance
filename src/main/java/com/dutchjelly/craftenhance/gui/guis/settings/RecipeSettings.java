package com.dutchjelly.craftenhance.gui.guis.settings;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.guis.CategoryList;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import com.dutchjelly.craftenhance.util.PermissionTypes;
import lombok.Getter;
import lombok.NonNull;
import org.broken.arrow.menu.button.manager.library.utility.MenuButtonData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.broken.arrow.menu.library.MenuUtility;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.holder.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeSettings<RecipeT extends EnhancedRecipe> extends MenuHolder {

	protected final int page;
	private final MenuSettingsCache menuSettingsCache = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	@Getter
	private final RecipeT recipe;
	private final ButtonType editorType;
	private final CategoryData categoryData;
	private String permission;
	private ItemMatchers.MatchType recipeMatchType;

	public RecipeSettings(final RecipeT recipe, int pageNumber, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		page = pageNumber;
		if (permission == null || permission.equals("")) this.permission = recipe.getPermission();
		else this.permission = permission;
		this.editorType = editorType;
		this.recipe = recipe;
		this.categoryData = categoryData;
		this.recipeMatchType = recipe.getMatchType();
		String menu = "RecipeSettingsCrafting";
		if (recipe instanceof FurnaceRecipe) {
			menu = "RecipeSettingsFurnace";
		}
		if (recipe instanceof BrewingRecipe) {
			menu = "RecipeSettingsBrewing";
		}
		this.setUseColorConversion(true);
		this.setIgnoreItemCheck(true);

		menuTemplate = menuSettingsCache.getTemplate(menu);

		if (menuTemplate != null) {
			setMenuSize(GuiUtil.invSize("RecipeSettings", this.menuTemplate.getAmountOfButtons()));
			final String title = menuTemplate.getMenuTitle() == null ? "editor" : menuTemplate.getMenuTitle().replace(InfoItemPlaceHolders.Recipe_type.getPlaceHolder(), recipe.getType().name().toLowerCase());
			setTitle(() -> title);
			//setFillSpace(menuTemplate.getFillSlots());
			setMenuOpenSound(this.menuTemplate.getSound());
		}
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
				final Map<String, Object> placeHolders = setPlaceholders();

				ItemStack itemStack = getConversingItem(ButtonType.valueOfType(value.getActionType()));
				if (itemStack != null) return itemStack;

				org.broken.arrow.menu.button.manager.library.utility.MenuButton button = value.getPassiveButton();
				itemStack = Adapter.getItemStack(button.getMaterial(), button.getDisplayName(), button.getLore(), button.getExtra(), button.isGlow());
				if (itemStack == null)
					return null;

				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
		};
	}


	public boolean run(final MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {
		if (value.isActionTypeEqual(ButtonType.SetPosition.name())) {
			new HandleChatInput(this, this::handlePositionChange).setMessages("Type specify a page and slot number separated by a space or type q, exit or cancel to close conversion.").start(player);
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.AllowedWorldsCraft.name())) {
			if (player.isConversing()) return true;
			if (click.isRightClick() && click.isShiftClick()) {
				recipe.getAllowedWorlds().clear();
				return true;
			}
			new HandleChatInput(this, message -> {
				if (!this.handleSetWorld(message, click)) {
					this.runTask(() -> this.menuOpen(player));
					return false;
				}
				return true;
			}).setMessages("Type a valid world:")
					.start(player);
			return false;
		}


		if (value.isActionTypeEqual(ButtonType.SwitchShaped.name())) {
			if (recipe instanceof WBRecipe) {
				final WBRecipe wbRecipe = ((WBRecipe) this.recipe);
				final boolean shapeless = !wbRecipe.isShapeless();
				wbRecipe.setShapeless(shapeless);
			}
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.SwitchHidden.name())) {
			final boolean hidden = !this.recipe.isHidden();
			this.recipe.setHidden(hidden);
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.SwitchMatchMeta.name())) {
			switchMatchMeta();
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.SetPermission.name())) {
			if (player.isConversing()) return true;
			if (click.isRightClick()) {
				recipe.setPermission(null);
				return true;
			}
			new HandleChatInput(this, msg -> {
				if (!handlePermissionSetCB(msg)) {
					this.runTask(() -> this.menuOpen(player));
					return false;
				}
				return true;
			}).setMessages("Set your own permission on a recipe or type 'non' or 'null' to unset permission. Only players some has this permission can craft the item.", " Type q,exit,cancel to turn it off").start(getViewer());
			return true;
		}

		if (value.isActionTypeEqual(ButtonType.SetCommand.name())) {
			if (!player.hasPermission(PermissionTypes.Edit.getPerm())) {
				Debug.error("A player attempting to modify the command without correct permissions. The player:" + player.getName());
				return false;
			}
			if (player.isOp()) {
				Debug.error("OP mode introduces serious security risks by bypassing safety checks. Its use is strongly discouraged, " +
						"and future versions of this plugin will restrict it to prevent abuse — especially regarding set command in menu.");
			}

			if (click.isRightClick()) {
				recipe.setOnCraftCommand(null);
				return true;
			}

			if (player.isConversing()) return true;
			new HandleChatInput(this, msg -> {
				if (!handleCommand(msg)) {
					this.runTask(() -> this.menuOpen(player));
					return false;
				}
				return true;
			}).setMessages("Set a custom command to run when the player picks up the recipe result. Type 'none' or 'null' to clear the command. Type q, exit, or cancel to exit.").start(getViewer());
			return false;
		}

		if (value.isActionTypeEqual(ButtonType.ChangeCategoryList.name())) {
			new CategoryList<>(this.recipe, this.categoryData, this.permission, this.editorType, "").menuOpen(player);
		}

		if (value.isActionTypeEqual(ButtonType.ChangeCategory.name())) {
			if (player.isConversing()) return true;
			new HandleChatInput(this, msg -> {
				if (!GuiUtil.changeOrCreateCategory(msg, player, this.recipe)) {
					this.runTask(() -> this.menuOpen(player));
					return false;
				}
				return true;
			}).setMessages("Change category name and you can also change item (if not set it will use the old one). Like this 'category new_category_name crafting_table' without '. If you want create new category recomend use this format 'category crafting_table' without '", "Type q,exit,cancel to turn it off.").start(getViewer());
		}
		if (value.isActionTypeEqual(ButtonType.SetPartialMatch.name())) {
			final boolean partialMatch = !this.recipe.isCheckPartialMatch();
			this.recipe.setCheckPartialMatch(partialMatch);
			return true;
		}
		if (value.isActionTypeEqual(ButtonType.Back.name())) {
			if (this.recipe instanceof WBRecipe) {
				new RecipeEditor<>((WBRecipe) this.recipe, this.page, this.categoryData, null, ButtonType.ChooseWorkbenchType, false).menuOpen(player);
			}
			handleBack(this.recipe, categoryData, player);
		}
		return onPlayerClick(this.recipe, value.getActionType(), player);
	}

	@Override
	public void menuClose(final InventoryCloseEvent event, final MenuUtility menu) {
	}

	private boolean handlePermissionSetCB(String message) {
		if (message == null || message.trim().isEmpty()) return false;

		message = message.trim();

		final String messageLowerCase = message.toLowerCase();
		if (messageLowerCase.equals("q") || messageLowerCase.equals("cancel") || messageLowerCase.equals("quit") || messageLowerCase.equals("exit"))
			return false;

		if (message.equals("-")) {
			permission = "";
			return false;
		}

		if (message.contains(" ")) {
			Messenger.Message("A permission can't contain a space.", getViewer());
			return true;
		}
		final String permission = message;
		this.recipe.setPermission(permission);
		return false;
	}

	private boolean handleCommand(String message) {
		if (message == null || message.trim().isEmpty()) return false;

		message = message.trim();

		final String messageLowerCase = message.toLowerCase();
		if (messageLowerCase.equals("q") || messageLowerCase.equals("cancel") || messageLowerCase.equals("quit") || messageLowerCase.equals("exit"))
			return false;

		if (messageLowerCase.equals("non") || messageLowerCase.equals("null")) {
			this.recipe.setOnCraftCommand(null);
			return false;
		}

		final String permission = message;
		this.recipe.setOnCraftCommand(permission);
		return false;
	}

	private void switchMatchMeta() {
		final ItemMatchers.MatchType[] matchTypes = ItemMatchers.MatchType.values();
		final int index = this.recipeMatchType.ordinal();
		final ItemMatchers.MatchType matchType;
		if (index + 1 == matchTypes.length) {
			matchType = matchTypes[0];
		} else
			matchType = matchTypes[index + 1];
		this.recipeMatchType = matchType;
		this.recipe.setMatchType(matchType);
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
		this.runTask(() -> this.menuOpen(player));
		return false;
	}

	public boolean handleSetWorld(final String message, final ClickType click) {
		if (message == null || message.trim().equals("")) return false;

		if (message.equals("") || message.equalsIgnoreCase("q") || message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit"))
			return false;
		final World allowedWorlds = Bukkit.getWorld(message);

		if (!(click.isRightClick() || click.isShiftClick()) && allowedWorlds == null) {
			Messenger.Message("Please specify a world that exist. This world either don't exist or not loaded " + message, getViewer());
			return true;
		}
		Set<String> worlds = recipe.getAllowedWorlds();
		if (worlds == null) worlds = new HashSet<>();
		if (!click.isRightClick() && worlds.stream().anyMatch(world -> world.equals(message))) {
			Messenger.Message("This world is already set." + message, getViewer());
			return true;
		}
		if (click.isLeftClick()) worlds.add(message);
		else worlds.remove(message);

		recipe.setAllowedWorlds(worlds);
		if (click.isLeftClick())
			Messenger.Message("You have now set this world " + message + ", and players can only make this recipe in this world.", getViewer());
		else
			Messenger.Message("You have now remove this world " + message + ", and players can't make this recipe in this world. Unless no world is set after you remove this world.", getViewer());

		return false;
	}

	public ItemStack getConversingItem(final ButtonType buttonType) {
		if (!player.isConversing()) return null;
		final ItemStack itemStack = new ItemStack(Material.BARRIER);
		final ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta != null) {
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4You can't set this right now."));
			final List<String> lore = new ArrayList<>();
			lore.add(ChatColor.translateAlternateColorCodes('&', "&4"));
			lore.add(ChatColor.translateAlternateColorCodes('&', "&4You can't set this so long"));
			lore.add(ChatColor.translateAlternateColorCodes('&', "&4you are conversing."));
			lore.add(ChatColor.translateAlternateColorCodes('&', "&4"));
			lore.add(ChatColor.translateAlternateColorCodes('&', "&fYou have to type quit,exit,q"));
			lore.add(ChatColor.translateAlternateColorCodes('&', "&fin the chat and enter."));
			itemMeta.setLore(lore);
		}
		itemStack.setItemMeta(itemMeta);
		if (buttonType != null)
			switch (buttonType) {
				case SetPosition:
				case AllowedWorldsCraft:
				case SetCookTime:
				case SetExp:
				case SwitchMatchMeta:
				case SetPermission:
				case ChangeCategory:
					return itemStack;
				default:
					return null;
			}
		return null;
	}

	public Map<String, Object> setPlaceholders() {
		Map<String, Object> placeholders = new HashMap<String, Object>() {{
			final CraftEnhance self = self();
			final String permission = recipe.getPermission();
			final String recipeCraftCommand = recipe.getOnCraftCommand();
			final MatchType matchType = recipe.getMatchType();

			put(InfoItemPlaceHolders.Key.getPlaceHolder(), recipe.getKey() == null ? "null" : recipe.getKey());
			if (recipe instanceof WBRecipe)
				put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), ((WBRecipe) recipe).isShapeless() ? self.getText("shapeless_recipe") : self.getText("shaped_recipe"));


			put(InfoItemPlaceHolders.MatchMeta.getPlaceHolder(), matchType.getMatchName());
			put(InfoItemPlaceHolders.MatchDescription.getPlaceHolder(), matchType.getMatchDescription());

			put(InfoItemPlaceHolders.Hidden.getPlaceHolder(), recipe.isHidden() ? self.getText("recipe_hidden") : self.getText("recipe_not_hidden"));

			put(InfoItemPlaceHolders.Permission.getPlaceHolder(), permission == null || permission.trim().equals("") ? self.getText("permission_non_set") : permission);
			put(InfoItemPlaceHolders.RecipeCommand.getPlaceHolder(), recipeCraftCommand == null || recipeCraftCommand.trim().isEmpty() ? self.getText("craft_command_non_set") : recipeCraftCommand);

			put(InfoItemPlaceHolders.Slot.getPlaceHolder(), String.valueOf(recipe.getSlot()));
			put(InfoItemPlaceHolders.Page.getPlaceHolder(), String.valueOf(recipe.getPage()));
			put(InfoItemPlaceHolders.Partial_match.getPlaceHolder(), recipe.isCheckPartialMatch() ? "checks for partial match" : "doesn't check for partial match");
			put(InfoItemPlaceHolders.Worlds.getPlaceHolder(), recipe.getAllowedWorlds() != null && !recipe.getAllowedWorlds().isEmpty() ?
					recipe.getAllowedWorldsFormatted() : "non set");
			if (categoryData != null)
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), categoryData.getRecipeCategory());
			else
				put(InfoItemPlaceHolders.Category.getPlaceHolder(), recipe.getRecipeCategory() != null ? recipe.getRecipeCategory() : "default");

		}};
		Map<String, String> extraPlaceholders = recipePlaceholders(recipe);
		if (extraPlaceholders != null && !extraPlaceholders.isEmpty())
			placeholders.putAll(extraPlaceholders);

		return placeholders;
	}

	protected void runTask(final Runnable runnable) {
		CraftEnhance.runTaskLater(1, runnable);
	}

	protected boolean onPlayerClick(final RecipeT recipe, final String buttonAction, final Player player) {
		return false;
	}

	protected void handleBack(final RecipeT recipe, final CategoryData categoryData, final Player player) {
	}

	protected Map<String, String> recipePlaceholders(final RecipeT recipe) {
		return null;
	}
}
