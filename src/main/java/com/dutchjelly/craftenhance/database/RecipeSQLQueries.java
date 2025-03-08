package com.dutchjelly.craftenhance.database;

public interface RecipeSQLQueries {

	String SELECT_RECIPE_SQL = "SELECT id, recipe_type, page, slot, result_slot, category, permission, matchtype, hidden, check_partial_match, on_craft_command, result_item_type, shapeless FROM recipes WHERE id = ?";
	String CHECK_ITEM_EXISTENCE_SQL = "SELECT slot FROM items WHERE recipe_id = ? AND slot = ?;";
	String SELECT_ITEM_FROM_RECIPE_SLOT_SQL = "SELECT recipe_id, slot FROM items WHERE recipe_id = ? AND slot = ?;";
	String SELECT_FURNACE_DATA_SQL = "SELECT recipe_id, duration, exp FROM furnace_data WHERE recipe_id = ?;";
	String SELECT_RECIPE_JOIN_SQL = "SELECT r.*, i.item_nbt AS result_nbt, furn.duration, furn.exp " +
			"FROM recipes r " +
			"JOIN items i ON r.result_item_type = i.type AND r.id = i.recipe_id " +
			"LEFT JOIN furnace_data furn ON r.id = furn.recipe_id" +
			"WHERE r.id = ?;";

	String SELECT_ALL_RECIPE_JOIN_SQL = "SELECT r.*, i.item_nbt AS result_nbt, furn.duration, furn.exp " +
			"FROM recipes r " +
			"JOIN items i ON r.result_item_type = i.type AND r.id = i.recipe_id " +
			"LEFT JOIN furnace_data furn ON r.id = furn.recipe_id" +
			";";
	String SELECT_INGREDIENTS_SQL = "SELECT i.item_nbt, i.slot " +
			"FROM items i " +
			"WHERE i.recipe_id = ? " +
			"AND i.slot BETWEEN 0 AND 8 " +
			"AND i.type = ? " +
			"ORDER BY i.slot;";
	String SELECT_WORLDS_SQL = "SELECT world FROM allowed_worlds WHERE recipe_id = ?;";

	String UPDATE_FURNACE_DATA_SQL = "UPDATE furnace_data SET duration = ?, exp = ?  WHERE recipe_id = ?;";
	String UPDATE_ITEM_SQL = "UPDATE items SET item_nbt = ? WHERE recipe_id = ? and slot = ?;";

	String UPDATE_RECIPE_SQL = "UPDATE recipes " +
			"SET page = ?, " +
			"    recipe_type = ?, " +
			"    slot = ?, " +
			"    result_slot = ?, " +
			"    category = ?, " +
			"    permission = ?, " +
			"    matchtype = ?, " +
			"    hidden = ?, " +
			"    check_partial_match = ?, " +
			"    on_craft_command = ?, " +
			"    result_item_type = ?, " +
			"    shapeless = ? " +
			"WHERE id = ?;";
	String UPDATE_WORLDS_SQL = "UPDATE allowed_worlds SET world = ? WHERE recipe_id = ? AND world = ?";

	String INSERT_RECIPES_SQL = "INSERT INTO recipes " +
			"(id, recipe_type, page, slot, result_slot, category, permission, matchtype, hidden, check_partial_match, on_craft_command, result_item_type, shapeless) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	String INSERT_OR_REPLACE_WORLDS_SQL = "INSERT INTO allowed_worlds (recipe_id, world) VALUES (?, ?) " +
			"ON CONFLICT(recipe_id, world) DO UPDATE SET world = excluded.world;";

	String INSERT_WORLDS_SQL = "INSERT INTO allowed_worlds (recipe_id, world) VALUES (?, ?);";
	String INSERT_ITEM_SQL = "INSERT INTO items (recipe_id, slot, name, item_nbt, type) VALUES (?, ?, ?, ?, ?);";
	String INSERT_FURNACE_DATA_SQL = "INSERT INTO furnace_data (recipe_id, duration, exp) VALUES (?, ?, ?);";

	String DELETE_FURNACE_DATA_SQL = "DELETE FROM furnace_data WHERE recipe_id = ?;";

	String DELETE_WORLD_SQL = "DELETE FROM allowed_worlds WHERE recipe_id = ? AND world = ?;";
}