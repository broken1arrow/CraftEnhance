package com.dutchjelly.craftenhance.crafthandling.recipes;

import org.bukkit.inventory.Recipe;


public interface ServerLoadable {

    String getKey();

    Recipe getServerRecipe();

    boolean isSimilar(Recipe r);

    boolean isSimilar(EnhancedRecipe r);

    boolean isAlwaysSimilar(Recipe r);


}