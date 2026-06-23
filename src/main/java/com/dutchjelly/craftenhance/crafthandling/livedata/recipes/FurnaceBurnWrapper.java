package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareRecipeContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FurnaceBurnWrapper implements RecipeWrapper {
	private final FurnaceRecipe furnaceRecipe;
	private final String key;

	public FurnaceBurnWrapper(final FurnaceRecipe furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;

		StringBuilder builder = new StringBuilder(furnaceRecipe.getResult().getType().name());
		builder.append("|");
		String content = Arrays.stream(furnaceRecipe.getContent())
				.filter(Objects::nonNull)
				.map(i -> i.getType().name())
				.sorted()
				.collect(Collectors.joining(","));
		builder.append(content);
		builder.append("|");
		this.key = builder.toString();
	}

	@Nonnull
	@Override
	public String getRecipeKey() {
		return furnaceRecipe.getKey();
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
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.ENHANCED);
			} else {
				Debug.Send(fRecipe, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_PERMISSION);
			}
		} else {
			final boolean isVanillaRecipe = serverRecipe != null && fRecipe.matchesType(new ItemStack[]{((org.bukkit.inventory.FurnaceRecipe) serverRecipe).getInput()}) && !fRecipe.getResult().isSimilar(serverRecipe.getResult());
			if (fRecipe.isCheckPartialMatch() && isVanillaRecipe) {
				Debug.Send(fRecipe, () -> "Recipe partial match match: input '" + Arrays.toString(srcMatrix) + " , furnace burn result " + fRecipe.getResult() + " | " + (RecipeAdapter.entityCanCraft(player, fRecipe) ? "'." : "' and no perms."));
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.PARTIAL_MATCH);
			}
			if (isVanillaRecipe) {
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_MATCH);
			}
			Debug.Send(fRecipe, () -> "found recipe doesn't match '" + Arrays.toString(srcMatrix) + (RecipeAdapter.entityCanCraft(player, fRecipe) ? "'." : "' and no perms."));
		}
		return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_MATCH);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof FurnaceBurnWrapper)) return false;
		final FurnaceBurnWrapper that = (FurnaceBurnWrapper) o;
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
		builder.append("########## Enhanced recipe ################").append("\n");
		builder.append("Key: ").append(this.furnaceRecipe.getKey()).append("\n");
		builder.append("Result: ").append(this.furnaceRecipe.getResult()).append("\n");
		builder.append("Ingredients: ").append(Arrays.toString(this.furnaceRecipe.getContent())).append("\n");
		builder.append("########## Enhanced recipe ################\n");
		return builder.toString();
	}
}
