package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.bukkitadapter.Adapter.RecipeContext;
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
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaCraftWrapper implements RecipeWrapper {
	private final Recipe recipe;
	private final EnumMap<Material, Integer> ingredients;
	private final int totalSlotCount;
	private final String key;


	public VanillaCraftWrapper(@NonNull final Recipe recipe) {
		this.recipe = recipe;
		RecipeContext recipeContext = Adapter.getFullIngredientsList(recipe);
		this.ingredients = recipeContext.getMap();
		this.totalSlotCount = recipeContext.getTotalSlotCount();

		StringBuilder builder = new StringBuilder(recipe.getResult().getType().name());
		builder.append("|");
		String joined = Adapter.getIngredientsList(recipe).stream()
				.filter(Objects::nonNull)
				.map(stack -> stack.getType().name())
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
		return false;
	}

	@Override
	public EnumMap<Material, Integer> getIngredients() {
		return ingredients;
	}

	@Override
	public int getTotalSlotCount() {
		return totalSlotCount;
	}

	@Override
	public int getAmountOfIngredient(final Material material) {
		Integer amount = this.ingredients.get(material);
		if (amount != null)
			return amount;
		return 0;
	}


	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareItemCraftContext prepareItemCraftContext = new PrepareItemCraftContext();
		contextConsumer.accept(prepareItemCraftContext);
		final ItemStack[] matrix = prepareItemCraftContext.getRecipeMatrix();
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

		Debug.send(Type.Crafting, "vanilla recipe", () -> "It will check if recipe allowed for this world, not disabled and this is a valid vanilla recipe:\n" + RecipeDebug.formatOneStack(recipe.getResult()));
		if (RecipeAdapter.checkForDisabledRecipe(disabledServerRecipes, recipe.getResult())) {
			Debug.send(Type.Crafting, "Vanilla recipe", () -> "This recipe is disabled and will not be crafted");
			return new ResultContext(null, ResultType.DISABLED);
		}

		if (recipe instanceof ShapedRecipe) {
			Debug.send(Type.Crafting, "injecting | crafting_vanilla", () -> "[ShapedRecipe] crafting table matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.send(Type.Crafting, "injecting | crafting_vanilla", () -> {
				final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) recipe);
				return "[ShapedRecipe] ingredients for this recipe: " + RecipeDebug.convertItemStackArrayToString(content);
			});
			if (WBRecipeComparer.shapedMatch((ShapedRecipe) recipe, matrix)) {
				Debug.send(Type.Crafting, "result | crafting_vanilla", () -> "Matched a ShapedRecipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		} else if (recipe instanceof ShapelessRecipe) {
			Debug.send(Type.Crafting, "injecting | crafting_vanilla", () -> "[ShapelessRecipe] crafting table matrix to match: " + RecipeDebug.convertItemStackArrayToString(matrix));
			Debug.send(Type.Crafting, "injecting | crafting_vanilla", () -> {
				final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) recipe);
				return "[ShapelessRecipe] ingredients for this recipe: " + RecipeDebug.convertItemStackArrayToString(ingredients);
			});

			if (WBRecipeComparer.shapelessMatch((ShapelessRecipe) recipe, matrix)) {
				Debug.send(Type.Crafting, "result | crafting_vanilla", () -> "Matched a shapeless recipe and will allow this server recipe.");
				return new ResultContext(recipe.getResult(), ResultType.VANILLA);
			}
		}
		Debug.send(Type.Crafting, "no_match | crafting_vanilla", () -> "Crafting table matrix did not match this crafting recipe.");
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
		builder.append("___________<  crafting_vanilla >___________")
				.append("\n");
		builder.append("Result: ")
				.append(RecipeDebug.formatOneStack(this.recipe.getResult()))
				.append("\n");

		boolean isModern = self().getVersionChecker()
				.newerThan(ServerVersion.v1_13);

		if (recipe instanceof ShapelessRecipe) {
			final ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
			if (isModern) {
				builder.append("Key: ").append(shapeless.getKey())
						.append("\n");
			}
			builder.append("Ingredients: ");

			if (isModern) {
				String joined = shapeless.getChoiceList().stream()
						.map(this::formatChoice)
						.collect(Collectors.joining(", "));
				builder.append(joined).append("\n");
			} else {
				// Legacy: Loopa igenom ItemStack
				String joined = shapeless.getIngredientList().stream()
						.map(stack -> stack != null ?
								stack.getType().name() : "empty")
						.collect(Collectors.joining(", "));
				builder.append(joined).append("\n");
			}
		}

		if (recipe instanceof ShapedRecipe) {
			final ShapedRecipe shaped = (ShapedRecipe) recipe;
			if (isModern) {
				builder.append("Key: ").append(shaped.getKey())
						.append("\n");
			}

			final String[] shape = shaped.getShape();
			builder.append("Shape Layout:\n");

			for (int i = 0; i < shape.length; i++) {
				builder.append("  Row ").append(i + 1).append(": ");
				List<String> rowItems = new ArrayList<>();

				for (char c : shape[i].toCharArray()) {
					if (isModern) {
						RecipeChoice choice = shaped.getChoiceMap().get(c);
						rowItems.add(formatChoice(choice));
					} else {
						ItemStack item = shaped.getIngredientMap().get(c);
						rowItems.add(item != null ?
								item.getType().name() : "empty");
					}
				}
				builder.append(String.join(", ", rowItems)).append("\n");
			}
		}

		builder.append("___________<  crafting_vanilla end >___________\n");
		return builder.toString();
	}

	private String formatChoice(RecipeChoice choice) {
		if (choice == null) {
			return "empty";
		}
		if (choice instanceof RecipeChoice.MaterialChoice) {
			List<Material> mats = ((RecipeChoice.MaterialChoice) choice)
					.getChoices();
			String names = mats.stream()
					.limit(5)
					.map(Enum::name)
					.collect(Collectors.joining("|"));

			if (mats.size() > 5) {
				names += "...";
			}
			return "[" + names + "]";
		}
		return choice.getItemStack() != null ?
				choice.getItemStack().getType().name() : "empty";
	}
}
