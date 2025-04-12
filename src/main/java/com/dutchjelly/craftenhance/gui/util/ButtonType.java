package com.dutchjelly.craftenhance.gui.util;

public enum ButtonType {
    NxtPage(""),
    PrvPage(""),
    Back(""),
    SaveRecipe(""),
    DeleteRecipe(""),
    SwitchShaped(""),
    SwitchMatchMeta(""),
    ResetRecipe(""),
    SetPosition(""),
    SwitchHidden(""),
    SetPermission(""),
    SwitchDisablerMode(""),
    SetCookTime(""),
    SetBrewingTime(""),
    SetExp(""),
    ChooseWorkbenchType("WBRecipeEditor"),
    ChooseFurnaceType("FurnaceRecipeEditor"),
    ChooseBlastType("FurnaceRecipeEditor"),
    ChooseSmokerType("FurnaceRecipeEditor"),
    ChooseBrewingType("BrewingRecipeEditor"),
    Search(""),
    NewCategory(""),
    ChangeCategoryName(""),
    ChangeCategoryList(""),
    ChangeCategory(""),
    ChangeCategoryItem(""),
    RemoveCategory(""),
    FillItems(""),
    AllowedWorldsCraft(""),
    RecipeSettings(""),
	SetPartialMatch(""),
    edit_recipe(""),
    Save(""),
    ;

    private final String type;

    ButtonType(final String type) {

        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ButtonType valueOfType(final String buttontype) {
        final ButtonType[] buttonTypes = ButtonType.values();
        for (final ButtonType buttonType : buttonTypes) {
            if (buttonType.name().equalsIgnoreCase(buttontype))
                return buttonType;
        }
        return null;
    }
    }
