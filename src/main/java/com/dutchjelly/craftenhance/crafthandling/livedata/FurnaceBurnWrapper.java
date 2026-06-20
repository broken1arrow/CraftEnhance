package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerCache;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerData;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.util.RecipeResult;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FurnaceBurnWrapper implements RecipeWrapper {
	private final FurnaceRecipe furnaceRecipe;

	public FurnaceBurnWrapper(final FurnaceRecipe furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;
	}

	@Nonnull
	@Override
	public RecipeType getRecipeType() {
		return furnaceRecipe.getType();
	}

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@Override
	public <T> Optional<T> getRecipe(final Class<T> type) {
		if (type.isInstance(this.furnaceRecipe))
			return Optional.of(type.cast(this.furnaceRecipe));
		return Optional.empty();
	}

	@Override
	public ResultContext matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
		final PrepareFurnaceContext furnaceContext = new PrepareFurnaceContext();
		contextConsumer.accept(furnaceContext);
		final BlockOwnerCache blockOwnerCache = self().getBlockOwnerCache();
		final Furnace furnace = furnaceContext.getFurnace();
		final BlockOwnerData containerOwner = blockOwnerCache.getContainerOwner(furnace.getLocation());
		final Player player = containerOwner == null ? null : self().getServer().getPlayer(containerOwner.getCurrentOwner());
		final ItemStack[] srcMatrix = furnaceContext.getRecipeMatrix();
		final FurnaceRecipe fRecipe = this.furnaceRecipe;

		Debug.Send(fRecipe, () -> "Furnace belongs to player: " + player + " the id " + (player != null ? player.getName() : "ID not found in cache."));
		Debug.Send(fRecipe, () -> "Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");
		Debug.Send(fRecipe, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

		if (fRecipe.matches(srcMatrix)) {
			if (RecipeAdapter.entityCanCraft(player, fRecipe)) {
				Debug.Send(fRecipe, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
				Debug.Send(fRecipe, () -> "Matching ingredients are " + Arrays.toString(srcMatrix) + " .");
				furnaceContext.setFurnaceResult((RecipeResult.setResult(fRecipe)));
				return null;
			} else {
				Debug.Send(fRecipe, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
			}
		} else {
			Debug.Send(fRecipe, () -> "found recipe doesn't match '" + Arrays.toString(srcMatrix) + (RecipeAdapter.entityCanCraft(player, fRecipe) ? "'." : "' and no perms."));
		}
		furnaceContext.setFurnaceResult(RecipeResult.setNone());
		return null;
	}
}
