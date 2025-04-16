package com.dutchjelly.craftenhance.files;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.util.ConfigurationSerializeUtility;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoryData implements ConfigurationSerializeUtility {

	private final ItemStack recipeCategoryItem;
	private String recipeCategory;
	private String displayName;
	private List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();

	private CategoryData(final ItemStack recipeCategoryItem, final String recipeCategory, final String displayName) {
		this.recipeCategoryItem = recipeCategoryItem;
		this.recipeCategory = recipeCategory;
		this.displayName = displayName;
	}

	public static CategoryData of(final ItemStack recipeCategoryItem, final String recipeCategory, final String displayName) {
		return new CategoryData(recipeCategoryItem, recipeCategory, displayName);
	}

	public static CategoryData deserialize(final Map<String, Object> map) {
		final String recipeCategory = (String) map.getOrDefault("category.name", null);
		ItemStack itemStack = (ItemStack) map.getOrDefault("category.category_item", null);
		final String displayName = (String) map.getOrDefault("category.display_name", null);
		if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getType() == Material.getMaterial("END_PORTAL")) {
			Material material = Adapter.getMaterial("CRAFTING_TABLE");
			if (material == null)
				material = Material.CRAFTING_TABLE;
			itemStack = new ItemStack(material);
		}
		final CategoryData categoryData = new CategoryData(itemStack, recipeCategory, displayName);
		categoryData.setDisplayName(displayName);
		return categoryData;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public String getRecipeCategory() {
		return recipeCategory;
	}

	public void setRecipeCategory(final String recipeCategory) {
		this.recipeCategory = recipeCategory;
	}

	public ItemStack getRecipeCategoryItem() {
		return recipeCategoryItem;
	}

	public List<EnhancedRecipe> getEnhancedRecipes() {
		return enhancedRecipes;
	}

	public void setEnhancedRecipes(final List<EnhancedRecipe> enhancedRecipes) {
		this.enhancedRecipes = enhancedRecipes;
	}

	public List<EnhancedRecipe> getEnhancedRecipes(final String recipeSearchFor) {
		if (recipeSearchFor == null || recipeSearchFor.equals(""))
			return enhancedRecipes;
		return enhancedRecipes.stream().filter(x -> x.getKey().contains(recipeSearchFor)).collect(Collectors.toList());
	}

	public void addEnhancedRecipes(final EnhancedRecipe enhancedRecipes) {
		this.enhancedRecipes.add(enhancedRecipes);
	}

	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("category.name", recipeCategory);
			put("category.category_item", recipeCategoryItem);
			put("category.display_name", displayName);
		}};
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
