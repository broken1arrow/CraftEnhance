package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FormatRecipeContents {

	public static <RecipeT extends EnhancedRecipe> List<?> formatRecipes(RecipeT recipe ){
		if (recipe == null) return new ArrayList<>();
		List<Object> list = new ArrayList<>(Arrays.asList(recipe.getContent()));
		//todo fix so it auto set right craftingslot System.out.println("recipe.getResultSlot() " + recipe.getResultSlot());
		int index;
		if (list.size() < 6)
			index = 1;
		else
			index = 6;
		list.add(index,recipe.getResult());
		return list;
	}

	public static List<EnhancedRecipe> canSeeRecipes(List<EnhancedRecipe> enhancedRecipes, Player p) {
		return enhancedRecipes.stream().filter(x -> (
				!self().getConfig().getBoolean("only-show-available") ||
						x.getPermissions() == null ||
						Objects.equals(x.getPermissions(), "") ||
						p.hasPermission(x.getPermissions()))
				&& (!x.isHidden() ||
				p.hasPermission(PermissionTypes.Edit.getPerm()) ||
				p.hasPermission(x.getPermissions() + ".hidden"))).collect(Collectors.toList());
	}

}
