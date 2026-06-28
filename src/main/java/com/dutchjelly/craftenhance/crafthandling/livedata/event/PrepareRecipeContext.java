package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface PrepareRecipeContext {

	ItemStack[] getRecipeMatrix();

	void setRecipeMatrix(ItemStack[] recipeMatrix);

	void setViewers(List<HumanEntity> viewers);

	List<HumanEntity> getViewers();
}
