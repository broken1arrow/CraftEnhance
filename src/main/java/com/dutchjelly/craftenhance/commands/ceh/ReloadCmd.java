package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.dutchjelly.craftenhance.CraftEnhance.self;


@CommandRoute(cmdPath = {"ceh.reload", "ceh.rl"}, perms = "perms.recipe-editor")
public class ReloadCmd implements ICommand {
	private static Set<UUID> players = new HashSet<>();
	private static Set<String> console = new HashSet<>();

	@Override
	public String getDescription() {
		return "Reloads CraftEnhance entirely.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		if (args.length > 0 && args[0].equals("--force")) {
			forceRefresh(p);
			return;
		}
		self().reload();
		if (self().getConfig().getBoolean("learn-recipes"))
			Bukkit.updateRecipes();
		Messenger.Message("&fThe plugin was reloaded", p);
	}
	

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		if (args.length > 0 && args[0].equals("--force")) {
			forceRefrechConsole(sender);
			return;
		}
		self().reload();
		if (self().getConfig().getBoolean("learn-recipes"))
			Bukkit.updateRecipes();
		Messenger.Message("The plugin was reloaded", sender);
	}

	@Override
	public List<String> handleTabCompletion(final CommandSender sender, final String[] args) {
		List<String> list = new ArrayList<>();
		if (args.length == 2) {
			list.add("--force");
			return list;
		}
		return ICommand.super.handleTabCompletion(sender, args);
	}

	private static void forceRefrechConsole(final CommandSender sender) {
		if (console.add(sender.getName())) {
			Messenger.Message("Warn!! using force will lag the server, use it with caution. To run the command just type same command again.");
			CraftEnhance.runTaskLater(20 * 10, () -> console.remove(sender.getName()));
			return;
		}
		console.remove(sender.getName());
		long startTime = System.currentTimeMillis();
		RecipeLoader.refreshInstance();
		long duration = System.currentTimeMillis() - startTime;
		final String timeStr = duration >= 1000 ? String.format("%.2fs", duration / 1000.0) : duration + "ms";
		Messenger.Message("The plugin was reloaded (" + timeStr + ")");
	}

	private static void forceRefresh(final Player p) {
		if (players.add(p.getUniqueId())) {
			Messenger.Message("&4Warn!!&3 using force will lag the server, use it with caution. To run the command just type same command again.", p);
			CraftEnhance.runTaskLater(20 * 10, () -> {
				if (players.remove(p.getUniqueId())) {
					Messenger.Message("&cThe confirmation time for --force has expired.", p);
				}
			});
			return;
		}
		players.remove(p.getUniqueId());
		long startTime = System.currentTimeMillis();
		RecipeLoader.refreshInstance();
		long duration = System.currentTimeMillis() - startTime;
		final String timeStr = duration >= 1000 ? String.format("%.2fs", duration / 1000.0) : duration + "ms";
		Messenger.Message("&fThe plugin was reloaded &3(" + timeStr + ")", p);
	}
}
