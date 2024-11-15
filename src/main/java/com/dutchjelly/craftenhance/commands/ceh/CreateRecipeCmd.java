package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.gui.guis.EditorTypeSelector;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorBlast;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorFurnace;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditorSmoker;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandRoute(cmdPath = "ceh.createrecipe", perms = "perms.recipe-editor")
public class CreateRecipeCmd implements ICommand {

	private CustomCmdHandler handler;

	public CreateRecipeCmd(CustomCmdHandler handler) {
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "The create recipe command allows users to create a recipe and open the editor of it. The usage is /ceh createrecipe [key] [permission]. You can leave both parameters empty. However, if you do want to customise recipe keys and permissions: the key has to be unique, and the permission can be empty to not have any permission. An example: /ceh createrecipe army_chest ceh.army-chest. The now created recipe has a key of army_chest and a permission of ceh.army-chest.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {

		//Fill in a unique key and empty permission for the user.
		if (args.length == 0) {
			int uniqueKeyIndex = 1;
			while (!handler.getMain().getFm().isUniqueRecipeKey("recipe" + uniqueKeyIndex))
				uniqueKeyIndex++;
            /*EditorTypeSelector gui = new EditorTypeSelector(handler.getMain().getGuiManager(), handler.getMain().getGuiTemplatesFile().getTemplate(EditorTypeSelector.class), null, p, "recipe" + uniqueKeyIndex, null);
            handler.getMain().getGuiManager().openGUI(p, gui);*/
			EditorTypeSelector editorTypeSelector = new EditorTypeSelector("recipe" + uniqueKeyIndex, null);
			editorTypeSelector.menuOpen(p);
			return;
		}
		if (args.length < 2) {
			Messenger.MessageFromConfig("messages.commands.few-arguments", p, "2");
			return;
		}
		if (!handler.getMain().getFm().isUniqueRecipeKey(args[1])) {
			Messenger.Message("The specified recipe key isn't unique.", p);
			return;
		}
		openEditor(p, args);
	}

	private static void openEditor(final Player p, final String[] args) {
		String recipeType = args[0].toLowerCase(Locale.ROOT);

		String permission = null;
		if (args.length > 2)
			permission = args[2];

		EnhancedRecipe recipe;
		switch (recipeType) {
			case "workbench":
				recipe = new WBRecipe(permission , null, new ItemStack[9]);
				recipe.setKey(args[1]);
				RecipeEditor<EnhancedRecipe> recipeEditor = new RecipeEditor<>(recipe, null, null, ButtonType.ChooseWorkbenchType);
				recipeEditor.menuOpen(p);
				break;
			case "furnace":
				recipe = new FurnaceRecipe(permission , null, new ItemStack[1]);
				recipe.setKey(args[1]);
				RecipeEditorFurnace editorFurnace = new RecipeEditorFurnace((FurnaceRecipe) recipe, null, null, ButtonType.ChooseWorkbenchType);
				editorFurnace.menuOpen(p);
				break;
			case "blast":
				recipe = new BlastRecipe(permission , null, new ItemStack[1]);
				recipe.setKey(args[1]);
				RecipeEditorBlast editorBlast = new RecipeEditorBlast((BlastRecipe)  recipe, null, null, ButtonType.ChooseWorkbenchType,true);
				editorBlast.menuOpen(p);
				break;
			case "smoker":
				recipe = new SmokerRecipe(permission , null, new ItemStack[1]);
				recipe.setKey(args[1]);
				RecipeEditorSmoker editorSmoker = new RecipeEditorSmoker((SmokerRecipe)  recipe, null, null, ButtonType.ChooseWorkbenchType,true);
				editorSmoker.menuOpen(p);
				break;
			default:
				Messenger.MessageFromConfig("messages.commands.not-valid-recipe-option", p, (map) -> {
					map.put("[recipe-edit]", recipeType);
					map.put("[recipe-options]", "<workbench, furnace, blast, smoker>");
				});

		}
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}

	@Override
	public List<String> handleTabCompletion(final CommandSender sender, final String[] args) {
		List<String> tab = new ArrayList<>();
		if (args.length == 2) {
			tab.add("workbench");
			tab.add("furnace");
			tab.add("blast");
			tab.add("smoker");
		}
		if (args.length == 3) {
			tab.add("key");
		}
		return tab;
	}

	//Create a new array object so return that.
	private String[] addEmptyString(String[] args) {
		String[] newArray = new String[args.length + 1];
		newArray[0] = args[0];
		newArray[1] = "";
		return newArray;
	}
}
