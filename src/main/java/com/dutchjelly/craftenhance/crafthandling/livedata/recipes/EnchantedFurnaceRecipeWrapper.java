package com.dutchjelly.craftenhance.crafthandling.livedata.recipes;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.crafthandling.RecipeDebug;
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
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EnchantedFurnaceRecipeWrapper implements RecipeWrapper {
	private final FurnaceRecipe furnaceRecipe;
	private final EnumMap<Material, Integer> ingredients = new EnumMap<>(Material.class);
	private int totalSlotCount;
	private final String key;

	public EnchantedFurnaceRecipeWrapper(@Nonnull final FurnaceRecipe furnaceRecipe) {
		this.furnaceRecipe = furnaceRecipe;
		final ItemStack[] content = furnaceRecipe.getContent();
		for (ItemStack stack : content) {
			if (stack == null) continue;
			Material type = stack.getType();
			if (type == Material.AIR) continue;
			ingredients.merge(type, 1, Integer::sum);
			totalSlotCount++;
		}

		StringBuilder builder = new StringBuilder(furnaceRecipe.getResult().getType().name());
		builder.append("|");
		String collect = Arrays.stream(furnaceRecipe.getContent())
				.filter(Objects::nonNull)
				.map(i -> i.getType().name())
				.sorted()
				.collect(Collectors.joining(","));
		builder.append(collect);
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
	public @Nullable Recipe getRecipe() {
		return this.furnaceRecipe.getServerRecipe();
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

		Debug.send(Type.Smelting, "furnace=" + fRecipe.getKey(), () -> {
			String serverRecipeInfo = "";
			if (serverRecipe != null && fRecipe.getResult().getType() != serverRecipe.getResult().getType()) {
				//serverRecipeInfo = RecipeDebug.formatOneStack(serverRecipe.getResult());
				serverRecipeInfo = ". Server recipe differ to enhanced ";
			}
			return "It will check if recipe allowed for this world, not disabled "+serverRecipeInfo+"and this is a enchanted recipe:\n" + RecipeDebug.formatOneStack(fRecipe.getResult());
		});

		Debug.send(Type.Smelting, "furnace=" + fRecipe.getKey(), () -> "Furnace belongs to player: " + player + " the id " + (player != null ? player.getName() : "ID not found in cache."));
		Debug.send(Type.Smelting, "furnace=" + fRecipe.getKey(), () -> "The smelting matrix item:\n" + RecipeDebug.convertItemStackArrayToString(srcMatrix));

		if (fRecipe.matches(srcMatrix)) {
			if (RecipeAdapter.entityCanCraft(player, fRecipe)) {
				Debug.send(Type.Smelting, "result | furnace=" + fRecipe.getKey(), () -> "Found the enhanced recipe that match this for enchanted furnace recipe.");
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.ENHANCED);
			} else {
				Debug.send(Type.Smelting, "result | furnace=" + fRecipe.getKey(), () -> "Found a matching recipe,but player has not this permission: " + fRecipe.getPermission());
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_PERMISSION);
			}
		} else {
			final boolean isVanillaRecipe = serverRecipe != null && fRecipe.matchesType(new ItemStack[]{((org.bukkit.inventory.FurnaceRecipe) serverRecipe).getInput()}) && !fRecipe.getResult().isSimilar(serverRecipe.getResult());
			if (fRecipe.isCheckPartialMatch() && isVanillaRecipe) {
				Debug.send(Type.Smelting, "vanilla_match | furnace=" + fRecipe.getKey(), () -> "Recipe partial match match for this recipe output: \n" + RecipeDebug.formatOneStack(serverRecipe.getResult()));
				Debug.send(Type.Smelting, "vanilla_match | furnace=" + fRecipe.getKey(), () -> "Recipe partial match match with this matrix input: \n" + RecipeDebug.convertItemStackArrayToString(srcMatrix));
				Debug.send(Type.Smelting, "vanilla_match | furnace=" + fRecipe.getKey(), () -> "And the player " +
						(RecipeAdapter.entityCanCraft(player, fRecipe) ? "have permission to craft the recipe." : "did not have permission to craft the recipe."));
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.PARTIAL_MATCH);
			}
			if (isVanillaRecipe) {
				return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_MATCH);
			}
			Debug.send(Type.Smelting, "no_match | furnace=" + fRecipe.getKey(), () -> "Found smelting matrix doesn't match this enchanted furnace recipe.");
		}
		return new ResultContext(fRecipe, fRecipe.getResult(), ResultType.NO_MATCH);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) return false;
		if (!(o instanceof EnchantedFurnaceRecipeWrapper)) return false;
		final EnchantedFurnaceRecipeWrapper that = (EnchantedFurnaceRecipeWrapper) o;
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
		final ItemStack[] content = this.furnaceRecipe.getContent();
		builder.append("___________< Enhanced furnace recipe >___________").append("\n")
				.append("Key: ").append(this.furnaceRecipe.getKey()).append("\n")
				.append("Result: ").append(RecipeDebug.formatOneStack(this.furnaceRecipe.getResult())).append("\n")
				.append("Ingredients:")
				.append(RecipeDebug.convertItemStackArrayToString(content))
				.append("\n")
				.append("___________< Enhanced furnace recipe end >___________\n");
		return builder.toString();
	}
}
