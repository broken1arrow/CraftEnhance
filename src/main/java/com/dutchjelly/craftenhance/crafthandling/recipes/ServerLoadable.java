package com.dutchjelly.craftenhance.crafthandling.recipes;

import org.bukkit.inventory.Recipe;

import javax.annotation.Nullable;


public interface ServerLoadable {

    String getKey();

    @Nullable
    String getGroup();
    Recipe getServerRecipe();
    Recipe getServerRecipe(final String groupName);

    boolean isSimilar(Recipe r);

    boolean isSimilar(EnhancedRecipe r);

    boolean isAlwaysSimilar(Recipe r);


}