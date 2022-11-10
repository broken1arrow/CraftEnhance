package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormatRecipeContents {

	public static <RecipeT extends EnhancedRecipe> List<?> formatRecipes(RecipeT recipe ){
		if (recipe == null) return new ArrayList<>();
		List<Object> list = new ArrayList<>(Arrays.asList(recipe.getContent()));
		list.add(6,recipe.getResult());
		return list;
	}
}
