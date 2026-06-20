package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.RecipeResult;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Smoker;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class VanillaFurnaceWrapper implements RecipeWrapper {
	final FurnaceRecipe furnaceRecipe;

	public VanillaFurnaceWrapper(final FurnaceRecipe furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;
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
		final FurnaceRecipe fRecipe = this.furnaceRecipe;
		Debug.Send(Type.Smelting, () -> "Checking if enhanced recipe for " + fRecipe.getResult() + " matches.");
		Debug.Send(Type.Smelting, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

		if (matchesType(fRecipe, srcMatrix)) {
			Debug.Send(Type.Smelting, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
			Debug.Send(Type.Smelting, () -> "Matching ingredients are " + Arrays.toString(srcMatrix) + " .");
			furnaceContext.setFurnaceResult(RecipeResult.setVanilla(furnaceContext.getFurnace()));
			return null;
		} else {
			Debug.Send(Type.Smelting, () -> "found recipe doesn't match '" + Arrays.toString(srcMatrix) + "'.");
		}
		furnaceContext.setFurnaceResult(RecipeResult.setNone());

		return null;
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
}
