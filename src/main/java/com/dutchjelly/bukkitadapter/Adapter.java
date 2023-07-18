package com.dutchjelly.bukkitadapter;


import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.util.SkullCreator;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class Adapter {


	public static List<String> CompatibleVersions() {
		return Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16", "1.17", "1.18", "1.19","1.20");
	}

	public final static String GUI_SKULL_MATERIAL_NAME = "GUI_SKULL_ITEM";

	@Nullable
	public static ItemStack getItemStack(final String material, String displayName, List<String> lore, final String color, final boolean glow) {
		final ItemStack item;

		if (color == null) {
			final Material mat = Adapter.getMaterial(material);
			if (mat == null) {
				Messenger.Error("Could not find " + material);
				return null;
			}
			item = new ItemStack(mat);
		} else {
			//Tricky way to support skull meta's using the color attribute as data.
			if (material.equalsIgnoreCase(GUI_SKULL_MATERIAL_NAME)) {
				if (color.startsWith("uuid"))
					item = SkullCreator.itemFromUuid(UUID.fromString(color.replaceFirst("uuid", "")));
				else if (color.startsWith("base64"))
					item = SkullCreator.itemFromBase64(color.replaceFirst("base64", ""));
				else if (color.startsWith("url"))
					item = SkullCreator.itemFromUrl(color.replaceFirst("url", ""));
				else throw new ConfigError("specified skull meta is invalid");
			} else {
				final DyeColor dColor = dyeColor(color);
				if (dColor == null) throw new ConfigError("color " + color + " not found");
				item = Adapter.getColoredItem(material, dColor);
			}
		}
		if (item == null) return null;

		if (lore != null)
			lore = lore.stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList());

		if (displayName != null)
			displayName = ChatColor.translateAlternateColorCodes('&', displayName);

		final ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setLore(lore == null ? new ArrayList<>() : lore); //avoid null lore

			meta.setDisplayName(displayName);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			if (glow)
				meta.addEnchant(Enchantment.DURABILITY, 10, true);
			item.setItemMeta(meta);
		}
		return item;
	}

	@Nullable
	public static DyeColor dyeColor(final String dyeColor) {
		final DyeColor[] dyeColors = DyeColor.values();

		for (final DyeColor color : dyeColors) {
			if (color.name().equalsIgnoreCase(dyeColor)) {
				return color;
			}
		}
		return null;
	}

	@Nullable
	public static Material getMaterial(String name) {
		if (name == null) return null;
		name = name.toUpperCase();
		final Material material = Material.getMaterial(name);
		if (material != null) {
			if (self().getVersionChecker().olderThan(VersionChecker.ServerVersion.v1_13)) {
				if (name.equals("WRITTEN_BOOK"))
					return Material.valueOf("PAPER");
			}
			return material;
		}
		if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_12)) {
			switch (name) {
				case "WORKBENCH":
					return Material.valueOf("CRAFTING_TABLE");
				case "WEB":
					return Material.valueOf("COBWEB");
				case "EXP_BOTTLE":
					return Material.valueOf("EXPERIENCE_BOTTLE");
				case "BOOK_AND_QUILL":
					return Material.valueOf("WRITABLE_BOOK");
				default:
					Messenger.Error("Could not find " + name + " try load legacy suport");
					return Material.matchMaterial("LEGACY_" + name);
			}
		} else {
			switch (name) {
				case "CRAFTING_TABLE":
					return Material.valueOf("WORKBENCH");
				case "WEB":
					return Material.valueOf("COBWEB");
				case "EXP_BOTTLE":
					return Material.valueOf("EXPERIENCE_BOTTLE");
				case "CLOCK":
					return Material.valueOf("WATCH");
				case "WRITABLE_BOOK":
					return Material.valueOf("BOOK_AND_QUILL");
				case "WRITTEN_BOOK":
				case "BOOK":
					return Material.valueOf("COAL");
			}
		}
		return null;
	}

	public static ItemStack getColoredItem(final String name, final DyeColor color) {
		try {
			return new ItemStack(Material.valueOf(color.name() + "_" + name));
		} catch (final Exception e) {
			try {
				return new ItemStack(Material.valueOf(name), 1, (short) color.getWoolData());
			} catch (final Exception e2) {
			}
		}
		return null;
	}

	private static Optional<Boolean> canUseModeldata = Optional.empty();

	public static boolean canUseModeldata() {
		if (canUseModeldata.isPresent()) {
			return canUseModeldata.get();
		}
		try {
			ItemMeta.class.getMethod("getCustomModelData");
			canUseModeldata = Optional.of(true);
			return true;
		} catch (final NoSuchMethodException e) {
			canUseModeldata = Optional.of(false);
			return false;
		}
	}

	private static Object getNameSpacedKey(final JavaPlugin plugin, final String key) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//        return new NamespacedKey(plugin, key);
		return Class.forName("org.bukkit.NamespacedKey").getConstructor(org.bukkit.plugin.Plugin.class, String.class).newInstance(plugin, key);
	}

	public static ShapedRecipe GetShapedRecipe(final JavaPlugin plugin, final String key, final ItemStack result) {
		try {
			return ShapedRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
		}
		return new ShapedRecipe(result);
	}

	public static ShapelessRecipe GetShapelessRecipe(final JavaPlugin plugin, final String key, final ItemStack result) {
		try {
			return ShapelessRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
		}
		return new ShapelessRecipe(result);
	}

	public static ItemStack SetDurability(final ItemStack item, final int damage) {
		item.setDurability((short) damage);
		return item;
	}

	public static void SetIngredient(final ShapedRecipe recipe, final char key, final ItemStack ingredient) {
		if (!self().getConfig().getBoolean("learn-recipes")) {
			if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_14)) {
				if (ingredient == null) return;
				final Material md = ingredient.getType();
				if (md != ingredient.getType() || md == Material.AIR) {
					recipe.setIngredient(key, ingredient.getType());
				} else {
					recipe.setIngredient(key, md);
				}
				return;
			} else {
				final MaterialData md = ingredient.getData();
				if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
					recipe.setIngredient(key, ingredient.getType());
				} else {
					recipe.setIngredient(key, md);
				}
			}
			return;
		}
		try {
			recipe.getClass().getMethod("setIngredient", char.class, Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice")).invoke(recipe,
					key, Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice").getConstructor(ItemStack.class).newInstance(ingredient)
			);
		} catch (final Exception e) {
			recipe.setIngredient(key, ingredient.getType());
		}
	}

	public static void AddIngredient(final ShapelessRecipe recipe, final ItemStack ingredient) {
		if (!self().getConfig().getBoolean("learn-recipes")) {
			final MaterialData md = ingredient.getData();
			if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
				recipe.addIngredient(ingredient.getType());
			} else {
				recipe.addIngredient(md);
			}
			return;
		}
		try {
			recipe.getClass().getMethod("addIngredient", Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice")).invoke(recipe,
					Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice").getConstructor(ItemStack.class).newInstance(ingredient)
			);
		} catch (final Exception e) {
			recipe.addIngredient(ingredient.getType());
		}
	}


	private static <T> boolean callSingleParamMethod(final String methodName, final T param, final Class<T> paramType, final Object instance, final Class<?> instanceType) {
		try {
			final Method m = instanceType.getMethod(methodName, paramType);
			m.invoke(instance, param);
			return true;
		} catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			return false;
		}
	}

	public static org.bukkit.inventory.FurnaceRecipe GetFurnaceRecipe(final JavaPlugin plugin, final String key, final ItemStack result, final ItemStack source, final int duration, final float exp) {
		//public FurnaceRecipe(@NotNull NamespacedKey key, @NotNull ItemStack result, @NotNull Material source, float experience, int cookingTime) {
		try {

		/*	return org.bukkit.inventory.FurnaceRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, RecipeChoice.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, new RecipeChoice.ExactChoice(Collections.singletonList(source)), exp, duration);*/
			return org.bukkit.inventory.FurnaceRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, Material.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, source.getType(), exp, duration);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
			//e.printStackTrace();
			final org.bukkit.inventory.FurnaceRecipe recipe = new org.bukkit.inventory.FurnaceRecipe(result, source.getType());
			if (!callSingleParamMethod("setCookingTime", duration, Integer.class, recipe, FurnaceRecipe.class))
				Debug.Send("Custom cooking time is not supported.");
			recipe.setExperience(exp);
			return recipe;
		}
	}

	public static void DiscoverRecipes(final Player player, final List<Recipe> recipes) {
		try {
			for (final Recipe recipe : recipes) {
				if (recipe instanceof ShapedRecipe) {
					final ShapedRecipe shaped = (ShapedRecipe) recipe;;
					if (shaped.getKey().getNamespace().contains("craftenhance")) {
						player.discoverRecipe(shaped.getKey());
					}
				} else if (recipe instanceof ShapelessRecipe) {
					final ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
					if (shapeless.getKey().getNamespace().contains("craftenhance")) {
						player.discoverRecipe(shapeless.getKey());
					}
				}
			}
		} catch (final Exception e) {
		}
	}

	public static void SetOwningPlayer(final SkullMeta meta, final OfflinePlayer player) {
		try {
			meta.setOwningPlayer(player);
		} catch (final Exception e) {
			meta.setOwner(player.getName());
		}
	}

	public static Recipe FilterRecipes(final List<Recipe> recipes, final String name) {
		for (final Recipe r : recipes) {
			final String id = GetRecipeIdentifier(r);
			if (id == null) continue;
			if (id.equalsIgnoreCase(name))
				return r;
		}

		return recipes.stream().filter(x -> x != null).filter(x -> x.getResult().getType().name().equalsIgnoreCase(name)).findFirst().orElse(null);

	}

	public static boolean ContainsSubKey(final Recipe r, final String key) {
		final String keyString = GetRecipeIdentifier(r);
		return keyString == null ? key == null : keyString.contains(key);
	}

	public static String GetRecipeIdentifier(final Recipe r) {
		try {
			//reflection is so damn powerful!! You can even invoke methods from derived classes.
			final Object obj = r.getClass().getMethod("getKey").invoke(r);
			if (obj != null) return obj.toString();
		} catch (final Exception e) {
		}

		return r.getResult().getType().name();
	}

}
