package com.dutchjelly.craftenhance.files;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryData implements ConfigurationSerializeUtility {

	private String recipeCategory;
	private ItemStack recipeCategoryItem;
	private final List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();

	public CategoryData() {

	}

	private CategoryData(ItemStack recipeCategoryItem, String recipeCategory) {
		this.recipeCategoryItem = recipeCategoryItem;
		this.recipeCategory = recipeCategory;
	}



	public String getRecipeCategory() {
		return recipeCategory;
	}

	public ItemStack getRecipeCategoryItem() {
		return recipeCategoryItem;
	}

	public List<EnhancedRecipe> getEnhancedRecipes() {
		return enhancedRecipes;
	}

	public void setEnhancedRecipes(EnhancedRecipe enhancedRecipes) {
		this.enhancedRecipes.add(enhancedRecipes);
	}


	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("category.name", recipeCategory);
			put("category.category_item", recipeCategoryItem);
		}};
	}
	public static CategoryData deserialize(Map< String, Object> map) {
		String recipeCategory = (String) map.getOrDefault("category.name", null);
		ItemStack itemStack = (ItemStack) map.getOrDefault("category.category_item", null);
		if (itemStack == null) {
			Material material = Adapter.getMaterial("CRAFTING_TABLE");
			if (material == null)
				material = Material.CRAFTING_TABLE;
			itemStack = new ItemStack(material);
		}
		return new CategoryData(itemStack,recipeCategory);
	}

}
