package com.dutchjelly.craftenhance.database;

public enum RecipeIngredientType {

	RESULT,
	INGREDIENT;


	public static RecipeIngredientType getType(String type) {
		if (type == null) return INGREDIENT;

		RecipeIngredientType[] recipeIngredientTypes = values();
		String typeUp = type.toUpperCase();

		for (RecipeIngredientType recipeIngredientType : recipeIngredientTypes) {
			if (recipeIngredientType.name().equals(typeUp)) {
				return recipeIngredientType;
			}
		}

		return INGREDIENT;
	}
}
