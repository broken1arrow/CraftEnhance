package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRoute(cmdPath="ceh.cleanitemfile", perms="perms.recipe-editor")
public class CleanItemFileCmd implements ICommand {

	private CustomCmdHandler handler;
	
	public CleanItemFileCmd(CustomCmdHandler handler){
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Removes the unused items from the config. The usage is /ceh cleanitemfile.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		//handler.getMain().getFm().cleanItemFile();
		Messenger.Message("Not used any more, as it store items inside a SQL database and not allowing duplicates. But in case it is needed it will stay a little longer.", p);
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		//handler.getMain().getFm().cleanItemFile();
		Messenger.Message("Not used any more, as it store items inside a SQL database and not allowing duplicates. But in case it is needed it will stay a little longer.", sender);
	}
}
