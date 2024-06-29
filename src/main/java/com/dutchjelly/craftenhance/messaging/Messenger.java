package com.dutchjelly.craftenhance.messaging;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.util.StripColors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Map.Entry;

public class Messenger {

	private static CraftEnhance plugin;
	private static String prefix;

	public static void Init(final CraftEnhance plugin){
		Messenger.plugin = plugin;
		Messenger.prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("global-prefix"));
	}

	public static void Message(final String message){
        Bukkit.getConsoleSender().sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
    }

	public static void Message(String message, final CommandSender sender) {
		if(sender == null) {
		    Message(message);
		    return;
        }
        message = ChatColor.translateAlternateColorCodes('&', message);
        message = prefix + message;
        SendMessage(message, sender);
    }

	public static String getMessage(String message, final CommandSender sender) {
		if (sender == null) {
			final String pre = StripColors.stripColors( new String(prefix));
			return pre +  message;
		}
		message = ChatColor.translateAlternateColorCodes('&', message);
		message = prefix + message;
		return message;
	}
	public static void MessageFromConfig(final String path, final CommandSender sender, final String placeHolder){
		if(path == null || sender == null || placeHolder == null) return;
		final String message = plugin.getConfig().getString(path).replace("[PLACEHOLDER]", placeHolder);
		SendMessage(message, sender);
	}

	public static void MessageFromConfig(final String path, final CommandSender sender, final Map<String, String> placeHolders) {
		if (path == null || sender == null || placeHolders == null) return;
		final String message = plugin.getConfig().getString(path);
		if (message == null || message.isEmpty()) {
			SendMessage("This message" + path + "don't exist in the config. Remove the old config.yml and then run /ceh realod", sender);
			return;
		}

		String sendMessage =  prefix + message;
		if (!placeHolders.isEmpty())
			for (Entry<String, String> value : placeHolders.entrySet()) {
				sendMessage = sendMessage.replace(value.getKey(), value.getValue());
			}
		SendMessage(sendMessage, sender);
	}

	public static void MessageFromConfig(final String path, final CommandSender sender){
		if(path == null || sender == null) return;
		final String message = plugin.getConfig().getString(path);
		SendMessage(message, sender);
	}

	private static void SendMessage(String s, final CommandSender sender) {
		if (s == null) s = "";
		if (sender instanceof Player) {
			if (((Player) sender).isConversing())
				((Player) sender).sendRawMessage(s);
		}
		sender.sendMessage(s);
	}

	public static void Error(final String error){
		Message("&4&lError&r -- " + error);
	}

}
