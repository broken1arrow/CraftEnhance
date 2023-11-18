package com.dutchjelly.craftenhance.files;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.broken.arrow.menu.library.utility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.UUID;
import java.util.logging.Logger;

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

	private FileManager(final boolean useJson) {
		this.useJson = useJson;
	}

	public static FileManager init(final CraftEnhance main) {
//		FileManager fm = new FileManager(main.getConfig().getBoolean("use-json"));
		final FileManager fm = new FileManager(main.getConfig().getBoolean("use-json"));
		fm.items = new HashMap<>();
		fm.recipes = new ArrayList<>();
		fm.logger = main.getLogger();
		fm.dataFolder = main.getDataFolder();
		fm.dataFolder.mkdir();
		fm.itemsFile = fm.getFile(fm.useJson ? "items.json" : "items.yml");
		fm.recipesFile = fm.getFile("recipes.yml");
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
			if (ServerVersion.newerThan(ServerVersion.V1_8))
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
		Debug.Send("The file manager is caching recipes...");
		EnhancedRecipe keyValue;
		recipesConfig = getYamlConfig(recipesFile);
		recipes.clear();
		for (final String key : recipesConfig.getKeys(false)) {
			Debug.Send("Caching recipe with key " + key);
			keyValue = (EnhancedRecipe) recipesConfig.get(key);
			final String validation = keyValue.validate();
			if (validation != null) {
				Messenger.Error("Recipe with key " + key + " has issues: " + validation);
				Messenger.Error("This recipe will not be cached and loaded.");
				continue;
			}
			keyValue.setKey(key);
			recipes.add(keyValue);
		}
	}

	@SneakyThrows
	public void cacheItems() {

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
			return;
		}
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

	public Map<Location, UUID> getContainerOwners() {
		containerOwnerConfig = getYamlConfig(containerOwnerFile);
		final Map<Location, UUID> blockOwners = new HashMap<>();
		for (final String key : containerOwnerConfig.getKeys(false)) {
			if (key == null) continue;
			final String[] parsedKey = key.split(",");
			final World world = Bukkit.getServer().getWorld(UUID.fromString(parsedKey[3]));
			if ( world != null){
			final Location loc = new Location(
					world,
					Integer.parseInt(parsedKey[0]),
					Integer.parseInt(parsedKey[1]),
					Integer.parseInt(parsedKey[2]));
			blockOwners.put(loc, UUID.fromString(containerOwnerConfig.getString(key)));
			}
		}
		return blockOwners;
	}

	public boolean saveContainerOwners(final Map<Location, UUID> blockOwners) {
		containerOwnerConfig.getKeys(false).forEach(x -> containerOwnerConfig.set(x, null));
		for (final Map.Entry<Location, UUID> blockOwnerSet : blockOwners.entrySet()) {
			final Location key = blockOwnerSet.getKey();
			final String keyString = key.getBlockX() + "," + key.getBlockY() + "," + key.getBlockZ() + "," + key.getWorld().getUID();
			containerOwnerConfig.set(keyString, blockOwnerSet.getValue().toString());
		}
		try {
			containerOwnerConfig.save(containerOwnerFile);
		} catch (final IOException e) {
			return false;
		}
		return true;
	}

	public void saveRecipe(final EnhancedRecipe recipe) {
		Debug.Send("Saving recipe " + recipe.toString() + " with key " + recipe.getKey());
		String recipeKey = recipe.getKey();
		if (recipe.getKey().contains(".")) {
			recipeKey = recipeKey.replace(".", "_");
			Messenger.Message("your recipe key contains '.', it is removed now. Before " + recipe.getKey() + " after removed " + recipeKey);
			recipe.setKey(recipeKey);
		}

		recipesConfig = getYamlConfig(recipesFile);
		recipesConfig.set(recipeKey, recipe);
		try {
			recipesConfig.save(recipesFile);
			if (getRecipe(recipe.getKey()) == null)
				recipes.add(recipe);
			Debug.Send("Succesfully saved the recipe, there are now " + recipes.size() + " recipes cached.");
		} catch (final IOException e) {
			logger.severe("Error saving a recipe to the recipes.yml file.");
		}
	}

	public void removeRecipe(final EnhancedRecipe recipe) {
		Debug.Send("Removing recipe " + recipe.toString() + " with key " + recipe.getKey());
		String recipeKey = recipe.getKey();
		if (recipe.getKey().contains(".")) {
			recipeKey = recipeKey.replace(".", "_");
			Messenger.Message("your recipe key contains '.', it is removed now. Before " + recipe.getKey() + " after removed " + recipeKey);
			recipe.setKey(recipeKey);
		}

		recipesConfig = getYamlConfig(recipesFile);
		recipesConfig.set(recipeKey, null);
		recipes.remove(recipe);
		try {
			recipesConfig.save(recipesFile);
		} catch (final IOException e) {
			logger.severe("Error removing a recipe.");
		}
	}

	public void overrideSave() {
		Debug.Send("Overriding saved recipes with new list..");
		final List<EnhancedRecipe> cloned = new ArrayList<>();
		recipes.forEach(x -> cloned.add(x));
		removeAllRecipes();
		cloned.forEach(x -> saveRecipe(x));
		recipes = cloned;
		recipesConfig = getYamlConfig(recipesFile);
	}

	private void removeAllRecipes() {
		if (recipes.isEmpty()) return;
		removeRecipe(recipes.get(0));
		removeAllRecipes();
	}

	public void cleanItemFile() {
		Debug.Send("Cleaning up unused items.");
		for (final String itemKey : items.keySet()) {
			if (!isItemInUse(items.get(itemKey))) {
				Debug.Send("Item with key " + itemKey + " is not used and will be removed.");
				itemsConfig.set(itemKey, null);
				try {
					itemsConfig.save(itemsFile);
				} catch (final IOException e) {
					Debug.Send("Failed saving itemsConfig");
				}
			}
		}
	}

	private boolean isItemInUse(final ItemStack item) {
		for (final EnhancedRecipe r : recipes) {
			if (r.getResult().equals(item)) return true;
			for (final ItemStack inRecipe : r.getContent()) {
				if (inRecipe != null && inRecipe.equals(item)) return true;
			}

		}
		return false;
	}

}