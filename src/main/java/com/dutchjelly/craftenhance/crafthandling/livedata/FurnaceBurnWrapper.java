package com.dutchjelly.craftenhance.crafthandling.livedata;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerCache;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerData;
import com.dutchjelly.craftenhance.messaging.Debug;
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
		return -1;
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

	public FurnaceRecipe matches(@Nonnull final Furnace furnace, @Nonnull final ItemStack source) {
		BlockOwnerCache blockOwnerCache = self().getBlockOwnerCache();
		final BlockOwnerData containerOwner = blockOwnerCache.getContainerOwner(furnace.getLocation());
		final Player player = containerOwner == null ? null : self().getServer().getPlayer(containerOwner.getCurrentOwner());

		final ItemStack[] srcMatrix = new ItemStack[]{source};
		final FurnaceRecipe fRecipe = this.furnaceRecipe;
		Debug.Send(fRecipe, () -> "Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");
		Debug.Send(fRecipe, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

		if (fRecipe.matches(srcMatrix)) {
			if (RecipeAdapter.entityCanCraft(player, fRecipe)) {
				Debug.Send(fRecipe, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
				Debug.Send(fRecipe, () -> "Matching ingredients are " + source + " .");
				return fRecipe;
			} else {
				Debug.Send(fRecipe, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
				return null;
			}
		} else {
			Debug.Send(fRecipe, () -> "found recipe doesn't match '" + source.getType() + (RecipeAdapter.entityCanCraft(player, fRecipe) ? "'." : "' and no perms."));
		}
		return null;
	}


	@Override
	public void matches(@Nonnull final Recipe serverRecipe, @Nonnull final Consumer<PrepareRecipeContext> contextConsumer) {
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
				furnaceContext.acceptResult(fRecipe.getResult());
			} else {
				Debug.Send(fRecipe, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
			}
		} else {
			Debug.Send(fRecipe, () -> "found recipe doesn't match '" + Arrays.toString(srcMatrix) + (RecipeAdapter.entityCanCraft(player, fRecipe) ? "'." : "' and no perms."));
		}
		furnaceContext.acceptResult(null);
	}
}
