package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeGroup {

    public RecipeGroup(){}

    public RecipeGroup(List<EnhancedRecipe> enhanced, List<Recipe> server){
        this.enhancedRecipes.addAll(enhanced);
        this.serverRecipes.addAll(server);
    }

    @Getter @Setter
    private List<Recipe> serverRecipes = new ArrayList<>();

    @Getter @Setter
    private List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();


    public RecipeGroup addIfNotExist(EnhancedRecipe enhancedRecipe){
        if (!enhancedRecipes.contains(enhancedRecipe))
            enhancedRecipes.add(enhancedRecipe);
        return this;
    }

    public RecipeGroup addIfNotExist(Recipe recipe){
        if (!serverRecipes.contains(recipe))
            serverRecipes.add(recipe);
        return this;
    }
    public RecipeGroup addAllNotExist(List<Recipe> recipes){
        for (Recipe recipe :recipes)
            if (!serverRecipes.contains(recipe))
                serverRecipes.add(recipe);
        return this;
    }
    //Returns this for chaining purposes.
    public RecipeGroup mergeWith(@NonNull RecipeGroup othergroup){
        List<Recipe> mergedServerRecipes = new ArrayList<>();
        mergedServerRecipes.addAll(serverRecipes);
        mergedServerRecipes.addAll(othergroup.serverRecipes);
        serverRecipes =  mergedServerRecipes.stream().distinct().collect(Collectors.toList());
        List<EnhancedRecipe> mergedEnhancedRecipes = new ArrayList<>();
        mergedEnhancedRecipes.addAll(enhancedRecipes);
        mergedEnhancedRecipes.addAll(othergroup.enhancedRecipes);
        enhancedRecipes = mergedEnhancedRecipes.stream().distinct().collect(Collectors.toList());
        return this;
    }

    @Override
    public String toString() {
        return "RecipeGroup{" +
                "serverRecipes=" + serverRecipes +
                ", enhancedRecipes=" + enhancedRecipes +
                '}';
    }
}
