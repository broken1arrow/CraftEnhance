package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.files.FileManager;
import com.dutchjelly.craftenhance.gui.interfaces.GuiPlacable;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.util.StringUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class EnhancedRecipe extends GuiPlacable implements ConfigurationSerializable, ServerLoadable {


	protected EnhancedRecipe(EnhancedRecipe enhancedRecipe) {
		this("", null, null);
		this.key = enhancedRecipe.getKey();
		this.onCraftCommand = enhancedRecipe.getOnCraftCommand();
		this.allowedWorlds = enhancedRecipe.getAllowedWorlds();
		this.checkPartialMatch = enhancedRecipe.isCheckPartialMatch();
		this.matchType = enhancedRecipe.getMatchType();
		this.deserialize = enhancedRecipe.getDeserialize();
		this.id = enhancedRecipe.getId();
		this.hidden = enhancedRecipe.isHidden();
		this.serialize = enhancedRecipe.getSerialize();
		this.remove = enhancedRecipe.isRemove();
	}

	public EnhancedRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
		this.permission = perm;
		this.result = result;
		this.content = content;
	}

	protected EnhancedRecipe(final Map<String, Object> args) {
		super(args);
		final FileManager fm = CraftEnhance.self().getFm();

		final List<String> recipeKeys;
		result = fm.getItem((String) args.get("result"));
		permission = (String) args.get("permission");
		if (args.containsKey("matchtype")) {
			matchType = ItemMatchers.MatchType.valueOf((String) args.get("matchtype"));
		} else if (args.containsKey("matchmeta")) {
			matchType = (Boolean) args.get("matchmeta") ?
					ItemMatchers.MatchType.MATCH_META :
					ItemMatchers.MatchType.MATCH_TYPE;
		}

		if (args.containsKey("oncraftcommand")) {
			final Object oncraftcommand = args.get("oncraftcommand");
			onCraftCommand = oncraftcommand instanceof Boolean ? "" : (String) oncraftcommand;
		}

		if (args.containsKey("hidden"))
			hidden = (Boolean) args.get("hidden");

		if (args.containsKey("check_partial_match")) {
			Object partialMatch = args.get("check_partial_match");
			if (partialMatch instanceof Boolean)
				this.checkPartialMatch = (boolean) args.get("check_partial_match");
			else
				this.checkPartialMatch = Boolean.parseBoolean(args.get("check_partial_match") + "");
		}

		recipeKeys = (List<String>) args.get("recipe");
		final List<String> worldsList = (List<String>) args.getOrDefault("allowed_worlds", null);
		final Set<String> worlds = new HashSet<>();
		if (worldsList != null && !worldsList.isEmpty())
			worldsList.forEach(world -> {
				if (world != null) {
					final World bukkitWorld = Bukkit.getWorld(world);
					if (bukkitWorld == null)
						Messenger.Error("This world seams to not exist or not loaded yet: '" + world + "'. Will still be added and will attempt to load in later stage.");
					worlds.add(world);
				}
			});
		this.allowedWorlds = worlds;
		if (recipeKeys != null) {
			setContent(new ItemStack[recipeKeys.size()]);
			for (int i = 0; i < content.length; i++) {
				content[i] = fm.getItem(recipeKeys.get(i));
			}
		}
		this.deserialize = args;
	}

	@Getter
	@Setter
	private int id;

	@Getter
	@Setter
	private String key;

	@Getter
	@Setter
	private ItemStack result;

	@Getter
	@Setter
	private ItemStack[] content;

	@Getter
	@Setter
	private ItemMatchers.MatchType matchType = ItemMatchers.MatchType.MATCH_META;

	@Getter
	@Setter
	private String permission;

	@Getter
	@Setter
	private boolean hidden;

	@Getter
	@Setter
	private String onCraftCommand;

	@Getter
	private final RecipeType type = RecipeType.WORKBENCH;

	@Getter
	@Setter
	private Set<String> allowedWorlds;
	@Getter
	private Map<String, Object> deserialize;
	@Getter
	private Map<String, Object> serialize;
	@Getter
	@Setter
	private boolean checkPartialMatch;
	@Getter
	@Setter
	private boolean remove;
	public EnhancedRecipe copy() {
		switch (this.getType()) {
			case FURNACE:
				return new FurnaceRecipe(this);
			case BLAST:
				return new BlastRecipe(this);
			case SMOKER:
				return new SmokerRecipe(this);
			case BREWING:
				return new BrewingRecipe(this);
			default:
				return new WBRecipe(this);
		}
	}

	public ItemStack getResult() {
		if (result == null)
			return null;
		return result.clone();
	}

	@Nonnull
	@Override
	public Map<String, Object> serialize() {
		final FileManager fm = CraftEnhance.getPlugin(CraftEnhance.class).getFm();
		return new LinkedHashMap<String, Object>() {{
			putAll(EnhancedRecipe.super.serialize());
			put("permission", permission);
			put("matchtype", matchType.name());
			put("hidden", hidden);
			put("check_partial_match", checkPartialMatch);
			put("oncraftcommand", onCraftCommand);
			put("result", fm.getItemKey(result));
			put("recipe", Arrays.stream(content).map(fm::getItemKey).toArray(String[]::new));
			put("allowed_worlds", allowedWorlds != null ? new ArrayList<>(allowedWorlds) : new ArrayList<>());
			if (serialize != null && !serialize.isEmpty())
				putAll(serialize);
		}};
	}

	public String validate() {

		if (result == null)
			return "recipe cannot have null result";
		if (!Adapter.canUseModeldata() && matchType == ItemMatchers.MatchType.MATCH_MODELDATA_AND_TYPE)
			return "recipe is using modeldata match while the server doesn't support it";
		if (content.length == 0 || !Arrays.stream(content).anyMatch(x -> x != null))
			return "recipe content cannot be empty";
		return null;
	}

	@Override
	public String toString() {
		return "EnhancedRecipe{" +
				"recipe_name='" + key + '\'' +
				", result=" + result +
				", content=" + Arrays.toString(content) +
				", matchType=" + matchType +
				", permission='" + permission + '\'' +
				", hidden=" + hidden +
				", on_craft_command='" + onCraftCommand + '\'' +
				", type=" + this.getType() +
				", allowedWorlds=" + allowedWorlds +
				", checkPartialMatch=" + checkPartialMatch +
				"} ";
	}

	public void setPermission(final String permission) {
		this.permission = (permission == null || permission.isEmpty() || permission.equals("null") || permission.equals("non") ? null : permission);
	}

	/*	@Override
	public String toString() {
		return "EnhancedRecipe{" +
				"key='" + key + '\'' +
				", result=" + (this.result == null ? "null" : result) +
				"contents"+ Arrays.toString(this.getContent()) +
				'}';
	}*/
