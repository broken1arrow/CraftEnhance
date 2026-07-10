package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.CraftEnhance;

public enum PermissionTypes {
	Edit("perms.recipe-editor"),
	View("perms.recipe-viewer"),
	View_ALL("perms.view-all"),
	EditItem("perms.edit-item"),
	Category_editor("perms.category-editor"),
	;

	private final String permPath;

	PermissionTypes(final String permPath) {
		this.permPath = permPath;
	}

	public String getPerm() {
		String permission = CraftEnhance.self().getConfig().getString(permPath);
		if (this == Category_editor && permission == null) {
			permission = CraftEnhance.self().getConfig().getString("perms.categorys-editor");
		}
		if (permission == null)
			permission = "craftenhance." + this.name().toLowerCase();
		return permission;
	}
}
