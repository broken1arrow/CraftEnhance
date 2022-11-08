package com.dutchjelly.bukkitadapter;


<<<<<<< Updated upstream
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.messaging.Debug;
=======
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.gui.util.SkullCreator;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Keyed;
>>>>>>> Stashed changes
import org.bukkit.Material;

import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.DyeColor;

import java.lang.reflect.InvocationTargetException;
<<<<<<< Updated upstream
import java.util.Arrays;
import java.util.List;

public class Adapter {


    public static List<String> CompatibleVersions(){
        return Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16");
    }


    public static Material getMaterial(String name){
        try{
            return Material.valueOf(name);
        }catch(Exception e){
            if(name.equals("WORKBENCH"))
                return Material.valueOf("CRAFTING_TABLE");
            try{
                return Material.matchMaterial("LEGACY_" + name);
            }catch(Exception e2) {}
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getColoredItem(String name, DyeColor color){
        try{
            return new ItemStack(Material.valueOf(color.name() + "_" + name));
        }catch(Exception e){
            try{
                return new ItemStack(Material.valueOf(name), 1, (short)color.getWoolData());
            }catch(Exception e2){ }
        }
        return null;
    }

    private static Object getNameSpacedKey(JavaPlugin plugin, String key) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
=======
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
	public static List<String> CompatibleVersions() {
		return Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16", "1.17", "1.18","1.19");
	}

	@Nullable
	public static ItemStack getItemStack(String material, String displayName, List<String> lore, String color, boolean glow) {
		ItemStack item;

		if (color == null) {
			Material mat = Adapter.getMaterial(material);
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
				DyeColor dColor = dyeColor(color);
				if (dColor == null) throw new ConfigError("color " + color + " not found");
				item = Adapter.getColoredItem(material, dColor);
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
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			if (glow)
				meta.addEnchant(Enchantment.DURABILITY, 10, true);
			item.setItemMeta(meta);
		}
		return item;
	}
	@Nullable
	public static DyeColor dyeColor(String dyeColor){
		DyeColor[] dyeColors = DyeColor.values();

		for (DyeColor color :dyeColors) {
			if ( color .name().equalsIgnoreCase(dyeColor)){
				return color;
			}
		}
		return null;
	}
	@Nullable
	public static Material getMaterial(String name) {
		if (name == null) return null;
		name = name.toUpperCase();
		Material material = Material.getMaterial(name);
		if (material != null)
			return material;
		if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_12)) {
			switch (name) {
				case "WORKBENCH":
					return Material.valueOf("CRAFTING_TABLE");
				case "WEB":
					return Material.valueOf("COBWEB");
				case "EXP_BOTTLE":
					return Material.valueOf("EXPERIENCE_BOTTLE");
				default:
					Messenger.Error("Could not find " + name + " try load legacy suport");
					return Material.matchMaterial("LEGACY_" + name);
			}
		} else {
			if (name.equals("CLOCK"))
				return Material.valueOf("WATCH");
		}
		return null;
	}

	public static ItemStack getColoredItem(String name, DyeColor color) {
		try {
			return new ItemStack(Material.valueOf(color.name() + "_" + name));
		} catch (Exception e) {
			try {
				return new ItemStack(Material.valueOf(name), 1, (short) color.getWoolData());
			} catch (Exception e2) {
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
		} catch (NoSuchMethodException e) {
			canUseModeldata = Optional.of(false);
			return false;
		}
	}

	private static Object getNameSpacedKey(JavaPlugin plugin, String key) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
>>>>>>> Stashed changes
//        return new NamespacedKey(plugin, key);
        return Class.forName("org.bukkit.NamespacedKey").getConstructor(org.bukkit.plugin.Plugin.class, String.class).newInstance(plugin, key);
    }

    public static ShapedRecipe GetShapedRecipe(JavaPlugin plugin, String key, ItemStack result){
        try{
            return ShapedRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
        }
        return new ShapedRecipe(result);
    }

    public static ShapelessRecipe GetShapelessRecipe(JavaPlugin plugin, String key, ItemStack result){
        try {
            return ShapelessRecipe.class.getConstructor(Class.forName("org.bukkit.NamespacedKey"), ItemStack.class).newInstance(getNameSpacedKey(plugin, key), result);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            Debug.Send("Couldn't use namespaced key: " + e.getMessage() + "\n" + e.getStackTrace());
        }
        return new ShapelessRecipe(result);
    }

    public static ItemStack SetDurability(ItemStack item, int damage){
        item.setDurability((short)damage);
        return item;
    }

    public static void SetIngredient(ShapedRecipe recipe, char key, ItemStack ingredient){
        if(!CraftEnhance.self().getConfig().getBoolean("learn-recipes")){
            MaterialData md = ingredient.getData();
            if(md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)){
                recipe.setIngredient(key, ingredient.getType());
            }else{
                recipe.setIngredient(key, md);
            }
            return;
        }
        try{
            recipe.getClass().getMethod("setIngredient", char.class, Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice")).invoke(recipe,
                    key, Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice").getConstructor(ItemStack.class).newInstance(ingredient)
            );
        }catch(Exception e){
            recipe.setIngredient(key, ingredient.getType());
        }
    }

    public static void AddIngredient(ShapelessRecipe recipe, ItemStack ingredient){
        if(!CraftEnhance.self().getConfig().getBoolean("learn-recipes")){
            MaterialData md = ingredient.getData();
            if(md == null || !md.getItemType().equals(ingredient.getType()) || md.getItemType().equals(Material.AIR)){
                recipe.addIngredient(ingredient.getType());
            }else{
                recipe.addIngredient(md);
            }
            return;
        }
        try{
            recipe.getClass().getMethod("addIngredient", Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice")).invoke(recipe,
                    Class.forName("org.bukkit.inventory.RecipeChoice.ExactChoice").getConstructor(ItemStack.class).newInstance(ingredient)
            );
        }catch(Exception e){
            recipe.addIngredient(ingredient.getType());
        }
    }

    public static void DiscoverRecipes(Player player, List<Recipe> recipes){
        try{
            for (Recipe recipe : recipes) {
                if(recipe instanceof ShapedRecipe){
                    ShapedRecipe shaped = (ShapedRecipe) recipe;
                    player.discoverRecipe(shaped.getKey());
                }else if(recipe instanceof ShapelessRecipe){
                    ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
                    player.discoverRecipe(shapeless.getKey());
                }
            }
        }catch(Exception e){ }
    }

    public static void SetOwningPlayer(SkullMeta meta, OfflinePlayer player){
        try{
            meta.setOwningPlayer(player);
        }catch(Exception e){
            meta.setOwner(player.getName());
        }
    }

    public static Recipe FilterRecipes(List<Recipe> recipes, String name){
        for(Recipe r : recipes){
            String id = GetRecipeIdentifier(r);
            if(id == null) continue;
            if(id.equalsIgnoreCase(name))
                return r;
        }

        return recipes.stream().filter(x -> x != null).filter(x -> x.getResult().getType().name().equalsIgnoreCase(name)).findFirst().orElse(null);

    }


    public static String GetRecipeIdentifier(Recipe r){
        try{
            //reflection is so damn powerful!! You can even invoke methods from derived classes.
            Object obj = r.getClass().getMethod("getKey").invoke(r);
            if(obj != null) return obj.toString();
        }catch(Exception e){
        }

        return r.getResult().getType().name();
    }

}
