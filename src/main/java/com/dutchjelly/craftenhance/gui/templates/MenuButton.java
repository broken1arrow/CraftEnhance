package com.dutchjelly.craftenhance.gui.templates;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.files.ConfigurationSerializeUtility;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MenuButton implements ConfigurationSerializeUtility {

	private final ItemStack itemStack;
	private final DyeColor color;
	private final Material material;
	private final String displayName;
	private final List<String> lore;
	private final boolean glow;
	private final ButtonType buttonType;

	public MenuButton(Builder builder) {
		this.itemStack = builder.itemStack;
		this.color = builder.color;
		this.material = builder.material;
		this.displayName = builder.displayName;
		this.lore = builder.lore;
		this.glow = builder.glow;
		this.buttonType = builder.buttonType;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public DyeColor getColor() {
		return color;
	}

	public Material getMaterial() {
		return material;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getLore() {
		return lore;
	}

	public boolean isGlow() {
		return glow;
	}

	public ButtonType getButtonType() {
		return buttonType;
	}

	public static class Builder {

		public ItemStack itemStack;
		private DyeColor color;
		private Material material;
		private String displayName;
		private List<String> lore;
		private boolean glow;
		private ButtonType buttonType;

		public Builder setItemStack(ItemStack itemStack) {
			this.itemStack = itemStack;
			return this;
		}

		public Builder setColor(DyeColor color) {
			this.color = color;
			return this;
		}

		public Builder setMaterial(Material material) {
			this.material = material;
			return this;
		}

		public Builder setDisplayName(String displayName) {
			this.displayName = displayName;
			return this;
		}

		public Builder setLore(List<String> lore) {
			this.lore = lore;
			return this;
		}

		public Builder setGlow(boolean glow) {
			this.glow = glow;
			return this;
		}

		public Builder setButtonType(ButtonType buttonType) {
			this.buttonType = buttonType;
			return this;
		}

		public MenuButton build() {
			return new MenuButton(this);
		}
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("color", color);
		map.put("material", material + "");
		map.put("name", displayName);
		map.put("lore", lore);
		map.put("glow", glow);
		if (buttonType != null)
			map.put("buttonType", buttonType);
		return map;
	}

	public static MenuButton deserialize(Map< String, Object> map) {
		String color = (String) map.get("color");
		String material = (String) map.get("material");
		String displayName = (String) map.get("name");
		List<String> lore = (List<String>) map.get("lore");
		boolean glow = (boolean) map.getOrDefault("glow", false);
		String buttonType = (String) map.get("buttonType");

		ItemStack itemStack = Adapter.getItemStack(material, displayName, lore, color, glow);
		Builder builder = new Builder();
		builder.setButtonType(ButtonType.valueOfType(buttonType))
				.setItemStack(itemStack)
				.setColor(Adapter.dyeColor(color))
				.setDisplayName(displayName)
				.setMaterial(Material.getMaterial(material))
				.setGlow(glow)
				.setLore(lore);
		return builder.build();
	}

	@Override
	public String toString() {
		return "MenuButton{" +
				"itemStack=" + itemStack +
				", color=" + color +
				", material=" + material +
				", displayName='" + displayName + '\'' +
				", lore=" + lore +
				", glow=" + glow +
				", buttonType=" + buttonType +
				'}';
	}
}
