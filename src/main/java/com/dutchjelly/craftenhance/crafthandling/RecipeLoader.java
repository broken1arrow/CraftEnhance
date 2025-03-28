package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.CategoryDataCache;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class RecipeLoader {

	//Ensure one instance
	private static RecipeLoader instance = null;

	public static RecipeLoader getInstance() {
		return instance == null ? instance = new RecipeLoader(Bukkit.getServer(),self().getCategoryDataCache()) : instance;
	}

	public static void clearInstance() {
		for (final EnhancedRecipe loaded : RecipeLoader.getInstance().getLoadedRecipes()){
			instance.unloadRecipe( loaded.getServerRecipe());
		}
		instance = null;
	}

	@Getter
	private List<Recipe> serverRecipes = new ArrayList<>();
	@Getter
	private List<Recipe> disabledServerRecipes = new ArrayList<>();
	@Getter
	private List<EnhancedRecipe> loadedRecipes = new ArrayList<>();
	@Getter
	private  Map<ItemStack,ItemStack> similarVanillaRecipe = new HashMap<>();
	private Map<String, Recipe> loaded = new HashMap<>();
	@Getter
	private Map<RecipeType, List<RecipeGroup>> mappedGroupedRecipes = new HashMap<>();

	private final CategoryDataCache categoryDataCache;
	private final Server server;

	private RecipeLoader(final Server server, final CategoryDataCache categoryDataCache) {
		this.server = server;
		try {
			server.recipeIterator().forEachRemaining(serverRecipes::add);
		} catch (IllegalArgumentException e) {
			self().getLogger().log(Level.SEVERE, "This server recipe contains air, will not be loaded.",e);
		}

/*		for (final Iterator<Recipe> it = server.recipeIterator(); it.hasNext(); ) {
			final Recipe data = it.next();

		}*/
		for (final RecipeType type : RecipeType.values()) {
			mappedGroupedRecipes.put(type, new ArrayList<>());
		}
		this.categoryDataCache = categoryDataCache;
	}

	//Adds or merges group with existing group.
	private RecipeGroup addGroup(final List<Recipe> serverRecipes, final EnhancedRecipe enhancedRecipe) {
		Debug.Send("[AddGroup] is now add recipe group.");
		List<RecipeGroup> groupedRecipes = mappedGroupedRecipes.get(enhancedRecipe.getType());
		//            Debug.Send("Looking if two enhanced recipes are similar for merge.");
		if (groupedRecipes == null)
			groupedRecipes = new ArrayList<>();

		for (final RecipeGroup group : groupedRecipes) {
			group.addAllNotExist(serverRecipes);
			return group.addIfNotExist(enhancedRecipe);
		}
		final RecipeGroup newGroup = new RecipeGroup();
		if (groupedRecipes.isEmpty()){
			newGroup.addIfNotExist(enhancedRecipe);
			newGroup.setServerRecipes(serverRecipes);
			groupedRecipes.add(newGroup);
		}
		return newGroup;
	}

	public RecipeGroup findGroup(final EnhancedRecipe recipe) {
		return mappedGroupedRecipes.get(recipe.getType()).stream().filter(x -> x.getEnhancedRecipes().contains(recipe)).findFirst().orElse(null);
	}

	//Find groups that contain at least one recipe that maps to result.
	public List<RecipeGroup> findGroupsByResult(final ItemStack result, final RecipeType type) {
		final List<RecipeGroup> originGroups = new ArrayList<>();
		for (final RecipeGroup group : mappedGroupedRecipes.get(type)) {
			if (group.getEnhancedRecipes().stream().anyMatch(x -> result.equals(x.getResult())))
				originGroups.add(group);
			else if (group.getServerRecipes().stream().anyMatch(x -> result.equals(x.getResult())))
				originGroups.add(group);
		}
		return originGroups;
	}

	public RecipeGroup findMatchingGroup(final ItemStack[] matrix, final RecipeType type) {
		for (final RecipeGroup group : mappedGroupedRecipes.get(type)) {
			if (group.getEnhancedRecipes().stream().anyMatch(x -> x.matches(matrix)))
				return group;
		}
		return null;
	}

	public RecipeGroup findSimilarGroup(final EnhancedRecipe recipe) {
		return mappedGroupedRecipes.get(recipe.getType()).stream().filter(x ->
				x.getEnhancedRecipes().stream().anyMatch(y -> y.isSimilar(recipe)) ||
						x.getServerRecipes().stream().anyMatch(recipe::isSimilar)
		).findFirst().orElse(null);
	}

	//There can only be one group that matches a matrix, because that's how they're grouped
//    public List<RecipeGroup> findMatchingGroups(ItemStack[] matrix, RecipeType type){
//        List<RecipeGroup> matchingGroups = new ArrayList<>();
//        for(RecipeGroup group : mappedGroupedRecipes.get(type)){
//            try{
//                if(group.getEnhancedRecipes().stream().anyMatch(x -> x.matches(matrix)))
//                    matchingGroups.add(group);
//            } catch(ClassCastException e){}
//        }
//        return matchingGroups;
//    }

	public boolean isLoadedAsServerRecipe(final EnhancedRecipe recipe) {
		return loaded.containsKey(recipe.getKey());
	}

	private void unloadCehRecipes() {
		final Iterator<Recipe> it = server.recipeIterator();
		while (it.hasNext()) {
			final Recipe r = it.next();
			if (Adapter.ContainsSubKey(r, ServerRecipeTranslator.KeyPrefix)) {
				it.remove();
			}
		}
	}

	private void unloadRecipe(final Recipe r) {
		final Iterator<Recipe> it = server.recipeIterator();
		while (it.hasNext()) {
			final Recipe currentRecipe = it.next();
			if (currentRecipe.equals(r)) {
				it.remove();
				return;
			}
		}
	}

	public void unloadAll() {
		disabledServerRecipes.forEach(x ->
				enableServerRecipe(x)
		);
		mappedGroupedRecipes.clear();
		for (final RecipeType type : RecipeType.values()) {
			mappedGroupedRecipes.put(type, new ArrayList<>());
		}
		serverRecipes.clear();
		loaded.clear();
		unloadCehRecipes();
	}

	public void unloadRecipe(final EnhancedRecipe recipe) {
		final RecipeGroup group = findGroup(recipe);
		loadedRecipes.remove(recipe);

		final CategoryData categoryData = categoryDataCache.get(recipe.getRecipeCategory());
		if (categoryData != null && categoryData.getEnhancedRecipes() != null)
			categoryData.getEnhancedRecipes().remove(recipe);

		final Recipe serverRecipe = loaded.get(recipe.getKey());

		//Only unload from server if there are no similar server recipes.
		if (serverRecipe != null) {
			loaded.remove(recipe.getKey());
			unloadRecipe(serverRecipe);
		}

		if (group == null) {
			printGroupsDebugInfo();
			Bukkit.getLogger().log(Level.SEVERE, "Could not unload recipe from group, because the recipe doesn't have a group.");
			return;
		}

        /* TODO (Optimization) When e.g. a shapeless recipe disappears from a recipe, not all recipes in that group are
            similar anymore. So detect this and make sure they get split up. */

		//Remove entire recipe group if it's the last enhanced recipe, or remove a single recipe from the group.
		if (group.getEnhancedRecipes().size() == 1)
			mappedGroupedRecipes.get(recipe.getType()).remove(group);
		else group.getEnhancedRecipes().remove(recipe);
		Debug.Send("Unloaded a recipe");
		printGroupsDebugInfo();
	}
	public void loadRecipe(@NonNull final EnhancedRecipe recipe) {
		loadRecipe(recipe,false);
	}
	public void loadRecipe(@NonNull final EnhancedRecipe recipe, final boolean isReloading) {
		if (recipe.validate() != null) {
			Messenger.Error("There's an issue with recipe " + recipe.getKey() + ": " + recipe.validate());
			return;
		}

		final boolean containsRecipe = loaded.containsKey(recipe.getKey());
		if (containsRecipe)
			unloadRecipe(recipe);

		final List<Recipe> similarServerRecipes = new ArrayList<>();
		for (final Recipe r : serverRecipes) {
			if (recipe.isSimilar(r)) {
				similarServerRecipes.add(r);
			}
		}

		Recipe alwaysSimilar = null;
		for (final Recipe r : similarServerRecipes) {
			if (recipe.isAlwaysSimilar(r)) {
				if (self().getVersionChecker().newerThan(ServerVersion.v1_12)) {
					if (r instanceof ShapedRecipe)
						if (((ShapedRecipe) r).getKey().getNamespace().contains("craftenhance"))
							continue;
					if (r instanceof ShapelessRecipe)
						if (((ShapelessRecipe) r).getKey().getNamespace().contains("craftenhance"))
							continue;
				}
				alwaysSimilar = r;
				break;
			}
		}
		//cache orginal recipe if user make furnace recipe and give right item as output.
		cacheSimilarVanilliaRecipe( recipe);
		//Only load the recipe if there is not a server recipe that's always similar.
		if (alwaysSimilar == null) {
			final Recipe serverRecipe = recipe.getServerRecipe();
			if (serverRecipe == null) {
				Debug.Send("Added server recipe is null for " + recipe.getKey());
				self().getLogger().log(Level.WARNING, "Recipe " + recipe.getKey() + " will not be cached becuse the result is null or invalid material type.");
				return;
			}
		if (!containsRecipe && !isReloading)
				server.addRecipe(serverRecipe);
			Debug.Send("Added server recipe for " + serverRecipe.getResult());

			loaded.put(recipe.getKey(), serverRecipe);
		} else {
			Debug.Send("Didn't add server recipe for " + recipe.getKey() + " because a similar one was already loaded: " + alwaysSimilar.toString() + " with the result " + alwaysSimilar.getResult().toString());
		}
		loadToCache(recipe);
		addGroup(similarServerRecipes, recipe);
		Debug.Send("AddGroupe done.");
		loadedRecipes.add(recipe);
	}

	private void loadToCache(@NonNull final EnhancedRecipe recipe) {
		String category = recipe.getRecipeCategory();
		if (recipe instanceof FurnaceRecipe)
			category = category == null || category.isEmpty() ? "furnace" : category;
		else
			category = category == null || category.isEmpty() ? "default" : category;
		if (recipe.getRecipeCategory() == null)
			recipe.setRecipeCategory(category);
		CategoryData recipeCategory = this.categoryDataCache.get(category);

		final List<EnhancedRecipe> enhancedRecipeList;
		if (recipeCategory != null){
			enhancedRecipeList = recipeCategory.getEnhancedRecipes();
			if (enhancedRecipeList.stream().noneMatch(cachedRecipe -> cachedRecipe.getKey().equals(recipe.getKey())))
			//if (!enhancedRecipeList.contains(recipe))
				enhancedRecipeList.add(recipe);
		} else {
			final ItemStack itemStack;
			if (recipe instanceof FurnaceRecipe)
				itemStack = new ItemStack(Adapter.getMaterial("FURNACE"));
			else
				itemStack = new ItemStack(Adapter.getMaterial("CRAFTING_TABLE") );
			recipeCategory = this.categoryDataCache.of(category,itemStack,null);
			recipeCategory.addEnhancedRecipes(recipe);
		}
		this.categoryDataCache.put(category, recipeCategory);
	}
	public void cacheSimilarVanilliaRecipe(final EnhancedRecipe recipe) {
		if (!(recipe instanceof FurnaceRecipe)) return;
		Debug.Send("Start to add Furnace recipe");
		for (final Recipe r : serverRecipes) {
			if (!(r instanceof org.bukkit.inventory.FurnaceRecipe)) continue;
			if (recipe.getContent().length <= 0 || recipe.getContent()[0] == null) continue;

			final org.bukkit.inventory.FurnaceRecipe serverRecipe = (org.bukkit.inventory.FurnaceRecipe) r;
			final ItemStack itemStack = serverRecipe.getInput();
			Debug.Send("Added Furnace recipe for " + serverRecipe.getResult());

			if (recipe.getContent()[0].getType() == itemStack.getType()) {
				this.similarVanillaRecipe.put( serverRecipe.getInput() ,serverRecipe.getResult());
				//Adapter.GetFurnaceRecipe(CraftEnhance.self(), ServerRecipeTranslator.GetFreeKey(itemStack.getType().name().toLowerCase()), itemStack, serverRecipe.getResult().getType(), serverRecipe.getCookingTime(), getExp(itemStack.getType()));
			}
		}
	}

	public List<Recipe> getLoadedServerRecipes() {
		return new ArrayList<>(loaded.values());
	}

	public void printGroupsDebugInfo() {
		if (!Debug.isGeneralDebugEnable()) return;

		for (final Map.Entry<RecipeType, List<RecipeGroup>> recipeGrouping : mappedGroupedRecipes.entrySet()) {
			Debug.Send("Groups for recipes of type: " + recipeGrouping.getKey().toString());
			for (final RecipeGroup group : recipeGrouping.getValue()) {
				Debug.Send("<group>");
				Debug.Send("Enhanced recipes: " + group.getEnhancedRecipes().stream().filter(Objects::nonNull).map(x -> x.getResult().toString()).collect(Collectors.joining("\nEnhanced recipes: ")));
				Debug.Send("Server recipes: " + group.getServerRecipes().stream().filter(Objects::nonNull).map(x -> x.getResult().toString()).collect(Collectors.joining("\nEnhanced recipes: ")));
			}
		}
	}

	public boolean disableServerRecipe(final Recipe r) {
		if (serverRecipes.contains(r)) {
			Debug.Send("[Recipe Loader] disabling server recipe for " + r.getResult().getType().name());

			serverRecipes.remove(r);
			disabledServerRecipes.add(r);
			unloadRecipe(r);

			final RecipeType type = RecipeType.getType(r);
			if (type != null) {
				for (final RecipeGroup recipeGroup : mappedGroupedRecipes.get(type)) {
					if (recipeGroup.getServerRecipes().contains(r)) {
						recipeGroup.getServerRecipes().remove(r);
						final EnhancedRecipe alwaysSimilar = recipeGroup.getEnhancedRecipes().stream().filter(x -> x.isAlwaysSimilar(r)).findFirst().orElse(null);
						//If there's a recipe that's always similar, we have to load it again.
						if (alwaysSimilar != null) {
							loaded.put(alwaysSimilar.getKey(), alwaysSimilar.getServerRecipe());
							server.addRecipe(alwaysSimilar.getServerRecipe());
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	public boolean enableServerRecipe(final Recipe r) {
		if (!serverRecipes.contains(r)) {
			Debug.Send("[Recipe Loader] enabling server recipe for " + r.getResult().getType().name());
			serverRecipes.add(r);
			disabledServerRecipes.remove(r);
			if (server.getRecipe(r.getResult().getType().getKey()) == null)
				server.addRecipe(r);

			final RecipeType type = RecipeType.getType(r);
			if (type != null) {
				for (final RecipeGroup recipeGroup : mappedGroupedRecipes.get(type)) {
					if (recipeGroup.getEnhancedRecipes().stream().anyMatch(x -> x.isSimilar(r))) {
						recipeGroup.getServerRecipes().add(r);
						final EnhancedRecipe alwaysSimilar = recipeGroup.getEnhancedRecipes().stream().filter(x -> x.isAlwaysSimilar(r)).findFirst().orElse(null);

						//If there's a recipe that's always similar, we have to unload it again.
						if (alwaysSimilar != null)
							loaded.remove(alwaysSimilar.getKey());
					}
				}
			}
			return true;
		}
		return false;
	}
	public EnhancedRecipe getLoadedRecipes(final Predicate<? super EnhancedRecipe>  predicate){
		return this.getLoadedRecipes().stream().filter(predicate).findFirst().orElse(null);
	}
	public void disableServerRecipes(final List<Recipe> disabledServerRecipes) {
		//No need to be efficient here, this'll only run once.
		disabledServerRecipes.forEach(x -> disableServerRecipe(x));
	}

	public void clearCache() {
		this.serverRecipes = new ArrayList<>();
		this.disabledServerRecipes = new ArrayList<>();
		this.similarVanillaRecipe = new HashMap<>();
		this.loaded = new HashMap<>();
		this.loadedRecipes = new ArrayList<>();
		this.mappedGroupedRecipes = new HashMap<>();
	}
}
