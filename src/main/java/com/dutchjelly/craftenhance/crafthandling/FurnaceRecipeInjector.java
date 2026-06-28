package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerCache;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerData;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.RecipeResult;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FurnaceRecipeInjector {
	//Stores info to pause furnaces from running their burn event every tick.
	private final Map<Furnace, FurnacesPauseContext> pausedFurnaces = new HashMap<>();
	//Keep track of the id's of the owners of containers.
	@Getter
	private final BlockOwnerCache blockOwnerCache;

	public FurnaceRecipeInjector(RecipeInjector recipeInjector) {
		this.blockOwnerCache = self().getBlockOwnerCache();
	}

	public void smeltTask(final FurnaceSmeltEvent event) {
		Debug.Send(Type.Smelting, () -> "Furnace has smelt item, will start check if the output item is allowed.");

		final ItemStack[] matrix = {new ItemStack(event.getSource())};
		final Furnace furnace = (Furnace) event.getBlock().getState();
		RecipeType recipeType = RecipeType.FURNACE;
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			if (furnace instanceof BlastFurnace)
				recipeType = RecipeType.BLAST;
			if (furnace instanceof Smoker)
				recipeType = RecipeType.SMOKER;
		}
		final List<RecipeWrapper> matchingRecipe = RecipeLoader.getInstance().findMatchingRecipe(recipeType, matrix);
		org.bukkit.inventory.Recipe recipe;
		ItemStack resultItem;
		if (self().getVersionChecker().olderThan(ServerVersion.v1_14)) {
			recipe = Bukkit.getRecipesFor(event.getResult()).stream().findFirst().orElse(null);
			resultItem = event.getResult();
		} else {
			recipe = event.getRecipe();
			resultItem = null;
		}

		final RecipeResult result = getFurnaceResult(recipe, resultItem, matchingRecipe, matrix, furnace);

		if (result.isEnhancedRecipe()) {
			Debug.Send(Type.Smelting, () -> "Custom item smelted, the result item: " + RecipeDebug.formatOneStack(result.getItem()));
			event.setResult(result.getItem());
		} else {
			if (result.isNone()) {
				event.setCancelled(true);
				Debug.Send(Type.Smelting, () -> "No similar matching to the vanilla recipe, will prevent the recipe.");
				return;
			}
			if (result.isVanilla()) {
				Debug.Send(Type.Smelting, () -> "Found similar vanilla recipe " + result.getItem());
				event.setResult(result.getItem());
			}
		}
	}

	public void burnTask(final FurnaceBurnEvent burnEvent) {
		if (burnEvent.isCancelled()) return;
		final Furnace furnace = (Furnace) burnEvent.getBlock().getState();

		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		final FurnacesPauseContext pauseContext = pausedFurnaces.getOrDefault(furnace, FurnacesPauseContext.of(LocalDateTime.now()));
		if (pauseContext.getLocalDateTime().isAfter(LocalDateTime.now())) {
			pauseContext.sendMessage();
			if (self().getVersionChecker().olderThan(ServerVersion.v1_26))
				burnEvent.setCancelled(true);
			return;
		}

		Debug.Send(Type.Smelting, () -> "Furnace start to burn the item, will check if item could be smelt.");
		RecipeType recipeType = RecipeType.FURNACE;
		if (self().getVersionChecker().newerThan(ServerVersion.v1_13)) {
			if (furnace instanceof BlastFurnace)
				recipeType = RecipeType.BLAST;
			if (furnace instanceof Smoker)
				recipeType = RecipeType.SMOKER;
		}

		final FurnaceInventory furnaceInventory = furnace.getInventory();
		final ItemStack smeltingStack = furnaceInventory.getSmelting();
		final ItemStack[] matrix = {(smeltingStack != null ? new ItemStack(smeltingStack) : new ItemStack(Material.AIR))};
		final List<RecipeWrapper> matchingRecipe = RecipeLoader.getInstance().findMatchingRecipe(recipeType, matrix);
		final RecipeResult result = getFurnaceResult(null, null, matchingRecipe, matrix, furnace);
		ItemStack itemInResulSlot = furnaceInventory.getResult();
		if (result.isEnhancedRecipe() && itemInResulSlot != null && itemInResulSlot.getType() != Material.AIR && !result.getItem().isSimilar(itemInResulSlot)) {
			Debug.Send(Type.Smelting, () -> "It is already an item inside the furnace, that is not similar. Can't smelt the item");
			burnEvent.setCancelled(true);
			return;
		}

		pausedFurnaces.put(furnace, FurnacesPauseContext.of(LocalDateTime.now().plusSeconds(5L)));
		if (result.isNone()) {
			Debug.Send(Type.Smelting, () -> "The recipe is not an enhanced recipe or vanilla recipe it will abort the burn event.");
			burnEvent.setCancelled(true);
		}
	}


	public void furnaceClick(final InventoryClickEvent furnaceClick) {
		if (furnaceClick.isCancelled() || furnaceClick.getClickedInventory() == null) return;
/*		System.out.println("blockOwnerCache. " + blockOwnerCache.getContainerOwners());
		blockOwnerCache.getContainerOwner(furnaceClick.getClickedInventory().getLocation(), blockOwnerData -> {
			System.out.println("blockdata Owner " + blockOwnerData.getCurrentOwner());
			System.out.println("blockdata loc " + blockOwnerData.getLocation());
		});*/

		final Furnace f = (Furnace) furnaceClick.getClickedInventory().getHolder();
		pausedFurnaces.remove(f);
	}


	public void furnacePlace(final BlockPlaceEvent furnacePlaced) {
		if (furnacePlaced.isCancelled()) return;

		blockOwnerCache.putContainerOwner(furnacePlaced.getBlock().getLocation(), blockOwnerData -> blockOwnerData.setCurrentOwner(furnacePlaced.getPlayer().getUniqueId()));
	}

	public void furnaceBreak(final BlockBreakEvent furnaceRemoved) {
		if (furnaceRemoved.isCancelled()) return;
		if (furnaceRemoved.getBlock().getType().equals(Material.FURNACE)) {
			final BlockOwnerData block = blockOwnerCache.remove(furnaceRemoved.getBlock().getLocation());
			pausedFurnaces.remove((Furnace) furnaceRemoved.getBlock().getState());
		}
	}

	//Add registrations of owners of containers.
	public void registerContainerOwners(final Map<String, BlockOwnerData> containerOwners) {
		//Make sure to only register containers, in case some are non existent anymore.
	/*	containerOwners.forEach((key, value) -> {
			if (key != null && value.getLocation() != null && value.getLocation().getWorld() != null)
				this.containerOwners.put(key, value);
		});*/
	}

	public BlockOwnerCache getBlockOwnerCache() {
		return blockOwnerCache;
	}

	@NonNull
	public RecipeResult getFurnaceResult(final Recipe serverRecipe, @Nullable final ItemStack resultItem, final List<RecipeWrapper> recipeWrappers, final ItemStack[] matrix, final Furnace furnace) {
		if (recipeWrappers.isEmpty()) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
			if (resultItem != null)
				return RecipeResult.setVanilla(resultItem);
			return RecipeResult.setVanilla(serverRecipe != null ? serverRecipe.getResult() : null);
		}

		for (RecipeWrapper group : recipeWrappers) {
			ResultContext resultContext = group.matches(serverRecipe, prepareRecipeContext -> {
				if (!(prepareRecipeContext instanceof PrepareFurnaceContext)) return;
				final PrepareFurnaceContext furnaceContext = (PrepareFurnaceContext) prepareRecipeContext;
				furnaceContext.setFurnace(furnace);
				furnaceContext.setRecipeMatrix(matrix);
			});
			if (resultContext == null) continue;

			if (resultContext.getResultType() == ResultType.ENHANCED) {
				return RecipeResult.setCustomRecipe(resultContext.getItemStack());
			}
			if (resultContext.getResultType() == ResultType.VANILLA) {
				if (resultItem != null)
					return RecipeResult.setVanilla(resultItem);
				return RecipeResult.setVanilla(serverRecipe != null ? serverRecipe.getResult() : null);
			}
			if (resultContext.getResultType() == ResultType.PARTIAL_MATCH) {
				return RecipeResult.setNone();
			}
		}
		return RecipeResult.setNone();
	}

	@NonNull
	public ResultContext getFurnaceContext(final Recipe serverRecipe, final List<RecipeWrapper> recipeWrappers, final ItemStack[] matrix, final Furnace furnace) {
		if (recipeWrappers.isEmpty()) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
			return new ResultContext(serverRecipe != null ? serverRecipe.getResult() : null, ResultType.VANILLA);
		}

		for (RecipeWrapper group : recipeWrappers) {
			ResultContext resultContext = group.matches(serverRecipe, prepareRecipeContext -> {
				if (!(prepareRecipeContext instanceof PrepareFurnaceContext)) return;
				final PrepareFurnaceContext furnaceContext = (PrepareFurnaceContext) prepareRecipeContext;
				furnaceContext.setFurnace(furnace);
				furnaceContext.setRecipeMatrix(matrix);
			});
			if (resultContext == null) continue;

			return resultContext;
		}
		return new ResultContext(null, ResultType.NO_MATCH);
	}

	final static class FurnacesPauseContext {
		private LocalDateTime localDateTime;
		private int sendMessaage;


		public FurnacesPauseContext(final LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}

		public static FurnacesPauseContext of(final LocalDateTime localDateTime) {
			return new FurnacesPauseContext(localDateTime);
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void sendMessage() {
			if (sendMessaage % 40 == 0) {
				Debug.Send(Type.Smelting, () -> "Furnace burn event check is on pause for couple of seconds.");
			}
			sendMessaage++;
		}

	}
}
