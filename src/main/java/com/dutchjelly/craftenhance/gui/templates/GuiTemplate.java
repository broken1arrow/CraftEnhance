package com.dutchjelly.craftenhance.gui.templates;

import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class GuiTemplate {

	private static final int RowSize = 9;
	@Getter
	private final int resultSlot;
	@Getter
	private final ItemStack[] stacksTemplate;

	@Getter
	private final String invTitle;

	@Getter
	private final List<String> invTitles;

	@Getter
	@NonNull
	private final Map<Integer, ButtonType> buttonMapping;

	@Getter
	@NonNull
	private final List<Integer> fillSpace;

	public GuiTemplate(final Consumer<Template> callback) {
		Template template = new Template();
		callback.accept(template);
		String name = template.getName();
		//List<String> names = (List<String>) config.get("names");

		if (name == null)
			throw new ConfigError("no gui name is specified");

		final List<Integer> templateFillSpace = template.getFillSpace();
		List<ItemStack> templateInventoryContent = new ArrayList<>();
		buttonMapping = new HashMap<>();

		for (Entry<Integer, TemplateItemStack> key : template.getTemplateInventoryContents().entrySet()) {
			Integer slot = key.getKey();
			final ItemStack item = key.getValue().getInventoryStack();
			while (templateInventoryContent.size() - 1 < slot) {
				//go in steps of 9 because inventories are always in rows of 9
				templateInventoryContent.addAll(Arrays.asList(new ItemStack[RowSize]));
			}
			if (templateFillSpace.contains(slot)) continue;
			buttonMapping.put(slot, key.getValue().getButtonType());
			templateInventoryContent.set(slot, item.clone());
		}
		invTitles = new ArrayList<>();
		invTitle = name;
		resultSlot = template.getResultSlot();
		stacksTemplate = templateInventoryContent.stream().toArray(ItemStack[]::new);
		fillSpace = templateFillSpace;

	}

	private List<Integer> parseRange(String range) {
		List<Integer> slots = new ArrayList<>();

		//Allow empty ranges.
		if (range == null || range == "") return slots;

		try {
			for (String subRange : range.split(",")) {
				if (subRange == "") continue;
				if (subRange.contains("-")) {
					int first = Integer.valueOf(subRange.split("-")[0]);
					int second = Integer.valueOf(subRange.split("-")[1]);
					slots.addAll(IntStream.range(first, second + 1).mapToObj(x -> x).collect(Collectors.toList()));
				} else slots.add(Integer.valueOf(subRange));
			}
		} catch (NumberFormatException e) {
			throw new ConfigError("Couldn't parse range " + range);
		}
		return slots;
	}

	public static class Template {
		private Map<Integer, TemplateItemStack> templateInventoryContents;
		private List<Integer> fillSpace;
		private String name;
		private int resultSlot = 13;

		public Map<Integer, TemplateItemStack> getTemplateInventoryContents() {
			return templateInventoryContents;
		}

		public void setTemplateInventoryContents(final Map<Integer, TemplateItemStack> templateInventoryContents) {
			this.templateInventoryContents = templateInventoryContents;
		}

		public List<Integer> getFillSpace() {
			return fillSpace;
		}

		public void setFillSpace(final List<Integer> fillSpace) {
			this.fillSpace = fillSpace;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public int getResultSlot() {
			return resultSlot;
		}

		public void setResultSlot(final int resultSlot) {
			this.resultSlot = resultSlot;
		}
	}

	public static class TemplateItemStack {
		private ItemStack inventoryStack;
		private ButtonType buttonType;

		public TemplateItemStack(final ItemStack inventoryStack, final ButtonType buttonType) {
			this.inventoryStack = inventoryStack;
			this.buttonType = buttonType;
		}

		public ItemStack getInventoryStack() {
			return inventoryStack;
		}

		public ButtonType getButtonType() {
			return buttonType;
		}

	}
}
