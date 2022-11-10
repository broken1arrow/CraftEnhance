package com.dutchjelly.craftenhance.gui.interfaces;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
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
		this.recipeCategory = (String) args.getOrDefault("category.name", null);
		ItemStack itemStack = (ItemStack) args.getOrDefault("category.category_item", null);
		if (itemStack == null) {
			Material material = Adapter.getMaterial("CRAFTING_TABLE");
			if (this instanceof FurnaceRecipe)
				material = Adapter.getMaterial("FURNACE");
			if (material == null)
				material = Material.CRAFTING_TABLE;
			itemStack = new ItemStack(material);
		}
		this.recipeCategoryItem = itemStack;
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
	private ItemStack recipeCategoryItem;
	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("page", page);
			put("slot", slot);
			put("result_slot", resultSlot);
			put("category.name", recipeCategory);
			put("category.category_item", recipeCategoryItem);
		}};
	}

	public abstract ItemStack getDisplayItem();
}