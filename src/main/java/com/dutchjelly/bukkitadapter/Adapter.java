package com.dutchjelly.bukkitadapter;


import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.util.SkullCreator;
import com.dutchjelly.craftenhance.itemcreation.EnchantmentUtil;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
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

	public final static String GUI_SKULL_MATERIAL_NAME = "GUI_SKULL_ITEM";
	private final static CraftEnhance plugin = self();
	private static Optional<Boolean> canUseModeldata = Optional.empty();

	public static List<String> CompatibleVersions() {
		return Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16", "1.17", "1.18", "1.19", "1.20", "1.21");
	}

	@Nullable
	public static ItemStack getItemStack(final String material, String displayName, List<String> lore, final String color, final boolean glow) {
		final ItemStack item;

		if (color == null) {
			item = getItemStack(material);
			if (item == null) return null;
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
				if (dColor != null) {
					item = Adapter.getColoredItem(material, dColor);
				} else {
					item = getItemStack(material);
				}
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
			if (self().getVersionChecker().newerThan(ServerVersion.v1_20)) {
				try {
					AttributeModifier dummyModifier = new AttributeModifier(UUID.randomUUID(), "dummy", 0, AttributeModifier.Operation.ADD_NUMBER);
					meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, dummyModifier);
				} catch (NoClassDefFoundError ex) {
					self().getLogger().warning("The AttributeModifier is no longer supported and the tooltip will probably be visible again.");
				}
				meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
			} else
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			if (glow) {
				if (self().getVersionChecker().newerThan(ServerVersion.v1_20)) {
					meta.addEnchant(Enchantment.AQUA_AFFINITY, 10, true);
				} else {
					meta.addEnchant(EnchantmentUtil.getByName("DURABILITY"), 10, true);
				}
			}
			for (ItemFlag flag : ItemFlag.values()) {
				meta.addItemFlags(flag);
			}

			item.setItemMeta(meta);
		}
		return item;
	}

	private static ItemStack getItemStack(final String material) {
		final Material mat = Adapter.getMaterial(material);
		if (mat == null) {
			Messenger.Error("Could not find the material. It is set too: " + material);
			return null;
		}
		final ItemStack item = new ItemStack(mat);
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
					Messenger.Error("Could not find " + name + " try load legacy support");
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
				case "BLAST_FURNACE":
				case "SMOKER":
					return Material.FURNACE;
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
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
		}
		return new ShapedRecipe(result);
	}

	public static ShapelessRecipe GetShapelessRecipe(final JavaPlugin plugin, final String key, final ItemStack result) {
		try {
			return ShapelessRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
		}
		return new ShapelessRecipe(result);
	}

	public static ItemStack SetDurability(final ItemStack item, final int damage) {
		item.setDurability((short) damage);
		return item;
	}

	public static void SetIngredient(final ShapedRecipe recipe, final char key, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR) return;

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
			final Class<?> recipeClass = recipe.getClass();
			final Class<?> recipeChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice");
			final Class<?> exactChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");

			Object choice = exactChoiceClass
					.getConstructor(ItemStack.class)
					.newInstance(ingredient);

			Method setIngredient = recipeClass.getMethod("setIngredient", char.class, recipeChoiceClass);
			setIngredient.invoke(recipe, key, choice);
		} catch (final Exception e) {
			recipe.setIngredient(key, ingredient.getType());
		}
	}

	public static <T extends CookingRecipe<?>> void setIngredient(final CookingRecipe<T> recipe, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR) return;

