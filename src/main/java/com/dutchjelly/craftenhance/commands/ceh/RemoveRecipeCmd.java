package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

@CommandRoute(cmdPath="ceh.removerecipe", perms="perms.recipe-editor")
public class RemoveRecipeCmd implements ICommand  {

	private CustomCmdHandler handler;

	public RemoveRecipeCmd(CustomCmdHandler handler){
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "The remove the recipe command. The usage is /ceh createrecipe [key] [permission].";
	}
	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		removeRecipe(p,args);
	}

	@Override
	public void handleConsoleCommand(final CommandSender sender, final String[] args) {
		removeRecipe(sender,args);
	}

	private void removeRecipe(final CommandSender sender, String[] args) {
		if (args.length == 0){return;}
		//Use the input that the user gave.
		if (args.length == 1) {
			args = addEmptyString(args);
		}
		List<EnhancedRecipe> enhancedRecipes = RecipeLoader.getInstance().getLoadedRecipes();
		for (EnhancedRecipe recipe : enhancedRecipes) {
			if (recipe.getKey().equals(args[0])) {
				self().getFm().removeRecipe(recipe);
				RecipeLoader.getInstance().unloadRecipe(recipe);
				Messenger.Message("The specified recipe is removed " + recipe.getKey(), sender);
				return;
			}
		}
	}

	@Override
	public List<String> handleTabCompletion(final CommandSender sender, final String[] args) {
		List<String> recipes = new ArrayList<>();
		if (args.length == 2){
			RecipeLoader.getInstance().getLoadedRecipes().forEach(recipe-> recipes.add(recipe.getKey()));
		}
		return recipes;
	}

	//Create a new array object so return that.
	private String[] addEmptyString(String[] args){
		String[] newArray = new String[args.length + 1];
		newArray[0] = args[0];
		newArray[1] = "";
		return newArray;
	}

}
