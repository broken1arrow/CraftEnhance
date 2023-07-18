package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.getRecipes;

public class RecipeDisabler extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;
	private final MenuButton fillSlots;
	//If true, you can enable *disabled* recipes.
	boolean enableMode;

	public RecipeDisabler(final List<Recipe> enabledRecipes, final List<Recipe> disabledRecipes, final boolean enableMode, final String recipesSeachFor) {
		super(getRecipes( enabledRecipes,disabledRecipes, enableMode,recipesSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplates().get("RecipeDisabler");
        this.enableMode = enableMode;
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitel());
		setMenuSize(GuiUtil.invSize("RecipeDisabler",this.menuTemplate.getAmountOfButtons()));
		setMenuOpenSound(this.menuTemplate.getSound());

		fillSlots = new MenuButton() {
			@Override
			public void onClickInsideMenu(final Player player, final Inventory inventory, final ClickType clickType, final ItemStack itemStack, final Object o) {
				if (o instanceof Recipe) {
					if (enableMode) {
						if (RecipeLoader.getInstance().enableServerRecipe((Recipe) o)) {
							//enabledRecipes.remove( o);
//               getRecipes().remove(recipe);
							updateButtons();
						}
					} else {
						if (RecipeLoader.getInstance().disableServerRecipe((Recipe) o)) {
							//disabledRecipes.remove( o);
//               getRecipes().remove(recipe);
							updateButtons();
						}
					}
				}
			}

			@Override
			public ItemStack getItem(final Object object) {
				if (object instanceof Recipe) {
					ItemStack result = ((Recipe)object).getResult();
					if(GuiUtil.isNull(result)) {
						result = new ItemStack(Material.BARRIER);
						final ItemMeta meta = result.getItemMeta();
						meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Complex Recipe: " + Adapter.GetRecipeIdentifier(((Recipe)object))));
						meta.setLore(Arrays.asList("&eWARN: &fThis recipe is complex, which", "&f means that the result is only known", " &f&oafter&r&f the content of the crafting table is sent", " &fto the server. Think of repairing or coloring recipes.", " &f&nSo disabling is not recommended!"));
						meta.setLore(meta.getLore().stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
						result.setItemMeta(meta);
					} else{
						final ItemMeta meta = result.getItemMeta();
						meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&3key: &f" + Adapter.GetRecipeIdentifier(((Recipe)object)))));
						result.setItemMeta(meta);
					}
					return result;
				}
				return null;
			}

			@Override
			public ItemStack getItem() {
				return null;
			}
		};
	}
	@Override
	public MenuButton getFillButtonAt(final Object object) {
		return fillSlots;
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
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				final Map<String, String> placeHolders = new HashMap<String,String>(){{
					put(InfoItemPlaceHolders.DisableMode.getPlaceHolder(), enableMode ? "enable recipes by clicking them" : "disable recipes by clicking them");
				}};
				if (value.getItemStack() == null)
					return null;
				return GuiUtil.ReplaceAllPlaceHolders(value.getItemStack().clone(), placeHolders);
			}
		};
	}
	public boolean run(final com.dutchjelly.craftenhance.gui.templates.MenuButton value, final Inventory menu, final Player player, final ClickType click) {
		if (value.getButtonType() == ButtonType.PrvPage){
			previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage){
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.SwitchDisablerMode){
			this.enableMode = !this.enableMode;
			new RecipeDisabler(RecipeLoader.getInstance().getServerRecipes(),RecipeLoader.getInstance().getDisabledServerRecipes(),this.enableMode,"").menuOpen(player);
			return true;
		}
		if (value.getButtonType() == ButtonType.Search) {
			if (click == ClickType.RIGHT) {
				new HandleChatInput(this, msg-> {
					if (GuiUtil.seachCategory(msg)) {
						new RecipeDisabler(RecipeLoader.getInstance().getServerRecipes(), RecipeLoader.getInstance().getDisabledServerRecipes(), this.enableMode, msg).menuOpen(getViewer());
						return false;
					}
					return true;
				}).setMessages("Search for recipe items")
						.start(getViewer());
		/*		Messenger.Message("Search for recipe items", player);
				self().getGuiManager().waitForChatInput(this, getViewer(), msg -> {
					if (GuiUtil.seachCategory(msg)) {
						new RecipeDisabler(RecipeLoader.getInstance().getServerRecipes(), RecipeLoader.getInstance().getDisabledServerRecipes(), this.enableMode, msg).menuOpen(getViewer());
						return false;
					}
					return true;
				});*/
			}
			else new RecipeDisabler(RecipeLoader.getInstance().getServerRecipes(),RecipeLoader.getInstance().getDisabledServerRecipes(), this.enableMode, "").menuOpen(player);
		}
		return false;
	}


}