/*		if (!self().getConfig().getBoolean("learn-recipes")) {
			if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_14)) {
				if (ingredient == null) return;
				final Material md = ingredient.getType();
				if (md != ingredient.getType() || md == Material.AIR) {
					recipe.setInput( ingredient.getType());
				} else {
					recipe.setInput( md);
				}
				return;
			} else {
				final MaterialData md = ingredient.getData();
				if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
					recipe.setInput( ingredient.getType());
				} else {
					recipe.setInput( md.getItemType());
				}
			}
			return;
		}*/
		try {
			final Class<?> recipeClass = recipe.getClass();
			final Class<?> recipeChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice");
			final Class<?> exactChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");

			Object choice = exactChoiceClass
					.getConstructor(ItemStack.class)
					.newInstance(ingredient);

			Method setIngredient = recipeClass.getMethod("setInputChoice", recipeChoiceClass);
			setIngredient.invoke(recipe, choice);
		} catch (final Exception e) {
			e.printStackTrace();
			recipe.setInput(ingredient.getType());
		}
	}

	public static void AddIngredient(final ShapelessRecipe recipe, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR) return;

		if (!self().getConfig().getBoolean("learn-recipes")) {
			if (self().getVersionChecker().olderThan(ServerVersion.v1_16)) {
				final MaterialData md = ingredient.getData();
				if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
					recipe.addIngredient(ingredient.getType());
				} else {
					recipe.addIngredient(md);
				}
			} else {
				recipe.addIngredient(ingredient.getType());
			}
			return;
		}
		try {
			final Class<?> recipeClass = recipe.getClass();
			final Class<?> recipeChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice");
			final Class<?> exactChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");

			Object choice = exactChoiceClass
					.getConstructor(ItemStack.class)
					.newInstance(ingredient);

			Method setIngredient = recipeClass.getMethod("setIngredient", recipeChoiceClass);
			setIngredient.invoke(recipe, choice);
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
			org.bukkit.inventory.FurnaceRecipe furnaceRecipe = org.bukkit.inventory.FurnaceRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, Material.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, source.getType(), exp, duration);
			setIngredient(furnaceRecipe, source);
			return furnaceRecipe;
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			e.getStackTrace();
			Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
			//e.printStackTrace();
			final org.bukkit.inventory.FurnaceRecipe recipe = new org.bukkit.inventory.FurnaceRecipe(result, source.getType());
			if (!callSingleParamMethod("setCookingTime", duration, Integer.class, recipe, FurnaceRecipe.class))
				Debug.Send("Custom cooking time is not supported.");
			try {
				recipe.setExperience(exp);
			} catch (NoSuchMethodError ex) {
				Debug.Send("Set experience is not supported.");
			}
			return recipe;
		}
	}

	public static org.bukkit.inventory.BlastingRecipe getBlastRecipe(final JavaPlugin plugin, final String key, final ItemStack result, final ItemStack source, final int duration, final float exp) {
		try {
			return org.bukkit.inventory.BlastingRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, Material.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, source.getType(), exp, duration);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't set blastfuranace recipe. Wrong serverversion?");
			return null;
		}
	}

	public static org.bukkit.inventory.SmokingRecipe getSmokingRecipe(final JavaPlugin plugin, final String key, final ItemStack result, final ItemStack source, final int duration, final float exp) {
		try {
			return org.bukkit.inventory.SmokingRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, Material.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, source.getType(), exp, duration);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.Send("Couldn't set smoker recipe. Wrong serverversion?");
			return null;
		}
	}

	public static void DiscoverRecipes(final Player player, final List<Recipe> recipes) {
		try {
			boolean serverIsNewer = self().getVersionChecker().newerThan(ServerVersion.v1_12);
			if (!serverIsNewer)
				return;

			for (final Recipe recipe : recipes) {
				if (recipe instanceof ShapedRecipe) {
					final ShapedRecipe shaped = (ShapedRecipe) recipe;

					if (shaped.getKey().getNamespace().contains("craftenhance")) {
						player.discoverRecipe(shaped.getKey());
					}
				} else if (recipe instanceof ShapelessRecipe) {
					final ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
					if (shapeless.getKey().getNamespace().contains("craftenhance")) {
						player.discoverRecipe(shapeless.getKey());
					}
				} else if (recipe instanceof CookingRecipe) {
					final CookingRecipe<?> cookingRecipe = (CookingRecipe<?>) recipe;
					if (cookingRecipe.getKey().getNamespace().contains("craftenhance")) {
						player.discoverRecipe(cookingRecipe.getKey());
					}
				}
			}
		} catch (final Exception e) {
			Messenger.Error("Could not discover the recipe at the player.");
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

	public static <T extends CookingRecipe<?>> void setGroup(@NonNull final CookingRecipe<T> furnaceRecipe, @NonNull final String groupName) {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_12))
			furnaceRecipe.setGroup(groupName);
	}

	public static void setGroup(final Recipe recipe, final String groupName) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.newerThan(ServerVersion.v1_12)) {
			if (versionChecker.newerThan(ServerVersion.v1_19) && recipe instanceof CraftingRecipe) {
				((CraftingRecipe) recipe).setGroup(groupName);
			} else {
				if (recipe instanceof ShapedRecipe) {
					((ShapedRecipe) recipe).setGroup(groupName);
				}
				if (recipe instanceof ShapelessRecipe) {
					((ShapelessRecipe) recipe).setGroup(groupName);
				}
			}
		}
	}

	public static boolean isCraftingRecipe(final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.newerThan(ServerVersion.v1_19) && recipe instanceof CraftingRecipe) {
			return true;
		}
		return recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe;
	}

	public static boolean recipeContainsNamespace(final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (isCraftingRecipe(recipe)) {
			if (versionChecker.newerThan(ServerVersion.v1_19))
				return (((CraftingRecipe) recipe).getKey().getNamespace().contains("craftenhance") || ((CraftingRecipe) recipe).getKey().getNamespace().contains("cehrecipe"));

			if (recipe instanceof ShapedRecipe) {
				return (((ShapedRecipe) recipe).getKey().getNamespace().contains("craftenhance") || ((ShapedRecipe) recipe).getKey().getNamespace().contains("cehrecipe"));
			}

			if (recipe instanceof ShapelessRecipe) {
				return (((ShapelessRecipe) recipe).getKey().getNamespace().contains("craftenhance") || ((ShapelessRecipe) recipe).getKey().getNamespace().contains("cehrecipe"));
			}
		}
		return 	recipe instanceof CookingRecipe && (((CookingRecipe<?>) recipe).getKey().getNamespace().contains("craftenhance") || ((CookingRecipe<?>) recipe).getKey().getNamespace().contains("cehrecipe"));
	}
}
