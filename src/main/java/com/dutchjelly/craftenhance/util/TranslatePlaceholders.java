package com.dutchjelly.craftenhance.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TranslatePlaceholders {

	public static ItemStack replacePlaceHolders(final ItemStack item, final Map<String, Object> placeholders,
	                                            Function<String, String> callBackText) {
		if (item == null)
			return null;
		if (placeholders != null)
			placeholders
					.forEach((key, value) -> replacePlaceHolder(item, key, value, callBackText));
		return item;
	}

	private static void replacePlaceHolder(final ItemStack item, final String placeHolder, final Object value,
	                                       Function<String, String> callBackText) {
		if (item == null)
			return;

		final ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return;
		final StringBuilder placeholderValue = new StringBuilder(value != null ? value.toString() : "");

		if (meta.getDisplayName().contains(placeHolder)) {
			meta.setDisplayName(meta.getDisplayName().replace(placeHolder, placeholderValue.toString()));
			item.setItemMeta(meta);
		}

		List<String> lore = meta.getLore();
		if (lore == null)
			return;
		if (value instanceof List)
			lore = getStringList(placeHolder, split((List<?>) value, 50), lore, callBackText);
		else
			lore = lore.stream()
					.map(line -> (line == null ? null
							: callBackText.apply(line.replace(placeHolder, value != null ? value.toString() : ""))))
					.collect(Collectors.toList());
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	@NotNull
	private static List<String> getStringList(String placeHolder, List<?> value, List<String> lore,
	                                          Function<String, String> callBackText) {
		final List<String> list = new ArrayList<>(lore.size());

		int index = getIndexOf(placeHolder, lore);
		final int size = lore.size() - index;
		if (size > 0) {
			list.add(null);
			index += 1;
		}
		for (final String itemLore : lore) {
			final int indexOfPlaceHolder = itemLore.indexOf(placeHolder);
			if (index > 0 && indexOfPlaceHolder > 0) {
				for (int i = 0; i < value.size(); i++)
					list.add(i + index, callBackText.apply(itemLore.replace(placeHolder, value.get(i) + "")));
			} else if (index > 0)
				list.add(itemLore.replace(placeHolder, ""));
			else
				list.add(itemLore);
		}
		return list;
	}

	private static int getIndexOf(String placeHolder, List<String> lore) {
		for (int i = 0; i < lore.size(); i++) {
			final String itemLore = lore.get(i);
			if (itemLore.contains(placeHolder))
				return i;
		}
		return -1;
	}

	private static List<String> split(List<?> input, int maxLineLength) {
		List<String> output = new ArrayList<>();

		for (Object line : input) {
			StringTokenizer tok = new StringTokenizer(line + "", " ");
			StringBuilder currentLine = new StringBuilder();

			while (tok.hasMoreTokens()) {
				String word = tok.nextToken();
				String color = "";
				if (currentLine.length() + word.length() > maxLineLength) {
					output.add(currentLine.toString().trim());

					String start = currentLine.substring(0, Math.min(2, currentLine.length()));
					if (start.contains("&") || start.contains("§")) {
						color = start;
					}
					currentLine = new StringBuilder();
				}
				currentLine.append(color).append(word).append(" ");
			}
			if (currentLine.length() > 0) {
				output.add(currentLine.toString().trim());
			}
		}
		return output;
	}


}
