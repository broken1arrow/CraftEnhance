package com.dutchjelly.craftenhance.files;

import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.util.SimpleYamlHelper;
import com.dutchjelly.craftenhance.gui.templates.MenuButton;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MenuSettingsCache extends SimpleYamlHelper {

	private final Plugin plugin;
	private static final int version = 3;
	private final Map<String, MenuTemplate> templates = new HashMap<>();


	public MenuSettingsCache(final Plugin plugin) {
		super("guitemplates.yml", true, true);
		this.plugin = plugin;
		checkFileVersion();
	}
	public void checkFileVersion() {
		final File file = new File(plugin.getDataFolder(), "guitemplates.yml");
		if (file.exists()) {
			final FileConfiguration templateConfig = YamlConfiguration.loadConfiguration(file);
			if (templateConfig.contains("Version")) {
				final int configVersion = templateConfig.getInt("Version");
				if (configVersion < version) {
					updateFile(file);
				}
			} else {
				updateFile(file);
			}
		}
	}
	public void updateFile(final File file) {

		try {
			Files.move(Paths.get(file.getPath()), Paths.get(plugin.getDataFolder().getPath(), "guitemplates_backup_"+ version +".yml"), REPLACE_EXISTING);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final InputStream file1 = this.plugin.getResource("guitemplates.yml");
		if (file1 != null) {
			final FileConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(file1));
			templateConfig.set("Version",null);
			for (final String templet : templateConfig.getKeys(true)) {
				templateConfig.set(templet, templateConfig.get(templet));
			}
			//templateConfig.set("Version", version);
			try {
				templateConfig.save(file);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			final File newFile = new File(plugin.getDataFolder(), "guitemplates.yml");
			try {
				final BufferedWriter bw = new BufferedWriter(new FileWriter( newFile, true));
				bw.append("#Do not change this.\n");
				bw.append("Version: "+ version);
				bw.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Map<String, MenuTemplate> getTemplates() {
		return templates;
	}

	@Override
	public void loadSettingsFromYaml(final File file) {
		final FileConfiguration templateConfig = this.getCustomConfig();

		for (final String key : templateConfig.getKeys(false)) {
			if (key.equalsIgnoreCase("Version")) continue;
			final ConfigurationSection menuData = templateConfig.getConfigurationSection(key + ".buttons");
			final Map<List<Integer>, MenuButton> menuButtonMap = new HashMap<>();

			final String menuSettings = templateConfig.getString(key + ".menu_settings.name");
			final List<Integer> fillSpace = parseRange(templateConfig.getString(key + ".menu_settings.fill-space"));

			if (menuData != null) {
				for (final String menuButtons : menuData.getKeys(false)) {
					final MenuButton menuButton = this.getData(key + ".buttons." + menuButtons, MenuButton.class);
					menuButtonMap.put(parseRange(menuButtons), menuButton);
				}
			}
			System.out.println("menuButtonMap " + menuButtonMap);
			final MenuTemplate menuTemplate = new MenuTemplate(menuSettings,fillSpace, menuButtonMap);

			templates.put(key,  menuTemplate);

			//Messenger.Error("There is a problem with loading the gui template of " + key + ". You're probably missing some new templates, which will automatically generate when just removing the guitemplates.yml file.\n");
			//Debug.Send("(Config Error)" + Arrays.toString(configError.getStackTrace()).replace(",","\n"));

		}
	}

	private List<Integer> parseRange(final String range) {
		final List<Integer> slots = new ArrayList<>();

		//Allow empty ranges.
		if (range == null || range.equals("")) return slots;

		try {
			for (final String subRange : range.split(",")) {
				if (Objects.equals(subRange, "")) continue;
				if (subRange.contains("-")) {
					final String[] numbers = subRange.split("-");
					if (numbers[0].isEmpty() || numbers[1].isEmpty()) {
						slots.add(Integer.parseInt(subRange));
						continue;
					}
					final int first = Integer.parseInt(numbers [0]);
					final int second = Integer.parseInt(numbers [1]);
					slots.addAll(IntStream.range(first, second + 1).boxed().collect(Collectors.toList()));
				} else slots.add(Integer.parseInt(subRange));
			}
		} catch (final NumberFormatException e) {
			throw new ConfigError("Couldn't parse range " + range);
		}
		return slots;
	}

	@Override
	protected void saveDataToFile(final File file) {

	}

}
