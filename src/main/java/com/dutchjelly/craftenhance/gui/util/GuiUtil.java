package com.dutchjelly.craftenhance.gui.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static org.broken.arrow.color.library.TextTranslator.toSpigotFormat;


public class GuiUtil {

    public static Inventory CopyInventory(final ItemStack[] invContent, final String title, final InventoryHolder holder) {
        if (invContent == null) return null;
        final List<ItemStack> copiedItems = Arrays.stream(invContent).map(x -> x == null ? null : x.clone()).collect(Collectors.toList());
        if (copiedItems.size() != invContent.length)
            throw new IllegalStateException("Failed to copy inventory items.");
        final Inventory copy = Bukkit.createInventory(holder, invContent.length, title);
        for (int i = 0; i < copiedItems.size(); i++)
            copy.setItem(i, copiedItems.get(i));
        return copy;
    }

    public static Inventory FillInventory(final Inventory inv, final List<Integer> fillSpots, final List<ItemStack> items) {
        if (inv == null)
            throw new ConfigError("Cannot fill null inventory");

        if (items.size() > fillSpots.size())
            throw new ConfigError("Too few slots to fill.");

        for (int i = 0; i < items.size(); i++) {
            if (fillSpots.get(i) >= inv.getSize())
                throw new ConfigError("Fill spot is outside inventory.");
            inv.setItem(fillSpots.get(i), items.get(i));
        }
        return inv;
    }

    @Nonnull
    public static ItemStack setTextItem(@Nonnull final ItemStack itemStack, final String displayName, final List<String> lore) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(setcolorName(displayName));
            meta.setLore(setcolorLore(lore));
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static String setcolorName(final String name) {
        if (name == null) return null;

        return toSpigotFormat(name);
    }

    public static List<String> setcolorLore(final List<String> lore) {
        final List<String> lores = new ArrayList<>();
        for (final String text : lore) {
            if (text == null) {
                lores.add(null);
                continue;
            }
            lores.add(toSpigotFormat(text));
        }
        return lores;
    }

    public static ItemStack ReplaceAllPlaceHolders(final ItemStack item, final Map<String, String> placeholders) {
        if (item == null) return null;
        placeholders.forEach((key, value) -> ReplacePlaceHolder(item, key, value));
        return item;
    }

    public static ItemStack ReplacePlaceHolder(final ItemStack item, final String placeHolder, final String value) {
        if (item == null) return null;
        if (value == null) return null;

        final ItemMeta meta = item.getItemMeta();
        if (meta.getDisplayName().contains(placeHolder)) {
            meta.setDisplayName(meta.getDisplayName().replace(placeHolder, value));
            item.setItemMeta(meta);
        }


        List<String> lore = meta.getLore();
        if (lore == null)
            return item;

        lore = lore.stream().map(x -> (x == null ? null : x.replace(placeHolder, value))).collect(Collectors.toList());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    //Finds the destination for item in inv, in format <slot, amount>. Sets non-fitting amount in slot -1.
    public static Map<Integer, Integer> findDestination(final ItemStack item, final Inventory inv, int amount, final boolean preferEmpty, final List<Integer> whitelist) {
        if (item == null)
            return new HashMap<>();
        if (inv == null)
            throw new RuntimeException("cannot try to fit item into null inventory");

        if (amount == -1)
            amount = item.getAmount();

        final ItemStack[] storage = inv.getStorageContents();
        final Map<Integer, Integer> destination = new HashMap<>();
        int remainingItemQuantity = amount;

        //Fill empty slots first if @preferEmpty is true.
        if (preferEmpty) {
            for (int i = 0; i < storage.length; i++) {
                if (whitelist != null && !whitelist.contains(i)) continue;
                if (storage[i] == null) {
                    destination.put(i, Math.min(remainingItemQuantity, item.getMaxStackSize()));
                    remainingItemQuantity -= destination.get(i);
                }
                if (remainingItemQuantity == 0)
                    return destination;
            }
        }

        //Fill slots from left to right if there's any room for @item.
        for (int i = 0; i < storage.length; i++) {
            if (whitelist != null && !whitelist.contains(i)) continue;
            if (storage[i] == null && !destination.containsKey(i)) {
                destination.put(i, Math.min(remainingItemQuantity, item.getMaxStackSize()));
                remainingItemQuantity -= destination.get(i);
            } else if (storage[i].getAmount() < storage[i].getMaxStackSize() && storage[i].isSimilar(item)) {
                final int room = Math.min(remainingItemQuantity, storage[i].getMaxStackSize() - storage[i].getAmount());
                destination.put(i, room);
                remainingItemQuantity -= room;
            }

            if (remainingItemQuantity == 0)
                return destination;
        }

        //Look if anything couldn't be filled. Give this slot index -1.
        if (remainingItemQuantity > 0)
            destination.put(-1, remainingItemQuantity);

        return destination;
    }

    public static <T> void swap(final T[] list, final int a, final int b) {
        final T t = list[a];
        list[a] = list[b];
        list[b] = t;
    }

    @Deprecated
    public static boolean isNull(final ItemStack item) {
        return item == null || item.getType().equals(Material.AIR);
    }

    /**
     * This method check if value can match inventory size like 9 * x and not go over inventory max size of 6 rows.
     *
     * @param size the size you want the inventory to be.
     * @return inventory size.
     */
    public static int invSize(final String menu, final int size) {
        if (size < 9) return 9;
        if (size % 9 == 0) return size;
        if (size <= 18) return 18;
        if (size <= 27) return 27;
        if (size <= 36) return 36;
        if (size <= 45) return 45;
        if (size > 54)
            Messenger.Error("This menu "+ menu + " has set bigger inventory size an it can handle, your set size " + size + ". will defult to 54.");
        return 54;
    }

    public static boolean changeCategoryName(final String currentCatogory, final String msg, final Player player) {
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            final CategoryData categoryData = self().getCategoryDataCache().get(currentCatogory);

            if (categoryData == null) {
                Messenger.Message("Your category name not exist", player);
                return true;
            }
            final CategoryData newCategoryData = self().getCategoryDataCache().of(currentCatogory, categoryData.getRecipeCategoryItem(), msg);
            newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
            self().getCategoryDataCache().put(currentCatogory, newCategoryData);
            Bukkit.getScheduler().runTaskLaterAsynchronously( self(),()-> self().getCategoryDataCache().save(),1L);
            return false;
        }
        return true;
    }

