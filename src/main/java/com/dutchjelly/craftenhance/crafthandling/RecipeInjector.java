package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.*;

import static com.dutchjelly.craftenhance.util.FurnaceDefultValues.getExp;

public class RecipeInjector implements Listener {

	private JavaPlugin plugin;
	private RecipeLoader loader;
	private boolean disableDefaultModeldataCrafts;
	private boolean makeItemsadderCompatible;

	//Stores info to pause furnaces from running their burn event every tick.
	private final Map<Furnace, LocalDateTime> pausedFurnaces = new HashMap<>();

	//Keep track of the id's of the owners of containers.
	@Getter
	private final Map<Location, UUID> containerOwners = new HashMap<>();
	private final Set<Location> notCustomItem = new HashSet<>();

	public RecipeInjector(JavaPlugin plugin) {
		this.plugin = plugin;
		loader = RecipeLoader.getInstance();
		disableDefaultModeldataCrafts = plugin.getConfig().getBoolean("disable-default-custom-model-data-crafts");
		makeItemsadderCompatible = plugin.getConfig().getBoolean("make-itemsadder-compatible");
	}

	//Add registrations of owners of containers.
	public void registerContainerOwners(Map<Location, UUID> containerOwners) {
		//Make sure to only register containers, in case some are non existent anymore.
		containerOwners.forEach((key, value) -> {
			if (key != null && key.getWorld() != null)
				this.containerOwners.put(key, value);
		});
	}

	private boolean containsModeldata(CraftingInventory inv) {
		return Arrays.stream(inv.getMatrix()).anyMatch(x -> x != null && x.hasItemMeta() && x.getItemMeta().hasCustomModelData());
	}

