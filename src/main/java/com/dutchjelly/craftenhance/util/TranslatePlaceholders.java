package com.dutchjelly.craftenhance.util;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		ItemFlag flag = getItemFlag();
		if (flag != null) {
			final ItemMeta meta = item.getItemMeta();
			if (meta != null)
				meta.removeItemFlags(flag);
			item.setItemMeta(meta);
		}
		return item;
	}

	private static void replacePlaceHolder(final ItemStack item, final String placeHolder, final Object value,
	                                       Function<String, String> callBackText) {
		if (item == null)
			return;

		final ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return;
		final StringBuilder placeholderValue = new StringBuilder();

		if (meta.getDisplayName().contains(placeHolder)) {
			if (value instanceof List) {
				((List<?>) value).forEach(o -> placeholderValue.append(o.toString()));
			} else {
				placeholderValue.append(value != null ? value.toString() : "");
			}
			meta.setDisplayName(meta.getDisplayName().replace(placeHolder, placeholderValue.toString()));
		}

		List<String> lore = meta.getLore();
		if (lore != null) {
			if (value instanceof List)
				lore = getStringList(placeHolder, split((List<?>) value, 50), lore, callBackText);
			else
				lore = lore.stream()
						.map(line -> {
							String string = getString(placeHolder, value, callBackText, line);
							System.out.println("line string  " + string);
							return string;
						})
						.collect(Collectors.toList());
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	private static @Nullable String getString(final String placeHolder, final Object value, final Function<String, String> callBackText, final String line) {
		if (line == null)
			return null;
		return callBackText.apply(line.replace(placeHolder, value != null ? value.toString() : ""));
	}

	@NotNull
	private static List<String> getStringList(String placeHolder, List<?> value, List<String> lore,
	                                          Function<String, String> callBackText) {
		final List<String> list = new ArrayList<>(lore.size());

		int index = getIndexOf(placeHolder, lore);

		for (final String itemLore : lore) {
			final int indexOfPlaceHolder = itemLore.indexOf(placeHolder);
			if (index > 0 && indexOfPlaceHolder > 0) {
				if (index > list.size()) {
					final int expand = (index - list.size()) + 1;
					for (int i = 0; i < expand; i++)
						list.add(null);
				}
				for (int i = 0; i < value.size(); i++) {
					list.add(i + index, callBackText.apply(itemLore.replace(placeHolder, value.get(i) + "")));
				}
			} else
				list.add(itemLore.replace(placeHolder, ""));
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

	private static @Nullable ItemFlag getItemFlag() {
		for (ItemFlag itemFlag : ItemFlag.values()) {
			if (itemFlag.name().equals("HIDE_LORE")) {
				return itemFlag;
			}
		}
		return null;
	}

}
