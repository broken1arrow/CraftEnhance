package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.cache.EnhancedRecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.PrepareFurnaceContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultContext;
import com.dutchjelly.craftenhance.crafthandling.livedata.event.ResultType;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerCache;
import com.dutchjelly.craftenhance.files.blockowner.BlockOwnerData;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import com.dutchjelly.craftenhance.util.RecipeResult;
import lombok.Getter;
import lombok.NonNull;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class FurnaceRecipeInjector {
	//Stores info to pause furnaces from running their burn event every tick.
	private final Map<Furnace, LocalDateTime> pausedFurnaces = new HashMap<>();
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
		final RecipeResult result = getFurnaceResult(event.getRecipe(), matchingRecipe, matrix, furnace);

		if (result.isEnhancedRecipe()) {
			Debug.Send(Type.Smelting, () -> "Custom item smelted, the result item: " + result);
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
		if (pausedFurnaces.getOrDefault(furnace, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			Debug.Send(Type.Smelting, () -> "Furnace burn event is on pause for couple of seconds.");
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
		final RecipeResult result = getFurnaceResult(null, matchingRecipe, matrix, furnace);
		ItemStack itemInResulSlot = furnaceInventory.getResult();
		if (result.isEnhancedRecipe() && itemInResulSlot != null && itemInResulSlot.getType() != Material.AIR && !result.getItem().isSimilar(itemInResulSlot)) {
			Debug.Send(Type.Smelting, () -> "It is already an item inside the furnace, that is not similar. Can't smelt the item");
			burnEvent.setCancelled(true);
			return;
		}

		if (!result.isEnhancedRecipe()) {
			if (smeltingStack != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(smeltingStack.getType())) != null)
				return;
			if (result.isVanilla())
				return;
			if (result.isNone()) {
				Debug.Send(Type.Smelting, () -> "The recipe is not an enhanced recipe or vanilla recipe it will abort the burn event.");
				burnEvent.setCancelled(true);
			}
			pausedFurnaces.put(furnace, LocalDateTime.now().plusSeconds(10L));
		}
	}

	private void smeltTask(final FurnaceSmeltEvent event, final RecipeResult result, final Furnace furnace, final List<RecipeGroup> groups) {
		if (result.isEnhancedRecipe()) {
			Debug.Send(Type.Smelting, () -> "Custom item smelted, the result item: " + result);
			ItemStack itemInResulSlot = furnace.getInventory().getResult();
		/*	if(!result.get().isSimilar(itemInResulSlot)) {
				System.out.println("result is not same");
				event.setCancelled(true);
				return;
			}*/
			event.setResult(result.getItem());
		} else {
			final ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(event.getSource().getType()));
			if (itemStack != null && groups != null) {
				Debug.Send(Type.Smelting, () -> "Found similar vanilla recipe " + itemStack);
				for (RecipeGroup group : groups) {
					final Collection<EnhancedRecipeWrapper> recipeCoreList = group == null ? null : group.getRecipeGroupCache().values();
					if (group == null || recipeCoreList.isEmpty()) {
						event.setResult(itemStack);
						return;
					}
					for (final EnhancedRecipeWrapper eRecipe : recipeCoreList) {
						final EnhancedRecipe enhancedRecipe = eRecipe.getEnhancedRecipe();
						if (!(enhancedRecipe instanceof FurnaceRecipe)) continue;
						final FurnaceRecipe fRecipe = (FurnaceRecipe) enhancedRecipe;

						final boolean isVanillaRecipe = fRecipe.matchesType(new ItemStack[]{event.getSource()}) && !fRecipe.getResult().isSimilar(itemStack);
						if (fRecipe.isCheckPartialMatch() && isVanillaRecipe) {
							event.setCancelled(true);
							break;
						}
						if (isVanillaRecipe) {
							event.setResult(itemStack);
							break;
						}
					}
				}
			} else {
				if (result.isNone())
					event.setCancelled(true);
				Debug.Send(Type.Smelting, () -> "No similar matching to the vanilla recipe, will not changing the outcome.");
			}
			// else
			//event.setCancelled(true);
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
	public RecipeResult getFurnaceResult(final Recipe serverRecipe, final List<RecipeWrapper> recipeWrappers, final ItemStack[] matrix, final Furnace furnace) {
		if (recipeWrappers.isEmpty()) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
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

/*

	public void burn(final FurnaceBurnEvent burnEvent) {
		if (burnEvent.isCancelled()) return;


		final Furnace furnace = (Furnace) burnEvent.getBlock().getState();
		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		if (pausedFurnaces.getOrDefault(furnace, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			Debug.Send(Type.Smelting, () -> "Furnace burn event is on pause for couple of seconds.");
			burnEvent.setCancelled(true);
			return;
		}
		Debug.Send(Type.Smelting, () -> "Furnace start to burn event for the item");
		final FurnaceInventory furnaceInventory = furnace.getInventory();
		final List<RecipeGroup> group = this.recipeInjector.getMatchingRecipeGroup(null, burnEvent.getBlock(), furnaceInventory.getSmelting());

		final RecipeResult result = this.getFurnaceResult(group, furnaceInventory.getSmelting(), furnace);
		ItemStack itemInResulSlot = furnaceInventory.getResult();
		if (result.isEnhancedRecipe() && itemInResulSlot != null && itemInResulSlot.getType() != Material.AIR && !result.getItem().isSimilar(itemInResulSlot)) {
			Debug.Send(Type.Smelting, () -> "It is already an item inside the furnace, that is not similar. Can't smelt the item");
			burnEvent.setCancelled(true);
			return;
		}

		if (!result.isEnhancedRecipe()) {
			if (furnaceInventory.getSmelting() != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(furnaceInventory.getSmelting().getType())) != null)
				return;
			if (result.isVanilla())
				return;
			if (result.isNone()) {
				Debug.Send(Type.Smelting, () -> "The recipe is not an enhanced recipe or vanilla recipe it will abort the burn event.");
				burnEvent.setCancelled(true);
			}
			pausedFurnaces.put(furnace, LocalDateTime.now().plusSeconds(10L));
		}
	}

	public RecipeResult getFurnaceResult(final List<RecipeGroup> groups, final ItemStack source, final Furnace furnace) {
		//FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
		//RecipeGroup group = RecipeLoader.getInstance().findSimilarGroup(recipe);

		if (groups == null || groups.isEmpty()) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
			return RecipeResult.setVanilla(null);
		}
		final BlockOwnerData containerOwner = this.blockOwnerCache.getContainerOwner(furnace.getLocation());
		final Player player = containerOwner == null ? null : plugin.getServer().getPlayer(containerOwner.getCurrentOwner());

		for (RecipeGroup group : groups) {        //Check if any grouped enhanced recipe is a match.
			FurnaceRecipe furnaceRecipe = getFurnaceRecipe(furnace.getType(), group, source, player);
			Debug.Send(DebugType.of(Type.Smelting, furnaceRecipe), () -> "Furnace belongs to player: " + player + " the id " + (containerOwner != null ? containerOwner.getCurrentOwner() : "ID not found."));
			Debug.Send(DebugType.of(Type.Smelting, furnaceRecipe), () -> "Furnace group: " + groups);
			Debug.Send(DebugType.of(Type.Smelting, furnaceRecipe), () -> "Furnace source item: " + source);

			if (furnaceRecipe != null) return RecipeResult.setCustomRecipe(furnaceRecipe.getResult());
			//Check for similar server recipes if no enhanced ones match.
			for (final Recipe sRecipe : group.getServerRecipes()) {
				if (Adapter.isCraftingRecipe(sRecipe)) continue;

				final org.bukkit.inventory.FurnaceRecipe fRecipe = (org.bukkit.inventory.FurnaceRecipe) sRecipe;
				if (RecipeAdapter.getTypeMatcher().match(fRecipe.getInput(), source)) {
					Debug.Send(Type.Smelting, () -> "Found similar server recipe for furnace, will prevent the recipe to be burnt.");
					Debug.Send(Type.Smelting, () -> "Source " + source);
					Debug.Send(Type.Smelting, () -> "Input: " + fRecipe.getInput());
					return RecipeResult.setNone();
				}
			}

		}
		*/
	/*todo does this part needed, if a recipe get detected wrong, as should be treated as normal vanilla recipe?
	 *//*

	 */
/*	   		for (final Recipe sRecipe : RecipeLoader.getInstance().getServerFurnaceRecipes()) {
				if(sRecipe instanceof CraftingRecipe) continue;

				final org.bukkit.inventory.CookingRecipe<?> fRecipe = (org.bukkit.inventory.CookingRecipe<?>) sRecipe;
			    final ItemStack inputStack = fRecipe.getInput();

				if(self().getVersionChecker().newerThan(ServerVersion.v1_12) && fRecipe.getKey().getNamespace().contains("craftenhance")){
					continue;
				}
				if (this.recipeInjector.getTypeMatcher().match(inputStack,source )) {
					Debug.Send(Type.Smelting, () -> "Found similar server recipe for furnace, will allowed to next step.");
					Debug.Send(Type.Smelting, () -> "Source " + source);
					Debug.Send(Type.Smelting, () -> "Input: " + inputStack);
					return RecipeResult.setVanilla();
				}
			}*//*

		return RecipeResult.setNone();
	}

	public FurnaceRecipe getFurnaceRecipe(final Material blockSmelting, final RecipeGroup group, final ItemStack source, final Player player) {
		if (group == null) return null;

		final ItemStack[] srcMatrix = new ItemStack[]{source};
		for (final EnhancedRecipeWrapper eRecipe : group.getRecipeGroupCache().values()) {
			final EnhancedRecipe enhancedRecipe = eRecipe.getEnhancedRecipe();
			if (!(enhancedRecipe instanceof FurnaceRecipe)) continue;
			if (!enhancedRecipe.matchesBlockType(blockSmelting)) {
				continue;
			}
			final FurnaceRecipe fRecipe = (FurnaceRecipe) enhancedRecipe;
			Debug.Send(fRecipe, () -> "Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");
			Debug.Send(fRecipe, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

			if (fRecipe.matches(srcMatrix)) {
				if (this.recipeInjector.entityCanCraft(player, fRecipe)) {
					Debug.Send(fRecipe, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
					Debug.Send(fRecipe, () -> "Matching ingredients are " + source + " .");
					return fRecipe;
				} else {
					Debug.Send(fRecipe, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
					break;
				}
			} else {
				Debug.Send(fRecipe, () -> "found recipe doesn't match '" + source.getType() + (this.recipeInjector.entityCanCraft(player, fRecipe) ? "'." : "and no perms.") + " Check next custom recipe if it exist.");
				//TODO should this code be removed?
			*/
/*	if (fRecipe.matcheType(srcMatrix)) {
					Debug.Send("Found similar match itemtype for furnace");
					Debug.Send("Is item similar= "  + fRecipe.getContent()[0].isSimilar(srcMatrix[0]));
					Debug.Send("For recipe: " + fRecipe.getResult());
					return Optional.empty();
				}
				Debug.Send("found recipe doesn't match " + (entityCanCraft(p, fRecipe) ? "." : "and no perms."));
				return null;*//*

			}
		}
		return null;
	}
*/


}