    public static boolean changeCategoryItem(final String currentCatogory, final String msg, final Player player) {
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit")|| msg.equals("q"))
            return false;
        if (!msg.isEmpty()) {
            final CategoryData categoryData = self().getCategoryDataCache().get(currentCatogory);
            if (categoryData == null) {
                Messenger.Message("Your category name not exist", player);
                return true;
            }
            final Material material = Material.getMaterial(msg.toUpperCase());
            if (material == null) {
                Messenger.Message("Your material name not exist " + msg, player);
                return true;
            }
            final CategoryData newCategoryData = self().getCategoryDataCache().of(currentCatogory, new ItemStack(material), categoryData.getDisplayName());
            newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
            self().getCategoryDataCache().put(currentCatogory, newCategoryData);
            Bukkit.getScheduler().runTaskLaterAsynchronously( self(),()-> self().getCategoryDataCache().save(),1L);
            return false;
        }
        return true;
    }

    public static boolean changeCategory(final String currentCatogory, final String msg, final Player player) {
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            final CategoryData categoryData = self().getCategoryDataCache().get(currentCatogory);
            if (categoryData == null) {
                Messenger.Message("Your category name not exist", player);
                return true;
            }
            final CategoryData movedcategoryData = self().getCategoryDataCache().move(currentCatogory, msg);
            Bukkit.getScheduler().runTaskLaterAsynchronously( self(),()-> {
                self().getCategoryDataCache().save();
                if (movedcategoryData != null){
                    movedcategoryData.getEnhancedRecipes().forEach(EnhancedRecipe::save);
                }

            },1L);
            return false;

        }
        return true;
    }

    public static boolean newCategory(final String msg, final Player player) {
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            final String[] split = msg.split(" ");
            if (split.length > 1) {
                final Material material = Material.getMaterial(split[1].toUpperCase());
                if (material == null) {
                    Messenger.Message("Please input valid item name. Your input " + split[1], player);
                    return true;
                }
                if (self().getCategoryDataCache().addCategory(split[0], new ItemStack(material), null)) {
                    Messenger.Message("Your category name alredy exist", player);
                    return true;
                } else {
                    Bukkit.getScheduler().runTaskLaterAsynchronously( self(),()-> self().getCategoryDataCache().save(),1L);
                    return false;
                }
            }else {
                Messenger.Message("Please input valid item name and category. Your input " + msg, player);
                Messenger.Message("Type it like this 'category' 'itemname' ", player);
            }
        }
        return true;

    }

    public static boolean changeOrCreateCategory(final String msg, final Player player, final EnhancedRecipe recipe) {
        if (msg.equals("") || msg.equalsIgnoreCase("q") || msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("quit") || msg.equalsIgnoreCase("exit"))
            return false;
        if (!msg.isEmpty()) {
            final String[] split = msg.split(" ");
            if (split.length > 1) {
                Material material = null;
                if (split.length >= 3)
                    material = Material.getMaterial(split[2].toUpperCase());
                final CategoryData categoryData = self().getCategoryDataCache().get(split[0]);
                if (categoryData == null) {
                    if (material == null)
                        material = Material.getMaterial(split[1].toUpperCase());
                    if (material == null) {
                        Messenger.Message("Please input valid item name. Your input " + msg, player);
                        return true;
                    }
                    self().getCategoryDataCache().addCategory(split[0], new ItemStack(material), null);
                    recipe.setRecipeCategory(split[0]);
                } else {
                    final CategoryData newCategoryData = self().getCategoryDataCache().of(split[1], material != null ? new ItemStack(material) : categoryData.getRecipeCategoryItem(), null);
                    self().getCategoryDataCache().remove(split[0]);
                    newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
                    self().getCategoryDataCache().put(split[1], newCategoryData);
                    recipe.setRecipeCategory(split[0]);
                }
                return false;
            } else {
                Messenger.Message("Please input valid item name and category. Your input " + msg, player);
                Messenger.Message("Type it like this 'category' 'itemname' ", player);
            }
        }
        return true;
    }


    public static boolean seachCategory(final String msg) {
        if (msg.equals(""))
            return false;
        return !msg.equals("cancel") && !msg.equals("quit") && !msg.equals("exit");
    }
}
