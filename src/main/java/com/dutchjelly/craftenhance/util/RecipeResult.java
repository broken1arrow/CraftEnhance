package com.dutchjelly.craftenhance.util;

import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class RecipeResult {
	private final Type type;
	private final ItemStack item;

	private RecipeResult(@NonNull final Type type, @Nullable final ItemStack item) {
		this.type = type;
		this.item = item;
	}

	@NonNull
	public static RecipeResult setCustomRecipe(@Nullable final ItemStack item) {
		return new RecipeResult(Type.CUSTOM,  item);
	}

	@NonNull
	public static RecipeResult setVanilla(@Nullable final ItemStack item) {
		return new RecipeResult(Type.VANILLA,  item);
	}

	@NonNull
	public static RecipeResult setNone() {
		return new RecipeResult(Type.NONE,  null);
	}

	@NonNull
	public Type getType() {
		return type;
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