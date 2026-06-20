package com.dutchjelly.craftenhance.crafthandling.livedata.event;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public interface PrepareRecipeContext {

	ItemStack[] getRecipeMatrix();

	void setRecipeMatrix(ItemStack[] recipeMatrix);

	void acceptResult(ItemStack itemStack);

	void setResult(Consumer<ItemStack> result);

	void setViewers(List<HumanEntity> viewers);

	List<HumanEntity> getViewers();
}
