package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRoute(cmdPath="ceh.setpermission", perms="perms.recipe-editor")
public class SetPermissionCmd implements ICommand {

	private final CustomCmdHandler handler;
	
	public SetPermissionCmd(CustomCmdHandler handler){
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		if(args.length != 2) {
			Messenger.MessageFromConfig("messages.commands.few-arguments", p, "2");
			return;
		}
		EnhancedRecipe recipe = handler.getMain().getCacheRecipes().getRecipe(args[0]);
		if(recipe == null) {
			Messenger.Message("That recipe key doesn't exist", p);
			return;
		}

		recipe.setPermission(args[1]);
		recipe.save();
		Messenger.Message("Successfully set the permissions of the recipe to " + args[1] + ".", p);
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}
}