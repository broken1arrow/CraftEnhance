package com.dutchjelly.craftenhance;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.cache.CacheRecipes;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commands.ceh.ChangeKeyCmd;
import com.dutchjelly.craftenhance.commands.ceh.CleanItemFileCmd;
import com.dutchjelly.craftenhance.commands.ceh.CreateRecipeCmd;
import com.dutchjelly.craftenhance.commands.ceh.Disabler;
import com.dutchjelly.craftenhance.commands.ceh.RecipesCmd;
import com.dutchjelly.craftenhance.commands.ceh.ReloadCmd;
import com.dutchjelly.craftenhance.commands.ceh.RemoveRecipeCmd;
import com.dutchjelly.craftenhance.commands.ceh.SetPermissionCmd;
import com.dutchjelly.craftenhance.commands.ceh.SpecsCommand;
import com.dutchjelly.craftenhance.commands.edititem.DisplayNameCmd;
import com.dutchjelly.craftenhance.commands.edititem.DurabilityCmd;
import com.dutchjelly.craftenhance.commands.edititem.EnchantCmd;
import com.dutchjelly.craftenhance.commands.edititem.ItemFlagCmd;
import com.dutchjelly.craftenhance.commands.edititem.LocalizedNameCmd;
import com.dutchjelly.craftenhance.commands.edititem.LoreCmd;
import com.dutchjelly.craftenhance.crafthandling.RecipeInjector;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.ServerLoadable;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.database.RecipeDatabase;
import com.dutchjelly.craftenhance.files.CategoryDataCache;
import com.dutchjelly.craftenhance.files.ConfigFormatter;
import com.dutchjelly.craftenhance.files.FileManager;
import com.dutchjelly.craftenhance.files.GuiTemplatesFile;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerCache;
import com.dutchjelly.craftenhance.gui.GuiManager;
import com.dutchjelly.craftenhance.gui.customcrafting.CustomCraftingTable;
import com.dutchjelly.craftenhance.gui.guis.editors.IngredientsCache;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.runnable.BrewingTask;
import com.dutchjelly.craftenhance.runnable.PlayerCheckTask;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.util.Metrics;
import lombok.Getter;
import org.broken.arrow.menu.library.RegisterMenuAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CraftEnhance extends JavaPlugin {

	private static CraftEnhance plugin;
	@Getter
	VersionChecker versionChecker;
	@Getter
	private SaveScheduler saveScheduler;
	@Getter
	private CacheRecipes cacheRecipes;
	@Getter
	private RecipeDatabase database;
	private Metrics metrics;
	@Getter
	private FileManager fm;
	@Getter
	private GuiManager guiManager;

	@Getter
	private GuiTemplatesFile guiTemplatesFile;
	private RegisterMenuAPI registerMenuAPI;
	private CustomCmdHandler commandHandler;
	private RecipeInjector injector;
	@Getter
	private boolean usingItemsAdder;
	private volatile boolean isReloading;
	@Getter
	private MenuSettingsCache menuSettingsCache;
	@Getter
	private CategoryDataCache categoryDataCache;
	@Getter
	private IngredientsCache ingredientsCache;
	@Getter
	private BrewingTask brewingTask;
	@Getter
	private BlockOwnerCache blockOwnerCache;

	public static CraftEnhance self() {
		return plugin;
	}

	@Override
	public void onEnable() {
		plugin = this;
		Debug.init(this);
		Messenger.Init(this);
		this.brewingTask = new BrewingTask();
		this.brewingTask.start();

		this.database = new RecipeDatabase();
		this.saveScheduler = new SaveScheduler();
		this.cacheRecipes = new CacheRecipes(this);
		new PlayerCheckTask().start();
		//The file manager needs serialization, so firstly register the classes.
		registerSerialization();
		versionChecker = VersionChecker.init(this);
		if (registerMenuAPI == null)
			registerMenuAPI = new RegisterMenuAPI(this);
		this.ingredientsCache = new IngredientsCache();
		if (this.blockOwnerCache == null)
			this.blockOwnerCache = new BlockOwnerCache();

		saveDefaultConfig();


		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			loadPluginData();
			loadRecipes();
		});

		if (injector == null)
			injector = new RecipeInjector(this);
		guiManager = new GuiManager(this);

		Debug.Send("Setting up listeners and commands");
		setupListeners();
		setupCommands();

		Messenger.Message("CraftEnhance is managed and developed by DutchJelly and BrokenArrow.");
		Messenger.Message("If you find a bug in the plugin, please report it to https://github.com/broken1arrow/CraftEnhance/issues.");
		if (!versionChecker.runVersionCheck()) {
			for (int i = 0; i < 4; i++)
				Messenger.Message("WARN: The installed version isn't tested to work with this version of the server.");
		}
		Bukkit.getScheduler().runTaskAsynchronously(this, versionChecker::runUpdateCheck);

		if (metrics == null) {
			final int metricsId = 9023;
			metrics = new Metrics(this, metricsId);
		}
		//CraftEnhanceAPI.registerListener(new ExecuteCommand());
		saveScheduler.start();
	}


	public void reload() {
		isReloading = true;
		Bukkit.getScheduler().runTask(this, () -> {
			this.reloadServerRecipes();
			reloadConfig();
			this.menuSettingsCache.reload();
			this.blockOwnerCache.reload();
			isReloading = false;
			injector.reload();
			reLearnRecipes();
		});
	}

	public void reloadServerRecipes() {
		RecipeLoader.clearInstance();
		RecipeLoader loader = RecipeLoader.getInstance();
		this.cacheRecipes.getRecipes().stream().filter(x -> x.validate() == null).forEach((recipe) -> loader.loadRecipe(recipe, isReloading));
		loader.printGroupsDebugInfo();
		loader.disableServerRecipes(
				fm.readDisabledServerRecipes().stream().map(x ->
						Adapter.FilterRecipes(loader.getServerRecipes(), x)
				).collect(Collectors.toList())
		);
	}

	public void reLearnRecipes() {
		//todo learn recipes are little broken. when you reload it. This is an attempt to force learn recipes too all players.
		if (!Bukkit.getOnlinePlayers().isEmpty() && self().getConfig().getBoolean("learn-recipes"))
			for (final Player player : Bukkit.getOnlinePlayers())
				Adapter.DiscoverRecipes(player, getCacheRecipes().getRecipes().stream()
								.filter(enhancedRecipe -> FormatListContents.canViewRecipe(enhancedRecipe,player))
						.map(ServerLoadable::getServerRecipe)
						.collect(Collectors.toList()));
	}

	@Override
	public void onDisable() {
		if (!this.isReloading)
			getServer().resetRecipes();
		this.saveAllData();
	}

	private void saveAllData() {
		Debug.info("Saving all data...");
		this.getBlockOwnerCache().save();
		fm.saveDisabledServerRecipes(RecipeLoader.getInstance().getDisabledServerRecipes().stream().map(x -> Adapter.GetRecipeIdentifier(x)).collect(Collectors.toList()));
		categoryDataCache.save();
		CompletableFuture<Void> saveTask = CompletableFuture.runAsync(() -> this.cacheRecipes.save());
		saveTask.join();
		Debug.info("Finish saving.");
	}

	@Override
	public boolean onCommand(@Nonnull final CommandSender sender, @Nonnull final Command cmd, @Nonnull final String label, @Nonnull final String[] args) {

		//Make sure that the user doesn't get a whole stacktrace when using an unsupported server jar.
		//Note that this error could only get caused by onEnable() not being called.
		if (commandHandler == null) {
			Messenger.Message("Could not execute the command.", sender);
			Messenger.Message("Something went wrong with initializing the commandHandler. Please make sure to use" +
					" Spigot or Bukkit when using this plugin. If you are using Spigot or Bukkit and still experiencing this " +
					"issue, please send a bug report here: https://dev.bukkit.org/projects/craftenhance.");
			Messenger.Message("Disabling the plugin...");
			getPluginLoader().disablePlugin(this);
		}

		commandHandler.handleCommand(sender, label, args);
		return true;
	}

	//Registers the classes that extend ConfigurationSerializable.
	private void registerSerialization() {
		ConfigurationSerialization.registerClass(WBRecipe.class, "EnhancedRecipe");
		ConfigurationSerialization.registerClass(WBRecipe.class, "Recipe");
		ConfigurationSerialization.registerClass(FurnaceRecipe.class, "FurnaceRecipe");
		ConfigurationSerialization.registerClass(BlastRecipe.class, "BlastRecipe");
		ConfigurationSerialization.registerClass(SmokerRecipe.class, "SmokerRecipe");
	}

	//Assigns executor classes for the commands.
	private void setupCommands() {
		commandHandler = new CustomCmdHandler(this);
		//All commands with the base /edititem
		commandHandler.loadCommandClasses(Arrays.asList(
				new DisplayNameCmd(commandHandler),
				new DurabilityCmd(commandHandler),
				new EnchantCmd(commandHandler),
				new ItemFlagCmd(commandHandler),
				new LocalizedNameCmd(commandHandler),
				new LoreCmd(commandHandler))
		);
		//All command with the base /ceh
		commandHandler.loadCommandClasses(Arrays.asList(
				new CreateRecipeCmd(commandHandler),
				new RecipesCmd(commandHandler),
				new SpecsCommand(commandHandler),
				new ChangeKeyCmd(commandHandler),
				new CleanItemFileCmd(commandHandler),
				new SetPermissionCmd(commandHandler),
				new ReloadCmd(),
				new Disabler(commandHandler),
				new RemoveRecipeCmd(commandHandler)
		));
	}

	//Registers the listener class to the server.
	private void setupListeners() {
	/*	HandlerList.unregisterAll(injector);
		HandlerList.unregisterAll(guiManager);
		HandlerList.unregisterAll(RecipeLoader.getInstance());*/
		guiManager = new GuiManager(this);
		getServer().getPluginManager().registerEvents(injector, this);
		getServer().getPluginManager().registerEvents(guiManager, this);
		//getServer().getPluginManager().registerEvents(RecipeLoader.getInstance(), this);
	}

	private void setupFileManager() {
		fm = FileManager.init(this);
		fm.cacheItems();
		fm.cacheRecipes();
	}

	private void loadPluginData() {
		if (categoryDataCache == null)
			categoryDataCache = new CategoryDataCache();
		categoryDataCache.reload();
		this.blockOwnerCache.reload();

		Debug.Send("Checking for config updates.");
		final File configFile = new File(getDataFolder(), "config.yml");
		FileManager.EnsureResourceUpdate("config.yml", configFile, YamlConfiguration.loadConfiguration(configFile), this);
		Debug.Send("Coloring config messages.");
		ConfigFormatter.init(this).formatConfigMessages();
		ItemMatchers.init(getConfig().getBoolean("enable-backwards-compatible-item-matching"));
		Debug.Send("Loading gui templates");

		if (menuSettingsCache == null)
			menuSettingsCache = new MenuSettingsCache(this);
	}

	private void loadRecipes() {
		this.usingItemsAdder = this.getServer().getPluginManager().getPlugin("ItemsAdder") != null;
		//Most other instances use the file manager, so setup before everything.
		Debug.Send("Setting up the file manager for recipes.");
		setupFileManager();
		Debug.Send("Loading recipes");
		final RecipeLoader loader = RecipeLoader.getInstance();
		final List<EnhancedRecipe> recipes = this.database.loadRecipes();
		this.cacheRecipes.addAll(recipes);
		Bukkit.getScheduler().runTask(this, () -> this.loadingRecipes(loader));
		injector.setLoader(loader);
		injector.reload();
	}

	private void loadingRecipes(final RecipeLoader loader) {
		this.cacheRecipes.getRecipes().stream().filter(x -> x.validate() == null).forEach((recipe) -> loader.loadRecipe(recipe, isReloading));
		loader.printGroupsDebugInfo();
		loader.disableServerRecipes(
				fm.readDisabledServerRecipes().stream().map(x ->
						Adapter.FilterRecipes(loader.getServerRecipes(), x)
				).collect(Collectors.toList())
		);
		this.cacheRecipes.setGroupCacheDirty(true);


	}

	public void openEnhancedCraftingTable(final Player p) {
		final CustomCraftingTable table = new CustomCraftingTable(
				getGuiManager(),
				getGuiTemplatesFile().getTemplate(null),
				null, p
		);
		getGuiManager().openGUI(p, table);
	}

}