	private IMatcher<ItemStack> getTypeMatcher() {
		return Adapter.canUseModeldata() && disableDefaultModeldataCrafts ?
				ItemMatchers.constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData)
				: ItemMatchers::matchType;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handleCrafting(PrepareItemCraftEvent e) {

		if (e.getRecipe() == null || e.getRecipe().getResult() == null || !plugin.getConfig().getBoolean("enable-recipes"))
			return;
		if (!(e.getInventory() instanceof CraftingInventory)) return;

		CraftingInventory inv = e.getInventory();
		Recipe serverRecipe = e.getRecipe();

		Debug.Send("The server wants to inject " + serverRecipe.getResult().toString() + " ceh will check or modify this.");

		List<RecipeGroup> possibleRecipeGroups = loader.findGroupsByResult(serverRecipe.getResult(), RecipeType.WORKBENCH);

		if (possibleRecipeGroups == null || possibleRecipeGroups.size() == 0) {
			if (disableDefaultModeldataCrafts && Adapter.canUseModeldata() && containsModeldata(inv)) {
				inv.setResult(null);
			}
			Debug.Send("no matching groups");
			return;
		}

		for (RecipeGroup group : possibleRecipeGroups) {

			//Check if any grouped enhanced recipe is a match.
			for (EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
				if (!(eRecipe instanceof WBRecipe)) return;

				WBRecipe wbRecipe = (WBRecipe) eRecipe;

				Debug.Send("Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches.");

				if (wbRecipe.matches(inv.getMatrix())
						&& e.getViewers().stream().allMatch(x -> entityCanCraft(x, wbRecipe))
						&& !CraftEnhanceAPI.fireEvent(wbRecipe, e.getViewers().size() > 0 ? (Player) e.getViewers().get(0) : null, inv, group)) {

					Debug.Send("Recipe matches, injecting " + wbRecipe.getResult().toString());
					if (makeItemsadderCompatible && containsModeldata(inv)) {
						Bukkit.getScheduler().runTask(CraftEnhance.self(), () -> {
							if (wbRecipe.matches(inv.getMatrix())) {
								inv.setResult(wbRecipe.getResult());
							}
						});
					} else inv.setResult(wbRecipe.getResult());
					return;
				}
				Debug.Send("Recipe doesn't match.");
			}

			//Check for similar server recipes if no enhanced ones match.
			for (Recipe sRecipe : group.getServerRecipes()) {
				if (sRecipe instanceof ShapedRecipe) {
					ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) sRecipe);
					if (WBRecipeComparer.shapeMatches(content, inv.getMatrix(), getTypeMatcher())) {
						inv.setResult(sRecipe.getResult());
						return;
					}
				} else if (sRecipe instanceof ShapelessRecipe) {
					ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) sRecipe);
					if (WBRecipeComparer.ingredientsMatch(ingredients, inv.getMatrix(), getTypeMatcher())) {
						inv.setResult(sRecipe.getResult());
						return;
					}
				}
			}
		}
		inv.setResult(null); //We found similar custom recipes, but none matched exactly. So set result to null.
	}

	public RecipeGroup getMatchingRecipeGroup(ItemStack source) {
		ItemStack[] srcMatrix = new ItemStack[]{source};
		FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
		return RecipeLoader.getInstance().findSimilarGroup(recipe);
	}

	public Optional<ItemStack> getFurnaceResult(RecipeGroup group, ItemStack source, Furnace furnace) {
		ItemStack[] srcMatrix = new ItemStack[]{source};
		//FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
		//RecipeGroup group = RecipeLoader.getInstance().findSimilarGroup(recipe);
		if (group == null) {
			Debug.Send("furnace recipe does not match any group, so not changing the outcome");
			return null;
		}
		UUID playerId = containerOwners.get(furnace.getLocation());
		Player p = playerId == null ? null : plugin.getServer().getPlayer(playerId);
		Debug.Send("Furnace belongs to player: " + p + " the id " + playerId);

		//Check if any grouped enhanced recipe is a match.
		for (EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
			FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;

			Debug.Send("Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");

			if (fRecipe.matches(srcMatrix)) {
				if (entityCanCraft(p, fRecipe)) {
					//TODO test if result can be changed here
					Debug.Send("found enhanced recipe result for furnace");
					return Optional.of(fRecipe.getResult());
				}
			} else {
				if (fRecipe.matcheType(srcMatrix)) {
					Debug.Send("found similar match itemtype for furnace");
					return Optional.empty();
				}
				Debug.Send("found recipe doesn't match " + (entityCanCraft(p, fRecipe) ? "." : "and no perms."));
				return null;
			}
			Debug.Send("found recipe match but " + (entityCanCraft(p, fRecipe) ? "." : "no perms."));
		}
		//Check for similar server recipes if no enhanced ones match.
		for (Recipe sRecipe : group.getServerRecipes()) {
			org.bukkit.inventory.FurnaceRecipe fRecipe = (org.bukkit.inventory.FurnaceRecipe) sRecipe;
			if (getTypeMatcher().match(fRecipe.getInput(), source)) {
				Debug.Send("found similar server recipe for furnace");
				return Optional.of(fRecipe.getResult());
			}
		}
		return Optional.empty();
	}

	@EventHandler
	public void exstract(FurnaceExtractEvent e) {

		if (!notCustomItem.isEmpty() && notCustomItem.contains(e.getBlock().getLocation())) {
			e.setExpToDrop(getExp(e.getItemType()));
			notCustomItem.remove(e.getBlock().getLocation());
		}
	}

	@EventHandler
	public void smelt(FurnaceSmeltEvent e) {
		Debug.Send("furnace smelt");
		RecipeGroup group = getMatchingRecipeGroup(e.getSource());
		Optional<ItemStack> result = getFurnaceResult(group, e.getSource(), (Furnace) e.getBlock().getState());
		if (result == null) return;
		if (result.isPresent()) {
			e.setResult(result.get());
		} else {
			ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(e.getSource().getType()));
			if (itemStack != null) {
				//Adapter.GetFurnaceRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(itemStack.getType().name().toLowerCase()), itemStack, e.getSource().getType(), 160, getExp(itemStack.getType()));
				//pausedFurnaces.put((Furnace) e.getBlock().getState(), LocalDateTime.now().plusSeconds(10L));
				e.setResult(itemStack);
				for (EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
					FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
					if (fRecipe.matcheType(new ItemStack[]{e.getSource()})) {
						notCustomItem.add(e.getBlock().getLocation());
						break;
					}
				}
			} else
				e.setCancelled(true);
		}


	}

	@EventHandler
	public void burn(FurnaceBurnEvent e) {
		Debug.Send("furnace burn");
		if (e.isCancelled()) return;
		Furnace f = (Furnace) e.getBlock().getState();
		//Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
		//instead of only clicks to unpause.
		if (pausedFurnaces.getOrDefault(f, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
			e.setCancelled(true);
			return;
		}
		RecipeGroup recipe = getMatchingRecipeGroup(f.getInventory().getSmelting());
		Optional<ItemStack> result = getFurnaceResult(recipe, f.getInventory().getSmelting(), (Furnace) e.getBlock().getState());
		if (result != null && !result.isPresent()) {
			if (f.getInventory().getSmelting() != null && RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(f.getInventory().getSmelting().getType())) != null)
				return;
			e.setCancelled(true);
			pausedFurnaces.put(f, LocalDateTime.now().plusSeconds(10L));
		}
	}

	@EventHandler
	public void furnaceClick(InventoryClickEvent e) {
		if (e.isCancelled()) return;
		if (e.getView().getTopInventory() instanceof FurnaceInventory) {
			Furnace f = (Furnace) e.getView().getTopInventory().getHolder();

			pausedFurnaces.remove(f);
		}
	}

	@EventHandler
	public void furnacePlace(BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		if (e.getBlock().getType().equals(Material.FURNACE)) {
			containerOwners.put(e.getBlock().getLocation(), e.getPlayer().getUniqueId());
		}
	}

	@EventHandler
	public void furnaceBreak(BlockBreakEvent e) {
		if (e.isCancelled()) return;
		if (e.getBlock().getType().equals(Material.FURNACE)) {
			containerOwners.remove(e.getBlock().getLocation());
			pausedFurnaces.remove((Furnace) e.getBlock().getState());
		}
	}

	private boolean entityCanCraft(Permissible entity, EnhancedRecipe group) {
		return group.getPermissions() == null || group.getPermissions().equals("")
				|| (entity != null && entity.hasPermission(group.getPermissions()));
	}
}
