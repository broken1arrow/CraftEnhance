package com.dutchjelly.craftenhance.messaging;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import lombok.NonNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dutchjelly.craftenhance.messaging.Debug.Type.*;

public class Debug {
	private static Map<Type, DebugConfig> CONFIGS;
	private static Logger logger;
	private static boolean startup_debug;
	private static boolean show_errors;
	private static boolean enable; //could be a config thing
	private static boolean enable_deap;
	private static boolean enable_brewing_debug;
	private static boolean enable_crafting_debug;
	private static boolean enable_smelting__debug;
	private static String prefix;

	public static void init(final CraftEnhance main) {
		startup_debug = main.getConfig().getBoolean("startup_debug");
		show_errors = main.getConfig().getBoolean("show_errors", false);
		enable = main.getConfig().getBoolean("enable-debug");
		enable_deap = main.getConfig().getBoolean("enable_debug_deap");
		enable_crafting_debug = main.getConfig().getBoolean("enable_crafting-debug");
		enable_smelting__debug = main.getConfig().getBoolean("enable_smelting-debug");
		enable_brewing_debug = main.getConfig().getBoolean("enable_brewing-debug");
		prefix = main.getConfig().getString("debug-prefix");
		logger = main.getLogger();
		rebuildConfigs();
	}

	public static void Send(final Object obj) {
		send(Type.Other, Type.Other.name(), () -> obj + "");
	}


	public static void Send(@NonNull final EnhancedRecipe recipe, final Supplier<String> obj) {
		final Type type = getDebugType(recipe);
		if (obj == null) {
			send(type, type.name(), () -> "message not set");
			return;
		}
		send(type, recipe.getKey(), obj);
	}

	public static void Send(final Type type, final Supplier<String> message) {
		if (message == null) {
			send(type, type.name(), () -> "message not set");
			return;
		}
		send(type, type.name(), message);
	}

	public static void send(final RecipeType type, final String action, Supplier<String> contextSupplier) {
		if (!anyDebugEnabled()) return;
		send(getDebugType(type), action, contextSupplier);
	}

	public static void send(Type type, final String action, final Supplier<String> supplier) {
		if (!anyDebugEnabled()) return;
		DebugConfig config = CONFIGS.get(type);
		if (config == null) {
			if (!enable) return;
			log(type, action, supplier);
			return;
		}
		if (!config.enabled) return;
		log(type, action, supplier);
	}

	private static void log(final Type type, final String action, final Supplier<String> supplier) {
		final String message = supplier != null ? supplier.get() : "missing message";
		final String debugHowName = action != null
				? " <" + action + "> "
				: " <" + type + "> ";

		final String output = prefix + "[" + type + "] " + debugHowName + message;
		logger.info(output);
	}


	public static void error(final String message) {
		if (show_errors)
			logger.log(Level.WARNING, prefix + message);
	}

	public static void errorDisablable(final String message, Throwable throwable) {
		if (show_errors)
			logger.log(Level.WARNING, prefix + message, throwable);
	}

	public static void error(final String message, Throwable throwable) {
		logger.log(Level.WARNING, prefix + message, throwable);
	}

	public static void info(final String message) {
		logger.log(Level.INFO, prefix + message);
	}

	public static boolean isGeneralDebugEnable() {
		return enable;
	}


	private static Type getDebugType(final @Nonnull EnhancedRecipe recipe) {
		return getDebugType(recipe.getType());
	}

	private static Type getDebugType(final RecipeType type) {
		if (type == null) return Other;

		switch (type) {
			case WORKBENCH:
				return Crafting;
			case FURNACE:
			case BLAST:
			case SMOKER:
				return Smelting;
			case BREWING:
				return Brewing;
		}
		return Other;
	}

	private static boolean anyDebugEnabled() {
		return enable ||
				enable_crafting_debug ||
				enable_smelting__debug ||
				enable_brewing_debug ||
				enable_deap ||
				startup_debug;
	}

	private static void rebuildConfigs() {
		CONFIGS = new HashMap<>();
		CONFIGS.put(Crafting, new DebugConfig(enable_crafting_debug, "crafting"));
		CONFIGS.put(Smelting, new DebugConfig(enable_smelting__debug, "furnace"));
		CONFIGS.put(Brewing, new DebugConfig(enable_brewing_debug, "brewing"));
		CONFIGS.put(Deep_lookup, new DebugConfig(enable_deap, "deep_lookup"));
		CONFIGS.put(Loading, new DebugConfig(startup_debug, "loading"));
		CONFIGS.put(Loading_yaml, new DebugConfig(startup_debug, "loading_yaml"));
	}


	private static class DebugConfig {
		final boolean enabled;
		final String label;

		DebugConfig(final boolean enabled, final String label) {
			this.enabled = enabled;
			this.label = label;

		}
	}

	public enum Type {
		Crafting,
		Smelting,
		Brewing,
		Other,
		Loading,
		Deep_lookup,
		Non, Loading_yaml;


	}

}
