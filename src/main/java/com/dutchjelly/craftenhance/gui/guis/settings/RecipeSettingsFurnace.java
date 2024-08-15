package com.dutchjelly.craftenhance.gui.guis.settings;

import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorFurnace;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.prompt.HandleChatInput;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RecipeSettingsFurnace extends RecipeSettings<FurnaceRecipe> {

	public RecipeSettingsFurnace(final FurnaceRecipe recipe, final CategoryData categoryData, final String permission, final ButtonType editorType) {
		super(recipe, categoryData, permission, editorType);
	}

	protected boolean onPlayerClick(final FurnaceRecipe furnaceRecipe, final String buttonAction, final Player player) {
		if (player.isConversing()) return true;

		if (buttonAction.equalsIgnoreCase(ButtonType.SetExp.name())) {
			new HandleChatInput(this, msg -> {
				int parsed;
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit")) {
					this.runTask(() -> this.menuOpen(player), player);
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
				if (furnaceRecipe != null) {
					furnaceRecipe.setExp(parsed);
				}
				this.runTask(() -> this.menuOpen(player), player);
				return false;
			}).setMessages("Please input an exp amount.Type q, exit, cancel to turn it off.").start(getViewer());
			return true;
		}
		if (buttonAction.equalsIgnoreCase(  ButtonType.SetCookTime.name())) {
			if (player.isConversing()) return true;
			new HandleChatInput(this, (msg) -> {
				short parsed;
				if (msg.equals("cancel") || msg.equals("quit") || msg.equals("exit") || msg.equals("q")) {
					this.runTask(() -> this.menuOpen(player), player);
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
				if (furnaceRecipe != null) {
					furnaceRecipe.setDuration(parsed);
				}
				this.runTask(() -> this.menuOpen(player), player);
				return false;
			}).setMessages("Please input a cook duration.Type q, exit, cancel to turn it off.").start(player);
			return true;
		}
		return false;
	}

	@Override
	protected void handleBack(final FurnaceRecipe recipe, final CategoryData categoryData, final Player player) {
		new RecipeEditorFurnace(recipe, categoryData, null, ButtonType.ChooseFurnaceType,false).menuOpen(player);
	}

	@Override
	protected Map<String, String> recipePlaceholders(final FurnaceRecipe recipe) {
		return new HashMap<String, String>() {{
			put(InfoItemPlaceHolders.Exp.getPlaceHolder(), String.valueOf(recipe.getExp()));
			put(InfoItemPlaceHolders.Duration.getPlaceHolder(), String.valueOf(recipe.getDuration()));
		}};
	}
}
