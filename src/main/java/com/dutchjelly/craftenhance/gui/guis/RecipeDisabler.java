package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.broken.arrow.menu.button.manager.library.utility.MenuButtonData;
import org.broken.arrow.menu.button.manager.library.utility.MenuTemplate;
import org.broken.arrow.menu.library.button.MenuButton;
import org.broken.arrow.menu.library.button.logic.ButtonUpdateAction;
import org.broken.arrow.menu.library.button.logic.FillMenuButton;
import org.broken.arrow.menu.library.holder.MenuHolderPage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.getRecipes;

public class RecipeDisabler extends MenuHolderPage<Recipe> {
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;

	//If true, you can enable *disabled* recipes.
	boolean enableMode;

	public RecipeDisabler(final List<Recipe> enabledRecipes, final List<Recipe> disabledRecipes, final boolean enableMode, final String recipesSeachFor) {
		super(getRecipes( enabledRecipes,disabledRecipes, enableMode,recipesSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplate("RecipeDisabler");
        this.enableMode = enableMode;
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitle());
		setMenuSize(GuiUtil.invSize("RecipeDisabler",this.menuTemplate.getAmountOfButtons()));
		setMenuOpenSound(this.menuTemplate.getSound());
		this.setUseColorConversion(true);

	}

	@Override
	public MenuButton getButtonAt(final int slot) {
		if (this.menuTemplate == null) return null;
		for (final Entry<List<Integer>, MenuButtonData> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
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
				final Map<String, String> placeHolders = new HashMap<String,String>(){{
					put(InfoItemPlaceHolders.DisableMode.getPlaceHolder(), enableMode ? "enable recipes by clicking them" : "disable recipes by clicking them");
				}};
				org.broken.arrow.menu.button.manager.library.utility.MenuButton button = null;
				if (enableMode)
					button = value.getActiveButton();
				if (button == null)
					button = value.getPassiveButton();
				ItemStack itemStack = Adapter.getItemStack(button.getMaterial(),button.getDisplayName(),button.getLore(),button.getExtra(),button.isGlow());
				if (itemStack == null)
					return null;

				return GuiUtil.ReplaceAllPlaceHolders(itemStack.clone(), placeHolders);
			}
		};
	}
	public boolean run(final  MenuButtonData value, final Inventory menu, final Player player, final ClickType click) {
		if (value.isActionTypeEqual( ButtonType.PrvPage.name())){
			previousPage();
			return true;
		}
		if (value.isActionTypeEqual(  ButtonType.NxtPage.name())){
			nextPage();
			return true;
		}
		if (value.isActionTypeEqual(  ButtonType.SwitchDisablerMode.name())){
			this.enableMode = !this.enableMode;
			new RecipeDisabler(RecipeLoader.getInstance().getServerRecipes(),RecipeLoader.getInstance().getDisabledServerRecipes(),this.enableMode,"").menuOpen(player);
			return true;
		}
		if (value.isActionTypeEqual(  ButtonType.Search.name())) {
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


	@Override
	public FillMenuButton<Recipe> createFillMenuButton() {
		return new FillMenuButton<>((player1, itemStacks, clickType, itemStack, recipe) -> {
			if (recipe != null) {
				if (enableMode) {
					if (RecipeLoader.getInstance().enableServerRecipe(recipe)) {
						//enabledRecipes.remove( o);
//               getRecipes().remove(recipe);
						return ButtonUpdateAction.ALL;
					}
				} else {
					if (RecipeLoader.getInstance().disableServerRecipe(recipe)) {
						//disabledRecipes.remove( o);
//               getRecipes().remove(recipe);
						return ButtonUpdateAction.ALL;
					}
				}
			}
			return ButtonUpdateAction.NONE;
		},
				(slot, recipe) -> {
					if (recipe != null) {
						ItemStack result = recipe.getResult();
						if(GuiUtil.isNull(result)) {
							result = new ItemStack(Material.BARRIER);
							final ItemMeta meta = result.getItemMeta();
							meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Complex Recipe: " + Adapter.GetRecipeIdentifier(((Recipe)recipe))));
							meta.setLore(Arrays.asList("&eWARN: &fThis recipe is complex, which", "&f means that the result is only known", " &f&oafter&r&f the content of the crafting table is sent", " &fto the server. Think of repairing or coloring recipes.", " &f&nSo disabling is not recommended!"));
							meta.setLore(meta.getLore().stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
							result.setItemMeta(meta);
						} else{
							final ItemMeta meta = result.getItemMeta();
							meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&3key: &f" + Adapter.GetRecipeIdentifier(((Recipe)recipe)))));
							result.setItemMeta(meta);
						}
						return result;
					}
					return null;
				});
	}
}
