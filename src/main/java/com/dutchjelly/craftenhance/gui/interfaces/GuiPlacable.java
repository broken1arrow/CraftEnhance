package com.dutchjelly.craftenhance.gui.interfaces;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class GuiPlacable implements ConfigurationSerializable {

	public GuiPlacable() {
	}

	public GuiPlacable(Map<String, Object> args) {
		this.page = (int) args.getOrDefault("page", -1);
		this.slot = (int) args.getOrDefault("slot", -1);
		this.resultSlot = (int) args.getOrDefault("result_slot", -1);
		this.recipeCategory = (String) args.getOrDefault("category", null);
	}

	@Getter
	@Setter
	private int page = -1;

	@Getter
	@Setter
	private int slot = -1;

	@Getter
	@Setter
	private int resultSlot;

	@Getter
	@Setter
	private String recipeCategory;

	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("page", page);
			put("slot", slot);
			put("result_slot", resultSlot);
			put("category", recipeCategory);
		}};
	}

	public abstract ItemStack getDisplayItem();
}