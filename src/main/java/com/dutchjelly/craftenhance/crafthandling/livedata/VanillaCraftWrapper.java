package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VanillaCraftWrapper implements RecipeWrapper {
	private final Recipe recipe;
	private String key;

	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
		StringBuilder builder = new StringBuilder(recipe.getResult().getType().name());
		builder.append("|");

		if (recipe instanceof ShapelessRecipe) {
			ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
			String joined = shapeless.getChoiceList().stream()
					.map(c -> c.getItemStack().getType().name())
					.sorted()
					.collect(Collectors.joining(","));

			builder.append("S|").append(joined);
		}

		if (recipe instanceof ShapedRecipe) {
			ShapedRecipe shaped = (ShapedRecipe) recipe;
			String joined = shaped.getChoiceMap().values().stream()
					.filter(Objects::nonNull)
					.map(c -> c.getItemStack().getType().name())
					.sorted()
					.collect(Collectors.joining(","));

			builder.append("H|").append(joined);
		}

		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return RecipeType.WORKBENCH;
	}

	@Override
	public int priority() {
		return 100;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareItemCraftContext prepareItemCraftContext = new PrepareItemCraftContext();
		contextConsumer.accept(prepareItemCraftContext);
		final ItemStack[] matrix = prepareItemCraftContext.getRecipeMatrix();

		if (recipe instanceof ShapedRecipe) {
			final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);

			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(content));
			if (WBRecipeComparer.shapeMatches(content, matrix, RecipeAdapter.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapedRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		} else if (recipe instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(ingredients));
			if (WBRecipeComparer.ingredientsMatch(ingredients, matrix, RecipeAdapter.getTypeMatcher())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapelessRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		}
		return null;
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.recipe))
			return Optional.of(type.cast(this.recipe));
		return Optional.empty();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof RecipeWrapper)) return false;
		final VanillaCraftWrapper that = (VanillaCraftWrapper) o;
		return that.key.equals(key);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 41 * hash + key.hashCode();
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("########## Recipe ################").append("\n");
		builder.append("Result: ").append(this.recipe.getResult()).append("\n");
		if (recipe instanceof ShapelessRecipe)
			builder.append("Ingredients: ").append(((ShapelessRecipe) recipe).getChoiceList()).append("\n");
		if (recipe instanceof ShapedRecipe)
			builder.append("Ingredients: ").append(((ShapedRecipe) recipe).getChoiceMap()).append("\n");
		builder.append("########## Recipe ################\n");

		return builder.toString();
	}

}
