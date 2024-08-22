package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.api.event.crafting.BeforeCraftOutputEvent;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers.MatchType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.permissions.Permissible;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeInjector implements Listener {

	private final CraftEnhance plugin;
	private RecipeLoader loader;
	private final boolean disableDefaultModeldataCrafts;
	private final boolean makeItemsadderCompatible;

	//Stores info to pause furnaces from running their burn event every tick.
	private final Map<Furnace, LocalDateTime> pausedFurnaces = new HashMap<>();

	//Keep track of the id's of the owners of containers.
	@Getter
	private final Map<Location, UUID> containerOwners = new HashMap<>();
	private final Set<Location> notCustomItem = new HashSet<>();

	public RecipeInjector(final CraftEnhance plugin) {
		this.plugin = plugin;
		disableDefaultModeldataCrafts = plugin.getConfig().getBoolean("disable-default-custom-model-data-crafts");
		makeItemsadderCompatible = plugin.getConfig().getBoolean("make-itemsadder-compatible");
		try {
			Bukkit.getPluginManager().registerEvents(new Smelt(), plugin);
		} catch (Throwable throwable) {
			Debug.Send("Some functions did not work on your serverversion. will be turned off.");
		}
	}

	public void setLoader(final RecipeLoader loader) {
		this.loader = loader;
	}

	//Add registrations of owners of containers.
	public void registerContainerOwners(final Map<Location, UUID> containerOwners) {
		//Make sure to only register containers, in case some are non existent anymore.
		containerOwners.forEach((key, value) -> {
			if (key != null && key.getWorld() != null)
				this.containerOwners.put(key, value);
		});
	}

	private boolean containsModeldata(final CraftingInventory inv) {
		return Arrays.stream(inv.getMatrix()).anyMatch(x -> x != null && x.hasItemMeta() && x.getItemMeta().hasCustomModelData());
	}

	private IMatcher<ItemStack> getTypeMatcher() {
		return Adapter.canUseModeldata() && disableDefaultModeldataCrafts ?
				ItemMatchers.constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData)
				: ItemMatchers::matchType;
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (self().getConfig().getBoolean("learn-recipes")) {
			try {
				for (final NamespacedKey namespacedKey : e.getPlayer().getDiscoveredRecipes()) {
					if (namespacedKey.getNamespace().contains("craftenhance")) {
						e.getPlayer().undiscoverRecipe(namespacedKey);
					}
				}
			} catch (final Exception ignored) {
			}
			Adapter.DiscoverRecipes(e.getPlayer(), RecipeLoader.getInstance().getLoadedServerRecipes());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void handleCrafting(final PrepareItemCraftEvent craftEvent) {
		if (craftEvent.getRecipe() == null || craftEvent.getRecipe().getResult() == null || !plugin.getConfig().getBoolean("enable-recipes"))
			return;
		if (!(craftEvent.getInventory() instanceof CraftingInventory)) return;

		final CraftingInventory inv = craftEvent.getInventory();
		final Recipe serverRecipe = craftEvent.getRecipe();
		Debug.Send(Type.Crafting, () ->  "The server wants to inject " + serverRecipe.getResult() + " ceh will check or modify this.");

		final List<RecipeGroup> possibleRecipeGroups = loader.findGroupsByResult(serverRecipe.getResult(), RecipeType.WORKBENCH);
		final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();

		if (possibleRecipeGroups == null || possibleRecipeGroups.isEmpty()) {
			if (disableDefaultModeldataCrafts && Adapter.canUseModeldata() && containsModeldata(inv)) {
				inv.setResult(null);
			}
			if (checkForDisabledRecipe(disabledServerRecipes, serverRecipe, serverRecipe.getResult())) {
				inv.setResult(null);
			}
			Debug.Send(Type.Crafting, () ->  "No matching groups");
			return;
		}

		for (final RecipeGroup group : possibleRecipeGroups) {
			boolean notAllowedToCraft = false;

			//Check if any grouped enhanced recipe is a match.
			for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
				if (!(eRecipe instanceof WBRecipe)) continue;
				final WBRecipe wbRecipe = (WBRecipe) eRecipe;

				notAllowedToCraft = isCrafingAllowedInWorld(craftEvent, eRecipe);
				if (notAllowedToCraft) {
					Debug.Send(Type.Crafting, () ->  "You are not allowed to craft.");
					continue;
				}

				if (checkForDisabledRecipe(disabledServerRecipes, wbRecipe, serverRecipe.getResult())) {
					inv.setResult(null);
					Debug.Send(Type.Crafting, () ->  "This recipe is disabled.");
					continue;
				}


				Debug.Send(Type.Crafting, () ->  "Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches.");
				final Player player = craftEvent.getViewers().size() > 0 ? (Player) craftEvent.getViewers().get(0) : null;

				if (wbRecipe.matches(inv.getMatrix())
						&& craftEvent.getViewers().stream().allMatch(x -> entityCanCraft(x, wbRecipe))
						&& !CraftEnhanceAPI.fireEvent(wbRecipe, player, inv, group)) {
					Debug.Send(Type.Crafting, () ->  "Recipe matches, injecting " + wbRecipe.getResult().toString());
					if (makeItemsadderCompatible && containsModeldata(inv)) {
						Debug.Send(Type.Crafting, () ->  "This recipe contains Modeldata and will be crafted if the recipe is not cancelled.");
						Bukkit.getScheduler().runTask(self(), () -> {
							if (wbRecipe.matches(inv.getMatrix())) {
								final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(eRecipe, wbRecipe, wbRecipe.getResult().clone());
								if (beforeCraftOutputEvent.isCancelled()) {
									Debug.Send(Type.Crafting, () ->  "This recipe is now cancelled and will not produce output item.");
									return;
								}
								Debug.Send(Type.Crafting, () ->  "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
								Debug.Send(Type.Crafting, () -> "The recipe matrix: " + Arrays.toString(inv.getMatrix()));
								inv.setResult(beforeCraftOutputEvent.getResultItem());
							}
						});
					} else {
						Debug.Send(Type.Crafting, () ->  "This recipe deosen't contains Modeldata and will be crafted if the recipe is not cancelled.");

						final BeforeCraftOutputEvent beforeCraftOutputEvent = new BeforeCraftOutputEvent(eRecipe, wbRecipe, wbRecipe.getResult().clone());
						if (beforeCraftOutputEvent.isCancelled()) {
							Debug.Send(Type.Crafting, () ->  "This recipe is now cancelled and will not produce output item.");
							continue;
						}
						Debug.Send(Type.Crafting, () ->  "The recipe is now crafted and output item is " + beforeCraftOutputEvent.getResultItem());
						Debug.Send(Type.Crafting, () ->  "The recipe matrix: " + Arrays.toString(inv.getMatrix()));
						inv.setResult(beforeCraftOutputEvent.getResultItem());
					}
					return;
				}
				Debug.Send(Type.Crafting, () ->  "Recipe matrix doesn't match.");
				Debug.Send(Type.Crafting, () ->  "The recipe matrix: " + Arrays.toString(wbRecipe.getContent()));
				Debug.Send(Type.Crafting, () ->  "The matrix on craftingtable: " + Arrays.toString(inv.getMatrix()));
				if (wbRecipe.isCheckPartialMatch() && wbRecipe.matches(inv.getMatrix(), MatchType.MATCH_TYPE.getMatcher())) {
					Debug.Send(Type.Crafting, () ->  "Partial matched recipe fond and will prevent craft this recipe.");
					inv.setResult(null);
					return;
				}
			}
			if (notAllowedToCraft)
				continue;

			Debug.Send(Type.Crafting, () ->  "Check for similar server recipes if no enhanced ones match.");
			//Check for similar server recipes if no enhanced ones match.
			for (final Recipe sRecipe : group.getServerRecipes()) {
				if (sRecipe instanceof ShapedRecipe) {
					final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) sRecipe);
					if (WBRecipeComparer.shapeMatches(content, inv.getMatrix(), getTypeMatcher())) {
						Debug.Send(Type.Crafting, () ->  "Match a ShapedRecipe.");
						Debug.Send(Type.Crafting, () ->  "The recipe matrix for this type: " + Arrays.toString(inv.getMatrix()));
						inv.setResult(sRecipe.getResult());
						return;
					}
				} else if (sRecipe instanceof ShapelessRecipe) {
					final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) sRecipe);
					if (WBRecipeComparer.ingredientsMatch(ingredients, inv.getMatrix(), getTypeMatcher())) {
						Debug.Send(Type.Crafting, () ->  "Match a ShapelessRecipe.");
						Debug.Send(Type.Crafting, () ->  "The recipe matrix for this type: " + Arrays.toString(inv.getMatrix()));
						inv.setResult(sRecipe.getResult());
						return;
					}
				}
			}
		}
		inv.setResult(null); //We found similar custom recipes, but none matched exactly. So set result to null.
	}

	public boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull Recipe recipe, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result)) {
					return true;
				}
			}
		return false;
	}

	public boolean checkForDisabledRecipe(final List<Recipe> disabledServerRecipes, final @NonNull WBRecipe wbRecipe, final @NonNull ItemStack result) {
		if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
			for (final Recipe disabledRecipe : disabledServerRecipes) {
				if (disabledRecipe.getResult().isSimilar(result) && wbRecipe.isSimilar(disabledRecipe)) {
					return true;
				}
			}
		return false;
	}
