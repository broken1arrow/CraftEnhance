package com.dutchjelly.craftenhance.files;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.broken.arrow.library.menu.utility.ServerVersion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileManager {
	private final boolean useJson;

	private File dataFolder;
	private File itemsFile;
	private File recipesFile;
	private File containerOwnerFile;
	private File serverRecipeFile;
	private FileConfiguration recipesConfig;
	private FileConfiguration itemsConfig;
	private FileConfiguration serverRecipeConfig;
	private FileConfiguration containerOwnerConfig;

	private String itemsJson;
	private Logger logger;

	private Map<String, ItemStack> items;
	private List<EnhancedRecipe> recipes;
	private CacheRecipes cacheRecipes;

	private FileManager(final boolean useJson) {
		this.useJson = useJson;
	}

	public static FileManager init(final CraftEnhance main) {
//		FileManager fm = new FileManager(main.getConfig().getBoolean("use-json"));
		final FileManager fm = new FileManager(main.getConfig().getBoolean("use-json"));
		fm.items = new HashMap<>();
		fm.recipes = new ArrayList<>();
		fm.logger = main.getLogger();
		fm.cacheRecipes = main.getCacheRecipes();
		fm.dataFolder = main.getDataFolder();
		fm.dataFolder.mkdir();
		fm.itemsFile = new File(fm.dataFolder, fm.useJson ? "items.json" : "items.yml");
		fm.recipesFile = new File(fm.dataFolder, "recipes.yml");
		fm.serverRecipeFile = fm.getFile("server-recipes.yml");
		fm.containerOwnerFile = fm.getFile("container-owners.yml");
		return fm;
	}

	@SneakyThrows
	public static boolean EnsureResourceUpdate(final String resourceName, final File file, final FileConfiguration fileConfig, final JavaPlugin plugin) {
		if (!file.exists()) {
			plugin.saveResource(resourceName, false);
			return false;
		}

		final Reader jarConfigReader = new InputStreamReader(plugin.getResource(resourceName));
		final FileConfiguration jarResourceConfig = YamlConfiguration.loadConfiguration(jarConfigReader);
		jarConfigReader.close();

		boolean unsavedChanges = false;

		for (final String key : jarResourceConfig.getKeys(false)) {
			if (ServerVersion.newerThan(8.8))
				if (!fileConfig.contains(key, false)) {
					fileConfig.set(key, jarResourceConfig.get(key));
					unsavedChanges = true;
				} else {
					if (!fileConfig.contains(key)) {
						fileConfig.set(key, jarResourceConfig.get(key));
						unsavedChanges = true;
					}
				}
		}

		if (unsavedChanges)
			fileConfig.save(file);
		return true;
	}

	private File ensureCreated(final File file) {
		if (!file.exists()) {
			logger.info(file.getName() + " doesn't exist... creating it.");
			try {
				file.createNewFile();
			} catch (final IOException e) {
				logger.warning("The file " + file.getName()
						+ " couldn't be created!");
			}
		}
		return file;
	}

	private File getFile(final String name) {
		final File file = new File(dataFolder, name);
		ensureCreated(file);
		return file;
	}

	private FileConfiguration getYamlConfig(final File file) {
		return YamlConfiguration.loadConfiguration(file);
	}

	public void cacheRecipes() {
		Debug.send(Debug.Type.Loading_yaml, "Loading yml", () -> "The file manager is caching recipes...");
		EnhancedRecipe keyValue;
		if (!recipesFile.exists())
			return;

		recipesConfig = getYamlConfig(recipesFile);
		recipes.clear();
		for (final String key : recipesConfig.getKeys(false)) {
			Debug.send(Debug.Type.Loading_yaml, "Loading yml", () -> "Caching recipe with key " + key);
			keyValue = (EnhancedRecipe) recipesConfig.get(key);
			final String validation = keyValue.validate();
			if (validation != null) {
				Messenger.Error("Recipe with key " + key + " has issues: " + validation);
				Messenger.Error("This recipe will not be cached and loaded.");
				continue;
			}
			keyValue.setKey(key);
			cacheRecipes.add(keyValue);
			//recipes.add(keyValue);
		}

		cacheRecipes.save();
		this.recipesFile.renameTo(new File(dataFolder, "recipe_copy.yml"));
	}

	@SneakyThrows
	public void cacheItems() {
		if (!itemsFile.exists())
			return;

		if (useJson) {

			final StringBuilder json = new StringBuilder("");
			final Scanner scanner = new Scanner(itemsFile);
			while (scanner.hasNextLine())
				json.append(scanner.nextLine());
			scanner.close();
			items.clear();
			final Type typeToken = new TypeToken<HashMap<String, Map<String, Object>>>() {
			}.getType();
			final Gson gson = new Gson();
			final Map<String, Map<String, Object>> serialized = gson.fromJson(json.toString(), typeToken);
			if (serialized != null)
				serialized.keySet().forEach(x -> items.put(x, ItemStack.deserialize(serialized.get(x))));
		} else {
			if (itemsConfig == null)
				itemsConfig = new YamlConfiguration();
			itemsConfig.load(itemsFile);
			//itemsConfig = getYamlConfig(itemsFile);
			items.clear();
			if (itemsConfig != null)
				for (final String key : itemsConfig.getKeys(false)) {
					items.put(key, itemsConfig.getItemStack(key));
				}
		}

		this.itemsFile.renameTo(new File(dataFolder, useJson ? "items-copy.json" : "items-copy.yml"));
	}

	public Map<String, ItemStack> getItems() {
		return items;
	}

	public ItemStack getItem(final String key) {
		return items.get(key);
	}

	public String getItemKey(final ItemStack item) {
		if (item == null) return null;
		for (final String key : items.keySet()) {
			if (item.equals(items.get(key)))
				return key;
		}
		final String uniqueKey = getUniqueItemKey(item);
		saveItem(uniqueKey, item);
		return uniqueKey;
	}

	private String getUniqueItemKey(final ItemStack item) {
		if (item == null) return null;
		String base = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ?
				item.getItemMeta().getDisplayName() : item.getType().name();
		base = base.replaceAll("\\.", "");
		String unique = base;
		int incrementer = 1;
		while (items.keySet().contains(unique))
			unique = base + incrementer++;
		return unique;
	}

	public List<EnhancedRecipe> getRecipes() {
		return recipes;
	}

	public EnhancedRecipe getRecipe(final String key) {
		for (final EnhancedRecipe recipe : recipes) {
			if (recipe.getKey().equals(key))
				return recipe;
		}
		return null;
	}

	public boolean isUniqueRecipeKey(final String key) {
		return getRecipe(key) == null;
	}

	@SneakyThrows
	public boolean saveItem(final String key, final ItemStack item) {

		if (useJson) {
			items.put(key, item);
			final Gson gson = new Gson();
			final Map<String, Map<String, Object>> serialized = new HashMap<>();
			items.keySet().forEach(x -> serialized.put(x, items.get(x).serialize()));
			itemsJson = gson.toJson(serialized, new TypeToken<HashMap<String, Map<String, Object>>>() {
			}.getType());
			final FileWriter writer = new FileWriter(itemsFile);
			writer.write(itemsJson);
			writer.close();
			return true;
		}

		itemsConfig = getYamlConfig(itemsFile);
		if (!itemsConfig.contains(key)) {
			itemsConfig.set(key, item);
			try {
				itemsConfig.save(itemsFile);
				items.put(key, item);
				return true;
			} catch (final IOException e) {
				logger.severe("Error saving an item to the items.yml file.");
			}
		}
		return false;
	}

	public List<String> readDisabledServerRecipes() {
		if (serverRecipeConfig == null)
			serverRecipeConfig = getYamlConfig(serverRecipeFile);
		return serverRecipeConfig.getStringList("disabled");
	}

	public boolean saveDisabledServerRecipes(final List<String> keys) {
		serverRecipeConfig.set("disabled", keys);
		try {
			serverRecipeConfig.save(serverRecipeFile);
		} catch (final IOException e) {
			return false;
		}
		return true;
	}

	public boolean saveAllDisabledServerRecipes() {
		serverRecipeConfig.set("disabled", RecipeLoader.getInstance().getDisabledServerRecipes().stream().map(Adapter::GetRecipeIdentifier).collect(Collectors.toList()));
		try {
			serverRecipeConfig.save(serverRecipeFile);
		} catch (final IOException e) {
			return false;
		}
		return true;
	}

}