/*
    @Override
    public String toString(){
        String s = "";
        s += "key = " + key + "\n";
        s += "result = " + (this.result == null ? "null" : result) + "\n";
        return s;
    }*/

	@NonNull
	public Set<String> getAllowedWorlds() {
		if(allowedWorlds == null)
			return new HashSet<>();
		return allowedWorlds;
	}

	public String getAllowedWorldsFormatted() {
		final StringBuilder stringBuilder = new StringBuilder();
		if (allowedWorlds != null)
			for (final String worlds : allowedWorlds)
				stringBuilder.append(StringUtil.capitalizeFully(worlds.toLowerCase())).append(", ");
		stringBuilder.setLength(stringBuilder.length() - 2);
		return stringBuilder.toString();
	}

	@Override
	public ItemStack getDisplayItem() {
		return getResult();
	}

	public void save() {
		if (validate() == null) {
			CraftEnhance.self().getCacheRecipes().save(this);
		}
	}

	public void remove() {
		CraftEnhance.self().getCacheRecipes().remove(this);
	}

	public void load() {
		RecipeLoader.getInstance().loadRecipe(this);
	}

	public RecipeType getType() {
		return type;
	}


	public abstract boolean matches(ItemStack[] content);

	public abstract boolean matches(ItemStack[] content, IMatcher<ItemStack> matcher);

	public abstract boolean matchesBlockType(final Material blockSmelting);
}