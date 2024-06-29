package com.dutchjelly.craftenhance.files;

import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import org.broken.arrow.menu.button.manager.library.MenusSettingsHandler;
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
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MenuSettingsCache  {

	private final Plugin plugin;
	private static final int version = 13;
	private final Map<String, MenuTemplate> templates = new HashMap<>();
	private final MenusSettingsHandler menusSettingsHandler;


	public MenuSettingsCache(final Plugin plugin) {
		this.menusSettingsHandler = new MenusSettingsHandler(plugin,"guitemplates.yml",true);
		this.plugin = plugin;
		checkFileVersion();
		this.menusSettingsHandler.reload();

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
	public org.broken.arrow.menu.button.manager.library.utility.MenuTemplate getTemplate(String menu) {
		return menusSettingsHandler.getTemplate(menu);
	}

	public void reload() {
		menusSettingsHandler.reload();
	}
}
