package com.dutchjelly.bukkitadapter;


import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.util.SkullCreator;
import com.dutchjelly.craftenhance.itemcreation.EnchantmentUtil;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class Adapter {

	public final static String GUI_SKULL_MATERIAL_NAME = "GUI_SKULL_ITEM";
	private final static CraftEnhance plugin = self();
	private static Optional<Boolean> canUseModeldata = Optional.empty();
	private static Method InventoryView;

	static {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_19)) {
			try {
				InventoryView = InventoryEvent.class.getMethod("getView");
			} catch (NoSuchMethodException e) {
				Messenger.Error("Could not find the view for the inventory event:\n " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

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

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setLore(lore == null ? new ArrayList<>() : lore); //avoid null lore

			meta.setDisplayName(displayName);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			if (self().getVersionChecker().newerThan(ServerVersion.v1_20)) {
				try {
					AttributeModifier dummyModifier = new AttributeModifier(UUID.randomUUID(), "dummy", 0, AttributeModifier.Operation.ADD_NUMBER);
					meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, dummyModifier);
				} catch (NoClassDefFoundError ex) {
					Debug.error("The AttributeModifier is no longer supported and the tooltip will probably be visible again.");
				} catch (NoSuchFieldError ex) {
					meta = getItemMeta(item, meta);
					//self().getLogger().warning("The AttributeModifier MOVEMENT_SPEED does not exist, the tooltip is probably not hidden.");
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
				case "REDSTONE_TORCH":
					return Material.getMaterial("REDSTONE_TORCH_ON");
				case "BLAST_FURNACE":
				case "SMOKER":
					return Material.FURNACE;
				case "OAK_LOG":
					return Material.getMaterial("LOG");
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

	public static boolean containsModelData(final ItemStack[] matrix) {
		if (!Adapter.canUseModeldata())
			return false;
		if (self().getVersionChecker().olderThan(ServerVersion.v1_14)) {
			return Arrays.stream(matrix).anyMatch(x -> x != null && x.hasItemMeta() && x.getDurability() > 0 && !hasDamageTag(x));
		}
		return Arrays.stream(matrix).anyMatch(x -> x != null && x.hasItemMeta() && x.getItemMeta().hasCustomModelData());
	}

	/**
	 * Gets the top inventory from the InventoryView of an InventoryEvent,
	 * using reflection to stay compatible with both old and new Spigot versions.
	 *
	 * @param event The InventoryEvent
	 * @return The top inventory, or null if unavailable
	 */
	@Nullable
	public static Inventory getTopInventory(InventoryEvent event) {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_19)) {
			try {
				if (InventoryView == null)
					return null;
				// Use reflection to avoid linking to InventoryView directly
				Object view = InventoryView.invoke(event);
				return (Inventory) view.getClass().getMethod("getTopInventory").invoke(view);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				Debug.error("Could not retrieve the view for the inventory event:\n ", e);
				return null;
			}
		} else {
			return event.getView().getTopInventory();
		}
	}

	public static ShapedRecipe GetShapedRecipe(final JavaPlugin plugin, final String key, final ItemStack result) {
		try {
			return ShapedRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.errorDisablable("Couldn't use namespaced key for shaped recipe: '" + key + "' will fallback to legacy option\n", e);
		}
		return new ShapedRecipe(result);
	}

	public static ShapelessRecipe GetShapelessRecipe(final JavaPlugin plugin, final String key, final ItemStack result) {
		try {
			return ShapelessRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.errorDisablable("Couldn't use namespaced key for shapeless recipe: '" + key + "' will fallback to legacy option\n", e);
		}
		return new ShapelessRecipe(result);
	}


	public static org.bukkit.inventory.FurnaceRecipe getFurnaceRecipe(@NonNull final FurnaceRecipe enhancedFurnaceRecipe) {
		final String key = ServerRecipeTranslator.GetFreeKey(enhancedFurnaceRecipe.getKey());
		final ItemStack result = enhancedFurnaceRecipe.getResult();
		final ItemStack ingredient = enhancedFurnaceRecipe.getContent()[0];
		final int duration = enhancedFurnaceRecipe.getDuration();
		final float exp = enhancedFurnaceRecipe.getExp();

		try {
			org.bukkit.inventory.FurnaceRecipe furnaceRecipe = org.bukkit.inventory.FurnaceRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, RecipeChoice.class, float.class, int.class)
					.newInstance(getNameSpacedKey(plugin, key), result, new RecipeChoice.ExactChoice(ingredient), exp, duration);
			setIngredient(furnaceRecipe, ingredient);
			return furnaceRecipe;
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.errorDisablable("Couldn't use namespaced key for furnace recipe:' " + key + "' will fallback to legacy option\n", e);

			final org.bukkit.inventory.FurnaceRecipe recipe = new org.bukkit.inventory.FurnaceRecipe(result, ingredient.getType());
			if (!callSingleParamMethod("setCookingTime", duration, Integer.class, recipe, FurnaceRecipe.class))
				Debug.send(Type.Other, "furnace recipe registering", () -> "Custom cooking time is not supported.");
			try {
				recipe.setExperience(exp);
			} catch (NoSuchMethodError ex) {
				Debug.send(Type.Other, "furnace recipe registering", () -> "Set experience is not supported.");
			}
			return recipe;
		}
	}

	public static org.bukkit.inventory.BlastingRecipe getBlastRecipe(@NonNull final BlastRecipe blastRecipe) {
		final String key = ServerRecipeTranslator.GetFreeKey(blastRecipe.getKey());
		final ItemStack result = blastRecipe.getResult();
		final ItemStack ingredient = blastRecipe.getContent()[0];
		final int duration = blastRecipe.getDuration();
		final float exp = blastRecipe.getExp();

		try {
			return org.bukkit.inventory.BlastingRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, RecipeChoice.class, float.class, int.class)
					.newInstance(getNameSpacedKey(CraftEnhance.self(), key), result, new RecipeChoice.ExactChoice(ingredient), exp, duration);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.send(Type.Other, "blast recipe registering", () -> "Couldn't set blast furnace recipe. Wrong server version?");
			return null;
		}
	}

	public static org.bukkit.inventory.SmokingRecipe getSmokingRecipe(@NonNull final SmokerRecipe smokerRecipe) {
		final String key = ServerRecipeTranslator.GetFreeKey(smokerRecipe.getKey());
		final ItemStack result = smokerRecipe.getResult();
		final ItemStack ingredient = smokerRecipe.getContent()[0];
		final int duration = smokerRecipe.getDuration();
		final float exp = smokerRecipe.getExp();

		try {
			return org.bukkit.inventory.SmokingRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class, RecipeChoice.class, float.class, int.class)
					.newInstance(getNameSpacedKey(CraftEnhance.self(), key), result, new RecipeChoice.ExactChoice(ingredient), exp, duration);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
		               NoSuchMethodException | ClassNotFoundException e) {
			Debug.send(Type.Other, "smoker recipe registering", () -> "Couldn't set smoker recipe. Wrong server version?");
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

	public static Recipe FilterRecipes(final Set<Recipe> recipes, final String name) {
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

	public static NamespacedKey getNamespacedKey(final Recipe recipe) {
		if (self().getVersionChecker().olderThan(ServerVersion.v1_13)) {
			return recipe.getResult().getType().getKey();
		}
		if (recipe instanceof Keyed) {
			return ((Keyed) recipe).getKey();
		}
		return null;
	}

	public static ItemStack SetDurability(final ItemStack item, final int damage) {
		item.setDurability((short) damage);
		return item;
	}

	public static boolean hasDamageTag(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) return false;
		Material mat = item.getType();
		String name = mat.name();

		if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")
				|| name.endsWith("_SPADE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")
				|| name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
				|| name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
			return true;
		}
		switch (name) {
			case "BOW":
			case "FISHING_ROD":
			case "SHEARS":
			case "FLINT_AND_STEEL":
			case "CARROT_STICK":
			case "CARROT_ON_A_STICK":
			case "SHIELD":
			case "ELYTRA":
				return true;
			default:
				return false;
		}
	}

	public static String getGroup(@NonNull final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.newerThan(ServerVersion.v1_12)) {
			if (recipe instanceof CookingRecipe<?>)
				return ((CookingRecipe<?>) recipe).getGroup();
			if (versionChecker.newerThan(ServerVersion.v1_19) && recipe instanceof CraftingRecipe)
				return ((CraftingRecipe) recipe).getGroup();
			if (recipe instanceof ShapedRecipe)
				return ((ShapedRecipe) recipe).getGroup();
			if (recipe instanceof ShapelessRecipe)
				return ((ShapelessRecipe) recipe).getGroup();
		}
		return "";
	}

	public static void setGroup(final Recipe recipe, final String groupName) {
		if (groupName.isEmpty()) return;

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
		if (isCookingRecipe(recipe)) {
			if (versionChecker.newerThan(ServerVersion.v1_12))
				setGroup((CookingRecipe<?>) recipe, groupName);
		/*	else
				((FurnaceRecipe) recipe).setGroup(groupName);*/
		}
	}

	private static <T extends CookingRecipe<?>> void setGroup(@NonNull final CookingRecipe<T> furnaceRecipe, @NonNull final String groupName) {
		FurnaceWrapper.setGroup(furnaceRecipe, groupName);
	}

	public static void setIngredient(final ShapedRecipe recipe, final char key, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR) return;

		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
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
				Debug.errorDisablable("Couldn't use full metadata for shaped recipe ingredient:' " + ingredient.getType() + "', will try to set type\n", e);
				recipe.setIngredient(key, ingredient.getType());
			}
		} else {
			final MaterialData md = ingredient.getData();
			if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
				recipe.setIngredient(key, ingredient.getType());
			} else {
				recipe.setIngredient(key, md);
			}
		}
	}

	public static void setIngredient(final Recipe recipe, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR || !(recipe instanceof org.bukkit.inventory.FurnaceRecipe)) return;
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
			FurnaceWrapper.setFurnaceIngredients((CookingRecipe<?>) recipe, ingredient);
		else
			FurnaceWrapper.setFurnaceIngredients((org.bukkit.inventory.FurnaceRecipe) recipe, ingredient);
	}

	public static void addIngredient(final ShapelessRecipe recipe, final ItemStack ingredient) {
		if (ingredient.getType() == Material.AIR) return;

		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			try {
				final Class<?> recipeClass = recipe.getClass();
				final Class<?> recipeChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice");
				final Class<?> exactChoiceClass = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");

				Object choice = exactChoiceClass
						.getConstructor(ItemStack.class)
						.newInstance(ingredient);

				Method setIngredient = recipeClass.getMethod("addIngredient", recipeChoiceClass);
				setIngredient.invoke(recipe, choice);
			} catch (final Exception e) {
				Debug.errorDisablable("Couldn't use full metadata for shapeless recipe ingredient:' " + ingredient.getType() + "', will try to set type\n", e);
				recipe.addIngredient(ingredient.getType());
			}
		} else {
			final MaterialData md = ingredient.getData();
			if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
				recipe.addIngredient(ingredient.getType());
			} else {
				recipe.addIngredient(md);
			}
		}
	}

	public static ItemStack[] getIngredients(@NonNull final Recipe recipe) {
		if (isCookingRecipe(recipe)) {
			return FurnaceWrapper.getFurnaceStack(recipe);
		}
		if (recipe instanceof ShapedRecipe) {
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
				return ((ShapedRecipe) recipe).getChoiceMap().values().stream()
						.flatMap(choice -> {
							if (choice == null)
								return null;
							if (choice instanceof RecipeChoice.MaterialChoice) {
								return ((RecipeChoice.MaterialChoice) choice).getChoices().stream()
										.map(ItemStack::new);
							} else if (choice instanceof RecipeChoice.ExactChoice) {
								return ((RecipeChoice.ExactChoice) choice).getChoices().stream()
										.map(ItemStack::new);
							} else
								return Stream.of(choice.getItemStack());
						}).toArray(ItemStack[]::new);
			}
			return ((ShapedRecipe) recipe).getIngredientMap().values().toArray(new ItemStack[0]);
		}
		if (recipe instanceof ShapelessRecipe) {
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
				return ((ShapelessRecipe) recipe).getChoiceList().stream()
						.flatMap(choice -> {
							if (choice == null)
								return null;
							if (choice instanceof RecipeChoice.MaterialChoice) {
								return ((RecipeChoice.MaterialChoice) choice).getChoices().stream()
										.map(ItemStack::new);
							} else if (choice instanceof RecipeChoice.ExactChoice) {
								return ((RecipeChoice.ExactChoice) choice).getChoices().stream()
										.map(ItemStack::new);
							} else
								return Stream.of(choice.getItemStack());
						}).toArray(ItemStack[]::new);
			}
			return ((ShapelessRecipe) recipe).getIngredientList().toArray(new ItemStack[0]);
		}
		return new ItemStack[0];
	}

	public static List<ItemStack> getIngredientsList(@NonNull final Recipe recipe) {
		if (isCookingRecipe(recipe)) {
			return Arrays.asList(FurnaceWrapper.getFurnaceStack(recipe));
		}
		if (recipe instanceof ShapedRecipe)
			return new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values());
		if (recipe instanceof ShapelessRecipe)
			return ((ShapelessRecipe) recipe).getIngredientList();
		return Collections.emptyList();
	}

	public static EnumMap<Material, Integer> getFullIngredientsList(@NonNull final Recipe recipe) {
		VersionChecker versionChecker = self().getVersionChecker();
		EnumMap<Material, Integer> map = new EnumMap<>(Material.class);
		if (isCookingRecipe(recipe)) {
			ItemStack furnaceStack = FurnaceWrapper.getFurnaceStack(recipe)[0];
			if (furnaceStack == null) return map;
			map.put(furnaceStack.getType(), 1);
			return map;
		}

		final Set<Material> materials = new HashSet<>();
		int amount = 0;

		if (versionChecker.newerThan(ServerVersion.v1_13)) {
			if (recipe instanceof ShapedRecipe) {
				ShapedRecipe shaped = (ShapedRecipe) recipe;
				for (Entry<Character, RecipeChoice> choiceEntry : shaped.getChoiceMap().entrySet()) {
					final RecipeChoice choice = choiceEntry.getValue();
					if (choice == null) continue;
					if (choice instanceof RecipeChoice.MaterialChoice) {
						((RecipeChoice.MaterialChoice) choice).getChoices().forEach(material -> materials.add(material));
					} else if (choice instanceof RecipeChoice.ExactChoice) {
						((RecipeChoice.ExactChoice) choice).getChoices().forEach(stack -> materials.add(stack.getType()));
					} else
						materials.add(choice.getItemStack().getType());
					amount++;
				}
			}
			if (recipe instanceof ShapelessRecipe) {
				ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
				for (RecipeChoice choice : shapeless.getChoiceList()) {
					if (choice == null) continue;
					materials.add(choice.getItemStack().getType());
					amount++;
				}
			}

		} else {
			if (recipe instanceof ShapedRecipe) {
				ShapedRecipe shaped = (ShapedRecipe) recipe;
				for (ItemStack stack : shaped.getIngredientMap().values()) {
					if (stack == null) continue;
					materials.add(stack.getType());
					amount++;
				}
			}
			if (recipe instanceof ShapelessRecipe) {
				ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
				for (ItemStack stack : shapeless.getIngredientList()) {
					if (stack == null) continue;
					materials.add(stack.getType());
					amount++;
				}
			}
		}

		if (!materials.isEmpty()) {
			final int finalAmount = amount;
			materials.forEach(material -> map.put(material, finalAmount));
		}
		return map;
	}


	public static boolean isCookingRecipe(final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.newerThan(ServerVersion.v1_13) && recipe instanceof CookingRecipe<?>) {
			return true;
		}
		return recipe instanceof FurnaceRecipe;
	}

	public static boolean recipeContainsNamespace(final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.olderThan(ServerVersion.v1_14)) {
			return false;
		}
		if (recipe instanceof Keyed) {
			final NamespacedKey key = ((Keyed) recipe).getKey();
			return key.getNamespace().contains("craftenhance") || key.getKey().contains("cehrecipe");
		}
		return false;
	}

	public static boolean isRecipeCustom(final Recipe recipe) {
		final VersionChecker versionChecker = self().getVersionChecker();
		if (versionChecker.olderThan(ServerVersion.v1_14)) {
			return hasCustomMeta(recipe.getResult());
		}
		if (recipe instanceof Keyed) {
			final NamespacedKey key = ((Keyed) recipe).getKey();
			return key.getNamespace().contains("craftenhance") || key.getKey().contains("cehrecipe");
		}
		return false;
	}

	public static void addAttributeTooltip(ItemStack item) {
		final UUID uuid = UUID.randomUUID();

		NBT.modifyComponents(item, root -> {
			ReadWriteNBT attributeModifiers = root.getOrCreateCompound("minecraft:attribute_modifiers");
			ReadWriteNBTCompoundList list = attributeModifiers.getCompoundList("modifiers");
			ReadWriteNBT compound = list.addCompound();
			compound.setString("type", "minecraft:generic.movement_speed");
			compound.setDouble("amount", 5.0);
			compound.setString("operation", "add_value");
			compound.setString("id", uuid.toString());
		});
	}


	private static Object getNameSpacedKey(final JavaPlugin plugin, final String key) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		//return new NamespacedKey(plugin, key);
		return Class.forName("org.bukkit.NamespacedKey").getConstructor(org.bukkit.plugin.Plugin.class, String.class).newInstance(plugin, key);
	}


	private static ItemMeta getItemMeta(final ItemStack item, ItemMeta meta) {
		item.setItemMeta(meta);
		addAttributeTooltip(item);
		return item.getItemMeta();
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

	private static boolean hasCustomMeta(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;

		if (meta.hasDisplayName()) return true;
		if (meta.hasLore()) return true;
		if (meta.hasEnchants()) return true;

		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			if (meta.hasCustomModelData()) return true;
			return !meta.getPersistentDataContainer().isEmpty();
		}

		return false;
	}

	static class FurnaceWrapper {


		public static void setFurnaceIngredients(final org.bukkit.inventory.FurnaceRecipe recipe, final ItemStack ingredient) {
			if (ingredient.getType() == Material.AIR) return;

			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
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
					Debug.errorDisablable("Couldn't use full metadata for this furnace ingredient:' " + ingredient.getType() + "', will try to set type\n", e);
					recipe.setInput(ingredient.getType());
				}
			} else {
				final MaterialData md = ingredient.getData();
				if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
					recipe.setInput(ingredient.getType());
				} else {
					recipe.setInput(md.getItemType());
				}
			}
		}


		public static <T extends CookingRecipe<?>> void setFurnaceIngredients(final CookingRecipe<T> recipe, final ItemStack ingredient) {
			if (ingredient.getType() == Material.AIR) return;

			if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
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
					Debug.errorDisablable("Couldn't use full metadata for this furnace ingredient:' " + ingredient.getType() + "', will try to set type\n", e);
					recipe.setInput(ingredient.getType());
				}
			} else {
				final MaterialData md = ingredient.getData();
				if (md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)) {
					recipe.setInput(ingredient.getType());
				} else {
					recipe.setInput(md.getItemType());
				}
			}
		}


		@Nonnull
		private static ItemStack[] getFurnaceStack(@Nonnull final Recipe recipe) {
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
				return new ItemStack[]{((CookingRecipe<?>) recipe).getInput()};
			return new ItemStack[]{((org.bukkit.inventory.FurnaceRecipe) recipe).getInput()};
		}

		private static <T extends CookingRecipe<?>> void setGroup(@NonNull final CookingRecipe<T> furnaceRecipe, @NonNull final String groupName) {
			if (self().getVersionChecker().newerThan(ServerVersion.v1_12))
				furnaceRecipe.setGroup(groupName);
		}

	}

}