@Nullable
	public RecipeGroup getMatchingRecipeGroup(final Block typeOfFurnace, final ItemStack source) {
		final ItemStack[] srcMatrix = new ItemStack[]{source};
		RecipeType recipeType = RecipeType.getType(typeOfFurnace);
		if (recipeType == null) return null;

		FurnaceRecipe recipe = null;
		switch (recipeType) {
			case WORKBENCH:
				//recipe = new WBRecipe(null, null, srcMatrix);
				break;
			case FURNACE:
				recipe = new FurnaceRecipe(null, null, srcMatrix);
				break;
			case BLAST:
				recipe = new BlastRecipe(null, null, srcMatrix);
				break;
			case SMOKER:
				recipe = new SmokerRecipe(null, null, srcMatrix);
				break;
		}
		if (recipe == null) return null;

		return RecipeLoader.getInstance().findSimilarGroup(recipe);
	}

	public Optional<ItemStack> getFurnaceResult(final RecipeGroup group, final ItemStack source, final Furnace furnace) {
		//FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
		//RecipeGroup group = RecipeLoader.getInstance().findSimilarGroup(recipe);

		if (group == null) {
			Debug.Send(Type.Smelting, () -> "furnace recipe does not match any recipe group.");
			return null;
		}
		final UUID playerId = containerOwners.get(furnace.getLocation());
		final Player p = playerId == null ? null : plugin.getServer().getPlayer(playerId);
		Debug.Send(Type.Smelting, () -> "Furnace belongs to player: " + p + " the id " + playerId);
		Debug.Send(Type.Smelting, () -> "Furnace group: " + group);
		Debug.Send(Type.Smelting, () -> "Furnace source item: " + source);
		//Check if any grouped enhanced recipe is a match.
		FurnaceRecipe furnaceRecipe = getFurnaceRecipe(furnace.getType(), group, source, p);
		if (furnaceRecipe != null) return Optional.of(furnaceRecipe.getResult());
		//Check for similar server recipes if no enhanced ones match.
		for (final Recipe sRecipe : group.getServerRecipes()) {
			final org.bukkit.inventory.FurnaceRecipe fRecipe = (org.bukkit.inventory.FurnaceRecipe) sRecipe;
			if (getTypeMatcher().match(fRecipe.getInput(), source)) {
				Debug.Send(Type.Smelting, () -> "found similar server recipe for furnace");
				Debug.Send(Type.Smelting, () -> "Source " + source);
				Debug.Send(Type.Smelting, () -> "Input: " + fRecipe.getInput());
				return null;
			}
		}
		return Optional.empty();
	}

	private FurnaceRecipe getFurnaceRecipe(final Material blockSmelting, final RecipeGroup group, final ItemStack source, final Player player) {
		if (group == null) return null;

		final ItemStack[] srcMatrix = new ItemStack[]{source};
		for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
			if (!eRecipe.matchesBlockType(blockSmelting)) {
				continue;
			}

			final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;

			Debug.Send(Type.Smelting, () -> "Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");
			Debug.Send(Type.Smelting, () -> "The srcMatrix " + Arrays.toString(srcMatrix) + ".");
			if (fRecipe.matches(srcMatrix)) {
				if (entityCanCraft(player, fRecipe)) {
					Debug.Send(Type.Smelting, () -> "Found enhanced recipe " + fRecipe.getResult() + " for furnace");
					Debug.Send(Type.Smelting, () -> "Matching ingridens are " + source + " .");
					return fRecipe;
				} else {
					Debug.Send(Type.Smelting, () -> "found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermission());
					break;
				}
			} else {
				Debug.Send(Type.Smelting, () -> "found recipe doesn't match '" + source.getType() + (entityCanCraft(player, fRecipe) ? "'." : "and no perms.") + " Check next recipe if it exist.");
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

	@EventHandler
	public void exstract(final FurnaceExtractEvent e) {

	/*	if (!notCustomItem.isEmpty() && notCustomItem.contains(e.getBlock().getLocation())) {
			e.setExpToDrop(getExp(e.getItemType()));
			notCustomItem.remove(e.getBlock().getLocation());
		}*/
	}

	@EventHandler
	public void smelt(final FurnaceSmeltEvent e) {

		Debug.Send(Type.Smelting, () -> "Furnace has smelt item");
		final RecipeGroup group = getMatchingRecipeGroup(e.getBlock(), e.getSource());
		final Optional<ItemStack> result = getFurnaceResult(group, e.getSource(), (Furnace) e.getBlock().getState());
		Debug.Send(Type.Smelting, () -> "Custom result " + result);
		if (result != null && result.isPresent()) {
			e.setResult(result.get());
		} else {
			final ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(e.getSource().getType()));
			if (itemStack != null) {
				Debug.Send(Type.Smelting, () -> "Found similar vanilla recipe " + itemStack);
				if (group == null || group.getEnhancedRecipes() == null || group.getEnhancedRecipes().isEmpty()) {
					e.setResult(itemStack);
					return;
				}
				for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
					final boolean isVanillaRecipe = fRecipe.matcheType(new ItemStack[]{e.getSource()}) && !fRecipe.getResult().isSimilar(itemStack);
					if (eRecipe.isCheckPartialMatch() && isVanillaRecipe) {
						e.setCancelled(true);
						break;
					}
					if (isVanillaRecipe) {
						e.setResult(itemStack);
						notCustomItem.add(e.getBlock().getLocation());
						break;
					}
				}
			} else {
				Debug.Send(Type.Smelting, () -> "No similar matching to the vanilla recipe, will not changing the outcome.");
			}
			// else
			//e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void burn(final FurnaceBurnEvent e) {
		Debug.Send(Type.Smelting, () -> "Furnace start to burn the item");
		if (e.isCancelled()) return;
		final Furnace f = (Furnace) e.getBlock().getState();
		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		if (pausedFurnaces.getOrDefault(f, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			e.setCancelled(true);
			return;
		}
		final RecipeGroup recipe = getMatchingRecipeGroup(e.getBlock(), f.getInventory().getSmelting());
		final Optional<ItemStack> result = getFurnaceResult(recipe, f.getInventory().getSmelting(), (Furnace) e.getBlock().getState());
		if (result != null && !result.isPresent()) {
			if (f.getInventory().getSmelting() != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(f.getInventory().getSmelting().getType())) != null)
				return;
			e.setCancelled(true);
			pausedFurnaces.put(f, LocalDateTime.now().plusSeconds(10L));
		}
	}

	@EventHandler
	public void furnaceClick(final InventoryClickEvent e) {
		if (e.isCancelled()) return;
		if (e.getView().getTopInventory() instanceof FurnaceInventory) {
			final Furnace f = (Furnace) e.getView().getTopInventory().getHolder();

			pausedFurnaces.remove(f);
		}
	}

	@EventHandler
	public void furnacePlace(final BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		Material material = e.getBlock().getType();
		if (material == Material.FURNACE || self().getVersionChecker().newerThan(ServerVersion.v1_13) && (material == Material.BLAST_FURNACE || material == Material.SMOKER)) {
			containerOwners.put(e.getBlock().getLocation(), e.getPlayer().getUniqueId());
		}
	}

	@EventHandler
	public void furnaceBreak(final BlockBreakEvent e) {
		if (e.isCancelled()) return;
		if (e.getBlock().getType().equals(Material.FURNACE)) {
			containerOwners.remove(e.getBlock().getLocation());
			pausedFurnaces.remove((Furnace) e.getBlock().getState());
		}
	}

	private boolean isCrafingAllowedInWorld(final PrepareItemCraftEvent craftEvent, final EnhancedRecipe eRecipe) {
		final Set<String> allowedWorlds = eRecipe.getAllowedWorlds();
		if (allowedWorlds == null || allowedWorlds.isEmpty()) return false;

		for (final HumanEntity viwer : craftEvent.getViewers()) {
			for (final String world : allowedWorlds) {
				if (viwer.getWorld().getName().equals(world)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean entityCanCraft(final Permissible entity, final EnhancedRecipe group) {
		return group.getPermission() == null || group.getPermission().equals("")
				|| (entity != null && entity.hasPermission(group.getPermission()));
	}

	private class Smelt implements Listener {

		@EventHandler
		public void startSmelt(FurnaceStartSmeltEvent event) {
			final RecipeGroup group = getMatchingRecipeGroup(event.getBlock(), event.getSource());
			FurnaceRecipe furnaceRecipe = getFurnaceRecipe(event.getBlock().getType(), group, event.getSource(), null);
			if (furnaceRecipe == null) {
				/*todo need to fix so you can stop it from progress if not allow to burn the item  */
				return;
			}
			event.setTotalCookTime(furnaceRecipe.getDuration());
		}
	}

	private enum Matchning {
		NON_MATCH,
		SIMILAR,
		MATCH,
	}
}
