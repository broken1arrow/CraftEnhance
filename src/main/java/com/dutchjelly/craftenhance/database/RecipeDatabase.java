package com.dutchjelly.craftenhance.database;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.messaging.Debug;
import lombok.NonNull;
import org.broken.arrow.nbt.library.RegisterNbtAPI;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

public class RecipeDatabase implements RecipeSQLQueries {
	private static final String URL = "jdbc:sqlite:" + self().getDataFolder() + "/recipes.db";

	public void saveRecipes() {

		save();
	}

	private void save() {
		try (Connection connection = connect()) {
			createTables(connection);
			connection.setAutoCommit(false);
			try {
				for (EnhancedRecipe recipe : self().getFm().getRecipes()) {
					saveRecipe(connection, recipe);

					getRecipe(connection, recipe.getKey());
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

	public void saveRecipe(@NonNull final EnhancedRecipe recipe) {
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			try {
				saveRecipe(connection, recipe);
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

	@Nullable
	public EnhancedRecipe loadRecipe(@NonNull final String recipeId) {
		try (Connection connection = connect()) {
			return this.getRecipe(connection, recipeId);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
		return null;
	}

	public void deleteRecipe(@NonNull final String recipeId) {
		try (Connection connection = connect()) {
			deleteRecipe(connection, recipeId);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	private void removeAllowedWorld(@NonNull final String recipeId, @NonNull final String world) {
		try (Connection connection = connect()) {
			removeAllowedWorld(connection, recipeId, world);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}

	public void deleteIngredient(@NonNull final String recipeId, @NonNull final ItemStack itemStack) {
		EnhancedRecipe recipe = self().getFm().getRecipes().stream().filter(enhancedRecipe -> enhancedRecipe.getKey().equals(recipeId)).findFirst().orElse(null);
		if (recipe == null) return;

		boolean isResult = recipe.getResult().isSimilar(itemStack);
		int slot = recipe.getResultSlot();
		if (!isResult) {
			slot = getSlotIngredient(itemStack, recipe, slot);
		}
		if (slot < 0) return;

		try (Connection connection = connect()) {
			deleteIngredient(connection, recipeId, slot);
		} catch (SQLException exception) {
			Debug.error("Failed to connect to database", exception);
		}
	}


	private Connection connect() throws SQLException {
		return DriverManager.getConnection(URL);
	}

	private static void updateSQL(final PreparedStatement updateItemStmt) throws SQLException {
		updateItemStmt.executeUpdate();
	}

	// Create tables
	public void createTables(Connection conn) {
		String createRecipesTable = " CREATE TABLE IF NOT EXISTS recipes ( "
				+ "id TEXT PRIMARY KEY, "
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

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(createItemsTable);
			stmt.execute(createRecipesTable);
			stmt.execute(createAllowedWorldsTable);
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
	public void insertRecipe(@NonNull Connection conn, EnhancedRecipe recipe, RecipeType recipeType) {

		try (PreparedStatement pstmt = conn.prepareStatement(INSERT_RECIPES_SQL, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, recipe.getKey());  // Explicitly set the recipe ID
			pstmt.setInt(2, recipe.getPage());
			pstmt.setInt(3, recipe.getSlot());
			pstmt.setInt(4, recipe.getResultSlot());
			pstmt.setString(5, recipe.getRecipeCategory());
			pstmt.setString(6, recipe.getPermission());
			pstmt.setString(7, String.valueOf(recipe.getMatchType()));
			pstmt.setBoolean(8, recipe.isHidden());
			pstmt.setBoolean(9, recipe.isCheckPartialMatch());
			pstmt.setString(10, recipe.getOnCraftCommand());
			pstmt.setString(11, recipeType.name());


			boolean isShapeless = (recipe instanceof WBRecipe) && ((WBRecipe) recipe).isShapeless();
			pstmt.setBoolean(12, isShapeless);
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


	// Retrieve a recipe with its ingredients
	public EnhancedRecipe getRecipe(Connection conn, String recipeId) {

		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_RECIPE_JOIN_SQL)) {

			pstmt.setString(1, recipeId);
			ResultSet rs = pstmt.executeQuery();
			if (!rs.next()) return null;

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
			map.put("oncraftcommand", rs.getBoolean("on_craft_command"));
			map.put("shapeless", rs.getBoolean("shapeless"));

			EnhancedRecipe recipe = WBRecipe.deserialize(map);
			recipe.setResult(resultItem);
			recipe.setKey(recipeId);

			List<ItemStack> ingredients = getRecipeIngredients(conn, recipeId);
			recipe.setContent(ingredients.toArray(new ItemStack[0]));

			Set<String> allowedWorlds = getAllowedWorlds(conn, recipeId);
			recipe.setAllowedWorlds(allowedWorlds);
			System.out.println("recipe: " + recipe);
			return recipe;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private List<ItemStack> getRecipeIngredients(Connection conn, String recipeId) throws SQLException {
		List<ItemStack> ingredients = new ArrayList<>(Collections.nCopies(9, null));

		try (PreparedStatement pstmt = conn.prepareStatement(SELECT_INGREDIENTS_SQL)) {
			pstmt.setString(1, recipeId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				byte[] nbtData = rs.getBytes("item_nbt");
				ItemStack[] items = deserializeItemStack(nbtData);
				int slot = rs.getInt("slot");

				if (slot >= 0 && slot < 9 && items != null && items.length > 0) {
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

	public void deleteRecipe(Connection conn, String recipeId) throws SQLException {
		String sql = "DELETE FROM recipes WHERE id = ?;";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, recipeId);
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("Recipe " + recipeId + " deleted successfully.");
			} else {
				System.out.println("Recipe " + recipeId + " not found.");
			}
		}
	}

	private void removeAllowedWorld(Connection conn, String recipeId, String world) throws SQLException {
		try (PreparedStatement pstmt = conn.prepareStatement(DELETE_WORLD_SQL)) {
			pstmt.setString(1, recipeId);
			pstmt.setString(2, world);
			pstmt.executeUpdate();
		}
	}

	public void deleteIngredient(Connection conn, String recipeId, int slot) throws SQLException {
		String sql = "DELETE FROM items WHERE recipe_id = ? AND slot = ?);";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, recipeId);
			pstmt.setInt(2, slot);
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("Ingredient removed from recipe " + recipeId);
			} else {
				System.out.println("Ingredient not found in recipe " + recipeId);
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
					insertRecipe(connection, recipe, RecipeType.RESULT);
				}
				insertOrUpdateItem(connection, ingredients -> {
					ingredients.setSlot( recipe.getResultSlot());
					ingredients.setRecipeName(recipe.getKey());
					final ItemStack result = recipe.getResult();
					ingredients.setItemData(serializeItemStack(new ItemStack[]{result}));
					ingredients.setItemName(this.getItemKey(result));
					ingredients.setRecipeType(RecipeType.RESULT);
				});
			}
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
			pstmt.setInt(2, recipe.getSlot());
			pstmt.setInt(3, recipe.getResultSlot());
			pstmt.setString(4, recipe.getRecipeCategory());
			pstmt.setString(5, recipe.getPermission());
			pstmt.setString(6, String.valueOf(recipe.getMatchType()));
			pstmt.setBoolean(7, recipe.isHidden());
			pstmt.setBoolean(8, recipe.isCheckPartialMatch());
			pstmt.setString(9, recipe.getOnCraftCommand());
			pstmt.setString(10, resultItemType);

			boolean isShapeless = (recipe instanceof WBRecipe) && ((WBRecipe) recipe).isShapeless();
			pstmt.setBoolean(11, isShapeless);

			pstmt.setString(12, recipeResultSet.getString("id"));
			updateSQL(pstmt);
		}
	}

	public void insertOrUpdateItem(Connection conn, Consumer<IngredientWrapper> callback) {
		if (callback == null) return;

		IngredientWrapper ingredientWrapper = new IngredientWrapper();
		callback.accept(ingredientWrapper);
		final int slot = ingredientWrapper.getSlot();
		final String recipeName = ingredientWrapper.getRecipeName();
		final String itemName = ingredientWrapper.getItemName();
		byte[] nbtData = ingredientWrapper.getItemData();
		RecipeType recipeType = ingredientWrapper.getRecipeType();


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
					insertStmt.setString(5, recipeType.name());
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

	private int getSlotIngredient(final ItemStack itemStack, final EnhancedRecipe recipe, int slot) {
		final ItemStack[] content = recipe.getContent();
		for (int i = 0; i < content.length; i++) {
			ItemStack item = content[0];
			if (itemStack.isSimilar(item)) {
				slot = i;
				break;
			}
		}
		return slot;
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
