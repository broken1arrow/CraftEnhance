package com.dutchjelly.craftenhance.crafthandling;


import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
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
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.dutchjelly.craftenhance.util.FurnaceDefultValues.getExp;

public class RecipeInjector implements Listener {

    private final JavaPlugin plugin;
    private final RecipeLoader loader;
    private final boolean disableDefaultModeldataCrafts;
    private final boolean makeItemsadderCompatible;

    //Stores info to pause furnaces from running their burn event every tick.
    private final Map<Furnace, LocalDateTime> pausedFurnaces = new HashMap<>();

    //Keep track of the id's of the owners of containers.
    @Getter
    private final Map<Location, UUID> containerOwners = new HashMap<>();
    private final Set<Location> notCustomItem = new HashSet<>();

    public RecipeInjector(final JavaPlugin plugin) {
        this.plugin = plugin;
        loader = RecipeLoader.getInstance();
        disableDefaultModeldataCrafts = plugin.getConfig().getBoolean("disable-default-custom-model-data-crafts");
        makeItemsadderCompatible = plugin.getConfig().getBoolean("make-itemsadder-compatible");
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleCrafting(final PrepareItemCraftEvent e) {
        if (e.getRecipe() == null || e.getRecipe().getResult() == null || !plugin.getConfig().getBoolean("enable-recipes"))
            return;
        if (!(e.getInventory() instanceof CraftingInventory)) return;

        final CraftingInventory inv = e.getInventory();
        final Recipe serverRecipe = e.getRecipe();
        Debug.Send("The server wants to inject " + serverRecipe.getResult().toString() + " ceh will check or modify this.");

        final List<RecipeGroup> possibleRecipeGroups = loader.findGroupsByResult(serverRecipe.getResult(), RecipeType.WORKBENCH);
        final List<Recipe> disabledServerRecipes = RecipeLoader.getInstance().getDisabledServerRecipes();
        if (disabledServerRecipes != null && !disabledServerRecipes.isEmpty())
            for (final Recipe disabledRecipe : disabledServerRecipes) {
                if (disabledRecipe.getResult().isSimilar(serverRecipe.getResult())) {
                    inv.setResult(null);
                    return;
                }
            }
        if (possibleRecipeGroups == null || possibleRecipeGroups.size() == 0) {
            if (disableDefaultModeldataCrafts && Adapter.canUseModeldata() && containsModeldata(inv)) {
                inv.setResult(null);
            }
            Debug.Send("no matching groups");
            return;
        }
        for (final RecipeGroup group : possibleRecipeGroups) {
            System.out.println("handleCrafting group  " + group);
            //Check if any grouped enhanced recipe is a match.
            for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
                if (!(eRecipe instanceof WBRecipe)) return;
                final WBRecipe wbRecipe = (WBRecipe) eRecipe;
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
                    } else {
                        inv.setResult(wbRecipe.getResult());
                    }
                    return;
                }
                Debug.Send("Recipe doesn't match.");
            }

