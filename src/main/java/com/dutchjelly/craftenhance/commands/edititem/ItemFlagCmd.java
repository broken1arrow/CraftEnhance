package com.dutchjelly.craftenhance.commands.edititem;

import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.itemcreation.ItemCreator;
import com.dutchjelly.craftenhance.itemcreation.ParseResult;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandRoute(cmdPath="edititem.itemflag", perms="perms.item-editor")
public class ItemFlagCmd implements ICommand {

	
	private CustomCmdHandler handler;
	
	public ItemFlagCmd(CustomCmdHandler handler){
		this.handler = handler;
	}
	
	@Override
	public String getDescription() {
		return "The itemflag command allows users to toggle itemflags of the held item. An example of the usage is /edititem itemflag hide_enchants hide_attributes. These itemflag names are documented in the bukkit documentation, google \"itemflags bukkit\", and the first result should contain a list of all itemflags and what they do.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		ItemCreator creator = new ItemCreator(p.getInventory().getItemInHand(), args);
		ParseResult result = creator.setItemFlags();
		p.getInventory().setItemInHand(creator.getItem());
		Messenger.Message(result.getMessage(), p);
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}

	@Override
	public List<String> handleTabCompletion(CommandSender sender, String[] args) {
		List<String> list = new ArrayList<>();
		if (args.length == 1) {
			list.add("itemflag");
	}
		if (args.length == 2) {
			String toComplete = args[args.length - 1];
			List<ItemFlag> enchants = Arrays.asList(ItemFlag.values());
			enchants.stream().filter(x ->
							x.name().toLowerCase().startsWith(toComplete.toLowerCase()))
					.collect(Collectors.toList())
					.forEach(x -> list.add(x.name().toLowerCase()));
		}
		return list;
	}
}
