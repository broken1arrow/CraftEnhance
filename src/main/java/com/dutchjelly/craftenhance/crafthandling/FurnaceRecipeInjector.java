package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FurnaceRecipeInjector {
	private final CraftEnhance plugin;
	//Stores info to pause furnaces from running their burn event every tick.
	private final Map<Furnace, LocalDateTime> pausedFurnaces = new HashMap<>();
	//Keep track of the id's of the owners of containers.
	@Getter
	private final Map<Location, UUID> containerOwners = new HashMap<>();
	private final RecipeInjector recipeInjector;
	private final boolean newerThanThirteen;

	public FurnaceRecipeInjector(RecipeInjector recipeInjector) {
		this.recipeInjector = recipeInjector;
		this.plugin = this.recipeInjector.getPlugin();
		this.newerThanThirteen = this.recipeInjector.getPlugin().getVersionChecker().newerThan(ServerVersion.v1_13);
	}

	public void smelt(final FurnaceSmeltEvent event) {

		Debug.Send(Type.Smelting, () -> "Furnace has smelt item");

		final RecipeGroup group = this.recipeInjector.getMatchingRecipeGroup(event.getBlock(), event.getSource());
		final Furnace furnace = (Furnace) event.getBlock().getState();
		final Optional<ItemStack> result = this.getFurnaceResult(group, event.getSource(), furnace);

		Debug.Send(Type.Smelting, () -> "Custom result " + result);
		if (result != null && result.isPresent()) {
			ItemStack itemInResulSlot = furnace.getInventory().getResult();
		/*	if(!result.get().isSimilar(itemInResulSlot)) {
				System.out.println("result is not same");
				event.setCancelled(true);
				return;
			}*/
			event.setResult(result.get());
		} else {
			final ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(event.getSource().getType()));
			if (itemStack != null) {
				Debug.Send(Type.Smelting, () -> "Found similar vanilla recipe " + itemStack);
				if (group == null || group.getEnhancedRecipes() == null || group.getEnhancedRecipes().isEmpty()) {
					event.setResult(itemStack);
					return;
				}
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
					final boolean isVanillaRecipe = fRecipe.matchesType(new ItemStack[]{event.getSource()}) && !fRecipe.getResult().isSimilar(itemStack);
					if (eRecipe.isCheckPartialMatch() && isVanillaRecipe) {
						event.setCancelled(true);
						break;
					}
					if (isVanillaRecipe) {
						event.setResult(itemStack);
						break;
					}
				}
			} else {
				Debug.Send(Type.Smelting, () -> "No similar matching to the vanilla recipe, will not changing the outcome.");
			}
			// else
			//event.setCancelled(true);
		}
	}

	public void burn(final FurnaceBurnEvent burnEvent) {
		Debug.Send(Type.Smelting, () -> "Furnace start to burn the item");
		if (burnEvent.isCancelled()) return;
		final Furnace furnace = (Furnace) burnEvent.getBlock().getState();
		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		if (pausedFurnaces.getOrDefault(furnace, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			burnEvent.setCancelled(true);
			return;
		}
		final RecipeGroup recipe = this.recipeInjector.getMatchingRecipeGroup(burnEvent.getBlock(), furnace.getInventory().getSmelting());
		final Optional<ItemStack> result = this.getFurnaceResult(recipe, furnace.getInventory().getSmelting(), furnace);
		ItemStack itemInResulSlot = furnace.getInventory().getResult();
		System.out.println("itemInResulSlot " + itemInResulSlot);
		if (result != null && result.isPresent() && itemInResulSlot != null && itemInResulSlot.getType() != Material.AIR && !result.get().isSimilar(itemInResulSlot)) {
			System.out.println("result is not same");
			burnEvent.setCancelled(true);
			return;
		}

		if (result != null && !result.isPresent()) {
			if (furnace.getInventory().getSmelting() != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(furnace.getInventory().getSmelting().getType())) != null)
				return;
			burnEvent.setCancelled(true);
			pausedFurnaces.put(furnace, LocalDateTime.now().plusSeconds(10L));
		}
	}

	public void furnaceClick(final InventoryClickEvent furnaceClick) {
		if (furnaceClick.isCancelled() || furnaceClick.getClickedInventory() == null) return;

		final Furnace f = (Furnace) furnaceClick.getClickedInventory().getHolder();
		pausedFurnaces.remove(f);
	}


	public void furnacePlace(final BlockPlaceEvent furnacePlaced) {
		if (furnacePlaced.isCancelled()) return;

		containerOwners.put(furnacePlaced.getBlock().getLocation(), furnacePlaced.getPlayer().getUniqueId());
	}

	public void furnaceBreak(final BlockBreakEvent furnaceRemoved) {
		if (furnaceRemoved.isCancelled()) return;
		if (furnaceRemoved.getBlock().getType().equals(Material.FURNACE)) {
			containerOwners.remove(furnaceRemoved.getBlock().getLocation());
			pausedFurnaces.remove((Furnace) furnaceRemoved.getBlock().getState());
		}
	}

	//Add registrations of owners of containers.
	public void registerContainerOwners(final Map<Location, UUID> containerOwners) {
		//Make sure to only register containers, in case some are non existent anymore.
		containerOwners.forEach((key, value) -> {
			if (key != null && key.getWorld() != null)
				this.containerOwners.put(key, value);
		});
	}
	public Optional<ItemStack> getFurnaceResult(final RecipeGroup group, final ItemStack source, final Furnace furnace) {
		//FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
		//RecipeGroup group = RecipeLoader.getInstance().findSimilarGroup(recipe);

		if (group == null) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
			return null;
		}
		final UUID playerId = this.containerOwners.get(furnace.getLocation());
		final Player player = playerId == null ? null : plugin.getServer().getPlayer(playerId);
		Debug.Send(Type.Smelting, () -> "Furnace belongs to player: " + player + " the id " + playerId);
		Debug.Send(Type.Smelting, () -> "Furnace group: " + group);
		Debug.Send(Type.Smelting, () -> "Furnace source item: " + source);
		//Check if any grouped enhanced recipe is a match.
		FurnaceRecipe furnaceRecipe = getFurnaceRecipe(furnace.getType(), group, source, player);
		if (furnaceRecipe != null) return Optional.of(furnaceRecipe.getResult());
		//Check for similar server recipes if no enhanced ones match.
		for (final Recipe sRecipe : group.getServerRecipes()) {
			final org.bukkit.inventory.FurnaceRecipe fRecipe = (org.bukkit.inventory.FurnaceRecipe) sRecipe;
			if (this.recipeInjector.getTypeMatcher().match(fRecipe.getInput(), source)) {
				Debug.Send(Type.Smelting, () -> "found similar server recipe for furnace");
				Debug.Send(Type.Smelting, () -> "Source " + source);
				Debug.Send(Type.Smelting, () -> "Input: " + fRecipe.getInput());
				return null;
			}
		}
		return Optional.empty();
	}

	public FurnaceRecipe getFurnaceRecipe(final Material blockSmelting, final RecipeGroup group, final ItemStack source, final Player player) {
		if (group == null) return null;

		final ItemStack[] srcMatrix = new ItemStack[]{source};
		for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
			if (!eRecipe.matchesBlockType(blockSmelting)) {
				continue;
			}
			final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
			Debug.Send(Type.Smelting, () -> "Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");
			Debug.Send(Type.Smelting, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");

			if (fRecipe.matches(srcMatrix) || (fRecipe.getMatchType() == MatchType.MATCH_META && fRecipe.matchesPartially(srcMatrix))) {
				if (this.recipeInjector.entityCanCraft(player, fRecipe)) {
					Debug.Send(Type.Smelting, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
					Debug.Send(Type.Smelting, () -> "Matching ingridens are " + source + " .");
					return fRecipe;
				} else {
					Debug.Send(Type.Smelting, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
					break;
				}
			} else {
				Debug.Send(Type.Smelting, () -> "found recipe doesn't match '" + source.getType() + (this.recipeInjector.entityCanCraft(player, fRecipe) ? "'." : "and no perms.") + " Check next recipe if it exist.");
				//TODO should this code be removed?
			/*	if (fRecipe.matcheType(srcMatrix)) {
					Debug.Send("Found similar match itemtype for furnace");
					Debug.Send("Is item similar= "  + fRecipe.getContent()[0].isSimilar(srcMatrix[0]));
					Debug.Send("For recipe: " + fRecipe.getResult());
					return Optional.empty();
				}
				Debug.Send("found recipe doesn't match " + (entityCanCraft(p, fRecipe) ? "." : "and no perms."));
				return null;*/
			}
		}
		return null;
	}


}
