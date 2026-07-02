package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedFurnaceRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.settings.RecipeSettingsFurnace;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import org.bukkit.entity.Player;

public class RecipeEditorFurnace extends RecipeEditor<EnhancedFurnaceRecipe> {


	public RecipeEditorFurnace(final EnhancedFurnaceRecipe recipe, final int page, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		super(recipe, page,categoryData, permission, editorType, true);
	}

	public RecipeEditorFurnace(final EnhancedFurnaceRecipe recipe, final int page, final CategoryData categoryData, final String permission, final ButtonType editorType, boolean clearItems) {
		super(recipe, page,categoryData, permission, editorType, clearItems);
	}

	@Override
	protected boolean onPlayerClick(final EnhancedFurnaceRecipe recipe, final CategoryData categoryData, final String permission, final String buttonAction, final Player player) {
		if (buttonAction.equalsIgnoreCase(ButtonType.RecipeSettings.name())) {
			new RecipeSettingsFurnace(recipe, categoryData, permission, ButtonType.ChooseFurnaceType)
					.menuOpen(player);
		}
		return false;
	}

	@Override
	protected void beforeSave(final EnhancedFurnaceRecipe recipe) {
		recipe.setDuration(recipe.getDuration());
		recipe.setExp(recipe.getExp());
	}


}
