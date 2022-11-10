package com.dutchjelly.craftenhance.files;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CategoryDataCache extends SimpleYamlHelper  {

	@Getter
	private final Map<String, CategoryData> recipeCategorys = new HashMap<>();

	public CategoryDataCache() {
		super("categorys.yml", true, true);
	}
	public CategoryData of(String category, ItemStack itemStack){
	return new CategoryData(itemStack,category);
	}
	public boolean addCategory(String category, ItemStack itemStack) {
		CategoryData categoryData = this.getRecipeCategorys().get(category);
		if (categoryData != null) return true;
		 recipeCategorys.put(category,new CategoryData(itemStack,category));
		return false;
	}

	@Override
	protected void saveDataToFile(File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("recipeCategorys " + recipeCategorys);
		for (Entry<String, CategoryData> entry : recipeCategorys.entrySet())
			this.setData(file, entry.getKey(), entry.getValue());
	}

	@Override
	protected void loadSettingsFromYaml(File file) {
		FileConfiguration templateConfig = this.getCustomConfig();
		for (String category : templateConfig.getKeys(false)) {
			CategoryData categoryData = this.getData(category, CategoryData.class);
			if (categoryData == null) continue;
			recipeCategorys.put(category, categoryData);
		}
	}
}
