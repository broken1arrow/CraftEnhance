package com.dutchjelly.craftenhance.database;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.BlastRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.furnace.SmokerRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.messaging.Debug;
import lombok.NonNull;
import org.broken.arrow.nbt.library.RegisterNbtAPI;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.messaging.Debug.Send;

public class RecipeDatabase implements RecipeSQLQueries {
	private static final String URL = "jdbc:sqlite:" + self().getDataFolder() + "/recipes.db";

	public RecipeDatabase() {
		File checkFile = self().getDataFolder();
		if(!checkFile.exists())
			checkFile.mkdirs();
		try (Connection connection = connect()) {
			createTables(connection);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}


	public void saveRecipes() {
		save();
	}

	public void saveRecipe(@NonNull final EnhancedRecipe recipe) {
		Send("Saving recipe '" + recipe.getKey() + "' with data: " + recipe.toString());
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			try {
				saveRecipe(connection, recipe);
			} finally {
				try {
					connection.commit();
					connection.setAutoCommit(true);
				} catch (SQLException e) {
					Debug.error("Could not not reset back the auto commit.", e);
				}
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	@NonNull
	public List<EnhancedRecipe> loadRecipes() {
		try (Connection connection = connect()) {
			return this.getAllRecipe(connection);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
		return new ArrayList<>();
	}

	@Nullable
	public EnhancedRecipe loadRecipe(@NonNull final String recipeId) {
		try (Connection connection = connect()) {
			return this.getRecipe(connection, recipeId);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
		return null;
	}

	public void deleteRecipe(@NonNull final EnhancedRecipe enhancedRecipe) {
		Send("Removing recipe '" + enhancedRecipe.getKey() + "' with data: " + enhancedRecipe.toString());

		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			deleteRecipe(connection, enhancedRecipe);
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				Debug.error("Could not not reset back the auto commit.", e);
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	public void removeAllowedWorlds(@NonNull final EnhancedRecipe enhancedRecipe) {
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			this.removeAllowedWorld(connection, enhancedRecipe.getKey(), enhancedRecipe.getAllowedWorlds().toArray(new String[0]));
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				Debug.error("Could not not reset back the auto commit.", e);
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	private void removeAllowedWorld(@NonNull final EnhancedRecipe enhancedRecipe, @NonNull final String world) {
		try (Connection connection = connect()) {
			removeAllowedWorld(connection, enhancedRecipe.getKey(), world);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	public void deleteAllIngredients(@NonNull final EnhancedRecipe enhancedRecipe) {
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			this.deleteAllIngredients(connection, enhancedRecipe);
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				Debug.error("Could not not reset back the auto commit.", e);
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	public void removeFurnaceData(@NonNull final EnhancedRecipe enhancedRecipe) {
		try (Connection connection = connect()) {
			this.removeFurnaceData(connection, enhancedRecipe);
			try {
				connection.commit();
			} catch (SQLException e) {
				Debug.error("Could not not commit changes.", e);
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	public void deleteIngredient(@NonNull final EnhancedRecipe enhancedRecipe, @NonNull final ItemStack itemStack) {
		boolean isResult = enhancedRecipe.getResult().isSimilar(itemStack);
		int slot = enhancedRecipe.getResultSlot();

		if (!isResult) {
			slot = getSlotIngredient(enhancedRecipe, itemStack, slot);
		}
		if (slot < 0) return;

		try (Connection connection = connect()) {
			this.deleteIngredient(connection, enhancedRecipe.getKey(), slot);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}


	private Connection connect() throws SQLException {
		return DriverManager.getConnection(URL);
	}

	private int updateSQL(final PreparedStatement updateItemStmt) throws SQLException {
		return updateItemStmt.executeUpdate();
	}

	// Create tables
	public void createTables(Connection conn) {
		String createRecipesTable = " CREATE TABLE IF NOT EXISTS recipes ( "
				+ "id TEXT PRIMARY KEY, "
				+ "recipe_type TEXT NOT NULL, "
				+ "page INTEGER NOT NULL, "
				+ "slot INTEGER NOT NULL, "
				+ "result_slot INTEGER NOT NULL, "
				+ "category TEXT NOT NULL, "
				+ "permission TEXT, "
				+ "matchtype TEXT NOT NULL, "
				+ "hidden BOOLEAN NOT NULL,"
				+ "check_partial_match BOOLEAN NOT NULL, "
				+ "on_craft_command TEXT, "
				+ "result_item_type TEXT NOT NULL, "
				+ "shapeless BOOLEAN NOT NULL, "
				+ "FOREIGN KEY (result_item_type) REFERENCES items(id));";

		String createItemsTable = " CREATE TABLE IF NOT EXISTS items ( "
				+ "recipe_id TEXT NOT NULL, "
				+ "slot INTEGER NOT NULL, "
				+ "name TEXT NOT NULL, "
				+ "item_nbt BLOB NOT NULL, "
				+ "type TEXT NOT NULL, "
				+ "PRIMARY KEY (recipe_id, slot), "
				+ "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE);";

		String createIngredientsTable = " CREATE TABLE IF NOT EXISTS ingredients ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ " recipe_id TEXT NOT NULL,"
				+ " item_id INTEGER NOT NULL,"
				+ " FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,"
				+ " FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE);";

		String createAllowedWorldsTable = " CREATE TABLE IF NOT EXISTS allowed_worlds ("
				+ "recipe_id TEXT NOT NULL, "
				+ "world TEXT NOT NULL, "
				+ "PRIMARY KEY (recipe_id, world), "
				+ "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE);";

		String furnaceData = " CREATE TABLE IF NOT EXISTS furnace_data ("
				+ "recipe_id TEXT NOT NULL, "
				+ "duration INTEGER NOT NULL, "
				+ "exp DECIMAL(10,5) NOT NULL, "
				+ "PRIMARY KEY (recipe_id), "
				+ "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE);";

		try (Statement stmt = conn.createStatement()) {
			stmt.addBatch(createItemsTable);
			stmt.addBatch(createRecipesTable);
			stmt.addBatch(createAllowedWorldsTable);
			stmt.addBatch(furnaceData);
			stmt.executeBatch();
		} catch (SQLException e) {
			Debug.error("Failed to create one of the tables", e);
		}
	}

	public void insertAllowedWorlds(Connection connection, String recipeId, Set<String> allowedWorlds) throws SQLException {
		if (allowedWorlds == null || allowedWorlds.isEmpty()) return;

		try (PreparedStatement updateStmt = connection.prepareStatement(UPDATE_WORLDS_SQL);
		     PreparedStatement insertStmt = connection.prepareStatement(INSERT_WORLDS_SQL)) {

			for (String world : allowedWorlds) {
				// First try to update
				updateStmt.setString(1, world);
				updateStmt.setString(2, recipeId);
				updateStmt.setString(3, world);
				int rowsAffected = updateStmt.executeUpdate();

				// If no rows were updated, insert instead
				if (rowsAffected == 0) {
					insertStmt.setString(1, recipeId);
					insertStmt.setString(2, world);
					insertStmt.addBatch();
				}
			}
			insertStmt.executeBatch();
		}
	}

	// Insert a recipe
	public void insertRecipe(@NonNull Connection conn, EnhancedRecipe recipe, RecipeIngredientType recipeIngredientType) {

		try (PreparedStatement pstmt = conn.prepareStatement(INSERT_RECIPES_SQL, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, recipe.getKey());
			pstmt.setString(2, recipe.getType().name());
			pstmt.setInt(3, recipe.getPage());
			pstmt.setInt(4, recipe.getSlot());
			pstmt.setInt(5, recipe.getResultSlot());
			pstmt.setString(6, recipe.getRecipeCategory());
			pstmt.setString(7, recipe.getPermission());
			pstmt.setString(8, String.valueOf(recipe.getMatchType()));
			pstmt.setBoolean(9, recipe.isHidden());
			pstmt.setBoolean(10, recipe.isCheckPartialMatch());
			pstmt.setString(11, recipe.getOnCraftCommand());
			pstmt.setString(12, recipeIngredientType.name());


			boolean isShapeless = (recipe instanceof WBRecipe) && ((WBRecipe) recipe).isShapeless();
			pstmt.setBoolean(13, isShapeless);
			try {
				updateSQL(pstmt);
			} catch (SQLException e) {
				Debug.error("Failed to insert recipe: " + recipe.getKey());
				Debug.error("Recipe data: " + recipe, e);
			}
		} catch (SQLException e) {
			Debug.error("Failed to insert data", e);
		}
	}

	private void updateFurnaces(final Connection conn, final EnhancedRecipe recipe) throws SQLException {
		if (!(recipe instanceof FurnaceRecipe)) return;

		FurnaceRecipe furnaceRecipe = ((FurnaceRecipe) recipe);
		try (PreparedStatement furnPreparedStatement = conn.prepareStatement(SELECT_FURNACE_DATA_SQL)) {
			furnPreparedStatement.setString(1, recipe.getKey());
			ResultSet set = furnPreparedStatement.executeQuery();
			if (set.next()) {
				try (PreparedStatement updateFurn = conn.prepareStatement(UPDATE_FURNACE_DATA_SQL)) {
					updateFurn.setInt(1, furnaceRecipe.getDuration());
					updateFurn.setFloat(2, furnaceRecipe.getExp());
					updateFurn.setString(3, recipe.getKey());
					this.updateSQL(updateFurn);
				}
			} else {
				try (PreparedStatement insertFurn = conn.prepareStatement(INSERT_FURNACE_DATA_SQL)) {
					insertFurn.setString(1, recipe.getKey());
					insertFurn.setInt(2, furnaceRecipe.getDuration());
					insertFurn.setFloat(3, furnaceRecipe.getExp());
					this.updateSQL(insertFurn);
				}
			}
		}
	}

	private void save() {
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			try {
				List<EnhancedRecipe> tempList = new ArrayList<>(self().getCacheRecipes().getRecipes());
				for (EnhancedRecipe recipe : tempList) {
					if(recipe.isRemove()) {
						this.deleteRecipe(connection, recipe);
						continue;
					}
					this.saveRecipe(connection, recipe);
				}
				connection.commit();
			} finally {
				try {
					connection.setAutoCommit(true);
				} catch (SQLException e) {
					Debug.error("Could not not reset back the auto commit.", e);
				}
			}
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}


	// Retrieve a recipe with its ingredients
	public EnhancedRecipe getRecipe(Connection conn, String recipeId) {
		EnhancedRecipe recipe;

		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_RECIPE_JOIN_SQL)) {

			pstmt.setString(1, recipeId);
			ResultSet rs = pstmt.executeQuery();
			if (!rs.next()) return null;

			recipe = getEnhancedRecipe(conn, recipeId, rs);
			return recipe;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<EnhancedRecipe> getAllRecipe(Connection conn) {
		List<EnhancedRecipe> enhancedRecipes = new ArrayList<>();
		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_RECIPE_JOIN_SQL)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				final EnhancedRecipe recipe = getEnhancedRecipe(conn, rs.getString("id"), rs);
				enhancedRecipes.add(recipe);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return enhancedRecipes;
	}

	private EnhancedRecipe getEnhancedRecipe(final Connection conn, final String recipeId, final ResultSet rs) throws SQLException {
		EnhancedRecipe recipe;
		Object resultNbt = rs.getBytes("result_nbt");
		ItemStack resultItem = null;
		if (resultNbt != null) {
			final ItemStack[] itemStacks = deserializeItemStack((byte[]) resultNbt);
			if (itemStacks != null && itemStacks.length > 0) resultItem = itemStacks[0];
		}

		Map<String, Object> map = new HashMap<>();
		map.put("page", rs.getInt("page"));
		map.put("slot", rs.getInt("slot"));
		map.put("result_slot", rs.getInt("result_slot"));
		map.put("category", rs.getString("category"));
		map.put("permission", rs.getString("permission"));
		map.put("matchtype", rs.getString("matchtype"));
		map.put("hidden", rs.getBoolean("hidden"));
		map.put("check_partial_match", rs.getBoolean("check_partial_match"));
		map.put("oncraftcommand", rs.getString("on_craft_command"));
		map.put("shapeless", rs.getBoolean("shapeless"));

		RecipeType type = RecipeType.getType(rs.getString("recipe_type"));
		if (type != null) {
			switch (type) {
				case FURNACE:
					map.put("duration", rs.getInt("duration"));
					map.put("exp", rs.getDouble("exp"));
					recipe = FurnaceRecipe.deserialize(map);
					break;
				case BLAST:
					map.put("duration", rs.getInt("duration"));
					map.put("exp", rs.getDouble("exp"));
					recipe = BlastRecipe.deserialize(map);
					break;
				case SMOKER:
					map.put("duration", rs.getInt("duration"));
					map.put("exp", rs.getDouble("exp"));
					recipe = SmokerRecipe.deserialize(map);
					break;
				default:
					recipe = WBRecipe.deserialize(map);
			}
		} else {
			recipe = WBRecipe.deserialize(map);
		}

		recipe.setResult(resultItem);
		recipe.setKey(recipeId);

		List<ItemStack> ingredients = getRecipeIngredients(conn, recipeId, type);
		recipe.setContent(ingredients.toArray(new ItemStack[0]));

		Set<String> allowedWorlds = getAllowedWorlds(conn, recipeId);
		recipe.setAllowedWorlds(allowedWorlds);
		return recipe;
	}

	private List<ItemStack> getRecipeIngredients(Connection conn, String recipeId, final RecipeType type) throws SQLException {
		int maxAmount = 9;
		if (type != RecipeType.WORKBENCH)
			maxAmount = 1;
		List<ItemStack> ingredients = new ArrayList<>(Collections.nCopies(maxAmount, null));

		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_INGREDIENTS_SQL)) {
			pstmt.setString(1, recipeId);
			pstmt.setString(2, RecipeIngredientType.INGREDIENT.name());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				byte[] nbtData = rs.getBytes("item_nbt");
				ItemStack[] items = deserializeItemStack(nbtData);
				int slot = rs.getInt("slot");
				if (slot >= 0 && slot < maxAmount && items != null && items.length > 0) {
					ingredients.set(slot, items[0]);
				}
			}
		}
		return ingredients;
	}

	private Set<String> getAllowedWorlds(Connection conn, String recipeId) throws SQLException {
		Set<String> worlds = new HashSet<>();

		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_WORLDS_SQL)) {
			pstmt.setString(1, recipeId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				worlds.add(rs.getString("world"));
			}
		}
		return worlds;
	}

	private void deleteRecipe(@NonNull Connection connection,@NonNull EnhancedRecipe enhancedRecipe) throws SQLException {
		String sql = "DELETE FROM recipes WHERE id = ?;";
		this.deleteAllIngredients(connection, enhancedRecipe);
		final String recipeName = enhancedRecipe.getKey();
		this.removeAllowedWorld(connection, recipeName, enhancedRecipe.getAllowedWorlds().toArray(new String[0]));
		this.removeFurnaceData(connection, enhancedRecipe);

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, recipeName);
			int rowsAffected = this.updateSQL(pstmt);
			if (rowsAffected > 0) {
				Send("Recipe '" + recipeName + "' deleted successfully.");
			} else {
				Send("Recipe '" + recipeName + "' not found.");
			}
		}
	}

	private void removeFurnaceData(final Connection connection, final EnhancedRecipe enhancedRecipe) throws SQLException {
		if (!(enhancedRecipe instanceof FurnaceRecipe)) return;

		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_FURNACE_DATA_SQL)) {
			pstmt.setString(1, enhancedRecipe.getKey());
			int rowsAffected = this.updateSQL(pstmt);
		}
	}


	private void removeAllowedWorld(Connection connection, String recipeId, String... worlds) throws SQLException {
		if (worlds == null || worlds.length == 0) return;
		boolean batchMode = worlds.length > 1;

		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_WORLD_SQL)) {
			for (String world : worlds) {
				pstmt.setString(1, recipeId);
				pstmt.setString(2, world);
				if (batchMode) {
					pstmt.addBatch();
				} else {
					int rowsAffected = pstmt.executeUpdate();
				}
			}
			if (batchMode) {
				int[] rowsAffected = pstmt.executeBatch();
			}
		}
	}

	private void deleteIngredient(Connection conn, String recipeId, Integer... slots) throws SQLException {
		if (slots == null || slots.length == 0) return;

		String sql = "DELETE FROM items WHERE recipe_id = ? AND slot = ?;";
		boolean batchMode = slots.length > 1;

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			for (int slot : slots) {
				pstmt.setString(1, recipeId);
				pstmt.setInt(2, slot);

				if (batchMode) {
					pstmt.addBatch(); // Add to batch
				} else {
					int rowsAffected = pstmt.executeUpdate(); // Execute immediately if not in batch mode
					logDeletionResult(recipeId, rowsAffected);
				}
			}
			if (batchMode) {
				int[] results = pstmt.executeBatch();
				for (int rowsAffected : results) {
					logDeletionResult(recipeId, rowsAffected);
				}
			}
		}
	}

	// Example usage

	private String getItemKey(final ItemStack item) {
		if (item == null) return null;
		String base = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
		base = base.replaceAll("\\.", "_");
		return base;
	}

	public void saveRecipe(Connection connection, EnhancedRecipe recipe) {

		try (PreparedStatement selectRecipeStmt = connection.prepareStatement(SELECT_RECIPE_SQL)) {

			final String recipeName = recipe.getKey();
			selectRecipeStmt.setString(1, recipeName);

			try (ResultSet recipeResultSet = selectRecipeStmt.executeQuery()) {
				if (recipeResultSet.next()) {
					updateRecipe(connection, recipe, recipeResultSet);
				} else {
					insertRecipe(connection, recipe, RecipeIngredientType.RESULT);
				}
				insertOrUpdateItem(connection, ingredients -> {
					ingredients.setSlot(recipe.getResultSlot());
					ingredients.setRecipeName(recipe.getKey());
					final ItemStack result = recipe.getResult();
					ingredients.setItemData(serializeItemStack(new ItemStack[]{result}));
					ingredients.setItemName(this.getItemKey(result));
					ingredients.setRecipeType(RecipeIngredientType.RESULT);
				});
			}
			this.updateFurnaces(connection, recipe);

			ItemStack[] itemStacks = recipe.getContent();
			for (int i = 0; i < itemStacks.length; i++) {
				ItemStack item = itemStacks[i];
				String itemName = getItemKey(item);
				final int slot = i;
				insertOrUpdateItem(connection, ingredients -> {
					ingredients.setSlot(slot);
					ingredients.setItemData(serializeItemStack(new ItemStack[]{item}));
					ingredients.setRecipeName(recipeName);
					ingredients.setItemName(itemName);
				});
			}
			this.insertAllowedWorlds(connection, recipeName, recipe.getAllowedWorlds());
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException rollbackEx) {
				Debug.error("Could not rollback changes to the database", rollbackEx);
			}
			Debug.error("Failed to save this recipe " + recipe.getKey() + ". It will now rollback all changes made", e);
		}
	}

