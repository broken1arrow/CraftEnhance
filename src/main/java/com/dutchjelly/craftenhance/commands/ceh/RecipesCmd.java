package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewerCategorys;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRoute(cmdPath={"recipes","ceh.viewer"}, perms="perms.recipe-viewer")
public class RecipesCmd implements ICommand {

	private CustomCmdHandler handler;
	
	public RecipesCmd(CustomCmdHandler handler){
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "The view command opens an inventory that contains all available recipes for the sender of the command, unless it's configured to show all. The usage is /ceh view or /recipes";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		RecipesViewerCategorys menu = new RecipesViewerCategorys("");
		menu.menuOpen(p);
		if(args.length == 1){
		    try{
                int pageIndex = Integer.valueOf(args[0]);
                //gui.setPage(pageIndex); //setpage will handle invalid indexes and will jump to the nearest valid page
            }catch(NumberFormatException e){
		        p.sendMessage("that's not a number");
            }
        }
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}
	

}