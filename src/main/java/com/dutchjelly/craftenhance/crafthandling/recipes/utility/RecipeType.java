package com.dutchjelly.craftenhance.crafthandling.recipes.utility;

import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public enum RecipeType {
    WORKBENCH, FURNACE,BLAST,SMOKER;

    public static RecipeType getType(Recipe r) {
        if(r instanceof ShapedRecipe) return WORKBENCH;
        if(r instanceof ShapelessRecipe) return WORKBENCH;
        if(r instanceof FurnaceRecipe) return FURNACE;
        if(r instanceof BlastRecipe) return BLAST;
        if(r instanceof SmokerRecipe) return SMOKER;
        return null;
    }
}