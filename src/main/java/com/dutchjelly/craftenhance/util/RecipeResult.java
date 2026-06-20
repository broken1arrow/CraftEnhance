package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeResult {
	private final Type type;
	private final ItemStack item;
	private final RecipeType recipeType;

	private RecipeResult(@NonNull final Type type, @NonNull final RecipeType recipeType, @Nullable final FurnaceRecipe furnaceRecipe) {
		this.type = type;
		this.recipeType = furnaceRecipe != null ? furnaceRecipe.getType() : recipeType;
		this.item = furnaceRecipe == null ? new ItemStack(Material.AIR) : furnaceRecipe.getResult();
	}

	@NonNull
	public static RecipeResult setResult(@NonNull final FurnaceRecipe furnaceRecipe) {
		return new RecipeResult(Type.CUSTOM, furnaceRecipe.getType(), furnaceRecipe);
	}

	@NonNull
	public static RecipeResult setVanilla(final Furnace furnace) {
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			RecipeType recipeType = RecipeType.FURNACE;
			if (furnace instanceof BlastFurnace)
				recipeType = RecipeType.BLAST;
			if (furnace instanceof Smoker)
				recipeType = RecipeType.SMOKER;
			return new RecipeResult(Type.VANILLA, recipeType, null);
		}
		return new RecipeResult(Type.VANILLA, RecipeType.FURNACE, null);
	}

	@NonNull
	public static RecipeResult setNone() {
		return new RecipeResult(Type.NONE, RecipeType.NON, null);
	}

	@NonNull
	public Type getType() {
		return type;
	}

	public RecipeType getRecipeType() {
		return recipeType;
	}

	@NonNull
	public ItemStack getItem() {
		return item;
	}

	public boolean isEnhancedRecipe() {
		return type == Type.CUSTOM;
	}

	public boolean isVanilla() {
		return type == Type.VANILLA;
	}

	public boolean isNone() {
		return type == Type.NONE;
	}

	public enum Type {
		CUSTOM, VANILLA, NONE
	}

	@Override
	public String toString() {
		return "RecipeResult{" +
				"type=" + type +
				", item=" + item +
				'}';
	}
}