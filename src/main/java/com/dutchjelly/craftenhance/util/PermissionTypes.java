package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.CraftEnhance;

public enum PermissionTypes {
    Edit("perms.recipe-editor"),
    View("perms.recipe-viewer"),
    View_ALL("perms.view-all"),
    EditItem("perms.edit-item"),
    Categorys_editor("perms.categorys-editor"),;

    public final String permPath;

    PermissionTypes(final String permPath){
        this.permPath = permPath;
    }

    public String getPerm() {
        String permission = CraftEnhance.self().getConfig().getString(permPath);
        if (permission == null)
            permission = "craftenhance.permission.not.set";
        return permission;
    }
}
