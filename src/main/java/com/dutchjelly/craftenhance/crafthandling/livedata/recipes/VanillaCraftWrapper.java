package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareItemCraftContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaCraftWrapper implements RecipeWrapper {
	private final Recipe recipe;
	private final String key;

	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
		StringBuilder builder = new StringBuilder(recipe.getResult().getType().name());
		builder.append("|");

		String joined = Adapter.getIngredientsList(recipe).stream()
				.filter(Objects::nonNull)
				.map(stack ->  stack.getType().name())
				.sorted()
				.collect(Collectors.joining(","));
		if (recipe instanceof ShapelessRecipe) {
			builder.append("S|").append(joined);
		}
		if (recipe instanceof ShapedRecipe) {
			builder.append("H|").append(joined);
		}
		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public String getRecipeKey() {
		return "vanilla_recipe:" + key;
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
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

		Debug.Send(Type.Crafting, () -> "Vanilla recipe check, the result to match: " + recipe.getResult());
		if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, recipe.getResult())) {
			Debug.Send(Type.Crafting, () -> "This recipe is disabled with result: " + recipe.getResult());
			return new ResultContext( null, ResultType.DISABLED);
		}

		if (recipe instanceof ShapedRecipe) {
			final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);

			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapedRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(content));
			if (WBRecipeComparer.shapeMatches(content, matrix, RecipeAdapter.getTypeMatcherNoMetadata())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapedRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		} else if (recipe instanceof ShapelessRecipe) {
			final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.Send(Type.Crafting, () -> "[ShapelessRecipe] ingredients" + RecipeDebug.convertItemStackArrayToString(ingredients));
			if (WBRecipeComparer.ingredientsMatch(ingredients, matrix, RecipeAdapter.getTypeMatcherNoMetadata())) {
				Debug.Send(Type.Crafting, () -> "Matched a ShapelessRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		}
		Debug.Send(Type.Crafting, () -> "Did not match this recipe.");
		return new ResultContext(null, ResultType.NO_MATCH);
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
		if (!(o instanceof VanillaCraftWrapper)) return false;
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
		builder.append("___________<  Vanilla Recipe >___________").append("\n");
		builder.append("Result: ").append(this.recipe.getResult()).append("\n");

		if (recipe instanceof ShapelessRecipe) {
			final ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
				builder.append("Key: ").append(shapelessRecipe.getKey()).append("\n");
			builder.append("Ingredients: ")
					.append(RecipeDebug.convertItemStackArrayToString(shapelessRecipe.getIngredientList()))
					.append("\n");
		}
		if (recipe instanceof ShapedRecipe) {
			final ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
			if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
				builder.append("Key: ").append(shapedRecipe.getKey()).append("\n");
			builder.append("Ingredients: ")
					.append(RecipeDebug.convertItemStackArrayToString(shapedRecipe.getIngredientMap().values()))
					.append("\n");
		}
		builder.append("___________<  Vanilla Recipe end >___________\n");
		return builder.toString();
	}

}
