package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.gui.GuiManager;
import com.dutchjelly.craftenhance.gui.guis.editors.FurnaceRecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.editors.WBRecipeEditor;
import com.dutchjelly.craftenhance.gui.templates.GuiTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class EditorTypeSelector extends GUIElement {

	private Inventory inventory;


	public EditorTypeSelector(GuiManager manager, GuiTemplate template, GUIElement previousGui, Player player, String key, String permission) {
		super(manager, template, previousGui, player);
		this.addBtnListener(ButtonType.ChooseWorkbenchType, (click,btn, type) -> {
			WBRecipe newRecipe = new WBRecipe(permission, null, new ItemStack[9]);

			newRecipe.setKey(getFreshKey(key));

			WBRecipeEditor gui = new WBRecipeEditor(
					self().getGuiManager(),
					self().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class),
					this, getPlayer(), newRecipe
			);
			getManager().openGUI(getPlayer(), gui);
		});
		this.addBtnListener(ButtonType.ChooseFurnaceType, (click,btn, type) -> {
			FurnaceRecipe newRecipe = new FurnaceRecipe(permission, null, new ItemStack[1]);

			newRecipe.setKey(getFreshKey(key));

			FurnaceRecipeEditor gui = new FurnaceRecipeEditor(
					self().getGuiManager(),
					self().getGuiTemplatesFile().getTemplate(FurnaceRecipeEditor.class),
					this, getPlayer(), newRecipe
			);
			getManager().openGUI(getPlayer(), gui);
		});
		inventory = GuiUtil.CopyInventory(getTemplate().getTemplate(), getTemplate().getInvTitle(), this);
	}

	private String getFreshKey(String keySeed) {
		if (keySeed == null || !self().getFm().isUniqueRecipeKey(keySeed)) {
			int uniqueKeyIndex = 1;
			keySeed = "recipe";

			while (!self().getFm().isUniqueRecipeKey(keySeed + uniqueKeyIndex))
				uniqueKeyIndex++;
			keySeed += uniqueKeyIndex;
		}
		return keySeed;
	}

	@Override
	public void handleEventRest(InventoryClickEvent e) {
	}

	@Override
	public boolean isCancelResponsible() {
		return false;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}