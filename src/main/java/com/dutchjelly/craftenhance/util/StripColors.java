package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class StripColors {

	private static final Pattern COLOR_AND_DECORATION_REGEX = Pattern.compile("(&|" + ChatColor.COLOR_CHAR + ")[0-9a-fk-orA-FK-OR]");

	public static final Pattern RGB_HEX_COLOR_REGEX = Pattern.compile("(?<!\\\\)(&|)#((?:[0-9a-fA-F]{3}){1,2})");
	private static final Pattern RGB_X_COLOR_REGEX = Pattern.compile("(" + ChatColor.COLOR_CHAR + "x)(" + ChatColor.COLOR_CHAR + "[0-9a-fA-F]){6}");


	public static List<String> stripLore(List<String> lores) {
		List<String> clearColors = new ArrayList<>();
		if (lores == null || lores.isEmpty()) return clearColors;
		for (String lore  :lores){
			clearColors.add(stripColors(lore));
		}
		return clearColors;
	}

	public static String stripColors(String message) {

		if (message == null || message.isEmpty())
			return message;

		// Replace & color codes
		Matcher matcher = COLOR_AND_DECORATION_REGEX.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		// Replace hex colors, both raw and parsed
		if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_15)) {
			matcher = RGB_HEX_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			matcher = RGB_X_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			message = message.replace(ChatColor.COLOR_CHAR + "x", "");
		}

		return message;
	}
}