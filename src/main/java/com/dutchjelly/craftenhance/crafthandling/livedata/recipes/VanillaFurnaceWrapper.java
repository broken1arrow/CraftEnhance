package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.bukkitadapter.Adapter.RecipeContext;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Smoker;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaFurnaceWrapper implements RecipeWrapper {
	private final FurnaceRecipe furnaceRecipe;
	private final EnumMap<Material, Integer> ingredients;
	private final int totalSlotCount;
	private String key;

	public VanillaFurnaceWrapper(final FurnaceRecipe furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;
		RecipeContext recipeContext = Adapter.getFullIngredientsList(furnaceRecipe);
		this.ingredients = recipeContext.getMap();
		this.totalSlotCount = recipeContext.getTotalSlotCount();

		StringBuilder builder = new StringBuilder(furnaceRecipe.getResult().getType().name());
		builder.append("|");
		builder.append(furnaceRecipe.getInputChoice().getItemStack().getType().name());
		builder.append("|");
		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public String getRecipeKey() {
		return "vanilla_furnace:" + key;
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		RecipeType recipeType = RecipeType.FURNACE;
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			if (furnaceRecipe instanceof BlastFurnace)
				recipeType = RecipeType.BLAST;
			if (furnaceRecipe instanceof Smoker)
				recipeType = RecipeType.SMOKER;
		}
		return recipeType;
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
		final PrepareFurnaceContext furnaceContext = new PrepareFurnaceContext();
		contextConsumer.accept(furnaceContext);
		final ItemStack[] srcMatrix = furnaceContext.getRecipeMatrix();
		final FurnaceRecipe fRecipe = this.furnaceRecipe;

		Debug.send(Type.Smelting, "furnace_recipe", () -> "It will check if recipe allowed for this world, not disabled and this is a valid vanilla furnace recipe output:\n" + RecipeDebug.formatOneStack(serverRecipe.getResult()));
		Debug.send(Type.Smelting, "furnace_recipe", () -> "The smelting matrix item:" + RecipeDebug.convertItemStackArrayToString(srcMatrix));

		if (matchesType(fRecipe, srcMatrix)) {
			Debug.send(Type.Smelting, "result | furnace_recipe", () -> "Matched vanilla furnace recipe.");
			return new ResultContext(fRecipe.getResult(), ResultType.VANILLA);
		} else {
			Debug.send(Type.Smelting, "result | furnace_recipe", () -> "Found furnace recipe doesn't match.");
		}
		Debug.send(Type.Smelting, "no_match | furnace_recipe", () -> "The smelt item did not match this recipe.");
		return new ResultContext(fRecipe.getResult(), ResultType.NO_MATCH);
	}

	public boolean matchesType(final FurnaceRecipe fRecipe, final ItemStack[] content) {
		if (content.length < 1)
			return false;
		return MatchType.MATCH_TYPE.getMatcher().match(fRecipe.getInput(), content[0]);
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.furnaceRecipe))
			return Optional.of(type.cast(this.furnaceRecipe));
		return Optional.empty();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof VanillaFurnaceWrapper)) return false;
		final VanillaFurnaceWrapper that = (VanillaFurnaceWrapper) o;
		return that.key.equals(key);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 41 * hash + key.hashCode();
		return hash;
	}


	public String toString() {
		StringBuilder builder = new StringBuilder();
		final ItemStack inputChoice = this.furnaceRecipe.getInput();
		builder.append("________< Vanilla furnace recipe >________").append("\n");
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
			builder.append("Key: ").append(this.furnaceRecipe.getKey()).append("\n");
		builder.append("Result: ").append(RecipeDebug.formatOneStack(this.furnaceRecipe.getResult())).append("\n");
		builder.append("Ingredients:");
		builder.append(RecipeDebug.formatOneStack(inputChoice));
		builder.append("\n");
		builder.append("________< Vanilla furnace recipe end >________\n");
		return builder.toString();
	}
}
