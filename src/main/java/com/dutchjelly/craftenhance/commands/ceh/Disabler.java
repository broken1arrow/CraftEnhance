package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.gui.guis.RecipeDisablerCopy;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRoute(cmdPath={"ceh.disabler", "ceh.disable"}, perms="perms.recipe-editor")
public class Disabler implements ICommand {

	private CustomCmdHandler handler;

	public Disabler(CustomCmdHandler handler){
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Use the command to open a gui in which you can disable/enable server recipes.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		//final CraftEnhance main = handler.getMain();
		RecipeDisablerCopy menu = new RecipeDisablerCopy(RecipeLoader.getInstance().getServerRecipes(), RecipeLoader.getInstance().getDisabledServerRecipes(),false,"");
		menu.menuOpen(p);
		/*	final GuiTemplate template = main.getGuiTemplatesFile().getTemplate(RecipeDisabler.class);
		final RecipeDisabler gui = new RecipeDisabler(main.getGuiManager(), template, null, p, RecipeLoader.getInstance().getServerRecipes(), RecipeLoader.getInstance().getDisabledServerRecipes(),"");
*/
		if(args.length == 1){
			try{
				int pageIndex = Integer.valueOf(args[0]);
				//gui.setPage(pageIndex); //setpage will handle invalid indexes and will jump to the nearest valid page
			}catch(NumberFormatException e){
				p.sendMessage("that's not a number");
			}
		}
		//handler.getMain().getGuiManager().openGUI(p, gui);
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}


}