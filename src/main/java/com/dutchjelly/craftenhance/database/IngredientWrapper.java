package com.dutchjelly.craftenhance.database;

public class IngredientWrapper {
		private byte[] itemData;
		private int slot;
		private String recipeName;
		private String itemName;
		private RecipeIngredientType recipeIngredientType = RecipeIngredientType.INGREDIENT;

		public IngredientWrapper() {
		}

		public byte[] getItemData() {
			return itemData;
		}

		public int getSlot() {
			return slot;
		}

		public void setItemData(final byte[] itemData) {
			this.itemData = itemData;
		}

		public void setSlot(final int slot) {
			this.slot = slot;
		}

		public String getRecipeName() {
			return recipeName;
		}

		public String getItemName() {
			return itemName;
		}

		public void setRecipeName(final String recipeName) {
			this.recipeName = recipeName;
		}

		public void setItemName(final String itemName) {
			this.itemName = itemName;
		}

		public RecipeIngredientType getRecipeType() {
			return recipeIngredientType;
		}

		public void setRecipeType(final RecipeIngredientType recipeIngredientType) {
			this.recipeIngredientType = recipeIngredientType;
		}
	}
