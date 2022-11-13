package com.dutchjelly.craftenhance.files;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.files.util.SimpleYamlHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CategoryDataCache extends SimpleYamlHelper {


	private final Map<String, CategoryData> recipeCategorys = new HashMap<>();

	public CategoryDataCache() {
		super("categorys.yml", true, true);
	}

	public CategoryData of(final String category, final ItemStack itemStack, final String displayName) {
		return CategoryData.of(itemStack, category, displayName);
	}

	public Collection<CategoryData> values(){
		return  recipeCategorys.values();
	}

	@Nullable
	public CategoryData get(final String category){
		return recipeCategorys.get(category);
	}
	public void put(final String category, final ItemStack itemStack, final String displayName){
		recipeCategorys.put(category,this.of(category,itemStack,displayName));
	}
	public void put(final String category, final CategoryData categoryData){
		recipeCategorys.put(category,categoryData);
	}

	public void remove(final String category){
		recipeCategorys.remove(category);
	}

	public CategoryData move(final String oldCategory, final String category, final EnhancedRecipe... recipes) {
		final CategoryData categoryDataOld = this.getRecipeCategorys().get(oldCategory);
		final CategoryData existingCategory = this.getRecipeCategorys().get(category);
		final CategoryData categoryData = this.createCategoryData( categoryDataOld,category,existingCategory != null);

		if (categoryDataOld != null && recipes != null && recipes.length > 0) {
			for (final EnhancedRecipe recipe : recipes) {
				categoryDataOld.getEnhancedRecipes().remove(recipe);
				if (existingCategory != null) {
					recipe.setRecipeCategory(category);
					if (!containsRecipe(existingCategory.getEnhancedRecipes(), recipe.getKey()))
						existingCategory.getEnhancedRecipes().add(recipe);
				} else if (categoryData != null) {
					recipe.setRecipeCategory(category);
					if (!containsRecipe(categoryData.getEnhancedRecipes(), recipe.getKey()))
						categoryData.getEnhancedRecipes().add(recipe);
				}
			}
		} else {
			if (categoryDataOld != null) {
				collectToNewList(category, categoryDataOld.getEnhancedRecipes(), existingCategory != null ? existingCategory.getEnhancedRecipes() : categoryData != null ? categoryData.getEnhancedRecipes() : new ArrayList<>());
				this.remove(oldCategory);
			}
		}
		if (existingCategory != null) {
			this.getRecipeCategorys().put(category, existingCategory);
			return existingCategory;
		} else if (categoryData != null) {
			this.getRecipeCategorys().put(category, categoryData);
			return categoryData;
		}
		return null;
	}
	public boolean addCategory(final String category, final ItemStack itemStack, final String displayname) {
		final CategoryData categoryData = this.getRecipeCategorys().get(category);
		if (categoryData != null) return true;
		 recipeCategorys.put(category,of(category,itemStack,displayname));
		return false;
	}

	private Map<String, CategoryData> getRecipeCategorys() {
		return recipeCategorys;
	}

	@Override
	protected void saveDataToFile(final File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		this.getCustomConfig().set("Categorys",null);
		for (final Entry<String, CategoryData> entry : recipeCategorys.entrySet())
			this.setData(file, "Categorys." + entry.getKey(), entry.getValue());
	}

	@Override
	protected void loadSettingsFromYaml(final File file) {
		final ConfigurationSection templateConfig = this.getCustomConfig().getConfigurationSection("Categorys");
		if (templateConfig == null) return;
		for (final String category : templateConfig.getKeys(false)) {
			final CategoryData categoryData = this.getData( "Categorys." + category, CategoryData.class);
			if (categoryData == null) continue;
			recipeCategorys.put(category, categoryData);
		}
	}
	public void collectToNewList(final String category,final List<EnhancedRecipe> fromList, final List<EnhancedRecipe> toList) {
		moveRecipesCategory(fromList,category).forEach(recipe -> {
			if (!containsRecipe(toList, recipe.getKey())){
				toList.add(recipe);
			}
		});
	}
	public boolean containsRecipe(final List<EnhancedRecipe> enhancedRecipes, final String recipeKey) {
		for (final EnhancedRecipe recipe : enhancedRecipes){
			if (recipe.getKey().equals(recipeKey))
				return  true;
		}
		return false;
	}
	public List<EnhancedRecipe> moveRecipesCategory(final List<EnhancedRecipe> enhancedRecipes, final String newCategory) {
		if (enhancedRecipes != null) {
			enhancedRecipes.forEach(recipe -> recipe.setRecipeCategory(newCategory));
		}
		return enhancedRecipes;
	}
	public CategoryData createCategoryData(final CategoryData categoryDataOld, final String category, final boolean doCategoryAllredyExist) {
		CategoryData categoryData = null;
		if (!doCategoryAllredyExist) {
			if (categoryDataOld != null)
				categoryData = this.of(category, categoryDataOld.getRecipeCategoryItem(), categoryDataOld.getDisplayName());
			else
				categoryData = this.of(category, new ItemStack(Adapter.getMaterial("CRAFTING_TABLE")), null);
		}
		return categoryData;
	}
}
