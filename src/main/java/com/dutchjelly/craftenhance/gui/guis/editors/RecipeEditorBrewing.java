package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettingsBrewing;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RecipeEditorBrewing extends RecipeEditor<BrewingRecipe> {
	private final BrewingRecipe brewingRecipe;

	public RecipeEditorBrewing(final BrewingRecipe brewingRecipe, final int page, final CategoryData categoryData, final String permission, final ButtonType editorType, final boolean clearItems) {
		super(brewingRecipe, page, categoryData, permission, editorType, clearItems);
		this.brewingRecipe = brewingRecipe;
	}

	@Override
	protected boolean onPlayerClick(final BrewingRecipe recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		if (buttonAction.equalsIgnoreCase(ButtonType.RecipeSettings.name())) {
			new RecipeSettingsBrewing(recipe, categoryData, permission, ButtonType.ChooseBrewingType)
					.menuOpen(player);
		}
		return false;
	}


	@Override
	protected void updateRecipeDisplay(final Inventory menu) {
		final List<Integer> fillSpace = this.getFillSpace();
		final ItemStack[] content = brewingRecipe.getCombinedContent();
		if (fillSpace.size() != content.length + 1)
			throw new ConfigError("fill space of Recipe editor must be " + (content.length + 1));
		int index = 0;
		for (final Integer slot : fillSpace) {
			if (slot == brewingRecipe.getResultSlot()) {
				menu.setItem(slot, brewingRecipe.getResult());
				continue;
			}
			if (slot > menu.getSize())
				throw new ConfigError("fill space spot " + slot + " is outside of inventory");
			if (index +1 > content.length)
				menu.setItem(slot, null);
			else
				menu.setItem(slot, content[index++]);
		}
		matchType = brewingRecipe.getMatchType();
		hidden = brewingRecipe.isHidden();
	}

	@Nullable
	protected ItemStack[] getIngredients(final Map<Integer, ItemStack> map, final Player player) {
		int resultSlot = 2;
		ItemStack[] itemStacks = new ItemStack[brewingRecipe.getCombinedContent().length];

		for (final Entry<Integer, ItemStack> stackEntry : map.entrySet()) {
			final ItemStack itemStack = stackEntry.getValue();
			final Integer key = stackEntry.getKey();

			if (key != resultSlot) {
				switch (key){
					case 19:
						itemStacks[0] = itemStack;
						break;
					case 20:
						itemStacks[1] = itemStack;
						break;
					case 21:
						itemStacks[2] = itemStack;
						break;
					case 13:
						itemStacks[3] = itemStack;
						break;
					case 14:
						itemStacks[4] = itemStack;
						break;
					case 15:
						itemStacks[5] = itemStack;
						break;
				}
			}
			if (key == resultSlot)
				this.brewingRecipe.setResultSlot(resultSlot);

		}
		this.result = map.remove(resultSlot);
		return  itemStacks;
	}

	@Override
	protected void beforeSave(final BrewingRecipe recipe) {
		recipe.setDuration(recipe.getDuration());
	}

	@Override
	protected Map<String, String> recipePlaceholders(final BrewingRecipe recipe) {
		return new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(0));
			put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(recipe.getDuration()));
		}};
	}

}