	private void updateRecipe(final Connection connection, final EnhancedRecipe recipe, final ResultSet recipeResultSet) throws SQLException {

		String resultItemType = recipeResultSet.getString("result_item_type");

		try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_RECIPE_SQL)) {
			pstmt.setInt(1, recipe.getPage());
			pstmt.setString(2, recipe.getType().name());
			pstmt.setInt(3, recipe.getSlot());
			pstmt.setInt(4, recipe.getResultSlot());
			pstmt.setString(5, recipe.getRecipeCategory());
			pstmt.setString(6, recipe.getPermission());
			pstmt.setString(7, String.valueOf(recipe.getMatchType()));
			pstmt.setBoolean(8, recipe.isHidden());
			pstmt.setBoolean(9, recipe.isCheckPartialMatch());
			pstmt.setString(10, recipe.getOnCraftCommand());
			pstmt.setString(11, resultItemType);

			boolean isShapeless = (recipe instanceof WBRecipe) && ((WBRecipe) recipe).isShapeless();
			pstmt.setBoolean(12, isShapeless);

			pstmt.setString(13, recipeResultSet.getString("id"));
			updateSQL(pstmt);
		}
	}

	private void deleteAllIngredients(final Connection connection, final EnhancedRecipe enhancedRecipe) throws SQLException {
		int resultSlot = enhancedRecipe.getResultSlot();
		List<Integer> slots = new ArrayList<>();
		slots.add(resultSlot);
		slots.addAll(getAllIngredientSlots(enhancedRecipe));
		this.deleteIngredient(connection, enhancedRecipe.getKey(), slots.toArray(new Integer[0]));
	}

	public void insertOrUpdateItem(Connection conn, Consumer<IngredientWrapper> callback) {
		if (callback == null) return;

		IngredientWrapper ingredientWrapper = new IngredientWrapper();
		callback.accept(ingredientWrapper);
		final int slot = ingredientWrapper.getSlot();
		final String recipeName = ingredientWrapper.getRecipeName();
		final String itemName = ingredientWrapper.getItemName();
		byte[] nbtData = ingredientWrapper.getItemData();
		RecipeIngredientType recipeIngredientType = ingredientWrapper.getRecipeType();


		if (itemName == null || nbtData == null) return;

		try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_ITEM_EXISTENCE_SQL)) {
			checkStmt.setString(1, recipeName);
			checkStmt.setInt(2, slot);
			ResultSet rs = checkStmt.executeQuery();

			if (rs.next()) {
				try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_ITEM_SQL)) {
					updateStmt.setBytes(1, nbtData);
					updateStmt.setString(2, recipeName);
					updateStmt.setInt(3, slot);
					updateSQL(updateStmt);
					rs.getInt(1);
				}
			} else {
				try (PreparedStatement insertStmt = conn.prepareStatement(INSERT_ITEM_SQL, Statement.RETURN_GENERATED_KEYS)) {
					insertStmt.setString(1, recipeName);
					insertStmt.setInt(2, slot);
					insertStmt.setString(3, itemName);
					insertStmt.setBytes(4, nbtData);
					insertStmt.setString(5, recipeIngredientType.name());
					updateSQL(insertStmt);

					ResultSet insertRs = insertStmt.getGeneratedKeys();
					if (insertRs.next()) {
						insertRs.getInt(1);
					}
				}
			}
		} catch (SQLException e) {
			Debug.error("Failed to insert or update the itemstack", e);
		}
	}

	private void logDeletionResult(String recipeId, int rowsAffected) {
		if (rowsAffected > 0) {
			Send("Ingredient removed from recipe " + recipeId);
		} else {
			Send("Ingredient not found in recipe " + recipeId);
		}
	}

	private int getSlotIngredient(final EnhancedRecipe recipe, final ItemStack itemStack, int slot) {
		final ItemStack[] content = recipe.getContent();
		for (int i = 0; i < content.length; i++) {
			ItemStack item = content[i];
			if (itemStack.isSimilar(item)) {
				slot = i;
				break;
			}
		}
		return slot;
	}

	private List<Integer> getAllIngredientSlots(final EnhancedRecipe recipe) {
		List<Integer> slots = new ArrayList<>();
		final ItemStack[] content = recipe.getContent();
		for (int i = 0; i < content.length; i++) {
			ItemStack item = content[i];
			if (item != null) {
				slots.add(i);
			}
		}
		return slots;
	}

	@Nonnull
	public byte[] serializeItemStack(final ItemStack[] itemStacks) {
		return RegisterNbtAPI.serializeItemStack(itemStacks);
	}

	@Nullable
	public ItemStack[] deserializeItemStack(final byte[] itemStacks) {
		return RegisterNbtAPI.deserializeItemStack(itemStacks);
	}

}