            //Check for similar server recipes if no enhanced ones match.
            for (final Recipe sRecipe : group.getServerRecipes()) {
                if (sRecipe instanceof ShapedRecipe) {
                    final ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe) sRecipe);
                    if (WBRecipeComparer.shapeMatches(content, inv.getMatrix(), getTypeMatcher())) {
                        inv.setResult(sRecipe.getResult());
                        return;
                    }
                } else if (sRecipe instanceof ShapelessRecipe) {
                    final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) sRecipe);
                    if (WBRecipeComparer.ingredientsMatch(ingredients, inv.getMatrix(), getTypeMatcher())) {
                        inv.setResult(sRecipe.getResult());
                        return;
                    }
                }
            }
        }
        inv.setResult(null); //We found similar custom recipes, but none matched exactly. So set result to null.
    }

    public RecipeGroup getMatchingRecipeGroup(final ItemStack source) {
        final ItemStack[] srcMatrix = new ItemStack[]{source};
        final FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
        return RecipeLoader.getInstance().findSimilarGroup(recipe);
    }

    public Optional<ItemStack> getFurnaceResult(final RecipeGroup group, final ItemStack source, final Furnace furnace) {
        final ItemStack[] srcMatrix = new ItemStack[]{source};
        //FurnaceRecipe recipe = new FurnaceRecipe(null, null, srcMatrix);
        //RecipeGroup group = RecipeLoader.getInstance().findSimilarGroup(recipe);
        if (group == null) {
            Debug.Send("furnace recipe does not match any group, so not changing the outcome");
            return null;
        }
        final UUID playerId = containerOwners.get(furnace.getLocation());
        final Player p = playerId == null ? null : plugin.getServer().getPlayer(playerId);
        Debug.Send("Furnace belongs to player: " + p + " the id " + playerId);
        Debug.Send("Furnace source item: " + source);

        //Check if any grouped enhanced recipe is a match.
        for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
            final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;

            Debug.Send("Checking if enhanced recipe for " + fRecipe.getResult().toString() + " matches.");

            if (fRecipe.matches(srcMatrix)) {
                if (entityCanCraft(p, fRecipe)) {
                    //TODO test if result can be changed here
                    Debug.Send("Found enhanced recipe " + fRecipe.getResult().toString() + " for furnace");
                    Debug.Send("Matching ingridens are " + source + " .");
                    return Optional.of(fRecipe.getResult());
                } else {
                    Debug.Send("found this recipe " + fRecipe.getResult().toString() + " match but, player has not this permission " + fRecipe.getPermissions());
                    break;
                }
            } else {
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
        //Check for similar server recipes if no enhanced ones match.
        for (final Recipe sRecipe : group.getServerRecipes()) {
            final org.bukkit.inventory.FurnaceRecipe fRecipe = (org.bukkit.inventory.FurnaceRecipe) sRecipe;
            if (getTypeMatcher().match(fRecipe.getInput(), source)) {
                Debug.Send("found similar server recipe for furnace");
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @EventHandler
    public void exstract(final FurnaceExtractEvent e) {

        if (!notCustomItem.isEmpty() && notCustomItem.contains(e.getBlock().getLocation())) {
            e.setExpToDrop(getExp(e.getItemType()));
            notCustomItem.remove(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void smelt(final FurnaceSmeltEvent e) {
        Debug.Send("furnace smelt");
        final RecipeGroup group = getMatchingRecipeGroup(e.getSource());
        final Optional<ItemStack> result = getFurnaceResult(group, e.getSource(), (Furnace) e.getBlock().getState());
        if (result == null) return;

        if (result.isPresent()) {
            e.setResult(result.get());
        } else {
            final ItemStack itemStack = RecipeLoader.getInstance().getSimilarVanillaRecipe().get(new ItemStack(e.getSource().getType()));
            if (itemStack != null) {
                //Adapter.GetFurnaceRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(itemStack.getType().name().toLowerCase()), itemStack, e.getSource().getType(), 160, getExp(itemStack.getType()));
                //pausedFurnaces.put((Furnace) e.getBlock().getState(), LocalDateTime.now().plusSeconds(10L));
                e.setResult(itemStack);
                for (final EnhancedRecipe eRecipe : group.getEnhancedRecipes()) {
                    final FurnaceRecipe fRecipe = (FurnaceRecipe) eRecipe;
                    if (fRecipe.matcheType(new ItemStack[]{e.getSource()})) {
                        notCustomItem.add(e.getBlock().getLocation());
                        break;
                    }
                }
            } else
                e.setCancelled(true);
        }


    }

    @EventHandler (ignoreCancelled = false)
    public void burn(final FurnaceBurnEvent e) {
        Debug.Send("furnace burn");
        if (e.isCancelled()) return;
        final Furnace f = (Furnace) e.getBlock().getState();
        //Reduce computing time by pausing furnaces. This can be removed if we also check for hoppers
        //instead of only clicks to unpause.
        if (pausedFurnaces.getOrDefault(f, LocalDateTime.now()).isAfter(LocalDateTime.now())) {
            e.setCancelled(true);
            return;
        }
        final RecipeGroup recipe = getMatchingRecipeGroup(f.getInventory().getSmelting());
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
        if (e.getBlock().getType().equals(Material.FURNACE)) {
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

    private boolean entityCanCraft(final Permissible entity, final EnhancedRecipe group) {
        return group.getPermissions() == null || group.getPermissions().equals("")
                || (entity != null && entity.hasPermission(group.getPermissions()));
    }
}
