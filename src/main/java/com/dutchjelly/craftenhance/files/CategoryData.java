package com.dutchjelly.craftenhance.files;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoryData implements ConfigurationSerializeUtility {

	private final String recipeCategory;
	private String displayName;
	private final ItemStack recipeCategoryItem;
	private List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();

	private CategoryData(ItemStack recipeCategoryItem, String recipeCategory,String displayName) {
		this.recipeCategoryItem = recipeCategoryItem;
		this.recipeCategory = recipeCategory;
		this.displayName = displayName;
	}
	public static CategoryData of(ItemStack recipeCategoryItem, String recipeCategory, String displayName) {
		return new CategoryData(recipeCategoryItem,recipeCategory,displayName);
	}
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
	public List<EnhancedRecipe> getEnhancedRecipes(String recipeSeachFor) {
		if (recipeSeachFor == null || recipeSeachFor.equals(""))
		return enhancedRecipes;
		return enhancedRecipes.stream().filter(x -> x.getKey().contains(recipeSeachFor)).collect(Collectors.toList());
	}
	public void addEnhancedRecipes(EnhancedRecipe enhancedRecipes) {
		this.enhancedRecipes.add(enhancedRecipes);
	}

	public void setEnhancedRecipes(List<EnhancedRecipe> enhancedRecipes) {
		this.enhancedRecipes = enhancedRecipes;
	}

	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("category.name", recipeCategory);
			put("category.category_item", recipeCategoryItem);
			put("category.display_name", displayName);
		}};
	}
	public static CategoryData deserialize(Map< String, Object> map) {
		String recipeCategory = (String) map.getOrDefault("category.name", null);
		ItemStack itemStack = (ItemStack) map.getOrDefault("category.category_item", null);
		String displayName = (String) map.getOrDefault("category.display_name", null);
		if (itemStack == null) {
			Material material = Adapter.getMaterial("CRAFTING_TABLE");
			if (material == null)
				material = Material.CRAFTING_TABLE;
			itemStack = new ItemStack(material);
		}
		CategoryData categoryData =  new CategoryData(itemStack,recipeCategory,displayName);
		categoryData.setDisplayName(displayName);
		return categoryData;
	}

	@Override
	public String toString() {
		return "CategoryData{" +
				"recipeCategory='" + recipeCategory + '\'' +
				", displayName='" + displayName + '\'' +
				", recipeCategoryItem=" + recipeCategoryItem +
				", enhancedRecipes=" + enhancedRecipes +
				'}';
	}
}
