package com.dutchjelly.craftenhance.gui.templates;

import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.SimpleYamlHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MenuSettingsCache extends SimpleYamlHelper {

	private JavaPlugin plugin;
	private final Map<String, MenuTemplate> templates = new HashMap<>();


	public MenuSettingsCache(JavaPlugin plugin) {
		super("guitemplates.yml",true, true);
		this.plugin = plugin;
	}

	public Map<String, MenuTemplate> getTemplates() {
		return templates;
	}

	public void loadSettingsFromYaml(File file) {
		FileConfiguration templateConfig = this.getCustomConfig();

		for (String key : templateConfig.getKeys(false)) {
			ConfigurationSection menuData = templateConfig.getConfigurationSection(key + ".buttons");
			Map<List<Integer>, MenuButton> menuButtonMap = new HashMap<>();

			String menuSettings = templateConfig.getString(key + ".menu_settings.name");
			List<Integer> fillSpace = parseRange(templateConfig.getString(key + ".menu_settings.fill-space"));

			if (menuData != null) {
				for (String menuButtons : menuData.getKeys(false)) {
					MenuButton menuButton = this.getData(key + ".buttons." + menuButtons, MenuButton.class);
					menuButtonMap.put(parseRange(menuButtons), menuButton);
				}
			}
			System.out.println("menuButtonMap " + menuButtonMap);
			MenuTemplate menuTemplate = new MenuTemplate(menuSettings,fillSpace, menuButtonMap);
			templates.put(key,  menuTemplate);

			//Messenger.Error("There is a problem with loading the gui template of " + key + ". You're probably missing some new templates, which will automatically generate when just removing the guitemplates.yml file.\n");
			//Debug.Send("(Config Error)" + Arrays.toString(configError.getStackTrace()).replace(",","\n"));

		}
	}

	private List<Integer> parseRange(String range) {
		List<Integer> slots = new ArrayList<>();

		//Allow empty ranges.
		if (range == null || range.equals("")) return slots;

		try {
			for (String subRange : range.split(",")) {
				if (Objects.equals(subRange, "")) continue;
				if (subRange.contains("-")) {
					int first = Integer.parseInt(subRange.split("-")[0]);
					int second = Integer.parseInt(subRange.split("-")[1]);
					slots.addAll(IntStream.range(first, second + 1).boxed().collect(Collectors.toList()));
				} else slots.add(Integer.valueOf(subRange));
			}
		} catch (NumberFormatException e) {
			throw new ConfigError("Couldn't parse range " + range);
		}
		return slots;
	}

	@Override
	protected void saveDataToFile(File file) {

	}

}
