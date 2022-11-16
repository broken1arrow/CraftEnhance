package com.dutchjelly.craftenhance.api;



import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.RecipeGroup;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class CraftEnhanceAPI {

    private static final List<CustomCraftListener> customCraftListeners = new ArrayList<>();

    public static void registerListener(final CustomCraftListener listener){
        if(!customCraftListeners.contains(listener))
            customCraftListeners.add(listener);
    }

    public static boolean fireEvent(final EnhancedRecipe recipe, final Player p, final Inventory craftingInventory, final RecipeGroup alternatives){
        try{
            System.out.println("customCraftListeners " + customCraftListeners);
            return customCraftListeners.stream().map(x -> x.listener(recipe, p, craftingInventory, alternatives)).anyMatch(x -> x);
        }catch(final Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
