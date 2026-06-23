package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

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
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Smoker;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaCookingWrapper implements RecipeWrapper {
	private final CookingRecipe<?> furnaceRecipe;
	private final String key;

	public VanillaCookingWrapper(final CookingRecipe<?> furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;

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
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareFurnaceContext furnaceContext = new PrepareFurnaceContext();
		contextConsumer.accept(furnaceContext);
		final ItemStack[] srcMatrix = furnaceContext.getRecipeMatrix();
		final CookingRecipe<?> fRecipe = this.furnaceRecipe;

		Debug.Send(Type.Smelting, () -> "Checking if vanilla recipe for " + fRecipe.getResult() + " matches.");
		Debug.Send(Type.Smelting, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

		if (matchesType(fRecipe, srcMatrix)) {
			Debug.Send(Type.Smelting, () -> "Found vanilla recipe " + fRecipe.getResult() + " for furnace.");
			Debug.Send(Type.Smelting, () -> "Matching ingredients are " + Arrays.toString(srcMatrix) + " .");
			return new ResultContext(fRecipe.getResult(), ResultType.VANILLA);
		} else {
			Debug.Send(Type.Smelting, () -> "Found recipe doesn't match '" + Arrays.toString(srcMatrix) + "' and output item " + fRecipe.getResult() + ".");
		}
		return new ResultContext(fRecipe.getResult(), ResultType.NO_MATCH);
	}

	public boolean matchesType(final CookingRecipe<?> fRecipe, final ItemStack[] content) {
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
		if (!(o instanceof VanillaCookingWrapper)) return false;
		final VanillaCookingWrapper that = (VanillaCookingWrapper) o;
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
		builder.append("########## Enhanced recipe ################").append("\n");
		//	if (self().getVersionChecker().newerThan(ServerVersion.v1_13))
		//		builder.append("Key: ").append(this.furnaceRecipe.getKey()).append("\n");
		builder.append("Result: ").append(this.furnaceRecipe.getResult()).append("\n");
		//	final RecipeChoice inputChoice = this.furnaceRecipe.getInputChoice();
		//	builder.append("Ingredients: ").append(inputChoice.getItemStack().getType()).append("\n");
		builder.append("########## Enhanced recipe ################\n");
		return builder.toString();
	}
}
