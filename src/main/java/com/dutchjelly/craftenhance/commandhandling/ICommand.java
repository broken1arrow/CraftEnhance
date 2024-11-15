package com.dutchjelly.craftenhance.commandhandling;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface ICommand {

	String getDescription();

	void handlePlayerCommand(Player p, String[] args);

	void handleConsoleCommand(CommandSender sender, String[] args);

	default List<String> handleTabCompletion(CommandSender sender, String[] args){
		return null;
	}
}
