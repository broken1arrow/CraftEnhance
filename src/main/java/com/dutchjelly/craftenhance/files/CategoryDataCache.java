package com.dutchjelly.craftenhance.files;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CategoryDataCache extends SimpleYamlHelper  {

	@Getter
	private final Map<String, CategoryData> recipeCategorys = new HashMap<>();

	public CategoryDataCache() {
		super("categorys.yml", true, true);
	}

	@Override
	protected void saveDataToFile(File file) {
		for (Entry<String, CategoryData> entry : recipeCategorys.entrySet())
			this.setData(file, entry.getKey(), entry.getValue());
	}

	@Override
	protected void loadSettingsFromYaml(File file) {
		FileConfiguration templateConfig = this.getCustomConfig();
		for (String category : templateConfig.getKeys(false)) {
			CategoryData categoryData = this.getData(category, CategoryData.class);
			recipeCategorys.put(category, categoryData);
		}
	}
}
