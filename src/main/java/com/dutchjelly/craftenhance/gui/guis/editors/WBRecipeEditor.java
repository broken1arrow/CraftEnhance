package com.dutchjelly.craftenhance.gui.guis.editors;

import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.gui.GuiManager;
import com.dutchjelly.craftenhance.gui.guis.GUIElement;
import com.dutchjelly.craftenhance.gui.templates.GuiTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.InfoItemPlaceHolders;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WBRecipeEditor extends RecipeEditor<WBRecipe> {
	

	private boolean shapeless;

	public WBRecipeEditor(GuiManager manager, GuiTemplate template, GUIElement previous, Player p, WBRecipe recipe){
	    super(manager,template,previous,p,recipe);
    }

    public WBRecipeEditor(GuiManager manager, GUIElement previous, Player p, WBRecipe recipe){
        super(manager,previous,p,recipe);
    }

    @Override
    protected void onRecipeDisplayUpdate() {
        shapeless = getRecipe().isShapeless();
    }

    @Override
    public Map<String, String> getPlaceHolders() {
        return new HashMap<String,String>(){{
            put(InfoItemPlaceHolders.Shaped.getPlaceHolder(), shapeless ? "shapeless" : "shaped");
        }};
    }

    @Override
    protected void initBtnListeners() {
        addBtnListener(ButtonType.SwitchShaped, (clickType, btn, btnType) -> {
            shapeless = !shapeless;
            updatePlaceHolders();
        });
    }

    @Override
    protected void beforeSave() {
        getRecipe().setShapeless(shapeless);
    }


}
