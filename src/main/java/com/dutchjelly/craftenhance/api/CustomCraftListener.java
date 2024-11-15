package com.dutchjelly.craftenhance.api;

import com.dutchjelly.craftenhance.crafthandling.RecipeGroup;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface CustomCraftListener {
    boolean listener(EnhancedRecipe recipe, Player p, Inventory craftingInventory, RecipeGroup alternatives);
}
