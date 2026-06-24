package com.dutchjelly.craftenhance.database.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLiteMigrationUtil {

	public static void addColumnsIfMissing(Connection conn, String tableName, List<ColumnDefinition> columns) throws SQLException {
		final Set<String> existingColumns = getExistingColumns(conn, tableName);
		System.out.println("Added column existingColumns " + existingColumns);
		try (Statement stmt = conn.createStatement()) {
			for (ColumnDefinition col : columns) {
				if (!existingColumns.contains(col.getName().toLowerCase())) {

					String sql = "ALTER TABLE " + tableName +
							" ADD COLUMN " + col.getName() +
							" " + col.getDefinition();

					stmt.executeUpdate(sql);
					System.out.println("Added column: " + col.getName());
				}
			}
		}
	}

	private static Set<String> getExistingColumns(final Connection conn, final String tableName) throws SQLException {
		final Set<String> columns = new HashSet<>();
		final String sql = "PRAGMA table_info(" + tableName + ")";
		try (PreparedStatement stmt = conn.prepareStatement(sql);
		     ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				columns.add(rs.getString("name").toLowerCase());
			}
		}
		return columns;
	}


}