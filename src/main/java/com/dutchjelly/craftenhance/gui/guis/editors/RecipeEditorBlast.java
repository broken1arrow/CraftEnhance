package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettingsBlast;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RecipeEditorBlast extends RecipeEditor<BlastRecipe> {

	public RecipeEditorBlast(final BlastRecipe recipe, final CategoryData categoryData, final String permission, final ButtonType editorType,boolean clearItems)  {
		super(recipe, categoryData, permission, editorType,clearItems);


	}

	@Override
	protected boolean onPlayerClick(final BlastRecipe recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		if (buttonAction.equalsIgnoreCase(ButtonType.RecipeSettings.name())) {
			new RecipeSettingsBlast(recipe, categoryData, permission, ButtonType.ChooseBlastType)
					.menuOpen(player);
		}
		return false;
	}

	@Override
	protected void beforeSave(final BlastRecipe recipe) {
		recipe.setDuration(recipe.getDuration());
		recipe.setExp(recipe.getExp());
	}

	@Override
	protected Map<String, String> recipePlaceholders(final BlastRecipe recipe) {
		return new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(recipe.getExp()));
			put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(recipe.getDuration()));
		}};
	}

}
