package com.dutchjelly.craftenhance.messaging;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import lombok.NonNull;

import javax.annotation.Nonnull;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dutchjelly.craftenhance.messaging.Debug.Type.*;

public class Debug {

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
		show_errors = main.getConfig().getBoolean("show_errors", true);
		enable = main.getConfig().getBoolean("enable-debug");
		enable_deap = main.getConfig().getBoolean("enable_debug_deap");
		enable_crafting_debug = main.getConfig().getBoolean("enable_crafting-debug");
		enable_smelting__debug = main.getConfig().getBoolean("enable_smelting-debug");
		enable_brewing_debug = main.getConfig().getBoolean("enable_brewing-debug");
		prefix = main.getConfig().getString("debug-prefix");
		logger = main.getLogger();
	}

	public static void Send(final Object obj) {
		Send(Type.Other, obj);
	}

	public static void Send(@NonNull final Debug.DebugContext debugContext, final Supplier<Object> obj) {
		final String typeOfAction = debugContext.getTypeOfAction();
		final Type type = debugContext.getType();
		Send(type, typeOfAction, obj);
	}

	public static void Send(@NonNull final EnhancedRecipe recipe, final Supplier<Object> obj) {
		final Type type = getDebugType(recipe);
		if (obj == null) {
			Send(type, "null");
			return;
		}
		Send(type, recipe.getKey(), obj);
	}

	public static void Send(final Type type, final Supplier<Object> obj) {
		if (obj == null) {
			Send(type, "null");
			return;
		}
		Send(type, "", obj);
	}

	private static void Send(final Type type, final String name, final Supplier<Object> obj) {
		String debugHowName = "";
		if (name != null)
			debugHowName = " <" + name + "> ";

		if (type == Crafting) {
			if (enable_crafting_debug)
				System.out.println(prefix + "[crafting] " + debugHowName + (obj != null ? obj.get().toString() : "null"));
			return;
		}
		if (type == Smelting) {
			if (enable_smelting__debug)
				System.out.println(prefix + "[furnace] " + debugHowName + (obj != null ? obj.get().toString() : "null"));
			return;
		}
		if (type == Brewing) {
			if (enable_brewing_debug)
				System.out.println(prefix + "[brewing] " + debugHowName + (obj != null ? obj.get().toString() : "null"));
			return;
		}
		if (type == Deep_lookup) {
			if (enable_deap)
				System.out.println(prefix + "[deep_lookup] " + debugHowName + (obj != null ? obj.get().toString() : "null"));
			return;
		}
		if (type == Loading) {
			if (startup_debug)
				logger.info(prefix + "[loading] " + debugHowName + (obj != null ? obj.get().toString() : "null"));
			return;
		}
		if (!enable) return;
		logger.info(prefix + debugHowName + (obj != null ? obj.get().toString() : "null"));
	}

	private static void Send(final Object sender, final Object obj) {
		if (!enable) return;

		logger.info(prefix + "<" + sender.getClass().getName() + "> " + (obj != null ? obj.toString() : "null"));
	}

	private static void Send(final Object[] arr) {
		if (arr == null) return;
		logger.info(prefix + " ");
		for (final Object o : arr) {
			if (o == null) continue;
			logger.info(o.toString());
		}
		logger.info("");
	}

	public static void error(final String message) {
		if (show_errors)
			logger.log(Level.WARNING, prefix + message);
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
		switch (recipe.getType()) {
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


	public enum Type {
		Crafting,
		Smelting,
		Brewing,
		Other,
		Loading,
		Deep_lookup,
		Non, Loading_yaml;
	}

	public static class DebugContext {
		private final Type type;
		private final String typeOfAction;

		private DebugContext(@NonNull final Type type, @Nonnull final String typeOfAction) {
			this.type = type;
			this.typeOfAction = typeOfAction;
		}

		public static DebugContext of(@NonNull final Type type, @Nonnull final String typeOfAction) {
			return new DebugContext(type, typeOfAction);
		}

		public Type getType() {
			return type;
		}

		public String getTypeOfAction() {
			return typeOfAction;
		}
	}
}
