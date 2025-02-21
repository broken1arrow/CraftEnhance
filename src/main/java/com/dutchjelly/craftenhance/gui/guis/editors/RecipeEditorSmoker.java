package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettingsSmoker;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RecipeEditorSmoker extends RecipeEditor<SmokerRecipe> {

	public RecipeEditorSmoker(final SmokerRecipe recipe,final int page, final CategoryData categoryData, final String permission, final ButtonType editorType,boolean clearItems) {
		super(recipe, page,categoryData, permission, editorType, clearItems);
	}

	@Override
	protected void beforeSave(final SmokerRecipe recipe) {
		recipe.setDuration(recipe.getDuration());
		recipe.setExp(recipe.getExp());
	}

	@Override
	protected boolean onPlayerClick(final SmokerRecipe recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		if (buttonAction.equalsIgnoreCase(ButtonType.RecipeSettings.name())) {
			new RecipeSettingsSmoker(recipe, categoryData, permission, ButtonType.ChooseSmokerType)
					.menuOpen(player);
		}
		return false;
	}

	@Override
	protected Map<String, String> recipePlaceholders(final SmokerRecipe recipe) {
		return new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(recipe.getExp()));
			put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(recipe.getDuration()));
		}};
	}

}

