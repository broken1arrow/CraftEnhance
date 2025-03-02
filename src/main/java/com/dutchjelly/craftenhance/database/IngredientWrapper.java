package com.dutchjelly.craftenhance.database;

public class IngredientWrapper {
		private byte[] itemData;
		private int slot;
		private String recipeName;
		private String itemName;
		private RecipeType recipeType = RecipeType.INGREDIENT;

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

		public RecipeType getRecipeType() {
			return recipeType;
		}

		public void setRecipeType(final RecipeType recipeType) {
			this.recipeType = recipeType;
		}
	}
